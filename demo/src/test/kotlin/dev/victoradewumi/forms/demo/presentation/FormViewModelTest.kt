package dev.victoradewumi.forms.demo.presentation

import dev.victoradewumi.forms.core.*
import dev.victoradewumi.forms.demo.domain.DraftRepository
import dev.victoradewumi.forms.demo.domain.FormRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Verifies behavior at the repository boundary, including pending and failed disk writes. */
@OptIn(ExperimentalCoroutinesApi::class)
class FormViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val definition = FormDefinition("site", "Site", 2, listOf(FieldDefinition("name", "Name", FieldType.TEXT, required = true)))
    private val forms = object : FormRepository { override suspend fun load() = definition }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `saved input restores and validates after a new session`() = runTest(dispatcher) {
        val drafts = MemoryDrafts()
        val first = FormViewModel(forms, drafts, FormCodec())
        advanceUntilIdle()
        first.change("name", "Lekki")
        first.saveDraft()
        advanceUntilIdle()
        val restored = FormViewModel(forms, drafts, FormCodec())
        advanceUntilIdle()
        assertEquals("Lekki", restored.state.value.form?.answers?.get("name"))
        assertFalse(restored.state.value.dirty)
    }

    @Test fun `pending writes disable edits and do not announce success early`() = runTest(dispatcher) {
        val drafts = MemoryDrafts().apply { gate = CompletableDeferred() }
        val vm = FormViewModel(forms, drafts, FormCodec())
        advanceUntilIdle()
        vm.change("name", "First")
        vm.saveDraft()
        runCurrent()
        assertTrue(vm.state.value.busy)
        assertTrue(vm.state.value.dirty)
        assertNull(vm.state.value.savedDraft)
        vm.change("name", "Second")
        assertEquals("First", vm.state.value.form?.answers?.get("name"))
        drafts.gate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.dirty)
        assertNotNull(vm.state.value.savedDraft)
    }

    @Test fun `write failure retains input and allows retry`() = runTest(dispatcher) {
        val drafts = MemoryDrafts().apply { failSave = true }
        val vm = FormViewModel(forms, drafts, FormCodec())
        advanceUntilIdle()
        vm.change("name", "Unsaved")
        vm.saveDraft()
        advanceUntilIdle()
        assertTrue(vm.state.value.dirty)
        assertFalse(vm.state.value.busy)
        assertEquals("Unsaved", vm.state.value.form?.answers?.get("name"))
        assertNull(vm.state.value.savedDraft)
        drafts.failSave = false
        vm.saveDraft()
        advanceUntilIdle()
        assertFalse(vm.state.value.dirty)
        assertNotNull(vm.state.value.savedDraft)
    }

    @Test fun `successful save shows a dated summary and continuing keeps the answers`() = runTest(dispatcher) {
        val siteDefinition = definition.copy(fields = listOf(FieldDefinition("site_name", "Site name", FieldType.TEXT)))
        val siteForms = object : FormRepository { override suspend fun load() = siteDefinition }
        val savedAt = Instant.parse("2026-10-12T23:00:00Z")
        val vm = FormViewModel(siteForms, MemoryDrafts(), FormCodec(), Clock.fixed(savedAt, ZoneOffset.UTC))
        advanceUntilIdle()
        vm.change("site_name", "Lekki Office Renovation")
        vm.saveDraft()
        advanceUntilIdle()

        assertEquals(SavedDraftSummary("Lekki Office Renovation", savedAt), vm.state.value.savedDraft)
        vm.continueEditing()
        assertNull(vm.state.value.savedDraft)
        assertEquals("Lekki Office Renovation", vm.state.value.form?.answers?.get("site_name"))
        assertFalse(vm.state.value.dirty)
        vm.change("site_name", "Updated site")
        assertTrue(vm.state.value.dirty)
    }

    @Test fun `an incomplete draft can still be saved with a fallback summary title`() = runTest(dispatcher) {
        val vm = FormViewModel(forms, MemoryDrafts(), FormCodec())
        advanceUntilIdle()
        vm.saveDraft()
        advanceUntilIdle()
        assertEquals("Untitled inspection", vm.state.value.savedDraft?.siteName)
        assertFalse(vm.state.value.form!!.isValid)
    }

    @Test fun `clear removes disk data and resets validation presentation`() = runTest(dispatcher) {
        val drafts = MemoryDrafts()
        val vm = FormViewModel(forms, drafts, FormCodec())
        advanceUntilIdle()
        vm.change("name", "Saved")
        vm.saveDraft()
        advanceUntilIdle()
        vm.previewSubmission()
        vm.clearDraft()
        advanceUntilIdle()
        assertNull(drafts.load("site", 2))
        assertTrue(vm.state.value.form!!.answers.isEmpty())
        assertFalse(vm.state.value.form!!.showErrors)
        assertNull(vm.state.value.savedDraft)
    }

    @Test fun `review rejects missing input and shows payload only after validation`() = runTest(dispatcher) {
        val vm = FormViewModel(forms, MemoryDrafts(), FormCodec())
        advanceUntilIdle()
        vm.previewSubmission()
        assertNull(vm.state.value.payload)
        assertTrue(vm.state.value.form!!.showErrors)
        vm.change("name", "Validated")
        vm.previewSubmission()
        assertTrue(vm.state.value.payload!!.contains("Validated"))
    }

    /** Controllable storage fake; tests observe externally meaningful state transitions. */
    private class MemoryDrafts : DraftRepository {
        val rows = mutableMapOf<Pair<String, Int>, Map<String, String>>()
        var failSave = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun load(formId: String, revision: Int) = rows[formId to revision]
        override suspend fun save(formId: String, revision: Int, answers: Map<String, String>) {
            gate?.await()
            check(!failSave) { "Disk full" }
            rows[formId to revision] = answers.toMap()
        }
        override suspend fun delete(formId: String, revision: Int) { rows.remove(formId to revision) }
    }
}

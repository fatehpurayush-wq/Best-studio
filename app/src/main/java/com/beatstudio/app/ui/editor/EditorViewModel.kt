package com.beatstudio.app.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beatstudio.app.audio.AudioEngine
import com.beatstudio.app.audio.MicRecorder
import com.beatstudio.app.audio.effects.SlowedReverb
import com.beatstudio.app.audio.instruments.Presets
import com.beatstudio.app.audio.instruments.SampleStore
import com.beatstudio.app.data.db.AppDatabase
import com.beatstudio.app.data.db.ProjectRepository
import com.beatstudio.app.data.settings.SettingsStore
import com.beatstudio.app.domain.*
import com.beatstudio.app.export.ExportFormat
import com.beatstudio.app.export.ExportService
import com.beatstudio.app.export.ExportedResultFile
import com.beatstudio.app.music.*
import com.beatstudio.app.util.FileUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.math.roundToInt

enum class EditorTab(val labelHinglish: String) {
    BEAT("BEAT"),
    MELODY("MELODY"),
    MIX("MIX")
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(AppDatabase.getInstance(application).projectDao())
    private val settingsStore = SettingsStore(application)
    val audioEngine = AudioEngine()
    val micRecorder = MicRecorder()

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _selectedTrackId = MutableStateFlow<String>("track_drums")
    val selectedTrackId: StateFlow<String> = _selectedTrackId.asStateFlow()

    private val _activeTab = MutableStateFlow(EditorTab.BEAT)
    val activeTab: StateFlow<EditorTab> = _activeTab.asStateFlow()

    private val _isProMode = MutableStateFlow(false)
    val isProMode: StateFlow<Boolean> = _isProMode.asStateFlow()

    private val _scaleLockEnabled = MutableStateFlow(true)
    val scaleLockEnabled: StateFlow<Boolean> = _scaleLockEnabled.asStateFlow()

    private val _isRecordingMidi = MutableStateFlow(false)
    val isRecordingMidi: StateFlow<Boolean> = _isRecordingMidi.asStateFlow()

    private val _isRecordingMic = MutableStateFlow(false)
    val isRecordingMic: StateFlow<Boolean> = _isRecordingMic.asStateFlow()

    private val _countInText = MutableStateFlow<String?>(null)
    val countInText: StateFlow<String?> = _countInText.asStateFlow()

    private val _showTour = MutableStateFlow(false)
    val showTour: StateFlow<Boolean> = _showTour.asStateFlow()

    private val _tourStep = MutableStateFlow(0)
    val tourStep: StateFlow<Int> = _tourStep.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _clipboardPattern = MutableStateFlow<Pattern?>(null)
    val hasClipboardPattern: StateFlow<Boolean> = _clipboardPattern.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2000), false)

    // Undo / Redo stacks
    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var saveJob: Job? = null
    private var tapTempoHistory = ArrayDeque<Long>()

    val isPlaying: StateFlow<Boolean> = audioEngine.isPlaying
    val playheadStep: StateFlow<Int> = audioEngine.playheadStep
    val playheadBar: StateFlow<Int> = audioEngine.playheadBar
    val masterLevel: StateFlow<Pair<Float, Float>> = audioEngine.masterLevel
    val trackLevels: StateFlow<Map<String, Float>> = audioEngine.trackLevels
    val exportState = ExportService.exportState

    init {
        viewModelScope.launch {
            val tourDone = settingsStore.hasCompletedTour.first()
            if (!tourDone) _showTour.value = true
            _isProMode.value = settingsStore.isProMode.first()
        }
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val loaded = repository.getProject(projectId)
                ?: GenreTemplates.createNewProject(projectId, "Naya Beat", GenreId.LOFI)
            _project.value = loaded
            _selectedTrackId.value = loaded.tracks.firstOrNull()?.id ?: ""
            undoStack.clear()
            redoStack.clear()
            _canUndo.value = false
            _canRedo.value = false
            // Preload any samples/vocals into SampleStore cache
            withContext(Dispatchers.IO) {
                loaded.tracks.forEach { t ->
                    SampleStore.getCachedOrLoad(t.samplerParams.sampleFilePath)
                    SampleStore.getCachedOrLoad(t.vocalFilePath)
                }
            }
            audioEngine.updateProject(loaded, _isProMode.value)
            audioEngine.startEngineIfNeeded()
        }
    }

    private fun commitProjectChange(recordUndo: Boolean = true, transform: (Project) -> Project) {
        val current = _project.value ?: return
        val next = transform(current).copy(updatedAt = System.currentTimeMillis())
        if (next == current) return

        if (recordUndo) {
            undoStack.addLast(current)
            if (undoStack.size > 40) undoStack.removeFirst()
            redoStack.clear()
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = false
        }

        _project.value = next
        audioEngine.updateProject(next, _isProMode.value)
        scheduleAutoSave(next)
    }

    private fun scheduleAutoSave(project: Project) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350)
            repository.saveProject(project)
        }
    }

    fun undo() {
        val current = _project.value ?: return
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(current)
        _project.value = prev
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        audioEngine.updateProject(prev, _isProMode.value)
        scheduleAutoSave(prev)
        showMessage("Undo ho gaya")
    }

    fun redo() {
        val current = _project.value ?: return
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(current)
        _project.value = next
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        audioEngine.updateProject(next, _isProMode.value)
        scheduleAutoSave(next)
        showMessage("Redo ho gaya")
    }

    fun selectTab(tab: EditorTab) {
        _activeTab.value = tab
        val proj = _project.value ?: return
        val curTrack = proj.tracks.firstOrNull { it.id == _selectedTrackId.value }
        if (tab == EditorTab.BEAT && curTrack?.type != TrackType.DRUMS) {
            proj.tracks.firstOrNull { it.type == TrackType.DRUMS }?.let { _selectedTrackId.value = it.id }
        } else if (tab == EditorTab.MELODY && curTrack?.type == TrackType.DRUMS) {
            proj.tracks.firstOrNull { it.type != TrackType.DRUMS }?.let { _selectedTrackId.value = it.id }
        }
    }

    fun selectTrack(trackId: String) {
        _selectedTrackId.value = trackId
    }

    fun toggleProMode() {
        val next = !_isProMode.value
        _isProMode.value = next
        _project.value?.let { audioEngine.updateProject(it, next) }
        viewModelScope.launch { settingsStore.setProMode(next) }
        showMessage(if (next) "PRO mode chalu!" else "SIMPLE mode chalu!")
    }

    fun toggleScaleLock() {
        _scaleLockEnabled.value = !_scaleLockEnabled.value
    }

    fun toggleArrangeMode(enabled: Boolean) {
        commitProjectChange { it.copy(isArrangeMode = enabled) }
    }

    fun setProjectName(newName: String) {
        val clean = newName.trim()
        if (clean.isNotEmpty()) {
            commitProjectChange { it.copy(name = clean) }
        }
    }

    fun setBpm(bpm: Int) {
        commitProjectChange { it.copy(bpm = bpm.coerceIn(40, 220)) }
    }

    fun registerTapTempo() {
        val now = System.currentTimeMillis()
        if (tapTempoHistory.isNotEmpty() && now - tapTempoHistory.last() > 2200L) {
            tapTempoHistory.clear()
        }
        tapTempoHistory.addLast(now)
        if (tapTempoHistory.size > 5) tapTempoHistory.removeFirst()
        if (tapTempoHistory.size >= 2) {
            val diffs = tapTempoHistory.zipWithNext { a, b -> (b - a).toDouble() }
            val avgMs = diffs.average()
            if (avgMs > 100.0) {
                val bpm = (60_000.0 / avgMs).roundToInt().coerceIn(40, 220)
                setBpm(bpm)
            }
        }
    }

    fun setSwing(swing: Float) {
        commitProjectChange { it.copy(swing = swing.coerceIn(0f, 0.6f)) }
    }

    fun setKeyAndScale(rootNote: Int, scale: MusicalScale) {
        commitProjectChange { it.copy(rootNote = ((rootNote % 12) + 12) % 12, scale = scale) }
    }

    fun setTimeSignature(top: Int) {
        commitProjectChange { it.copy(timeSignatureTop = top.coerceIn(3, 4)) }
    }

    fun toggleMetronome() {
        commitProjectChange(recordUndo = false) { it.copy(metronomeEnabled = !it.metronomeEnabled) }
    }

    fun toggleLoop() {
        commitProjectChange(recordUndo = false) { it.copy(loopEnabled = !it.loopEnabled) }
    }

    fun setLoopRange(startBar: Int, endBar: Int) {
        val s = startBar.coerceIn(0, 30)
        val e = endBar.coerceIn(s + 1, 32)
        commitProjectChange { it.copy(loopStartBar = s, loopEndBar = e) }
    }

    fun toggleSlowedReverb() {
        commitProjectChange { proj ->
            val next = !proj.slowedReverbEnabled
            proj.copy(
                slowedReverbEnabled = next,
                masterEffects = SlowedReverb.applySlowedReverbToMaster(proj.masterEffects, next)
            )
        }
    }

    fun togglePhonkDrive() {
        commitProjectChange { proj ->
            val next = !proj.phonkDriveEnabled
            proj.copy(
                phonkDriveEnabled = next,
                masterEffects = SlowedReverb.applyPhonkDrivePreset(proj.masterEffects, next)
            )
        }
    }

    fun applyGenrePreset(genre: GenreId) {
        val current = _project.value ?: return
        val fresh = GenreTemplates.createNewProject(current.id, current.name, genre, autoGenerate = true)
        commitProjectChange { fresh }
        showMessage("${genre.labelHinglish} preset lag gaya!")
    }

    fun generateAutoBeat(variation: Float, shuffle: Boolean) {
        val seed = if (shuffle) (System.nanoTime() and 0x7FFFFFFF).toInt() else 101
        commitProjectChange { proj ->
            val withBeat = AutoBeatGenerator.generateFullBeat(proj, variation, seed)
            ArrangeGenerator.generateSongArrangement(withBeat)
        }
        showMessage("Naya beat pattern ban gaya!")
    }

    fun generateSongArrangement() {
        commitProjectChange { proj ->
            ArrangeGenerator.generateSongArrangement(proj).copy(isArrangeMode = true)
        }
        showMessage("Poora song timeline ban gaya!")
    }

    // ---- Pattern & Beat Grid Operations ----
    fun selectTrackPattern(trackId: String, patternId: String) {
        commitProjectChange(recordUndo = false) { proj ->
            proj.copy(tracks = proj.tracks.map { t ->
                if (t.id == trackId) {
                    val pats = if (t.patterns.containsKey(patternId)) t.patterns
                    else t.patterns + (patternId to Pattern(id = patternId, stepCount = t.patterns[t.activePatternId]?.stepCount ?: 16))
                    t.copy(activePatternId = patternId, patterns = pats)
                } else t
            })
        }
    }

    fun setTrackPatternLength(trackId: String, stepCount: Int) {
        commitProjectChange { proj ->
            proj.copy(tracks = proj.tracks.map { t ->
                if (t.id != trackId) t else {
                    val pat = t.patterns[t.activePatternId] ?: Pattern(id = t.activePatternId)
                    val resizedRows = pat.drumRows.mapValues { (_, list) ->
                        List(stepCount) { idx -> list.getOrElse(idx) { DrumStep() } }
                    }
                    val updatedPat = pat.copy(stepCount = stepCount, drumRows = resizedRows)
                    t.copy(patterns = t.patterns + (pat.id to updatedPat))
                }
            })
        }
    }

    fun toggleTripletGrid(trackId: String) {
        commitProjectChange { proj ->
            proj.copy(tracks = proj.tracks.map { t ->
                if (t.id != trackId) t else {
                    val pat = t.patterns[t.activePatternId] ?: Pattern(id = t.activePatternId)
                    t.copy(patterns = t.patterns + (pat.id to pat.copy(isTripletGrid = !pat.isTripletGrid)))
                }
            })
        }
    }

    fun toggleDrumStep(trackId: String, rowIndex: Int, stepIndex: Int) {
        val proj = _project.value ?: return
        val track = proj.tracks.firstOrNull { it.id == trackId } ?: return
        val row = track.drumRows.firstOrNull { it.rowIndex == rowIndex }
        commitProjectChange { p ->
            p.copy(tracks = p.tracks.map { t ->
                if (t.id != trackId) t else {
                    val pat = t.patterns[t.activePatternId] ?: Pattern(id = t.activePatternId)
                    val curList = pat.drumRows[rowIndex]?.toMutableList() ?: MutableList(pat.stepCount) { DrumStep() }
                    while (curList.size < pat.stepCount) curList.add(DrumStep())
                    val oldStep = curList[stepIndex]
                    val nextActive = !oldStep.active
                    curList[stepIndex] = oldStep.copy(active = nextActive)
                    if (nextActive && row != null && !audioEngine.isPlaying.value) {
                        audioEngine.previewDrum(trackId, row, oldStep.velocity)
                    }
                    val newPat = pat.copy(drumRows = pat.drumRows + (rowIndex to curList))
                    t.copy(patterns = t.patterns + (pat.id to newPat))
                }
            })
        }
    }

    fun updateDrumStepParams(trackId: String, rowIndex: Int, stepIndex: Int, velocity: Float, probability: Float, rollCount: Int) {
        commitProjectChange { p ->
            p.copy(tracks = p.tracks.map { t ->
                if (t.id != trackId) t else {
                    val pat = t.patterns[t.activePatternId] ?: Pattern(id = t.activePatternId)
                    val curList = pat.drumRows[rowIndex]?.toMutableList() ?: MutableList(pat.stepCount) { DrumStep() }
                    while (curList.size < pat.stepCount) curList.add(DrumStep())
                    curList[stepIndex] = curList[stepIndex].copy(
                        active = true,
                        velocity = velocity.coerceIn(0.1f, 1f),
                        probability = probability.coerceIn(0.1f, 1f),
                        rollCount = rollCount.coerceIn(1, 4)
                    )
                    val newPat = pat.copy(drumRows = pat.drumRows + (rowIndex to curList))
                    t.copy(patterns = t.patterns + (pat.id to newPat))
                }
            })
        }
    }

    fun addHiHatRollFill(trackId: String) {
        commitProjectChange { p ->
            p.copy(tracks = p.tracks.map { t ->
                if (t.id != trackId) t else {
                    val hatRow = t.drumRows.firstOrNull { it.soundType == DrumSoundType.HIHAT_CLOSED } ?: return@map t
                    val pat = t.patterns[t.activePatternId] ?: Pattern(id = t.activePatternId)
                    val list = pat.drumRows[hatRow.rowIndex]?.toMutableList() ?: MutableList(pat.stepCount) { DrumStep() }
                    while (list.size < pat.stepCount) list.add(DrumStep())
                    for (s in 0 until pat.stepCount) {
                        if (s % 2 == 0) {
                            list[s] = DrumStep(active = true, velocity = 0.72f, probability = 1f, rollCount = 1)
                        }
                        if (s == pat.stepCount - 1 || s == pat.stepCount - 9) {
                            list[s] = DrumStep(active = true, velocity = 0.88f, probability = 1f, rollCount = 3)
                        }
                    }
                    val newPat = pat.copy(drumRows = pat.drumRows + (hatRow.rowIndex to list))
                    t.copy(patterns = t.patterns + (pat.id to newPat))
                }
            })
        }
        showMessage("Hi-hat roll fill lag gaya!")
    }

    fun copyActivePattern(trackId: String) {
        val track = _project.value?.tracks?.firstOrNull { it.id == trackId } ?: return
        val pat = track.patterns[track.activePatternId] ?: return
        _clipboardPattern.value = pat
        showMessage("Pattern ${pat.id} copy ho gaya")
    }

    fun pasteIntoActivePattern(trackId: String) {
        val clip = _clipboardPattern.value ?: return
        commitProjectChange { proj ->
            proj.copy(tracks = proj.tracks.map { t ->
                if (t.id != trackId) t else {
                    val targetId = t.activePatternId
                    t.copy(patterns = t.patterns + (targetId to clip.copy(id = targetId, name = "Pattern $targetId")))
                }
            })
        }
        showMessage("Pattern paste ho gaya!")
    }

    fun addDrumRow(trackId: String, soundType: DrumSoundType) {
        commitProjectChange { proj ->
            proj.copy(tracks = proj.tracks.map { t ->
                if (t.id != trackId) t else {
                    val nextIndex = (t.drumRows.maxOfOrNull { it.rowIndex } ?: -1) + 1
                    t.copy(drumRows = t.drumRows + DrumRowConfig(nextIndex, soundType, soundType.labelHinglish))
                }
            })
        }
    }

    fun updateDrumRowPitch(trackId: String, rowIndex: Int, pitchOffset: Float) {
        commitProjectChange { proj ->
            proj.copy(tracks = proj.tracks.map { t ->
                if (t.id != trackId) t else {
                    t.copy(drumRows = t.drumRows.map { r ->
                        if (r.rowIndex == rowIndex) r.copy(pitchOffset = pitchOffset) else r
                    })
                }
            })
        }
    }

    // ---- Piano Roll / Melody Operations ----
    fun toggleOrAddNote(trackId: String, midiPitch: Int, startStep: Float, durationSteps: Float = 1f) {
        val proj = _project.value ?: return
        val safeMidi = if (_scaleLockEnabled.value) {
            Scales.snapToScale(midiPitch, proj.rootNote, proj.scale)
        } else midiPitch

        commitProjectChange { p ->
            p.copy(tracks = p.tracks.map { t ->
                if (t.id != trackId) t else {
                    val pat = t.patterns[t.activePatternId] ?: Pattern(id = t.activePatternId)
                    val existing = pat.notes.firstOrNull { n ->
                        n.pitch == safeMidi && startStep >= n.startStep && startStep < n.startStep + n.durationSteps
                    }
                    val nextNotes = if (existing != null) {
                        pat.notes.filterNot { it.id == existing.id }
                    } else {
                        if (!audioEngine.isPlaying.value) {
                            if (t.type == TrackType.SAMPLER) {
                                audioEngine.previewSamplerNoteOn(t.id, safeMidi, 0.88f, t.samplerParams)
                            } else {
                                audioEngine.previewSynthNoteOn(t.id, safeMidi, 0.88f, t.synthParams)
                                viewModelScope.launch {
                                    delay(240)
                                    audioEngine.previewSynthNoteOff(t.id, safeMidi)
                                }
                            }
                        }
                        pat.notes + Note(
                            id = UUID.randomUUID().toString(),
                            pitch = safeMidi,
                            startStep = startStep,
                            durationSteps = durationSteps,
                            velocity = 0.86f
                        )
                    }
                    t.copy(patterns = t.patterns + (pat.id to pat.copy(notes = nextNotes)))
                }
            })
        }
    }

    fun updateNote(trackId: String, updatedNote: Note) {
        val proj = _project.value ?: return
        val safeMidi = if (_scaleLockEnabled.value) {
            Scales.snapToScale(updatedNote.pitch, proj.rootNote, proj.scale)
        } else updatedNote.pitch

        commitProjectChange { p ->
            p.copy(tracks = p.tracks.map { t ->
                if (t.id != trackId) t else {
                    val pat = t.patterns[t.activePatternId] ?: return@map t
                    val next = pat.notes.map { n ->
                        if (n.id == updatedNote.id) updatedNote.copy(pitch = safeMidi) else n
                    }
                    t.copy(patterns = t.patterns + (pat.id to pat.copy(notes = next)))
                }
            })
        }
    }

    fun deleteNote(trackId: String, noteId: String) {
        commitProjectChange { p ->
            p.copy(tracks = p.tracks.map { t ->
                if (t.id != trackId) t else {
                    val pat = t.patterns[t.activePatternId] ?: return@map t
                    t.copy(patterns = t.patterns + (pat.id to pat.copy(notes = pat.notes.filterNot { it.id == noteId })))
                }
            })
        }
    }

    fun quantizeNotes(trackId: String) {
        commitProjectChange { p ->
            p.copy(tracks = p.tracks.map { t ->
                if (t.id != trackId) t else {
                    val pat = t.patterns[t.activePatternId] ?: return@map t
                    val qNotes = pat.notes.map { n ->
                        n.copy(
                            startStep = n.startStep.roundToInt().toFloat().coerceIn(0f, (pat.stepCount - 1).toFloat()),
                            durationSteps = (n.durationSteps * 2f).roundToInt().coerceAtLeast(1) / 2f
                        )
                    }
                    t.copy(patterns = t.patterns + (pat.id to pat.copy(notes = qNotes)))
                }
            })
        }
        showMessage("Notes grid par quantize ho gaye!")
    }

    fun applyChordHelper(trackId: String, preset: ChordPreset) {
        val proj = _project.value ?: return
        commitProjectChange { p ->
            p.copy(tracks = p.tracks.map { t ->
                if (t.id != trackId) t else {
                    val pat = t.patterns[t.activePatternId] ?: Pattern(id = t.activePatternId)
                    val chordNotes = Chords.buildChordProgressionNotes(
                        preset = preset,
                        rootNote = proj.rootNote,
                        scale = proj.scale,
                        stepCount = pat.stepCount
                    )
                    t.copy(patterns = t.patterns + (pat.id to pat.copy(notes = chordNotes)))
                }
            })
        }
        showMessage("${preset.titleHinglish} chords add ho gaye!")
    }

    fun onLiveKeyDown(midi: Int) {
        val proj = _project.value ?: return
        val track = proj.tracks.firstOrNull { it.id == _selectedTrackId.value } ?: return
        val safeMidi = if (_scaleLockEnabled.value) Scales.snapToScale(midi, proj.rootNote, proj.scale) else midi

        when (track.type) {
            TrackType.SAMPLER -> audioEngine.previewSamplerNoteOn(track.id, safeMidi, 0.9f, track.samplerParams)
            TrackType.INSTRUMENT -> audioEngine.previewSynthNoteOn(track.id, safeMidi, 0.9f, track.synthParams)
            else -> Unit
        }

        if (_isRecordingMidi.value && audioEngine.isPlaying.value) {
            val step = audioEngine.playheadStep.value.toFloat()
            commitProjectChange(recordUndo = false) { p ->
                p.copy(tracks = p.tracks.map { t ->
                    if (t.id != track.id) t else {
                        val pat = t.patterns[t.activePatternId] ?: Pattern(id = t.activePatternId)
                        val note = Note(
                            id = UUID.randomUUID().toString(),
                            pitch = safeMidi,
                            startStep = step,
                            durationSteps = 1f,
                            velocity = 0.9f
                        )
                        t.copy(patterns = t.patterns + (pat.id to pat.copy(notes = pat.notes + note)))
                    }
                })
            }
        }
    }

    fun onLiveKeyUp(midi: Int) {
        val proj = _project.value ?: return
        val track = proj.tracks.firstOrNull { it.id == _selectedTrackId.value } ?: return
        val safeMidi = if (_scaleLockEnabled.value) Scales.snapToScale(midi, proj.rootNote, proj.scale) else midi
        audioEngine.previewSynthNoteOff(track.id, safeMidi)
    }

    fun toggleMidiRecord() {
        val next = !_isRecordingMidi.value
        _isRecordingMidi.value = next
        if (next && !audioEngine.isPlaying.value) {
            audioEngine.play()
        }
        showMessage(if (next) "Live recording chalu! Keys bajao" else "Recording band")
    }

    // ---- Arrange Block Operations ----
    fun addArrangeBlock(trackId: String, patternId: String, startBar: Int, lengthBars: Int = 2, label: String = "Beat") {
        commitProjectChange { proj ->
            val block = ArrangeBlock(UUID.randomUUID().toString(), trackId, patternId, startBar, lengthBars, label)
            proj.copy(arrangement = proj.arrangement + block)
        }
    }

    fun duplicateArrangeBlock(block: ArrangeBlock) {
        commitProjectChange { proj ->
            val copy = block.copy(
                id = UUID.randomUUID().toString(),
                startBar = block.startBar + block.lengthBars
            )
            proj.copy(arrangement = proj.arrangement + copy)
        }
    }

    fun splitArrangeBlock(block: ArrangeBlock) {
        if (block.lengthBars <= 1) return
        val half1 = block.lengthBars / 2
        val half2 = block.lengthBars - half1
        commitProjectChange { proj ->
            val b1 = block.copy(lengthBars = half1)
            val b2 = block.copy(id = UUID.randomUUID().toString(), startBar = block.startBar + half1, lengthBars = half2)
            proj.copy(arrangement = proj.arrangement.filterNot { it.id == block.id } + listOf(b1, b2))
        }
    }

    fun deleteArrangeBlock(blockId: String) {
        commitProjectChange { proj ->
            proj.copy(arrangement = proj.arrangement.filterNot { it.id == blockId })
        }
    }

    // ---- Track, Synth, Sampler, Mixer, Effects, Automation ----
    fun updateTrack(trackId: String, recordUndo: Boolean = true, transform: (Track) -> Track) {
        commitProjectChange(recordUndo = recordUndo) { proj ->
            proj.copy(tracks = proj.tracks.map { if (it.id == trackId) transform(it) else it })
        }
    }

    fun updateSynthPreset(trackId: String, presetId: SynthPresetId) {
        updateTrack(trackId) { t ->
            t.copy(synthParams = Presets.forPreset(presetId))
        }
    }

    fun updateMasterVolume(vol: Float) {
        commitProjectChange(recordUndo = false) { it.copy(masterVolume = vol.coerceIn(0f, 1.2f)) }
    }

    fun updateMasterEffects(transform: (EffectSettings) -> EffectSettings) {
        commitProjectChange { it.copy(masterEffects = transform(it.masterEffects)) }
    }

    fun importSampleFile(trackId: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val displayName = FileUtils.getDisplayName(ctx, uri)
            val result = SampleStore.importAudioUri(ctx, uri, displayName)
            result.onSuccess { buf ->
                withContext(Dispatchers.Main) {
                    updateTrack(trackId) { t ->
                        t.copy(
                            samplerParams = t.samplerParams.copy(
                                sampleName = displayName,
                                sampleFilePath = buf.filePath
                            )
                        )
                    }
                    showMessage("Sample '$displayName' load ho gaya!")
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    showMessage(err.message ?: "Unsupported audio file!")
                }
            }
        }
    }

    fun importMidiFile(trackId: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            try {
                val notes = ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    MidiIo.importMidiNotes(stream, maxSteps = 32)
                }.orEmpty()
                withContext(Dispatchers.Main) {
                    if (notes.isEmpty()) {
                        showMessage("MIDI file mein koi notes nahi mile ya format unsupported hai.")
                    } else {
                        updateTrack(trackId) { t ->
                            val pat = t.patterns[t.activePatternId] ?: Pattern(id = t.activePatternId)
                            t.copy(patterns = t.patterns + (pat.id to pat.copy(notes = notes, stepCount = 32)))
                        }
                        showMessage("${notes.size} MIDI notes import ho gaye!")
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("MIDI file read nahi ho payi.")
                }
            }
        }
    }

    fun startMicRecordingWithCountIn(trackId: String) {
        if (_isRecordingMic.value) {
            stopAndSaveMicRecording(trackId)
            return
        }
        viewModelScope.launch {
            // 4-beat count-in
            val proj = _project.value ?: return@launch
            val beatMs = (60_000L / proj.bpm.coerceIn(40, 220))
            for (beat in 4 downTo 1) {
                _countInText.value = "Taiyar ho jao... $beat"
                audioEngine.previewDrumSound(DrumSoundType.RIM, pitchOffset = if (beat == 4) 5f else 0f)
                delay(beatMs)
            }
            _countInText.value = null
            _isRecordingMic.value = true
            if (!audioEngine.isPlaying.value) {
                audioEngine.play()
            }
            micRecorder.startRecording { err ->
                viewModelScope.launch(Dispatchers.Main) {
                    _isRecordingMic.value = false
                    showMessage(err)
                }
            }
        }
    }

    fun stopAndSaveMicRecording(trackId: String) {
        val proj = _project.value ?: return
        val track = proj.tracks.firstOrNull { it.id == trackId } ?: return
        _isRecordingMic.value = false
        _countInText.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val buf = micRecorder.stopAndSave(
                context = getApplication(),
                applyPitchSnap = track.effects.pitchSnapEnabled,
                rootNote = proj.rootNote,
                scale = proj.scale,
                snapStrength = track.effects.pitchSnapStrength
            )
            withContext(Dispatchers.Main) {
                if (buf != null) {
                    updateTrack(trackId) { t -> t.copy(vocalFilePath = buf.filePath) }
                    showMessage(if (track.effects.pitchSnapEnabled) "Voice record & Auto-Tune snap save ho gaya!" else "Voice recording save ho gayi!")
                } else {
                    showMessage("Voice recording bahut chhoti thi.")
                }
            }
        }
    }

    fun startExport(format: ExportFormat, exportStems: Boolean) {
        val proj = _project.value ?: return
        ExportService.startExport(
            context = getApplication(),
            project = proj,
            format = format,
            exportStems = exportStems,
            isProMode = _isProMode.value
        )
    }

    fun cancelExport() {
        ExportService.cancelExport(getApplication())
    }

    fun clearExportResult() {
        ExportService.clearResult()
    }

    fun advanceTour() {
        val next = _tourStep.value + 1
        if (next >= 4) {
            dismissTour()
        } else {
            _tourStep.value = next
        }
    }

    fun previousTour() {
        _tourStep.value = (_tourStep.value - 1).coerceAtLeast(0)
    }

    fun dismissTour() {
        _showTour.value = false
        viewModelScope.launch { settingsStore.setTourCompleted(true) }
    }

    fun reopenTour() {
        _tourStep.value = 0
        _showTour.value = true
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        audioEngine.shutdown()
        micRecorder.cancel()
        super.onCleared()
    }
}

package com.beatstudio.app.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatstudio.app.domain.Track
import com.beatstudio.app.domain.TrackType
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(
    projectId: String,
    viewModel: EditorViewModel,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    BackHandler {
        viewModel.audioEngine.stop()
        onBackToHome()
    }

    val projectState by viewModel.project.collectAsStateWithLifecycle()
    val selectedTrackId by viewModel.selectedTrackId.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isProMode by viewModel.isProMode.collectAsStateWithLifecycle()
    val scaleLockEnabled by viewModel.scaleLockEnabled.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isRecordingMidi by viewModel.isRecordingMidi.collectAsStateWithLifecycle()
    val isRecordingMic by viewModel.isRecordingMic.collectAsStateWithLifecycle()
    val countInText by viewModel.countInText.collectAsStateWithLifecycle()
    val playheadStep by viewModel.playheadStep.collectAsStateWithLifecycle()
    val playheadBar by viewModel.playheadBar.collectAsStateWithLifecycle()
    val masterLevel by viewModel.masterLevel.collectAsStateWithLifecycle()
    val trackLevels by viewModel.trackLevels.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val hasClipboard by viewModel.hasClipboardPattern.collectAsStateWithLifecycle()
    val showTour by viewModel.showTour.collectAsStateWithLifecycle()
    val tourStep by viewModel.tourStep.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    var trackSheetTargetId by remember { mutableStateOf<String?>(null) }
    var effectSheetTrackId by remember { mutableStateOf<String?>(null) }
    var showMasterEffects by remember { mutableStateOf(false) }
    var showMagicSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val p = projectState
        if (granted && p != null) {
            val vocalTrack = p.tracks.firstOrNull { it.type == TrackType.VOCAL }
            if (vocalTrack != null) viewModel.startMicRecordingWithCountIn(vocalTrack.id)
        } else {
            viewModel.showMessage("Mic permission nahi mili! Voice record karne ke liye Microphone permission allow karein.")
        }
    }

    // Auto-clear snackbar/tooltip after 2.6 seconds
    LaunchedEffect(snackbarMsg) {
        if (snackbarMsg != null) {
            delay(2600)
            viewModel.clearMessage()
        }
    }

    val project = projectState
    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonCyan)
        }
        return
    }

    val selectedTrack = project.tracks.firstOrNull { it.id == selectedTrackId }
        ?: project.tracks.first()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopBar(
                    project = project,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    isProMode = isProMode,
                    onBack = {
                        viewModel.audioEngine.stop()
                        onBackToHome()
                    },
                    onRenameProject = viewModel::setProjectName,
                    onSetBpm = viewModel::setBpm,
                    onTapTempo = viewModel::registerTapTempo,
                    onSetTimeSig = viewModel::setTimeSignature,
                    onToggleMetronome = viewModel::toggleMetronome,
                    onToggleSlowedReverb = viewModel::toggleSlowedReverb,
                    onSetKeyScale = viewModel::setKeyAndScale,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onToggleProMode = viewModel::toggleProMode,
                    onOpenExport = { showExportSheet = true },
                    onOpenMagic = { showMagicSheet = true },
                    onOpenTour = viewModel::reopenTour,
                    onShowTooltip = viewModel::showMessage
                )
            },
            bottomBar = {
                TransportBar(
                    activeTab = activeTab,
                    isPlaying = isPlaying,
                    isRecording = isRecordingMidi || isRecordingMic,
                    onSelectTab = viewModel::selectTab,
                    onTogglePlay = { viewModel.audioEngine.togglePlay() },
                    onToggleRecord = {
                        if (selectedTrack.type == TrackType.VOCAL) {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                viewModel.startMicRecordingWithCountIn(selectedTrack.id)
                            } else {
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            viewModel.toggleMidiRecord()
                        }
                    },
                    onShowTooltip = viewModel::showMessage
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    EditorTab.BEAT -> {
                        if (project.isArrangeMode) {
                            ArrangeView(
                                project = project,
                                playheadBar = playheadBar,
                                isPlaying = isPlaying,
                                onBackToPattern = { viewModel.toggleArrangeMode(false) },
                                onGenerateSong = viewModel::generateSongArrangement,
                                onSetLoopRange = viewModel::setLoopRange,
                                onAddBlock = viewModel::addArrangeBlock,
                                onDuplicateBlock = viewModel::duplicateArrangeBlock,
                                onSplitBlock = viewModel::splitArrangeBlock,
                                onDeleteBlock = viewModel::deleteArrangeBlock
                            )
                        } else {
                            val drumTrack = project.tracks.firstOrNull { it.type == TrackType.DRUMS } ?: selectedTrack
                            BeatGrid(
                                project = project,
                                track = drumTrack,
                                playheadStep = playheadStep,
                                isPlaying = isPlaying,
                                hasClipboard = hasClipboard,
                                onSelectPattern = { pid -> viewModel.selectTrackPattern(drumTrack.id, pid) },
                                onSetStepCount = { count -> viewModel.setTrackPatternLength(drumTrack.id, count) },
                                onToggleTriplet = { viewModel.toggleTripletGrid(drumTrack.id) },
                                onSetSwing = viewModel::setSwing,
                                onToggleStep = { row, step -> viewModel.toggleDrumStep(drumTrack.id, row, step) },
                                onUpdateStepParams = { row, step, vel, prob, roll ->
                                    viewModel.updateDrumStepParams(drumTrack.id, row, step, vel, prob, roll)
                                },
                                onPreviewRow = { row -> viewModel.audioEngine.previewDrum(drumTrack.id, row) },
                                onUpdateRowPitch = { row, pitch -> viewModel.updateDrumRowPitch(drumTrack.id, row, pitch) },
                                onAddRollFill = { viewModel.addHiHatRollFill(drumTrack.id) },
                                onCopyPattern = { viewModel.copyActivePattern(drumTrack.id) },
                                onPastePattern = { viewModel.pasteIntoActivePattern(drumTrack.id) },
                                onOpenTrackSheet = { trackSheetTargetId = drumTrack.id },
                                onSwitchToArrange = { viewModel.toggleArrangeMode(true) },
                                onShowTooltip = viewModel::showMessage
                            )
                        }
                    }

                    EditorTab.MELODY -> {
                        val melodyTrack = if (selectedTrack.type != TrackType.DRUMS) {
                            selectedTrack
                        } else {
                            project.tracks.firstOrNull { it.type != TrackType.DRUMS } ?: selectedTrack
                        }
                        PianoRoll(
                            project = project,
                            tracks = project.tracks,
                            selectedTrack = melodyTrack,
                            scaleLockEnabled = scaleLockEnabled,
                            isProMode = isProMode,
                            playheadStep = playheadStep,
                            isPlaying = isPlaying,
                            onSelectTrack = viewModel::selectTrack,
                            onOpenTrackSheet = { t -> trackSheetTargetId = t.id },
                            onSelectPattern = { pid -> viewModel.selectTrackPattern(melodyTrack.id, pid) },
                            onToggleScaleLock = viewModel::toggleScaleLock,
                            onQuantize = { viewModel.quantizeNotes(melodyTrack.id) },
                            onApplyChord = { preset -> viewModel.applyChordHelper(melodyTrack.id, preset) },
                            onToggleOrAddNote = { midi, step -> viewModel.toggleOrAddNote(melodyTrack.id, midi, step) },
                            onUpdateNote = { note -> viewModel.updateNote(melodyTrack.id, note) },
                            onDeleteNote = { noteId -> viewModel.deleteNote(melodyTrack.id, noteId) },
                            onLiveKeyDown = viewModel::onLiveKeyDown,
                            onLiveKeyUp = viewModel::onLiveKeyUp,
                            onShowTooltip = viewModel::showMessage
                        )
                    }

                    EditorTab.MIX -> {
                        MixerView(
                            project = project,
                            isProMode = isProMode,
                            masterLevel = masterLevel,
                            trackLevels = trackLevels,
                            onUpdateTrack = viewModel::updateTrack,
                            onUpdateMasterVolume = viewModel::updateMasterVolume,
                            onTogglePhonkDrive = viewModel::togglePhonkDrive,
                            onToggleSlowedReverb = viewModel::toggleSlowedReverb,
                            onOpenTrackSheet = { t -> trackSheetTargetId = t.id },
                            onOpenTrackEffects = { t -> effectSheetTrackId = t.id },
                            onOpenMasterEffects = { showMasterEffects = true }
                        )
                    }
                }
            }
        }

        // Count-in Banner
        countInText?.let { cText ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PhonkCoral,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = cText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)
                    )
                }
            }
        }

        // Floating Hinglish Tooltip / Snackbar Pill
        AnimatedVisibility(
            visible = snackbarMsg != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 92.dp, start = 20.dp, end = 20.dp)
        ) {
            snackbarMsg?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, NeonCyan),
                    tonalElevation = 8.dp
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // 4-step First Launch Hinglish Guided Tour
        if (showTour) {
            GuidedTourOverlay(
                step = tourStep,
                onNext = viewModel::advanceTour,
                onPrevious = viewModel::previousTour,
                onDismiss = viewModel::dismissTour
            )
        }
    }

    // Track Sound & Control Sheet
    trackSheetTargetId?.let { tid ->
        val t = project.tracks.firstOrNull { it.id == tid }
        if (t != null) {
            TrackSheet(
                track = t,
                isProMode = isProMode,
                isRecordingMic = isRecordingMic,
                onDismiss = { trackSheetTargetId = null },
                onUpdateTrack = { transform -> viewModel.updateTrack(tid, true, transform) },
                onSelectPreset = { preset -> viewModel.updateSynthPreset(tid, preset) },
                onAddDrumRow = { sound -> viewModel.addDrumRow(tid, sound) },
                onImportSampleUri = { uri -> viewModel.importSampleFile(tid, uri) },
                onImportMidiUri = { uri -> viewModel.importMidiFile(tid, uri) },
                onToggleMicRecord = { viewModel.startMicRecordingWithCountIn(tid) },
                onOpenEffects = {
                    trackSheetTargetId = null
                    effectSheetTrackId = tid
                },
                onShowMessage = viewModel::showMessage
            )
        }
    }

    // Track Effect Sheet
    effectSheetTrackId?.let { tid ->
        val t = project.tracks.firstOrNull { it.id == tid }
        if (t != null) {
            EffectSheet(
                title = t.name,
                effects = t.effects,
                isProMode = isProMode,
                accentColor = Color(t.colorHex),
                onDismiss = { effectSheetTrackId = null },
                onUpdateEffects = { transform ->
                    viewModel.updateTrack(tid, true) { tr -> tr.copy(effects = transform(tr.effects)) }
                }
            )
        }
    }

    // Master Effect Sheet
    if (showMasterEffects) {
        EffectSheet(
            title = "Master Bus",
            effects = project.masterEffects,
            isProMode = isProMode,
            accentColor = NeonCyan,
            onDismiss = { showMasterEffects = false },
            onUpdateEffects = viewModel::updateMasterEffects
        )
    }

    // Magic Auto-Beat Sheet
    if (showMagicSheet) {
        MagicSheet(
            project = project,
            onDismiss = { showMagicSheet = false },
            onApplyGenrePreset = viewModel::applyGenrePreset,
            onAutoBeat = viewModel::generateAutoBeat,
            onGenerateSong = {
                viewModel.generateSongArrangement()
                viewModel.selectTab(EditorTab.BEAT)
            },
            onToggleSlowedReverb = viewModel::toggleSlowedReverb,
            onTogglePhonkDrive = viewModel::togglePhonkDrive
        )
    }

    // Export Sheet
    if (showExportSheet) {
        ExportSheet(
            exportState = exportState,
            onDismiss = { showExportSheet = false },
            onStartExport = viewModel::startExport,
            onCancelExport = viewModel::cancelExport,
            onClearResult = viewModel::clearExportResult
        )
    }
}

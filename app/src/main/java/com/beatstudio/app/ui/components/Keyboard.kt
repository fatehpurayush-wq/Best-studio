package com.beatstudio.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatstudio.app.domain.MusicalScale
import com.beatstudio.app.music.Scales
import com.beatstudio.app.ui.theme.StudioObsidian
import com.beatstudio.app.ui.theme.StudioSurfaceElevated
import com.beatstudio.app.ui.theme.StudioSurfaceHighlight

@Composable
fun PianoKeyboard(
    rootNote: Int,
    scale: MusicalScale,
    scaleLockEnabled: Boolean,
    octaveStart: Int = 3,
    octaveCount: Int = 2,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onNoteDown: (Int) -> Unit,
    onNoteUp: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val startMidi = (octaveStart + 1) * 12
    val endMidi = startMidi + octaveCount * 12 - 1

    val keyList = remember(startMidi, endMidi, rootNote, scale, scaleLockEnabled) {
        if (scaleLockEnabled && scale != MusicalScale.CHROMATIC) {
            (startMidi..endMidi).filter { Scales.isNoteInScale(it, rootNote, scale) }
        } else {
            (startMidi..endMidi).toList()
        }
    }

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(StudioObsidian, RoundedCornerShape(14.dp))
            .horizontalScroll(scrollState)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        keyList.forEach { midi ->
            val noteIndex = ((midi % 12) + 12) % 12
            val isBlack = noteIndex in listOf(1, 3, 6, 8, 10)
            val isRoot = noteIndex == ((rootNote % 12) + 12) % 12
            val inScale = Scales.isNoteInScale(midi, rootNote, scale)
            var pressed by remember { mutableStateOf(false) }

            val bgColor = when {
                pressed -> accentColor
                isRoot -> accentColor.copy(alpha = 0.28f)
                isBlack -> StudioSurfaceElevated
                inScale -> Color(0xFFE9EDF8)
                else -> StudioSurfaceHighlight.copy(alpha = 0.4f)
            }
            val textColor = when {
                pressed -> StudioObsidian
                isRoot -> accentColor
                isBlack -> Color.White.copy(alpha = 0.85f)
                inScale -> StudioObsidian
                else -> Color.White.copy(alpha = 0.4f)
            }

            Surface(
                modifier = Modifier
                    .width(if (scaleLockEnabled) 50.dp else if (isBlack) 38.dp else 46.dp)
                    .fillMaxHeight()
                    .testTag("piano_key_$midi")
                    .pointerInput(midi) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                onNoteDown(midi)
                                tryAwaitRelease()
                                pressed = false
                                onNoteUp(midi)
                            }
                        )
                    },
                shape = RoundedCornerShape(10.dp),
                color = bgColor,
                border = BorderStroke(
                    width = if (isRoot) 1.5.dp else 1.dp,
                    color = if (isRoot) accentColor else MaterialTheme.colorScheme.outline
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = Scales.midiToName(midi),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}

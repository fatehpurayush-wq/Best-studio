package com.beatstudio.app.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatstudio.app.ui.components.AppButton
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import com.beatstudio.app.ui.theme.StudioAmber
import com.beatstudio.app.ui.theme.StudioEmerald
import com.beatstudio.app.ui.theme.StudioObsidian

@Composable
fun GuidedTourOverlay(
    step: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, body, icon, accent) = when (step) {
        0 -> TourStep(
            "1/4 • BEAT Grid se Shuru Karein",
            "Boxes par tap karke Kick, Snare, Hi-hat aur Phonk Cowbell lagao. Kisi bhi step ko long-press karne par velocity, probability aur hi-hat rolls khulenge!",
            Icons.Default.GridView,
            PhonkCoral
        )
        1 -> TourStep(
            "2/4 • MELODY aur Scale Lock",
            "MELODY tab mein sirf sahi sur (Scale Lock) dikhte hain — galat note lagna namumkin! 'Chord Helper' se ek tap mein chords daalein.",
            Icons.Default.Piano,
            NeonCyan
        )
        2 -> TourStep(
            "3/4 • Magic 'Auto beat banao'",
            "Instant beat chahiye? Upar 'Magic' dabao — genre ke hisaab se poora drum, 808 bass, chords aur song arrangement turant ban jayega!",
            Icons.Default.AutoAwesome,
            StudioAmber
        )
        else -> TourStep(
            "4/4 • MIX, Slowed + Reverb & Export",
            "MIX tab mein har track ka sound, Reverb, Tape Lo-fi aur Sidechain set karein. Export dabakar WAV, M4A ya MIDI Music folder mein save karein!",
            Icons.Default.Tune,
            StudioEmerald
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, accent),
            modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accent.copy(alpha = 0.2f)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.padding(10.dp).size(26.dp)
                        )
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("tour_skip_button")) {
                        Text("Skip karein")
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )

                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Tip: Kisi bhi button ko daba kar rakhne (long-press) par Hinglish madad dikhegi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        OutlinedButton(onClick = onPrevious) {
                            Text("Pichhe")
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    AppButton(
                        text = if (step >= 3) "Samajh gaya!" else "Aage",
                        containerColor = accent,
                        contentColor = StudioObsidian,
                        onClick = onNext,
                        tag = "tour_next_button"
                    )
                }
            }
        }
    }
}

private data class TourStep(
    val title: String,
    val body: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color
)

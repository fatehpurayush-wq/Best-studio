package com.beatstudio.app.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beatstudio.app.domain.EffectSettings
import com.beatstudio.app.domain.ReverbType
import com.beatstudio.app.ui.components.AppChipButton
import com.beatstudio.app.ui.components.SliderRow
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import com.beatstudio.app.ui.theme.StudioAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectSheet(
    title: String,
    effects: EffectSettings,
    isProMode: Boolean,
    accentColor: Color = NeonCyan,
    onDismiss: () -> Unit,
    onUpdateEffects: ((EffectSettings) -> EffectSettings) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$title • Effects Rack",
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor
            )

            // 1. Reverb
            EffectSectionCard("Reverb (Echo Room)", effects.reverbEnabled, accentColor, { en ->
                onUpdateEffects { it.copy(reverbEnabled = en) }
            }) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReverbType.entries.forEach { rt ->
                        AppChipButton(rt.labelHinglish, effects.reverbType == rt, {
                            onUpdateEffects { it.copy(reverbType = rt) }
                        })
                    }
                }
                SliderRow("Mix", effects.reverbMix, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(reverbMix = v) } })
                SliderRow("Size", effects.reverbSize, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(reverbSize = v) } })
                if (isProMode) {
                    SliderRow("Damping", effects.reverbDamp, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(reverbDamp = v) } })
                }
            }

            // 2. Delay & Ping Pong
            EffectSectionCard("Delay / Ping-Pong", effects.delayEnabled, accentColor, { en ->
                onUpdateEffects { it.copy(delayEnabled = en) }
            }) {
                AppChipButton(
                    text = if (effects.delayPingPong) "Ping-Pong: ON" else "Stereo Delay",
                    selected = effects.delayPingPong,
                    onClick = { onUpdateEffects { it.copy(delayPingPong = !it.delayPingPong) } }
                )
                SliderRow("Time", effects.delayTimeMs, valueRange = 60f..850f, valueFormatter = { "${it.toInt()} ms" }, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(delayTimeMs = v) } })
                SliderRow("Feedback", effects.delayFeedback, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(delayFeedback = v) } })
                SliderRow("Mix", effects.delayMix, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(delayMix = v) } })
            }

            // 3. Sidechain (One tap pump)
            EffectSectionCard("Sidechain (Kick Pump)", effects.sidechainEnabled, PhonkCoral, { en ->
                onUpdateEffects { it.copy(sidechainEnabled = en) }
            }) {
                SliderRow("Pump Depth", effects.sidechainAmount, accentColor = PhonkCoral, onValueChange = { v -> onUpdateEffects { it.copy(sidechainAmount = v) } })
                SliderRow("Release", effects.sidechainRelease, accentColor = PhonkCoral, onValueChange = { v -> onUpdateEffects { it.copy(sidechainRelease = v) } })
            }

            // 4. Distortion / Overdrive
            EffectSectionCard("Distortion / Overdrive", effects.distortionEnabled, PhonkCoral, { en ->
                onUpdateEffects { it.copy(distortionEnabled = en) }
            }) {
                AppChipButton(
                    text = if (effects.isOverdrive) "Warm Overdrive" else "Hard Saturation",
                    selected = effects.isOverdrive,
                    accentColor = PhonkCoral,
                    onClick = { onUpdateEffects { it.copy(isOverdrive = !it.isOverdrive) } }
                )
                SliderRow("Drive", effects.distortionDrive, accentColor = PhonkCoral, onValueChange = { v -> onUpdateEffects { it.copy(distortionDrive = v) } })
                SliderRow("Tone", effects.distortionTone, accentColor = PhonkCoral, onValueChange = { v -> onUpdateEffects { it.copy(distortionTone = v) } })
                SliderRow("Mix", effects.distortionMix, accentColor = PhonkCoral, onValueChange = { v -> onUpdateEffects { it.copy(distortionMix = v) } })
            }

            // 5. Lo-fi Tape & Vinyl
            EffectSectionCard("Lo-fi Tape & Vinyl", effects.tapeEnabled || effects.vinylEnabled, StudioAmber, { en ->
                onUpdateEffects { it.copy(tapeEnabled = en, vinylEnabled = en) }
            }) {
                SliderRow("Tape Wobble", effects.tapeWobble, accentColor = StudioAmber, onValueChange = { v -> onUpdateEffects { it.copy(tapeWobble = v) } })
                SliderRow("Tape Warmth", effects.tapeWarmth, accentColor = StudioAmber, onValueChange = { v -> onUpdateEffects { it.copy(tapeWarmth = v) } })
                SliderRow("Tape Hiss", effects.tapeHiss, accentColor = StudioAmber, onValueChange = { v -> onUpdateEffects { it.copy(tapeHiss = v) } })
                SliderRow("Vinyl Crackle", effects.vinylLevel, accentColor = StudioAmber, onValueChange = { v -> onUpdateEffects { it.copy(vinylLevel = v) } })
            }

            // 6. EQ (3-band Simple, 8-band Pro)
            EffectSectionCard(
                if (isProMode) "8-Band Pro EQ" else "3-Band EQ",
                effects.eqEnabled,
                accentColor,
                { en -> onUpdateEffects { it.copy(eqEnabled = en) } }
            ) {
                if (!isProMode) {
                    SliderRow("Bass (Low)", effects.eqLow, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(eqLow = v) } })
                    SliderRow("Mid", effects.eqMid, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(eqMid = v) } })
                    SliderRow("Treble (High)", effects.eqHigh, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(eqHigh = v) } })
                } else {
                    val labels = listOf("60Hz", "140Hz", "320Hz", "750Hz", "1.6k", "3.5k", "7.2k", "13k")
                    effects.eqEightBands.forEachIndexed { idx, v ->
                        SliderRow(
                            label = labels[idx],
                            value = v,
                            accentColor = StudioAmber,
                            onValueChange = { nv ->
                                val next = effects.eqEightBands.toMutableList()
                                next[idx] = nv
                                onUpdateEffects { it.copy(eqEightBands = next) }
                            }
                        )
                    }
                }
            }

            // 7. Filter (LP / HP / BP)
            EffectSectionCard("Filter (LP / HP / BP)", effects.filterEnabled, accentColor, { en ->
                onUpdateEffects { it.copy(filterEnabled = en) }
            }) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppChipButton("Low-Pass", effects.filterMode == 0, { onUpdateEffects { it.copy(filterMode = 0) } })
                    AppChipButton("High-Pass", effects.filterMode == 1, { onUpdateEffects { it.copy(filterMode = 1) } })
                    AppChipButton("Band-Pass", effects.filterMode == 2, { onUpdateEffects { it.copy(filterMode = 2) } })
                }
                SliderRow("Cutoff", effects.filterCutoff, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(filterCutoff = v) } })
                SliderRow("Resonance", effects.filterResonance, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(filterResonance = v) } })
            }

            // 8. Bitcrusher
            EffectSectionCard("Bitcrusher (Retro)", effects.bitcrusherEnabled, accentColor, { en ->
                onUpdateEffects { it.copy(bitcrusherEnabled = en) }
            }) {
                SliderRow("Bit Depth", effects.bitDepth, valueRange = 2f..16f, valueFormatter = { "${it.toInt()}-bit" }, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(bitDepth = v) } })
                SliderRow("Downsample", effects.downsampleFactor, valueRange = 1f..24f, valueFormatter = { "${it.toInt()}x" }, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(downsampleFactor = v) } })
                SliderRow("Mix", effects.bitcrusherMix, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(bitcrusherMix = v) } })
            }

            // 9. Compressor
            EffectSectionCard("Compressor", effects.compressorEnabled, accentColor, { en ->
                onUpdateEffects { it.copy(compressorEnabled = en) }
            }) {
                SliderRow("Threshold", effects.compThreshold, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(compThreshold = v) } })
                SliderRow("Ratio", effects.compRatio, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(compRatio = v) } })
                SliderRow("Makeup", effects.compMakeup, accentColor = accentColor, onValueChange = { v -> onUpdateEffects { it.copy(compMakeup = v) } })
            }

            // 10. Chorus / Phaser / Flanger / Stereo Width
            EffectSectionCard("Chorus, Phaser & Flanger", effects.chorusEnabled || effects.phaserEnabled || effects.flangerEnabled, accentColor, { en ->
                onUpdateEffects { it.copy(chorusEnabled = en) }
            }) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppChipButton("Chorus", effects.chorusEnabled, { onUpdateEffects { it.copy(chorusEnabled = !it.chorusEnabled) } })
                    AppChipButton("Phaser", effects.phaserEnabled, { onUpdateEffects { it.copy(phaserEnabled = !it.phaserEnabled) } })
                    AppChipButton("Flanger", effects.flangerEnabled, { onUpdateEffects { it.copy(flangerEnabled = !it.flangerEnabled) } })
                }
                SliderRow("Mod Mix", effects.chorusMix, accentColor = accentColor, onValueChange = { v ->
                    onUpdateEffects { it.copy(chorusMix = v, phaserMix = v, flangerMix = v) }
                })
                SliderRow("Stereo Width", effects.stereoWidth, valueRange = 0f..2f, accentColor = accentColor, onValueChange = { v ->
                    onUpdateEffects { it.copy(stereoWidth = v) }
                })
            }

            // 11. Pitch Shifter & Stutter Gate
            EffectSectionCard("Pitch Shifter & Stutter Gate", effects.pitchShiftEnabled || effects.stutterEnabled, PhonkCoral, { en ->
                onUpdateEffects { it.copy(pitchShiftEnabled = en) }
            }) {
                SliderRow("Pitch Semitones", effects.pitchSemitones, valueRange = -12f..12f, valueFormatter = { "${it.toInt()} st" }, accentColor = PhonkCoral, onValueChange = { v ->
                    onUpdateEffects { it.copy(pitchSemitones = v) }
                })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stutter Gate", fontWeight = FontWeight.Medium)
                    Switch(checked = effects.stutterEnabled, onCheckedChange = { en ->
                        onUpdateEffects { it.copy(stutterEnabled = en) }
                    })
                }
                if (effects.stutterEnabled) {
                    SliderRow("Gate Depth", effects.stutterDepth, accentColor = PhonkCoral, onValueChange = { v ->
                        onUpdateEffects { it.copy(stutterDepth = v) }
                    })
                }
            }
        }
    }
}

@Composable
private fun EffectSectionCard(
    title: String,
    enabled: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (enabled) accentColor else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (enabled) accentColor else MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("fx_toggle_$title")
                )
            }
            if (enabled) {
                content()
            }
        }
    }
}

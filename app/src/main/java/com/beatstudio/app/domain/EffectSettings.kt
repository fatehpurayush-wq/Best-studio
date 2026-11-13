package com.beatstudio.app.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ReverbType(val labelHinglish: String) {
    ROOM("Room"),
    HALL("Hall"),
    PLATE("Plate"),
    HUGE("Huge")
}

@Serializable
enum class EqBandCount {
    THREE_BAND,
    EIGHT_BAND
}

@Serializable
data class EffectSettings(
    // Reverb
    val reverbEnabled: Boolean = false,
    val reverbType: ReverbType = ReverbType.ROOM,
    val reverbMix: Float = 0.25f,
    val reverbSize: Float = 0.55f,
    val reverbDamp: Float = 0.4f,

    // Delay
    val delayEnabled: Boolean = false,
    val delayPingPong: Boolean = false,
    val delayTimeMs: Float = 320f, // 50..900
    val delayFeedback: Float = 0.35f,
    val delayMix: Float = 0.22f,

    // Distortion / Overdrive / Saturation
    val distortionEnabled: Boolean = false,
    val distortionDrive: Float = 0.35f, // 0..1
    val distortionTone: Float = 0.6f,
    val distortionMix: Float = 0.5f,
    val isOverdrive: Boolean = false,

    // Bitcrusher
    val bitcrusherEnabled: Boolean = false,
    val bitDepth: Float = 8f, // 2..16
    val downsampleFactor: Float = 4f, // 1..24
    val bitcrusherMix: Float = 0.5f,

    // Lo-fi Tape & Vinyl
    val tapeEnabled: Boolean = false,
    val tapeWobble: Float = 0.3f,
    val tapeHiss: Float = 0.15f,
    val tapeWarmth: Float = 0.5f,

    val vinylEnabled: Boolean = false,
    val vinylLevel: Float = 0.25f,
    val vinylDensity: Float = 0.4f,

    // Filters & EQ
    val filterEnabled: Boolean = false,
    val filterMode: Int = 0, // 0 = LP, 1 = HP, 2 = BP
    val filterCutoff: Float = 0.8f, // normalized 0..1 -> 40Hz..18kHz
    val filterResonance: Float = 0.2f,

    val eqEnabled: Boolean = false,
    val eqLow: Float = 0.5f, // 0..1 (0.5 = 0dB, -12dB..+12dB)
    val eqMid: Float = 0.5f,
    val eqHigh: Float = 0.5f,
    val eqEightBands: List<Float> = List(8) { 0.5f }, // Pro 8-band EQ

    // Dynamics & Sidechain
    val compressorEnabled: Boolean = false,
    val compThreshold: Float = 0.55f,
    val compRatio: Float = 0.4f,
    val compMakeup: Float = 0.3f,

    val sidechainEnabled: Boolean = false,
    val sidechainAmount: Float = 0.65f,
    val sidechainRelease: Float = 0.35f,

    // Modulation & Space
    val chorusEnabled: Boolean = false,
    val chorusDepth: Float = 0.4f,
    val chorusRate: Float = 0.35f,
    val chorusMix: Float = 0.35f,

    val phaserEnabled: Boolean = false,
    val phaserRate: Float = 0.3f,
    val phaserDepth: Float = 0.5f,
    val phaserMix: Float = 0.4f,

    val flangerEnabled: Boolean = false,
    val flangerRate: Float = 0.25f,
    val flangerFeedback: Float = 0.45f,
    val flangerMix: Float = 0.35f,

    val stereoWidth: Float = 1.0f, // 0f (mono)..2.0f (wide)

    // Pitch & Stutter
    val pitchShiftEnabled: Boolean = false,
    val pitchSemitones: Float = 0f, // -12..+12
    val pitchMix: Float = 1f,

    val stutterEnabled: Boolean = false,
    val stutterRateSteps: Int = 1, // 1 = 16th, 2 = 8th
    val stutterDepth: Float = 0.7f,

    // Vocal Pitch Snap (Auto-Tune style)
    val pitchSnapEnabled: Boolean = false,
    val pitchSnapStrength: Float = 0.85f
)

package com.example.aura.backend

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object WavSynthHelper {

    /**
     * Generates a high quality musical ambient synth track as a standard PCM WAV file.
     */
    fun createAmbientWavTrack(
        context: Context,
        fileName: String,
        baseFreq: Double,
        bpm: Int = 110,
        durationSeconds: Int = 120
    ): File {
        val audioDir = File(context.filesDir, "aura_cloud_storage/audio")
        if (!audioDir.exists()) audioDir.mkdirs()
        val file = File(audioDir, fileName)
        if (file.exists() && file.length() > 1000) {
            return file
        }

        val sampleRate = 44100
        val numChannels = 2
        val bitsPerSample = 16
        val totalSamples = sampleRate * durationSeconds
        val dataSize = totalSamples * numChannels * (bitsPerSample / 8)

        FileOutputStream(file).use { fos ->
            // 44-byte WAV header
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put("RIFF".toByteArray())
                putInt(36 + dataSize)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16) // Subchunk1Size for PCM
                putShort(1.toShort()) // AudioFormat = 1 (PCM)
                putShort(numChannels.toShort())
                putInt(sampleRate)
                putInt(sampleRate * numChannels * (bitsPerSample / 8)) // ByteRate
                putShort((numChannels * (bitsPerSample / 8)).toShort()) // BlockAlign
                putShort(bitsPerSample.toShort())
                put("data".toByteArray())
                putInt(dataSize)
            }
            fos.write(header.array())

            // Generate musical ambient chords & pulse beats
            val bufferSize = 8192
            val sampleBuffer = ByteBuffer.allocate(bufferSize).apply {
                order(ByteOrder.LITTLE_ENDIAN)
            }

            val chordIntervals = doubleArrayOf(1.0, 1.25, 1.5, 1.875) // Major 7th / harmonic
            var sampleIndex = 0

            while (sampleIndex < totalSamples) {
                sampleBuffer.clear()
                val chunkSamples = minOf(bufferSize / (numChannels * 2), totalSamples - sampleIndex)

                for (i in 0 until chunkSamples) {
                    val t = (sampleIndex + i).toDouble() / sampleRate

                    // Smooth ambient pad chord
                    var wave = 0.0
                    for (ratio in chordIntervals) {
                        val freq = baseFreq * ratio
                        wave += sin(2.0 * PI * freq * t) * 0.22
                        // Subtle shimmer overtone
                        wave += sin(2.0 * PI * (freq * 2.0) * t + 0.3) * 0.08
                    }

                    // Gentle rhythmic low-end pulse (Kick / 808 sub pulse at specified BPM)
                    val beatFreq = bpm / 60.0
                    val beatPhase = (t * beatFreq) % 1.0
                    if (beatPhase < 0.25) {
                        val decay = 1.0 - (beatPhase / 0.25)
                        val subFreq = 55.0 * (1.0 + decay * 0.5)
                        wave += sin(2.0 * PI * subFreq * t) * 0.35 * decay
                    }

                    // Stereo panning shimmer
                    val panL = 0.85 + 0.15 * sin(2.0 * PI * 0.15 * t)
                    val panR = 0.85 + 0.15 * sin(2.0 * PI * 0.15 * t + PI)

                    // Master volume envelope (fade in / fade out)
                    val envelope = when {
                        t < 2.0 -> t / 2.0
                        t > (durationSeconds - 2.0) -> (durationSeconds - t) / 2.0
                        else -> 1.0
                    }

                    val leftSample = (wave * panL * envelope * 24000.0).toInt().coerceIn(-32768, 32767).toShort()
                    val rightSample = (wave * panR * envelope * 24000.0).toInt().coerceIn(-32768, 32767).toShort()

                    sampleBuffer.putShort(leftSample)
                    sampleBuffer.putShort(rightSample)
                }

                fos.write(sampleBuffer.array(), 0, sampleBuffer.position())
                sampleIndex += chunkSamples
            }
        }

        return file
    }

    /**
     * Generates a dynamic hard-hitting Desi Hip-Hop / Drill rap instrumental track (138 BPM, 808 sub, sliding bass, trap hats).
     */
    fun createDrillRapTrack(
        context: Context,
        fileName: String,
        bpm: Int = 138,
        durationSeconds: Int = 165
    ): File {
        val audioDir = File(context.filesDir, "aura_cloud_storage/audio")
        if (!audioDir.exists()) audioDir.mkdirs()
        val file = File(audioDir, fileName)
        if (file.exists() && file.length() > 2000) {
            return file
        }

        val sampleRate = 44100
        val numChannels = 2
        val bitsPerSample = 16
        val totalSamples = sampleRate * durationSeconds
        val dataSize = totalSamples * numChannels * (bitsPerSample / 8)

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put("RIFF".toByteArray())
                putInt(36 + dataSize)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1.toShort())
                putShort(numChannels.toShort())
                putInt(sampleRate)
                putInt(sampleRate * numChannels * (bitsPerSample / 8))
                putShort((numChannels * (bitsPerSample / 8)).toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray())
                putInt(dataSize)
            }
            fos.write(header.array())

            val bufferSize = 8192
            val sampleBuffer = ByteBuffer.allocate(bufferSize).apply {
                order(ByteOrder.LITTLE_ENDIAN)
            }

            // Dark harmonic minor scale frequencies for dark Bombay drill
            // C minor / Eb / G / Bb / Ab
            val minorMelody = doubleArrayOf(130.81, 155.56, 174.61, 196.00, 207.65, 261.63)
            var sampleIndex = 0

            while (sampleIndex < totalSamples) {
                sampleBuffer.clear()
                val chunkSamples = minOf(bufferSize / (numChannels * 2), totalSamples - sampleIndex)

                for (i in 0 until chunkSamples) {
                    val t = (sampleIndex + i).toDouble() / sampleRate
                    val beatTime = t * (bpm / 60.0)
                    val beatIndex = beatTime.toInt()
                    val beatFraction = beatTime - beatIndex

                    var mix = 0.0

                    // 1. Sliding 808 Sub-bass with distortion / saturation
                    val subPattern = beatIndex % 4
                    val hasSub = (subPattern == 0 || subPattern == 2 || (subPattern == 3 && beatFraction > 0.5))
                    if (hasSub) {
                        val subAge = if (subPattern == 3 && beatFraction > 0.5) beatFraction - 0.5 else beatFraction
                        val subSlide = 48.0 - (subAge * 8.0) // 808 pitch glide down
                        val subWave = sin(2.0 * PI * subSlide * t)
                        // Soft clipping / saturation for punch
                        val saturated = Math.tanh(subWave * 2.2) * 0.45 * (1.0 - subAge * 0.4)
                        mix += saturated
                    }

                    // 2. Snare / Drill Clap on beat 3 (in 4/4 half-time drill feel)
                    val isSnareBeat = (beatIndex % 4 == 2)
                    if (isSnareBeat && beatFraction < 0.22) {
                        val snareDecay = 1.0 - (beatFraction / 0.22)
                        // White noise + tonal snap
                        val noise = (Math.random() * 2.0 - 1.0) * 0.35 * snareDecay
                        val snap = sin(2.0 * PI * 220.0 * t) * 0.25 * snareDecay
                        mix += (noise + snap)
                    }

                    // 3. Drill Hi-Hat Triplets / rolls
                    val tripletPhase = (beatTime * 3.0) % 1.0
                    if (tripletPhase < 0.12) {
                        val hatDecay = 1.0 - (tripletPhase / 0.12)
                        val hatNoise = (Math.random() * 2.0 - 1.0) * 0.12 * hatDecay
                        mix += hatNoise
                    }

                    // 4. Dark Gothic Minor Bell Melody (Arpeggiated)
                    val noteStep = (beatIndex / 2) % minorMelody.size
                    val bellFreq = minorMelody[noteStep] * 2.0
                    val bellPhase = (beatTime * 2.0) % 1.0
                    val bellDecay = Math.exp(-bellPhase * 3.5)
                    val bellSound = (sin(2.0 * PI * bellFreq * t) + 0.3 * sin(2.0 * PI * bellFreq * 2.0 * t)) * 0.18 * bellDecay
                    mix += bellSound

                    // Master envelope
                    val envelope = when {
                        t < 1.5 -> t / 1.5
                        t > (durationSeconds - 2.5) -> (durationSeconds - t) / 2.5
                        else -> 1.0
                    }

                    val pannedL = mix * 0.98 * envelope
                    val pannedR = mix * 1.02 * envelope

                    val left = (pannedL * 25000.0).toInt().coerceIn(-32768, 32767).toShort()
                    val right = (pannedR * 25000.0).toInt().coerceIn(-32768, 32767).toShort()

                    sampleBuffer.putShort(left)
                    sampleBuffer.putShort(right)
                }

                fos.write(sampleBuffer.array(), 0, sampleBuffer.position())
                sampleIndex += chunkSamples
            }
        }

        return file
    }
}

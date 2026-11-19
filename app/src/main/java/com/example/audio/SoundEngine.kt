package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundEffect {
  CLICK,
  SUMMON,
  HIT_LIGHT,
  SLAM_HEAVY,
  FIRE,
  KUNTI,
  HEAL,
  CHARM,
  COIN,
  LIGHTNING,
  LEVEL_UP,
  VICTORY,
  DEFEAT,
}

class SoundEngine(context: Context) {
  private val soundPool: SoundPool?
  private val soundIds = mutableMapOf<SoundEffect, Int>()
  @Volatile var isMuted: Boolean = false

  init {
    val pool =
      runCatching {
        SoundPool.Builder()
          .setMaxStreams(8)
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .build()
      }.getOrNull()
    soundPool = pool
    if (pool != null) {
      runCatching {
        val cacheDir = File(context.cacheDir, "sfx_v1").apply { mkdirs() }
        SoundEffect.entries.forEach { effect ->
          val wavFile = File(cacheDir, "${effect.name.lowercase()}.wav")
          if (!wavFile.exists()) {
            val pcm = synthesize(effect)
            writeWav(wavFile, pcm, SAMPLE_RATE)
          }
          soundIds[effect] = pool.load(wavFile.absolutePath, 1)
        }
      }
    }
  }

  fun play(effect: SoundEffect, volume: Float = 0.7f) {
    if (isMuted) return
    val id = soundIds[effect] ?: return
    runCatching {
      soundPool?.play(id, volume, volume, 1, 0, 1f)
    }
  }

  fun release() {
    runCatching { soundPool?.release() }
  }

  private fun synthesize(effect: SoundEffect): ShortArray {
    return when (effect) {
      SoundEffect.CLICK -> toneSweep(0.06f, 620f, 880f, 0.35f)
      SoundEffect.SUMMON -> toneSweep(0.18f, 240f, 580f, 0.5f)
      SoundEffect.HIT_LIGHT -> noiseBurstWithTone(0.10f, 210f, 95f, 0.45f)
      SoundEffect.SLAM_HEAVY -> noiseBurstWithTone(0.28f, 130f, 45f, 0.75f)
      SoundEffect.FIRE -> noiseBurstWithTone(0.22f, 360f, 180f, 0.45f)
      SoundEffect.KUNTI -> eerieWarble(0.32f, 920f, 1280f, 0.42f)
      SoundEffect.HEAL -> chord(0.35f, floatArrayOf(523.25f, 659.25f, 783.99f), 0.42f)
      SoundEffect.CHARM -> eerieWarble(0.28f, 660f, 990f, 0.38f)
      SoundEffect.COIN -> twoTone(0.18f, 987.77f, 1318.51f, 0.45f)
      SoundEffect.LIGHTNING -> noiseBurstWithTone(0.38f, 480f, 65f, 0.8f)
      SoundEffect.LEVEL_UP -> arpeggio(0.38f, floatArrayOf(440f, 554.37f, 659.25f, 880f), 0.5f)
      SoundEffect.VICTORY -> arpeggio(0.65f, floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f), 0.55f)
      SoundEffect.DEFEAT -> arpeggio(0.65f, floatArrayOf(392f, 349.23f, 293.66f, 220f), 0.55f)
    }
  }

  private fun toneSweep(durSec: Float, f0: Float, f1: Float, amp: Float): ShortArray {
    val n = (durSec * SAMPLE_RATE).toInt()
    val out = ShortArray(n)
    var phase = 0.0
    for (i in 0 until n) {
      val t = i.toFloat() / n
      val freq = f0 + (f1 - f0) * t
      phase += 2.0 * PI * freq / SAMPLE_RATE
      val env = exp(-t * 5.0).toFloat()
      out[i] = (sin(phase) * env * amp * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return out
  }

  private fun noiseBurstWithTone(durSec: Float, f0: Float, f1: Float, amp: Float): ShortArray {
    val n = (durSec * SAMPLE_RATE).toInt()
    val out = ShortArray(n)
    var phase = 0.0
    val rng = Random(42)
    for (i in 0 until n) {
      val t = i.toFloat() / n
      val freq = f0 + (f1 - f0) * t
      phase += 2.0 * PI * freq / SAMPLE_RATE
      val env = exp(-t * 7.5).toFloat()
      val sample = 0.65 * sin(phase) + 0.35 * (rng.nextDouble() * 2.0 - 1.0)
      out[i] = (sample * env * amp * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return out
  }

  private fun eerieWarble(durSec: Float, f0: Float, f1: Float, amp: Float): ShortArray {
    val n = (durSec * SAMPLE_RATE).toInt()
    val out = ShortArray(n)
    var phase = 0.0
    for (i in 0 until n) {
      val t = i.toFloat() / n
      val vibrato = sin(2.0 * PI * 14.0 * t * durSec) * 60.0
      val freq = f0 + (f1 - f0) * t + vibrato
      phase += 2.0 * PI * freq / SAMPLE_RATE
      val env = sin(PI * t) * exp(-t * 2.2)
      out[i] = (sin(phase) * env * amp * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return out
  }

  private fun twoTone(durSec: Float, f1: Float, f2: Float, amp: Float): ShortArray {
    val n = (durSec * SAMPLE_RATE).toInt()
    val out = ShortArray(n)
    val split = n / 3
    for (i in 0 until n) {
      val f = if (i < split) f1 else f2
      val localT = (if (i < split) i else i - split).toFloat() / (if (i < split) split else n - split)
      val env = exp(-localT * 4.0)
      val s = sin(2.0 * PI * f * i / SAMPLE_RATE) * env * amp
      out[i] = (s * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return out
  }

  private fun chord(durSec: Float, freqs: FloatArray, amp: Float): ShortArray {
    val n = (durSec * SAMPLE_RATE).toInt()
    val out = ShortArray(n)
    for (i in 0 until n) {
      val t = i.toFloat() / n
      val env = sin(PI * minOf(1f, t * 5f)) * exp(-t * 3.5)
      var sum = 0.0
      for (f in freqs) sum += sin(2.0 * PI * f * i / SAMPLE_RATE)
      out[i] = ((sum / freqs.size) * env * amp * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return out
  }

  private fun arpeggio(durSec: Float, notes: FloatArray, amp: Float): ShortArray {
    val n = (durSec * SAMPLE_RATE).toInt()
    val out = ShortArray(n)
    val seg = n / notes.size
    for (i in 0 until n) {
      val idx = (i / seg).coerceAtMost(notes.lastIndex)
      val localT = (i - idx * seg).toFloat() / seg.coerceAtLeast(1)
      val env = exp(-localT * 3.2)
      val s = sin(2.0 * PI * notes[idx] * i / SAMPLE_RATE) * env * amp
      out[i] = (s * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
    }
    return out
  }

  private fun writeWav(file: File, pcm: ShortArray, sampleRate: Int) {
    val dataBytes = pcm.size * 2
    val bos = ByteArrayOutputStream(44 + dataBytes)
    fun writeStr(s: String) = bos.write(s.toByteArray(Charsets.US_ASCII))
    fun writeIntLE(v: Int) {
      bos.write(v and 0xFF)
      bos.write((v shr 8) and 0xFF)
      bos.write((v shr 16) and 0xFF)
      bos.write((v shr 24) and 0xFF)
    }
    fun writeShortLE(v: Int) {
      bos.write(v and 0xFF)
      bos.write((v shr 8) and 0xFF)
    }
    writeStr("RIFF")
    writeIntLE(36 + dataBytes)
    writeStr("WAVE")
    writeStr("fmt ")
    writeIntLE(16)
    writeShortLE(1)
    writeShortLE(1)
    writeIntLE(sampleRate)
    writeIntLE(sampleRate * 2)
    writeShortLE(2)
    writeShortLE(16)
    writeStr("data")
    writeIntLE(dataBytes)
    for (s in pcm) writeShortLE(s.toInt())
    FileOutputStream(file).use { it.write(bos.toByteArray()) }
  }

  companion object {
    private const val SAMPLE_RATE = 22050
  }
}

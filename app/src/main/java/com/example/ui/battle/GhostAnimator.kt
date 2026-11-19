package com.example.ui.battle

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.game.BattleUnit
import com.example.model.GhostId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Multi-pose anatomical skeletal mesh animator using Android hardware-accelerated
 * Canvas.drawBitmapMesh (14x18 grid). Produces seamless posture transitions
 * across IDLE, MOVE/STRIDE, WINDUP (anticipation), STRIKE, and AIRBORNE_HIT
 * poses for all 14 Nusantara ghosts without rectangular cut seams.
 */
object GhostAnimator {
  private const val MESH_W = 14
  private const val MESH_H = 18
  private const val COLS = MESH_W + 1
  private const val ROWS = MESH_H + 1

  private val verts = FloatArray(COLS * ROWS * 2)
  private val paint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

  private val hitFlashFilter =
    PorterDuffColorFilter(Color.White.copy(alpha = 0.62f).toArgb(), PorterDuff.Mode.SRC_ATOP)
  private val slowTintFilter =
    PorterDuffColorFilter(Color(0xFFC084FC).copy(alpha = 0.30f).toArgb(), PorterDuff.Mode.SRC_ATOP)
  private val burnTintFilter =
    PorterDuffColorFilter(Color(0xFFFB923C).copy(alpha = 0.32f).toArgb(), PorterDuff.Mode.SRC_ATOP)
  private val charmTintFilter =
    PorterDuffColorFilter(Color(0xFFF472B6).copy(alpha = 0.28f).toArgb(), PorterDuff.Mode.SRC_ATOP)

  fun drawAnimatedGhost(
    canvas: Canvas,
    bitmap: Bitmap,
    u: BattleUnit,
    w: Float,
    h: Float,
    clockSec: Float,
  ) {
    val g = u.ghost
    val isAttacking = u.animTime >= 0f
    val impact = g.impactTime.coerceAtLeast(0.05f)
    val dur = g.animDuration.coerceAtLeast(impact + 0.05f)

    // Compute smooth pose keyframe weights
    var windup = 0f
    var strike = 0f
    if (isAttacking) {
      if (u.animTime < impact) {
        val t = (u.animTime / impact).coerceIn(0f, 1f)
        // Coil ramps up strongly before impact, begins snapping into strike at t > 0.85
        windup = sin(t * PI.toFloat() * 0.85f)
        if (t > 0.85f) {
          strike = ((t - 0.85f) / 0.15f) * 0.45f
        }
      } else {
        val rem = ((u.animTime - impact) / (dur - impact)).coerceIn(0f, 1f)
        strike = (1f - rem * rem).coerceIn(0f, 1f)
        windup = max(0f, 1f - rem * 4f) * 0.2f
      }
    }

    val moveWeight = if (!isAttacking && u.isMoving) 1f else if (isAttacking) 0.15f else 0f
    val idleBreath = sin(u.phase * 0.9f + clockSec * 1.4f)
    val stride = sin(u.phase * 1.85f)
    val strideAbs = abs(stride)
    val hitWeight = (u.airZ / 45f + abs(u.recoilTilt) * 1.8f).coerceIn(0f, 1f)

    var idx = 0
    for (row in 0 until ROWS) {
      val v = row.toFloat() / MESH_H // 0 = top, 1 = bottom feet
      for (col in 0 until COLS) {
        val uCoord = col.toFloat() / MESH_W // 0 = back/left, 1 = front/right
        var x = (uCoord - 0.5f) * w
        var y = (v - 1.0f) * h

        // Subtle universal idle breathing around chest
        val chest = band(v, 0.42f, 0.28f)
        x += (uCoord - 0.5f) * w * 0.032f * idleBreath * chest
        y -= (1f - v) * h * 0.018f * idleBreath

        when (g) {
          GhostId.BUTO_IJO -> {
            // Club + right/back arm lives in u < 0.40, v < 0.68
            val clubW = smoothstep(0.45f, 0.18f, uCoord) * smoothstep(0.70f, 0.25f, v)
            // Left/front fist arm lives in u > 0.70, v in 0.28..0.64
            val frontArmW = smoothstep(0.65f, 0.88f, uCoord) * band(v, 0.46f, 0.22f)
            // Upper torso & head
            val torsoW = smoothstep(0.68f, 0.28f, v)
            // Striding legs (left leg back/front split)
            val legSplit = (uCoord - 0.5f) * 2f // -1 left, +1 right
            val legW = smoothstep(0.58f, 0.94f, v)

            // WALK stride: heavy stomping legs + club sway
            if (moveWeight > 0f) {
              val legOffX = legSplit * stride * w * 0.10f * legW * moveWeight
              val legLiftY = max(0f, legSplit * stride) * h * 0.08f * legW * moveWeight
              x += legOffX
              y -= legLiftY
              x += stride * w * 0.03f * torsoW * moveWeight
              val p = rotOffset(x, y, -0.18f * w, -0.58f * h, -stride * 0.16f * moveWeight)
              x += p.first * clubW
              y += p.second * clubW
            }

            // WINDUP: coil waist back + hoist giant stone club overhead
            if (windup > 0f) {
              val tRot = rotOffset(x, y, 0f, -0.35f * h, -0.22f * windup)
              x += tRot.first * torsoW
              y += tRot.second * torsoW
              val cRot = rotOffset(x, y, -0.12f * w, -0.58f * h, -0.95f * windup)
              x += cRot.first * clubW
              y += cRot.second * clubW
              y += h * 0.05f * torsoW * windup // crouch
            }

            // STRIKE: violent forward twist + slam spiked club all the way forward & down
            if (strike > 0f) {
              val tRot = rotOffset(x, y, 0f, -0.35f * h, 0.26f * strike)
              x += tRot.first * torsoW
              y += tRot.second * torsoW
              val cRot = rotOffset(x, y, -0.05f * w, -0.55f * h, 1.48f * strike)
              x += cRot.first * clubW + w * 0.26f * clubW * strike
              y += cRot.second * clubW + h * 0.18f * clubW * strike
              x += w * 0.10f * frontArmW * strike
            }
          }

          GhostId.PALASIK -> {
            // Hooded speed assassin facing right; tattered cloak trails left (u < 0.45)
            val cloakW = smoothstep(0.55f, 0.10f, uCoord) * smoothstep(0.15f, 0.85f, v)
            val frontClawW = smoothstep(0.62f, 0.90f, uCoord) * band(v, 0.50f, 0.28f)
            val torsoW = smoothstep(0.65f, 0.22f, v)
            val legW = smoothstep(0.60f, 0.95f, v)
            val legSplit = (uCoord - 0.5f) * 2f

            // Cloak always ripples dramatically backward
            x -= cloakW * (w * 0.08f * moveWeight + sin(u.phase * 2.6f - v * 4.5f) * w * 0.06f)
            y += cloakW * cos(u.phase * 2.2f - uCoord * 3f) * h * 0.03f

            if (moveWeight > 0f) {
              // Aggressive hunter sprint forward
              val lean = rotOffset(x, y, 0f, -0.3f * h, 0.24f * moveWeight)
              x += lean.first * torsoW + legSplit * stride * w * 0.13f * legW * moveWeight
              y += lean.second * torsoW - max(0f, legSplit * stride) * h * 0.09f * legW * moveWeight
            }
            if (windup > 0f) {
              // Panther-like crouch coil back
              y += h * 0.12f * torsoW * windup
              x -= w * 0.10f * torsoW * windup
              x -= w * 0.12f * frontClawW * windup
            }
            if (strike > 0f) {
              // Explosive claw slash lunge forward
              val lunge = rotOffset(x, y, 0f, -0.35f * h, 0.34f * strike)
              x += lunge.first * torsoW + w * 0.30f * frontClawW * strike
              y += lunge.second * torsoW - h * 0.08f * frontClawW * strike
            }
          }

          GhostId.BARONG -> {
            // Four-legged lion spirit facing right; mask/chest u > 0.55, legs v > 0.68, tail/flags u < 0.38
            val tailW = smoothstep(0.42f, 0.08f, uCoord) * smoothstep(0.60f, 0.18f, v)
            val headMaskW = smoothstep(0.55f, 0.85f, uCoord) * smoothstep(0.65f, 0.25f, v)
            val legW = smoothstep(0.66f, 0.96f, v)
            val frontBackSplit = if (uCoord > 0.5f) 1f else -1f

            // Tail flags & mane flutter cleanly
            x -= tailW * sin(u.phase * 2.0f + v * 3f) * w * 0.055f

            if (moveWeight > 0f) {
              x += frontBackSplit * stride * w * 0.09f * legW * moveWeight
              y -= max(0f, frontBackSplit * stride) * h * 0.10f * legW * moveWeight
              y -= strideAbs * h * 0.03f * headMaskW * moveWeight
            }
            if (windup > 0f) {
              // Rear up chest & mask before pounce
              val rear = rotOffset(x, y, -0.15f * w, -0.25f * h, -0.24f * windup)
              x += rear.first * (headMaskW + 0.4f * (1f - legW))
              y += rear.second * (headMaskW + 0.4f * (1f - legW))
            }
            if (strike > 0f) {
              // Forward lion pounce & jaw bite
              val pounce = rotOffset(x, y, 0f, -0.28f * h, 0.26f * strike)
              x += pounce.first * (1f - legW * 0.6f) + w * 0.14f * headMaskW * strike
              y += pounce.second * (1f - legW * 0.6f) + h * 0.06f * headMaskW * strike
            }
          }

          GhostId.RANGDA -> {
            // Levitating witch queen facing right; wild hair & skirt trail u < 0.45, lightning claws u > 0.65
            val hairSkirtW = smoothstep(0.52f, 0.12f, uCoord) * smoothstep(0.12f, 0.82f, v)
            val clawArmsW = smoothstep(0.62f, 0.92f, uCoord) * band(v, 0.36f, 0.20f)
            val feetW = smoothstep(0.75f, 0.96f, v)

            x -= hairSkirtW * (sin(u.phase * 1.9f + v * 4f) * w * 0.06f + w * 0.04f * moveWeight)
            y += feetW * sin(u.phase * 1.3f) * h * 0.04f

            if (windup > 0f) {
              // Raise both taloned hands skyward gathering dark lightning
              val armLift = rotOffset(x, y, 0.08f * w, -0.62f * h, -0.58f * windup)
              x += armLift.first * clawArmsW - w * 0.05f * windup
              y += armLift.second * clawArmsW - h * 0.06f * windup
            }
            if (strike > 0f) {
              // Thrust lightning claws sharply forward
              x += w * 0.26f * clawArmsW * strike
              y += h * 0.04f * clawArmsW * strike
            }
          }

          GhostId.NYI_BLORONG -> {
            // Upright royal torso v < 0.50; emerald serpent coils v > 0.50; staff u > 0.65
            val tailW = smoothstep(0.46f, 0.88f, v)
            val upperW = smoothstep(0.52f, 0.18f, v)
            val staffArmW = smoothstep(0.62f, 0.90f, uCoord) * band(v, 0.36f, 0.22f)

            // Propagating serpent S-wave across coiled tail
            val waveSpeed = if (moveWeight > 0f) 2.4f else 1.2f
            val slither = sin(u.phase * waveSpeed - v * 7.5f) * w * (0.045f + 0.04f * moveWeight)
            x += slither * tailW
            y += cos(u.phase * waveSpeed - uCoord * 5f) * h * 0.02f * tailW

            if (windup > 0f) {
              val back = rotOffset(x, y, 0f, -0.45f * h, -0.18f * windup)
              x += back.first * upperW
              y += back.second * upperW
              val sLift = rotOffset(x, y, 0.16f * w, -0.62f * h, -0.45f * windup)
              x += sLift.first * staffArmW
              y += sLift.second * staffArmW
            }
            if (strike > 0f) {
              val lunge = rotOffset(x, y, 0f, -0.45f * h, 0.22f * strike)
              x += lunge.first * upperW + w * 0.20f * staffArmW * strike
              y += lunge.second * upperW + h * 0.05f * staffArmW * strike
            }
          }

          GhostId.TUYUL -> {
            val leftArmW = smoothstep(0.36f, 0.10f, uCoord) * band(v, 0.56f, 0.24f)
            val sackArmW = smoothstep(0.66f, 0.92f, uCoord) * band(v, 0.55f, 0.28f)
            val legW = smoothstep(0.68f, 0.96f, v)
            val split = (uCoord - 0.5f) * 2f
            if (moveWeight > 0f) {
              x += split * stride * w * 0.09f * legW * moveWeight
              y -= max(0f, split * stride) * h * 0.09f * legW * moveWeight
              y += stride * h * 0.05f * sackArmW * moveWeight
            }
            if (windup > 0f) {
              y += h * 0.12f * (1f - legW) * windup
              x -= w * 0.14f * leftArmW * windup
            }
            if (strike > 0f) {
              x += w * 0.28f * (leftArmW + sackArmW) * strike
              y -= h * 0.10f * leftArmW * strike
            }
          }

          GhostId.POCONG -> {
            // Vertical wrapped shroud: squash base on windup/landing, stretch & arc forward on leap
            val headPin = smoothstep(0.32f, 0.65f, v)
            if (moveWeight > 0f) {
              val bounce = sin(u.phase * 1.1f)
              y += (1f - v) * h * 0.08f * bounce * moveWeight
              x += (1f - v) * w * 0.08f * cos(u.phase * 1.1f) * moveWeight
            }
            if (windup > 0f) {
              // Deep spring crouch before flying slam
              y += (1f - v) * h * 0.18f * windup
              x += (uCoord - 0.5f) * w * 0.16f * headPin * windup
            }
            if (strike > 0f) {
              // Stretched headbutt lunge
              val arch = rotOffset(x, y, 0f, -0.25f * h, 0.42f * strike)
              x += arch.first
              y += arch.second - (1f - v) * h * 0.08f * strike
            }
          }

          GhostId.GENDRUWO -> {
            val lArmW = smoothstep(0.32f, 0.08f, uCoord) * band(v, 0.46f, 0.28f)
            val rArmW = smoothstep(0.68f, 0.92f, uCoord) * band(v, 0.46f, 0.28f)
            val legW = smoothstep(0.68f, 0.96f, v)
            val split = (uCoord - 0.5f) * 2f

            if (moveWeight > 0f) {
              x += split * stride * w * 0.09f * legW * moveWeight
              y -= max(0f, split * stride) * h * 0.08f * legW * moveWeight
              y += stride * h * 0.06f * (lArmW - rArmW) * moveWeight
            }
            if (windup > 0f) {
              // Raise BOTH massive arms overhead
              val lR = rotOffset(x, y, -0.22f * w, -0.72f * h, 1.35f * windup)
              val rR = rotOffset(x, y, 0.22f * w, -0.72f * h, -1.35f * windup)
              x += lR.first * lArmW + rR.first * rArmW
              y += lR.second * lArmW + rR.second * rArmW - h * 0.08f * windup
            }
            if (strike > 0f) {
              // Earth-shaking two-fisted slam forward & down
              x += w * 0.16f * (lArmW + rArmW) * strike
              y += h * 0.22f * (lArmW + rArmW) * strike
            }
          }

          GhostId.LEAK -> {
            val lArmW = smoothstep(0.28f, 0.06f, uCoord) * band(v, 0.52f, 0.24f)
            val rArmW = smoothstep(0.72f, 0.94f, uCoord) * band(v, 0.52f, 0.24f)
            val tongueW = band(uCoord, 0.52f, 0.16f) * smoothstep(0.42f, 0.85f, v)
            if (windup > 0f) {
              x -= w * 0.10f * (lArmW + rArmW) * windup
              y -= h * 0.05f * windup
            }
            if (strike > 0f) {
              // Tongue darts violently toward enemy + claws flare
              x += w * 0.36f * tongueW * strike
              y += h * 0.06f * tongueW * strike
              x += w * 0.18f * rArmW * strike - w * 0.12f * lArmW * strike
            }
          }

          GhostId.WEWE -> {
            val lArmW = smoothstep(0.34f, 0.08f, uCoord) * band(v, 0.44f, 0.20f)
            val rArmW = smoothstep(0.66f, 0.92f, uCoord) * band(v, 0.44f, 0.24f)
            val skirtW = smoothstep(0.55f, 0.95f, v)
            x += sin(u.phase * 1.4f - v * 4f) * w * 0.045f * skirtW
            if (windup > 0f) {
              y -= h * 0.12f * (lArmW + rArmW) * windup
            }
            if (strike > 0f) {
              x += w * 0.22f * rArmW * strike - w * 0.14f * lArmW * strike
              y -= h * 0.06f * (lArmW + rArmW) * strike
            }
          }

          GhostId.KUNTI, GhostId.SUNDEL -> {
            // Keep head/face (v < 0.34) crisp and undistorted; billow dress & hair gracefully
            val lowerW = smoothstep(0.36f, 0.92f, v)
            val swayFreq = if (moveWeight > 0f) 2.2f else 1.2f
            x -= lowerW * (w * 0.07f * moveWeight + sin(u.phase * swayFreq - v * 5f) * w * 0.055f)
            if (windup > 0f) {
              val lean = rotOffset(x, y, 0f, -0.35f * h, -0.16f * windup)
              x += lean.first * (1f - lowerW * 0.5f)
              y += lean.second * (1f - lowerW * 0.5f)
            }
            if (strike > 0f) {
              val lunge = rotOffset(x, y, 0f, -0.35f * h, 0.24f * strike)
              x += lunge.first + w * 0.10f * (1f - lowerW) * strike
              y += lunge.second
            }
          }

          GhostId.KUYANG -> {
            // Head top (v < 0.42) tilts forward; hanging tendrils (v > 0.44) whip in fluid sine wave
            val tendrilW = smoothstep(0.40f, 0.88f, v)
            x -= tendrilW * (w * 0.10f * moveWeight + sin(u.phase * 2.8f - v * 6.5f) * w * 0.08f)
            if (windup > 0f) {
              x -= w * 0.09f * (1f - tendrilW) * windup
            }
            if (strike > 0f) {
              x += w * 0.22f * (1f - tendrilW) * strike
              x += (uCoord - 0.5f) * w * 0.18f * tendrilW * strike
            }
          }

          GhostId.API -> {
            // Flame tips rise/flicker dynamically; flare outward on fire cast
            val flameTop = smoothstep(0.45f, 0.05f, v)
            val tailBottom = smoothstep(0.48f, 0.94f, v)
            x += sin(u.phase * 4.2f - v * 6f) * w * 0.06f * (flameTop + tailBottom * 0.7f)
            if (windup > 0f) {
              x *= (1f - 0.16f * windup)
              y *= (1f - 0.10f * windup)
            }
            if (strike > 0f) {
              x *= (1f + 0.22f * strike)
              x += w * 0.18f * flameTop * strike
            }
          }
        }

        // Universal airborne knockback curl
        if (hitWeight > 0.02f) {
          val curl = rotOffset(x, y, 0f, -0.5f * h, -0.25f * hitWeight * (1f - v))
          x += curl.first
          y += curl.second
        }

        verts[idx++] = x
        verts[idx++] = y
      }
    }

    paint.colorFilter =
      when {
        u.hitFlash > 0f -> hitFlashFilter
        u.burnTimer > 0f -> burnTintFilter
        u.charmTimer > 0f -> charmTintFilter
        u.slowTimer > 0f -> slowTintFilter
        else -> null
      }

    canvas.drawBitmapMesh(
      bitmap,
      MESH_W,
      MESH_H,
      verts,
      0,
      null,
      0,
      paint,
    )
  }

  private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
  }

  private fun band(v: Float, center: Float, halfWidth: Float): Float {
    val d = abs(v - center) / halfWidth
    if (d >= 1f) return 0f
    val u = 1f - d
    return u * u * (3f - 2f * u)
  }

  private fun rotOffset(
    x: Float,
    y: Float,
    px: Float,
    py: Float,
    rad: Float,
  ): Pair<Float, Float> {
    val dx = x - px
    val dy = y - py
    val c = cos(rad)
    val s = sin(rad)
    val nx = px + dx * c - dy * s
    val ny = py + dx * s + dy * c
    return (nx - x) to (ny - y)
  }
}

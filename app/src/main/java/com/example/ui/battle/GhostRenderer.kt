package com.example.ui.battle

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.GameAssets
import com.example.game.BattleEngine
import com.example.game.BattleUnit
import com.example.game.ProjectileKind
import com.example.game.TeamSide
import com.example.model.GhostId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

object GhostRenderer {
  private val calloutPaint =
    Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

  private val labelPaint =
    Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.LEFT
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

  private var smoothCamX = 0f

  fun DrawScope.renderBattlefield(
    engine: BattleEngine,
    assets: GameAssets,
    clockSec: Float,
  ) {
    val scaleX = size.width / BattleEngine.WORLD_W
    val scaleY = size.height / BattleEngine.WORLD_H
    val shakeX =
      if (engine.shakeIntensity > 0f) (Random.nextFloat() * 2f - 1f) * engine.shakeIntensity * 14f else 0f
    val shakeY =
      if (engine.shakeIntensity > 0f) (Random.nextFloat() * 2f - 1f) * engine.shakeIntensity * 14f else 0f

    // Subtle dynamic camera pan toward active frontline for 3D parallax separation
    val targetCam =
      if (engine.units.isEmpty()) 0f
      else {
        val avgX = engine.units.map { it.x }.average().toFloat()
        ((avgX - BattleEngine.WORLD_W * 0.5f) / (BattleEngine.WORLD_W * 0.5f) * -14f).coerceIn(-14f, 14f)
      }
    smoothCamX += (targetCam - smoothCamX) * 0.08f

    withTransform({
      scale(scaleX, scaleY, pivot = Offset.Zero)
      translate(shakeX, shakeY)
    }) {
      // LAYER 1: High-Res 3D Stage Background with 0.35x Parallax Overscan
      val bgBitmap = assets.backgrounds[engine.config.bgIndex]
      if (bgBitmap != null) {
        drawImage(
          image = bgBitmap,
          dstOffset = IntOffset((-24f + smoothCamX * 0.35f).roundToInt(), -10),
          dstSize = IntSize((BattleEngine.WORLD_W + 48f).roundToInt(), (BattleEngine.WORLD_H + 20f).roundToInt()),
          filterQuality = FilterQuality.High,
        )
      } else {
        drawRect(
          brush = Brush.verticalGradient(listOf(Color(0xFF0A051A), Color(0xFF23113A), Color(0xFF0D0819))),
          size = Size(BattleEngine.WORLD_W, BattleEngine.WORLD_H),
        )
      }

      // Volumetric angled moonlight / astral shafts + atmospheric stage mood
      drawAtmosphericLightShafts(engine.config.tint, clockSec, smoothCamX)

      // LAYER 2: Ground depth blend, stage-themed ambient motes & midground fog
      drawRect(
        brush =
          Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xFF070412).copy(alpha = 0.35f)),
            startY = BattleEngine.GROUND_Y - 18f,
            endY = BattleEngine.WORLD_H,
          ),
        topLeft = Offset(-30f, BattleEngine.GROUND_Y - 18f),
        size = Size(BattleEngine.WORLD_W + 60f, BattleEngine.WORLD_H - BattleEngine.GROUND_Y + 30f),
      )

      val moteColor = stageMoteColor(engine.config.bgIndex, engine.config.tint)
      for (m in engine.ambientMotes) {
        val glow = 0.28f + 0.52f * (0.5f + 0.5f * sin(clockSec * 2.6f + m.phase))
        drawCircle(
          color = moteColor.copy(alpha = glow * 0.26f),
          radius = m.size * 2.6f,
          center = Offset(m.x + smoothCamX * 0.55f, m.y),
        )
        drawCircle(
          color = moteColor.copy(alpha = glow),
          radius = m.size,
          center = Offset(m.x + smoothCamX * 0.55f, m.y),
        )
      }

      val mistShift = engine.elapsedTime + clockSec * 0.25f
      for (i in 0 until 6) {
        val mx = ((i * 175f + mistShift * 16f) % (BattleEngine.WORLD_W + 320f)) - 160f
        drawOval(
          color = engine.config.tint.copy(alpha = 0.08f),
          topLeft = Offset(mx - 140f + smoothCamX * 0.6f, BattleEngine.GROUND_Y - 6f + (i % 3) * 12f),
          size = Size(280f, 34f),
        )
      }

      // LAYER 3: Realistic 3D Player Pendopo & Enemy Obsidian Citadel Bases (1.0x Plane)
      withTransform({ translate(smoothCamX, 0f) }) {
        drawRealisticBase(
          bitmap = assets.basePlayer,
          x = BattleEngine.PLAYER_BASE_X + 18f,
          isPlayer = true,
          hpRatio = (engine.playerHp / engine.playerMaxHp).coerceIn(0f, 1f),
          flash = engine.playerBaseFlash > 0f,
          clockSec = clockSec,
        )
        drawRealisticBase(
          bitmap = assets.baseEnemy,
          x = BattleEngine.ENEMY_BASE_X - 18f,
          isPlayer = false,
          hpRatio = (engine.enemyHp / engine.enemyMaxHp).coerceIn(0f, 1f),
          flash = engine.enemyBaseFlash > 0f,
          clockSec = clockSec,
        )

        // Seismic ground cracks under heavy slams
        for (gc in engine.groundCracks) {
          val alpha = (gc.life / gc.maxLife).coerceIn(0f, 1f)
          for (arm in 0 until 5) {
            val ang = -0.7f + arm * 0.35f
            val r = gc.radius * (0.65f + (arm % 2) * 0.35f)
            val ex = gc.x + cos(ang) * r * (if (arm % 2 == 0) 1f else -1f)
            val ey = gc.y + sin(ang) * 14f
            drawLine(
              color = gc.color.copy(alpha = alpha * 0.78f),
              start = Offset(gc.x, gc.y),
              end = Offset((gc.x + ex) * 0.5f, ey - 4f),
              strokeWidth = 3f,
              cap = StrokeCap.Round,
            )
            drawLine(
              color = Color.Black.copy(alpha = alpha * 0.65f),
              start = Offset((gc.x + ex) * 0.5f, ey - 4f),
              end = Offset(ex, ey),
              strokeWidth = 2f,
              cap = StrokeCap.Round,
            )
          }
        }

        // Spectral afterimage trails behind dashing/charging ghosts
        for (ai in engine.afterimages) {
          val sp = assets.sprites[ai.ghost] ?: continue
          val alpha = (ai.life / ai.maxLife).coerceIn(0f, 1f) * 0.45f
          val hh = ai.ghost.spriteHeight
          val w = hh * sp.width.toFloat() / sp.height.toFloat()
          withTransform({
            translate(ai.x, ai.y + (if (ai.ghost.isFlying) -10f else 0f))
            scale(ai.facing, 1f, pivot = Offset.Zero)
            rotate(degrees = ai.rotDeg, pivot = Offset.Zero)
          }) {
            drawImage(
              image = sp,
              dstOffset = IntOffset((-w / 2f).roundToInt(), (-hh).roundToInt()),
              dstSize = IntSize(w.roundToInt(), hh.roundToInt()),
              alpha = alpha,
              colorFilter = ColorFilter.tint(ai.tint, BlendMode.SrcAtop),
              filterQuality = FilterQuality.Medium,
            )
          }
        }

        // LAYER 4: Units sorted by Y depth using Multi-Pose Skeletal Mesh Animator
        val sorted = engine.units.sortedBy { it.y }
        for (u in sorted) {
          drawUnit(u, assets, engine.kerisBuff > 0f, clockSec)
        }

        // Combat Slash Arcs, Projectiles, Beams, Lightning, Particles, Rings & Callouts
        drawSlashArcs(engine)
        drawProjectiles(engine)
        drawBeamsAndBolts(engine)

        for (p in engine.particles) {
          val a = (p.life / p.maxLife).coerceIn(0f, 1f)
          drawCircle(
            color = p.color.copy(alpha = a),
            radius = p.radius,
            center = Offset(p.x, p.y),
          )
        }

        for (r in engine.rings) {
          val a = r.life.coerceIn(0f, 1f)
          val stroke = max(1.5f, 4.5f * a)
          if (r.dir != 0) {
            val startAngle = if (r.dir > 0) -46f else 134f
            drawArc(
              color = r.color.copy(alpha = a),
              startAngle = startAngle,
              sweepAngle = 92f,
              useCenter = false,
              topLeft = Offset(r.x - r.radius, r.y - r.radius),
              size = Size(r.radius * 2f, r.radius * 2f),
              style = Stroke(width = stroke),
            )
          } else {
            drawCircle(
              color = r.color.copy(alpha = a),
              radius = r.radius,
              center = Offset(r.x, r.y),
              style = Stroke(width = stroke),
            )
          }
        }

        for (q in engine.callouts) {
          val alpha = min(1f, q.life * 2f).coerceIn(0f, 1f)
          drawContext.canvas.nativeCanvas.apply {
            calloutPaint.textSize = 16f
            calloutPaint.style = Paint.Style.STROKE
            calloutPaint.strokeWidth = 4f
            calloutPaint.color = Color(0xFF0A061E).copy(alpha = alpha * 0.9f).toArgb()
            drawText(q.text, q.x, q.y, calloutPaint)

            calloutPaint.style = Paint.Style.FILL
            calloutPaint.color = q.color.copy(alpha = alpha).toArgb()
            drawText(q.text, q.x, q.y, calloutPaint)
          }
        }
      }

      // LAYER 5: Foreground Parallax Depth (Low Ground Mist + Alang-Alang & Stones + Embers)
      drawForegroundParallaxLayer(
        fgBitmap = assets.foregroundParallax,
        tint = engine.config.tint,
        moteColor = moteColor,
        camX = smoothCamX,
        shake = engine.shakeIntensity,
        clockSec = clockSec,
      )

      // Subtle atmospheric dark vignette around arena frame
      drawRect(
        brush =
          Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.34f)),
            center = Offset(BattleEngine.WORLD_W * 0.5f, BattleEngine.WORLD_H * 0.45f),
            radius = BattleEngine.WORLD_W * 0.65f,
          ),
        size = Size(BattleEngine.WORLD_W, BattleEngine.WORLD_H),
      )

      // Crisp top-left stage badge
      drawRoundRect(
        color = Color(0xFF090518).copy(alpha = 0.68f),
        topLeft = Offset(10f, 8f),
        size = Size(235f, 24f),
        cornerRadius = CornerRadius(12f, 12f),
      )
      drawContext.canvas.nativeCanvas.apply {
        labelPaint.textSize = 12.5f
        labelPaint.color = Color(0xFFF3E8FF).toArgb()
        drawText(engine.config.label, 20f, 24f, labelPaint)
      }
    }
  }

  private fun stageMoteColor(bgIndex: Int, tint: Color): Color =
    when (bgIndex) {
      1, 7 -> Color(0xFF34D399)
      6, 8 -> Color(0xFFFB923C)
      9 -> Color(0xFFC084FC)
      else -> Color(0xFFFDE68A).copy(alpha = 0.95f)
    }

  fun DrawScope.drawPuraGelapBackground(tint: Color, clockSec: Float) {
    drawRect(
      brush = Brush.verticalGradient(listOf(Color(0xFF090518), Color(0xFF280E3E), Color(0xFF4A154B))),
      size = Size(BattleEngine.WORLD_W, BattleEngine.WORLD_H),
    )
    val moonCenter = Offset(620f, 92f)
    drawCircle(
      brush = Brush.radialGradient(listOf(tint.copy(alpha = 0.55f), Color.Transparent), moonCenter, 125f),
      radius = 125f,
      center = moonCenter,
    )
    drawCircle(color = Color(0xFFFDE68A), radius = 34f, center = moonCenter)
    drawRect(
      brush =
        Brush.verticalGradient(
          colors = listOf(Color(0xFF2D174A), Color(0xFF0D071B)),
          startY = BattleEngine.GROUND_Y - 14f,
          endY = BattleEngine.WORLD_H,
        ),
      topLeft = Offset(0f, BattleEngine.GROUND_Y - 14f),
      size = Size(BattleEngine.WORLD_W, BattleEngine.WORLD_H - BattleEngine.GROUND_Y + 14f),
    )
  }

  private fun DrawScope.drawAtmosphericLightShafts(tint: Color, clockSec: Float, camX: Float) {
    val pulse = 0.75f + 0.25f * sin(clockSec * 1.1f)
    val beamColors = listOf(tint.copy(alpha = 0.11f * pulse), Color.Transparent)
    val origins = listOf(180f, 430f, 640f)
    origins.forEachIndexed { idx, ox ->
      val shift = camX * 0.45f + sin(clockSec * 0.6f + idx) * 8f
      val path =
        Path().apply {
          moveTo(ox - 24f + shift, 0f)
          lineTo(ox + 34f + shift, 0f)
          lineTo(ox + 125f + shift, BattleEngine.GROUND_Y + 25f)
          lineTo(ox + 20f + shift, BattleEngine.GROUND_Y + 25f)
          close()
        }
      drawPath(
        path = path,
        brush = Brush.verticalGradient(beamColors, startY = 0f, endY = BattleEngine.GROUND_Y + 25f),
      )
    }
  }

  private fun DrawScope.drawRealisticBase(
    bitmap: ImageBitmap?,
    x: Float,
    isPlayer: Boolean,
    hpRatio: Float,
    flash: Boolean,
    clockSec: Float,
  ) {
    val jitter = if (flash) (Random.nextFloat() * 4f - 2f) else 0f
    val bx = x + jitter
    val by = BattleEngine.GROUND_Y + 24f
    val accent = if (isPlayer) Color(0xFFA78BFA) else Color(0xFFFB7185)

    // Wide realistic ground contact shadow under fortress
    drawOval(
      color = Color.Black.copy(alpha = 0.56f),
      topLeft = Offset(bx - 64f, by - 11f),
      size = Size(128f, 22f),
    )

    // Ambient mystic glow behind base
    val glowPulse = 0.8f + 0.2f * sin(clockSec * 3.4f)
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(accent.copy(alpha = 0.36f * glowPulse), Color.Transparent),
          center = Offset(bx, by - 78f),
          radius = 112f,
        ),
      radius = 112f,
      center = Offset(bx, by - 78f),
    )

    if (bitmap != null) {
      val drawH = 166f
      val drawW = drawH * bitmap.width.toFloat() / bitmap.height.toFloat()
      val flashFilter =
        if (flash) ColorFilter.tint(Color.White.copy(alpha = 0.65f), BlendMode.SrcAtop) else null

      drawImage(
        image = bitmap,
        dstOffset = IntOffset((bx - drawW / 2f).roundToInt(), (by - drawH).roundToInt()),
        dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
        colorFilter = flashFilter,
        filterQuality = FilterQuality.High,
      )

      // Animated spirit sparks / hellfire braziers in front of the 3D fortress
      repeat(3) { i ->
        val t = (clockSec * 0.9f + i * 0.33f) % 1f
        val sx = bx + sin(clockSec * 3f + i * 2.1f) * 16f + (if (isPlayer) 8f else -8f)
        val sy = by - 36f - t * 65f
        val sparkColor = if (isPlayer) Color(0xFFD8B4FE) else Color(0xFFFB7185)
        drawCircle(
          color = sparkColor.copy(alpha = (1f - t) * 0.8f),
          radius = 2.6f * (1f - t * 0.4f),
          center = Offset(sx, sy),
        )
      }

      // Smoke/damage embers when base is heavily damaged (< 55% HP)
      if (hpRatio < 0.55f) {
        val dmgRatio = (0.55f - hpRatio) / 0.55f
        repeat(3) { i ->
          val t = (clockSec * 1.4f + i * 0.37f) % 1f
          val sx = bx + (i - 1) * 18f + sin(clockSec * 4f + i) * 6f
          val sy = by - 65f - t * 54f
          drawCircle(
            color = Color(0xFFFB923C).copy(alpha = (1f - t) * 0.55f * dmgRatio),
            radius = 4.5f + t * 5f,
            center = Offset(sx, sy),
          )
        }
      }
    } else {
      // Fallback vector tower
      val bodyColor = if (flash) Color.White else if (isPlayer) Color(0xFF2C2258) else Color(0xFF3E1426)
      drawRoundRect(
        color = bodyColor,
        topLeft = Offset(bx - 36f, by - 96f),
        size = Size(72f, 96f),
        cornerRadius = CornerRadius(4f, 4f),
      )
    }

    // Crisp dark-stone & metallic HP bar floating above fortress roof
    val barTop = by - 180f
    drawRoundRect(
      color = Color(0xFF0A0618).copy(alpha = 0.85f),
      topLeft = Offset(bx - 42f, barTop),
      size = Size(84f, 11f),
      cornerRadius = CornerRadius(5.5f, 5.5f),
    )
    drawRoundRect(
      color = accent.copy(alpha = 0.65f),
      topLeft = Offset(bx - 42f, barTop),
      size = Size(84f, 11f),
      cornerRadius = CornerRadius(5.5f, 5.5f),
      style = Stroke(width = 1.2f),
    )
    val hpColor =
      when {
        hpRatio > 0.5f -> Color(0xFF34D399)
        hpRatio > 0.25f -> Color(0xFFFBBF24)
        else -> Color(0xFFF43F5E)
      }
    drawRoundRect(
      color = hpColor,
      topLeft = Offset(bx - 40f, barTop + 2f),
      size = Size(80f * hpRatio, 7f),
      cornerRadius = CornerRadius(3.5f, 3.5f),
    )
  }

  fun DrawScope.drawUnit(
    u: BattleUnit,
    assets: GameAssets,
    kerisActive: Boolean,
    clockSec: Float,
  ) {
    val g = u.ghost
    val flyingOffset = if (g.isFlying) -10f else 0f
    val shadowScale = (1f - u.airZ / 135f).coerceIn(0.42f, 1f)

    // Height-responsive ground contact shadow & team indicator ring
    drawOval(
      color = Color.Black.copy(alpha = 0.44f * shadowScale),
      topLeft = Offset(u.x - g.spriteHeight * 0.28f * shadowScale, u.y - 3f),
      size = Size(g.spriteHeight * 0.56f * shadowScale, 10f * shadowScale),
    )
    val ringColor =
      when {
        u.charmTimer > 0f -> Color(0xFFF472B6)
        u.side == TeamSide.PLAYER && kerisActive -> Color(0xFFFBBF24)
        u.side == TeamSide.PLAYER -> Color(0xFFA78BFA).copy(alpha = 0.85f)
        else -> Color(0xFFFB7185).copy(alpha = 0.85f)
      }
    drawOval(
      color = ringColor,
      topLeft = Offset(u.x - g.spriteHeight * 0.32f, u.y - 5f),
      size = Size(g.spriteHeight * 0.64f, 14f),
      style = Stroke(width = 2.1f),
    )

    val isAttacking = u.animTime >= 0f
    val q = if (isAttacking) min(1f, u.animTime / g.animDuration) else 0f
    val sn = sin(q * PI.toFloat())
    var ox = 0f
    var oy = 0f
    var rotRad = 0f

    when (g) {
      GhostId.POCONG -> {
        if (isAttacking) {
          val z = min(1f, u.animTime / g.impactTime)
          val s2 = sin(z * PI.toFloat())
          oy = -s2 * 38f
          ox = s2 * 12f
          rotRad = s2 * 0.45f
        } else {
          val h = abs(sin(u.phase * 0.55f))
          oy = -h * 15f
          rotRad = sin(u.phase * 0.55f) * 0.08f
        }
      }
      GhostId.TUYUL -> if (isAttacking) ox = sn * 14f else oy = -abs(sin(u.phase * 1.3f)) * 5f
      GhostId.PALASIK -> {
        if (isAttacking) ox = sn * 28f
        else if (u.isMoving) oy = -abs(sin(u.phase * 2.2f)) * 6f
      }
      GhostId.BARONG -> {
        if (isAttacking) {
          oy = -sn * 12f
          ox = sn * 16f
        } else if (u.isMoving) {
          oy = -abs(sin(u.phase * 1.15f)) * 7f
        }
      }
      GhostId.BUTO_IJO -> {
        if (isAttacking) ox = if (q >= 0.55f) 12f * max(0f, 1f - (q - 0.55f) * 3f) else -5f * (q / 0.55f)
        else if (u.isMoving) oy = -abs(sin(u.phase * 1.25f)) * 4.5f
      }
      GhostId.RANGDA -> {
        oy = sin(u.phase * 0.9f) * 5f - if (isAttacking) sn * 9f else 0f
      }
      GhostId.NYI_BLORONG -> {
        ox = if (isAttacking) sn * 14f else 0f
        oy = -abs(cos(u.phase * 1.1f)) * 3f - if (isAttacking) sn * 5f else 0f
      }
      else -> {
        if (isAttacking) ox = sn * 10f
        else if (u.isMoving && !g.isFlying) oy = -abs(sin(u.phase * 1.5f)) * 4f
      }
    }

    val flashOffset = if (u.hitFlash > 0f) (Random.nextFloat() * 3f - 1.5f) else 0f
    val bobY = sin(u.phase) * (if (g.isFlying) 4.2f else 1f)
    val facing = if (u.side == TeamSide.PLAYER) 1f else -1f
    val totalRotDeg = ((rotRad + u.recoilTilt * facing) * 180f / PI.toFloat())

    // Pre-strike charge aura during wind-up anticipation
    if (isAttacking && u.animTime < g.impactTime && g.impactTime > 0.05f) {
      val charge = (u.animTime / g.impactTime).coerceIn(0f, 1f)
      val chargeR = (1f - charge * 0.55f) * (g.spriteHeight * 0.36f)
      drawCircle(
        color = g.accentColor.copy(alpha = 0.25f * charge),
        radius = chargeR,
        center = Offset(u.x + facing * 14f, u.y - u.airZ - g.spriteHeight * 0.45f),
      )
    }

    val nativeBmp = assets.androidSprites[g]
    val composeSprite = assets.sprites[g]

    withTransform({
      translate(u.x + flashOffset, u.y - u.airZ + flyingOffset + bobY)
      scale(facing, 1f, pivot = Offset.Zero)
      translate(ox, oy)
      rotate(degrees = totalRotDeg, pivot = Offset.Zero)
    }) {
      if (nativeBmp != null) {
        val hh = g.spriteHeight
        val w = hh * nativeBmp.width.toFloat() / nativeBmp.height.toFloat()
        GhostAnimator.drawAnimatedGhost(
          canvas = drawContext.canvas.nativeCanvas,
          bitmap = nativeBmp,
          u = u,
          w = w,
          h = hh,
          clockSec = clockSec,
        )
      } else if (composeSprite != null) {
        val hh = g.spriteHeight
        val w = hh * composeSprite.width.toFloat() / composeSprite.height.toFloat()
        val flashFilter =
          if (u.hitFlash > 0f) ColorFilter.tint(Color.White.copy(alpha = 0.55f), BlendMode.SrcAtop) else null
        drawImage(
          image = composeSprite,
          dstOffset = IntOffset((-w / 2f).roundToInt(), (-hh).roundToInt()),
          dstSize = IntSize(w.roundToInt(), hh.roundToInt()),
          colorFilter = flashFilter,
          filterQuality = FilterQuality.High,
        )
      } else {
        drawCircle(g.accentColor, radius = 20f, center = Offset(0f, -30f))
      }
    }

    // Elemental & Status VFX overlays
    if (u.shieldTimer > 0f) {
      val shieldPulse = 0.65f + 0.35f * sin(clockSec * 7f)
      val rShield = g.spriteHeight * 0.48f
      drawCircle(
        color = Color(0xFFFBBF24).copy(alpha = 0.14f * shieldPulse),
        radius = rShield,
        center = Offset(u.x, u.y - g.spriteHeight * 0.48f),
      )
      drawCircle(
        color = Color(0xFFFDE047).copy(alpha = 0.62f * shieldPulse),
        radius = rShield,
        center = Offset(u.x, u.y - g.spriteHeight * 0.48f),
        style = Stroke(width = 2.2f),
      )
    }
    if (u.burnTimer > 0f) {
      val flamePulse = 0.6f + 0.4f * sin(clockSec * 15f)
      drawCircle(
        color = Color(0xFFFB923C).copy(alpha = 0.30f * flamePulse),
        radius = 18f,
        center = Offset(u.x, u.y - g.spriteHeight * 0.45f),
      )
    }

    // Crisp unit HP bar if damaged
    if (u.hp < u.maxHp) {
      val ratio = (u.hp / u.maxHp).coerceIn(0f, 1f)
      val barY = u.y - u.airZ - g.spriteHeight - 14f
      drawRoundRect(
        color = Color.Black.copy(alpha = 0.72f),
        topLeft = Offset(u.x - 16f, barY),
        size = Size(32f, 5f),
        cornerRadius = CornerRadius(2.5f, 2.5f),
      )
      drawRoundRect(
        color = if (u.side == TeamSide.PLAYER) Color(0xFF34D399) else Color(0xFFFB7185),
        topLeft = Offset(u.x - 15f, barY + 1f),
        size = Size(30f * ratio, 3f),
        cornerRadius = CornerRadius(1.5f, 1.5f),
      )
    }

    // Stun / Slow / Charm status icon above head
    if (u.stunTimer > 0f || u.slowTimer > 0f || u.charmTimer > 0f) {
      val syTop = u.y - u.airZ - g.spriteHeight - 22f
      when {
        u.stunTimer > 0f -> {
          repeat(3) { i ->
            val ang = clockSec * 6f + i * 2.1f
            drawCircle(
              color = Color(0xFFFDE047),
              radius = 3f,
              center = Offset(u.x + cos(ang) * 11f, syTop + sin(ang) * 4f),
            )
          }
        }
        u.charmTimer > 0f -> drawCircle(Color(0xFFF472B6), radius = 4.5f, center = Offset(u.x, syTop))
        else -> drawCircle(Color(0xFFC084FC), radius = 4f, center = Offset(u.x, syTop))
      }
    }
  }

  private fun DrawScope.drawSlashArcs(engine: BattleEngine) {
    for (sa in engine.slashArcs) {
      val t = (sa.life / sa.maxLife).coerceIn(0f, 1f)
      val startAngle = if (sa.dir > 0) -65f else 115f
      drawArc(
        color = sa.color.copy(alpha = t * 0.9f),
        startAngle = startAngle,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(sa.x - sa.radius, sa.y - sa.radius),
        size = Size(sa.radius * 2f, sa.radius * 2f),
        style = Stroke(width = 8f * t, cap = StrokeCap.Round),
      )
      drawArc(
        color = Color.White.copy(alpha = t),
        startAngle = startAngle + 15f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(sa.x - sa.radius * 0.92f, sa.y - sa.radius * 0.92f),
        size = Size(sa.radius * 1.84f, sa.radius * 1.84f),
        style = Stroke(width = 3f * t, cap = StrokeCap.Round),
      )
    }
  }

  private fun DrawScope.drawProjectiles(engine: BattleEngine) {
    for (pr in engine.projectiles) {
      val a = (pr.life / pr.maxLife).coerceIn(0f, 1f)
      when (pr.kind) {
        ProjectileKind.SONIC_WAVE -> {
          val startAng = if (pr.dir > 0) -55f else 125f
          drawArc(
            color = pr.color.copy(alpha = a),
            startAngle = startAng,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(pr.x - pr.radius, pr.y - pr.radius),
            size = Size(pr.radius * 2f, pr.radius * 2f),
            style = Stroke(width = 6f, cap = StrokeCap.Round),
          )
          drawArc(
            color = Color.White.copy(alpha = a * 0.85f),
            startAngle = startAng + 12f,
            sweepAngle = 86f,
            useCenter = false,
            topLeft = Offset(pr.x - pr.radius * 0.72f, pr.y - pr.radius * 0.72f),
            size = Size(pr.radius * 1.44f, pr.radius * 1.44f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round),
          )
        }
        ProjectileKind.TIDAL_WAVE -> {
          val startAng = if (pr.dir > 0) -60f else 120f
          drawArc(
            color = pr.color.copy(alpha = a * 0.9f),
            startAngle = startAng,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(pr.x - pr.radius, pr.y - pr.radius),
            size = Size(pr.radius * 2f, pr.radius * 2f),
            style = Stroke(width = 10f, cap = StrokeCap.Round),
          )
          drawArc(
            color = Color(0xFFFDE047).copy(alpha = a * 0.85f),
            startAngle = startAng + 10f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(pr.x - pr.radius * 0.78f, pr.y - pr.radius * 0.78f),
            size = Size(pr.radius * 1.56f, pr.radius * 1.56f),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round),
          )
        }
        ProjectileKind.FIRE_ORB, ProjectileKind.HEAL_ORB -> {
          drawCircle(color = pr.color.copy(alpha = a * 0.45f), radius = pr.radius * 1.4f, center = Offset(pr.x, pr.y))
          drawCircle(color = Color.White.copy(alpha = a * 0.95f), radius = pr.radius * 0.55f, center = Offset(pr.x, pr.y))
        }
      }
    }
  }

  private fun DrawScope.drawBeamsAndBolts(engine: BattleEngine) {
    for (b in engine.beams) {
      val alpha = min(1f, b.life * 4f)
      drawLine(
        color = Color(0xFFF0ABFC).copy(alpha = alpha),
        start = Offset(b.x1, b.y1),
        end = Offset(b.x2, b.y2),
        strokeWidth = 5f,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = Color.White.copy(alpha = alpha * 0.8f),
        start = Offset(b.x1, b.y1),
        end = Offset(b.x2, b.y2),
        strokeWidth = 2f,
        cap = StrokeCap.Round,
      )
    }
    for (bolt in engine.bolts) {
      val alpha = bolt.life.coerceIn(0f, 1f)
      for (i in 0 until bolt.points.lastIndex) {
        val p1 = bolt.points[i]
        val p2 = bolt.points[i + 1]
        drawLine(
          color = bolt.color.copy(alpha = alpha),
          start = Offset(p1.first, p1.second),
          end = Offset(p2.first, p2.second),
          strokeWidth = 6f,
          cap = StrokeCap.Round,
        )
        drawLine(
          color = Color.White.copy(alpha = alpha),
          start = Offset(p1.first, p1.second),
          end = Offset(p2.first, p2.second),
          strokeWidth = 2.2f,
          cap = StrokeCap.Round,
        )
      }
    }
  }

  private fun DrawScope.drawForegroundParallaxLayer(
    fgBitmap: ImageBitmap?,
    tint: Color,
    moteColor: Color,
    camX: Float,
    shake: Float,
    clockSec: Float,
  ) {
    val fgOffsetX = camX * 1.48f
    val gustTilt = sin(clockSec * 1.6f) * 2.5f + shake * 12f

    // Foreground low volumetric mist drifting IN FRONT of ghosts' ankles
    for (i in 0 until 5) {
      val mx = ((i * 210f + clockSec * 28f) % (BattleEngine.WORLD_W + 260f)) - 130f + fgOffsetX
      drawOval(
        color = Color(0xFFDDD6FE).copy(alpha = 0.08f),
        topLeft = Offset(mx - 120f, BattleEngine.GROUND_Y + 12f + (i % 2) * 14f),
        size = Size(240f, 32f),
      )
    }

    // Drifting out-of-focus foreground embers/spores crossing the lens
    for (i in 0 until 6) {
      val fx = ((i * 155f + clockSec * (32f + i * 6f)) % (BattleEngine.WORLD_W + 40f)) - 20f + fgOffsetX
      val fy = BattleEngine.GROUND_Y - 10f + sin(clockSec * 1.8f + i) * 34f
      drawCircle(
        color = moteColor.copy(alpha = 0.24f),
        radius = 4.2f + (i % 2) * 2.2f,
        center = Offset(fx, fy),
      )
    }

    // High-detail silhouetted foreground grass, ferns, roots & tombstones strip
    if (fgBitmap != null) {
      val fgHeight = 68f
      val fgWidth = BattleEngine.WORLD_W + 70f
      val fgX = -35f + fgOffsetX + gustTilt * 0.4f
      val fgY = BattleEngine.WORLD_H - fgHeight + 6f
      drawImage(
        image = fgBitmap,
        dstOffset = IntOffset(fgX.roundToInt(), fgY.roundToInt()),
        dstSize = IntSize(fgWidth.roundToInt(), fgHeight.roundToInt()),
        alpha = 0.92f,
        filterQuality = FilterQuality.High,
      )
    }

    // Dynamic wind-swaying procedural alang-alang foreground blades on left & right corners
    for (side in listOf(-1f, 1f)) {
      val baseX = if (side < 0) 18f else BattleEngine.WORLD_W - 18f
      for (b in 0 until 7) {
        val bx = baseX + side * (-12f - b * 14f) + fgOffsetX * 1.15f
        val sway = sin(clockSec * 2.4f + b * 0.7f) * 6f + gustTilt
        val bladeH = 26f + (b % 3) * 11f
        val path =
          Path().apply {
            moveTo(bx - 2.5f, BattleEngine.WORLD_H)
            quadraticTo(bx + sway * 0.4f, BattleEngine.WORLD_H - bladeH * 0.5f, bx + sway, BattleEngine.WORLD_H - bladeH)
            quadraticTo(bx + sway * 0.4f + 2f, BattleEngine.WORLD_H - bladeH * 0.5f, bx + 2.5f, BattleEngine.WORLD_H)
            close()
          }
        drawPath(path, Color(0xFF06040D).copy(alpha = 0.92f))
        drawLine(
          color = tint.copy(alpha = 0.22f),
          start = Offset(bx, BattleEngine.WORLD_H - 4f),
          end = Offset(bx + sway, BattleEngine.WORLD_H - bladeH),
          strokeWidth = 1.2f,
        )
      }
    }
  }
}

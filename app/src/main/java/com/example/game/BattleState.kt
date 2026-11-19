package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.model.BattleConfig
import com.example.model.GhostId

enum class TeamSide { PLAYER, ENEMY }

data class BattleUnit(
  val uid: Long,
  var side: TeamSide,
  val ghost: GhostId,
  var x: Float,
  val y: Float,
  var hp: Float,
  val maxHp: Float,
  val dmg: Float,
  var atkTimer: Float = 0f,
  var hitFlash: Float = 0f,
  var animTime: Float = -1f,
  var hitDelivered: Boolean = false,
  var slowTimer: Float = 0f,
  var stunTimer: Float = 0f,
  var burnTimer: Float = 0f,
  var burnDps: Float = 0f,
  var charmTimer: Float = 0f,
  var shieldTimer: Float = 0f,
  var airZ: Float = 0f,
  var vz: Float = 0f,
  var kbVx: Float = 0f,
  var recoilTilt: Float = 0f,
  var phase: Float = 0f,
  var isMoving: Boolean = false,
)

data class ParticleFx(
  var x: Float,
  var y: Float,
  val vx: Float,
  val vy: Float,
  var life: Float,
  val maxLife: Float,
  val radius: Float,
  val color: Color,
  val gravity: Float = 0f,
)

data class ShockwaveRing(
  val x: Float,
  val y: Float,
  var radius: Float,
  val maxRadius: Float,
  var life: Float,
  val color: Color,
  val dir: Int = 0, // 0 = full ring, 1 = right arc, -1 = left arc
)

data class SlashArcFx(
  val x: Float,
  val y: Float,
  val radius: Float,
  val dir: Int,
  val color: Color,
  var life: Float,
  val maxLife: Float = 0.24f,
)

enum class ProjectileKind { SONIC_WAVE, TIDAL_WAVE, FIRE_ORB, HEAL_ORB }

data class ProjectileFx(
  var x: Float,
  var y: Float,
  val vx: Float,
  val vy: Float,
  val radius: Float,
  val dir: Int,
  val kind: ProjectileKind,
  val color: Color,
  var life: Float,
  val maxLife: Float,
)

data class AfterimageFx(
  val ghost: GhostId,
  val x: Float,
  val y: Float,
  val facing: Float,
  val rotDeg: Float,
  val tint: Color,
  var life: Float,
  val maxLife: Float = 0.26f,
)

data class GroundCrackFx(
  val x: Float,
  val y: Float,
  val radius: Float,
  val color: Color,
  var life: Float,
  val maxLife: Float = 1.4f,
)

data class TongueBeam(
  val x1: Float,
  val y1: Float,
  val x2: Float,
  val y2: Float,
  var life: Float,
)

data class LightningBolt(
  val points: List<Pair<Float, Float>>,
  var life: Float,
  val color: Color = Color(0xFFFDE047),
)

data class FloatingCallout(
  val x: Float,
  var y: Float,
  val text: String,
  val color: Color,
  var life: Float,
)

data class AmbientMote(
  var x: Float,
  var y: Float,
  val vx: Float,
  val vy: Float,
  val size: Float,
  val phase: Float,
)

data class BattleResultSummary(
  val won: Boolean,
  val config: BattleConfig,
  val totalCoinsEarned: Int,
  val bonusRewardCoins: Int,
  val battleKillCoins: Int,
  val enemiesDefeated: Int,
  val ghostsSummoned: Int,
  val damageDealt: Int,
  val durationSec: Int,
  val message: String,
  val isFirstStageClear: Boolean = false,
  val isNewInfinityRecord: Boolean = false,
)

data class BattleHudSnapshot(
  val souls: Float = 100f,
  val maxSouls: Float = 300f,
  val soulRegenPerSec: Float = 22f,
  val fountainLevel: Int = 1,
  val fountainUpgradeCost: Int = 80,
  val playerHp: Float = 450f,
  val playerMaxHp: Float = 450f,
  val enemyHp: Float = 450f,
  val enemyMaxHp: Float = 450f,
  val coinsInMatch: Int = 0,
  val elapsedSec: Int = 0,
  val cooldowns: Map<GhostId, Float> = emptyMap(),
  val maxCooldowns: Map<GhostId, Float> = emptyMap(),
  val kerisRemaining: Float = 0f,
  val kemenyanRemaining: Float = 0f,
  val garamRemaining: Float = 0f,
  val activeBanner: String? = null,
  val playerUnitCount: Int = 0,
  val enemyUnitCount: Int = 0,
  val isOver: Boolean = false,
)

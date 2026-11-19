package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.SoundEffect
import com.example.model.BattleConfig
import com.example.model.BattleModeType
import com.example.model.GameCatalog
import com.example.model.GhostId
import com.example.model.JimatId
import com.example.model.PlayerProgress
import com.example.model.UpgradeId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class BattleEngine(
  val config: BattleConfig,
  private val progress: PlayerProgress,
  private val onSound: (SoundEffect) -> Unit,
  private val onBattleEnded: (BattleResultSummary) -> Unit,
) {
  companion object {
    const val WORLD_W = 800f
    const val WORLD_H = 440f
    const val GROUND_Y = 365f
    const val PLAYER_BASE_X = 55f
    const val ENEMY_BASE_X = WORLD_W - 55f
    const val PLAYER_FRONT_X = 90f
    const val ENEMY_FRONT_X = WORLD_W - 90f
    const val MAX_UNITS_PER_SIDE = 22
    const val MAX_FOUNTAIN_LEVEL = 5
  }

  private val rng = Random(System.currentTimeMillis())
  private var nextUid = 1L

  var elapsedTime: Float = 0f
    private set

  var souls: Float = 100f
    private set
  var maxSouls: Float = 300f + 100f * progress.upgradeLevel(UpgradeId.GUDANG)
    private set
  var baseSoulRegen: Float = 22f + 4f * progress.upgradeLevel(UpgradeId.SUMUR)
    private set
  var fountainLevel: Int = 1
    private set

  val cooldownMul: Float = 1f - 0.08f * progress.upgradeLevel(UpgradeId.DUKUN)
  val playerMaxHp: Float = (config.enemyBaseHp * (1f + 0.15f * progress.upgradeLevel(UpgradeId.BENTENG))).roundToInt().toFloat()
  val enemyMaxHp: Float = config.enemyBaseHp

  var playerHp: Float = playerMaxHp
    private set
  var enemyHp: Float = enemyMaxHp
    private set

  var playerBaseFlash: Float = 0f
    private set
  var enemyBaseFlash: Float = 0f
    private set

  var kerisBuff: Float = 0f
    private set
  var kemenyanBuff: Float = 0f
    private set
  var garamBuff: Float = 0f
    private set

  var shakeIntensity: Float = 0f
    private set
  var coinsInMatch: Int = 0
    private set
  var enemiesDefeated: Int = 0
    private set
  var ghostsSummoned: Int = 0
    private set
  var damageDealt: Float = 0f
    private set
  var isOver: Boolean = false
    private set
  var autoSummon: Boolean = false

  private var spawnTimer: Float = 3.0f
  private var bossBannerCooldown: Float = 0f
  var activeBanner: String? = null
    private set
  private var bannerTimer: Float = 0f

  private val cooldowns = GhostId.entries.associateWith { 0f }.toMutableMap()
  private val maxCooldowns = GhostId.entries.associateWith { it.cooldown * cooldownMul }

  val units = mutableListOf<BattleUnit>()
  val particles = mutableListOf<ParticleFx>()
  val rings = mutableListOf<ShockwaveRing>()
  val slashArcs = mutableListOf<SlashArcFx>()
  val projectiles = mutableListOf<ProjectileFx>()
  val afterimages = mutableListOf<AfterimageFx>()
  val groundCracks = mutableListOf<GroundCrackFx>()
  val beams = mutableListOf<TongueBeam>()
  val bolts = mutableListOf<LightningBolt>()
  val callouts = mutableListOf<FloatingCallout>()
  val ambientMotes =
    List(22) {
      AmbientMote(
        x = rng.nextFloat() * WORLD_W,
        y = 40f + rng.nextFloat() * (GROUND_Y - 30f),
        vx = -12f + rng.nextFloat() * 24f,
        vy = -6f + rng.nextFloat() * 12f,
        size = 1.5f + rng.nextFloat() * 2.5f,
        phase = rng.nextFloat() * (2f * PI.toFloat()),
      )
    }

  private var autoSummonCheckTimer = 0f

  fun fountainUpgradeCost(): Int = 80 * fountainLevel

  fun upgradeFountain(): Boolean {
    if (isOver || fountainLevel >= MAX_FOUNTAIN_LEVEL) return false
    val cost = fountainUpgradeCost()
    if (souls < cost) return false
    souls -= cost
    fountainLevel++
    maxSouls += 60f
    baseSoulRegen += 6f
    onSound(SoundEffect.LEVEL_UP)
    addRing(PLAYER_BASE_X, GROUND_Y - 40f, 90f, Color(0xFF38BDF8))
    addCallout(PLAYER_BASE_X + 35f, GROUND_Y - 125f, "Sumur Lv.$fountainLevel!", Color(0xFF38BDF8))
    return true
  }

  fun summonPlayerGhost(ghost: GhostId): Boolean {
    if (isOver) return false
    if (ghost !in progress.ownedGhosts) return false
    if (souls < ghost.cost) return false
    if ((cooldowns[ghost] ?: 0f) > 0f) return false
    if (units.count { it.side == TeamSide.PLAYER } >= MAX_UNITS_PER_SIDE) return false
    souls -= ghost.cost
    cooldowns[ghost] = ghost.cooldown * cooldownMul
    ghostsSummoned++
    spawnUnit(TeamSide.PLAYER, ghost)
    onSound(SoundEffect.SUMMON)
    return true
  }

  fun useJimat(jimat: JimatId): Boolean {
    if (isOver) return false
    when (jimat) {
      JimatId.KERIS -> {
        kerisBuff = 15f
        onSound(SoundEffect.LEVEL_UP)
        addRing(PLAYER_BASE_X + 40f, GROUND_Y - 30f, 160f, Color(0xFFFBBF24))
      }
      JimatId.KEMENYAN -> {
        kemenyanBuff = 12f
        onSound(SoundEffect.HEAL)
        addRing(PLAYER_BASE_X + 40f, GROUND_Y - 30f, 140f, Color(0xFF38BDF8))
      }
      JimatId.KEMBANG -> {
        units.filter { it.side == TeamSide.PLAYER }.forEach { u ->
          u.hp = u.maxHp
          spawnBurst(u.x, u.y - 30f, TeamSide.PLAYER, 5, 1f)
        }
        playerHp = min(playerMaxHp, playerHp + 150f)
        addRing(120f, GROUND_Y - 20f, 300f, Color(0xFFF9A8D4))
        onSound(SoundEffect.HEAL)
      }
      JimatId.GARAM -> {
        garamBuff = 8f
        addRing(WORLD_W / 2f, GROUND_Y - 20f, 340f, Color(0xFFE2E8F0))
        onSound(SoundEffect.KUNTI)
      }
      JimatId.PETIR -> {
        val enemies = units.filter { it.side == TeamSide.ENEMY && it.hp > 0f }
        if (enemies.isEmpty()) {
          repeat(3) { i ->
            addLightningBolt(240f + i * 160f, GROUND_Y - 10f)
          }
        } else {
          enemies.forEach { v ->
            v.hp -= 120f
            v.hitFlash = 0.22f
            damageDealt += 120f
            spawnBurst(v.x, v.y - 35f, TeamSide.ENEMY, 8, 1.6f)
            addLightningBolt(v.x, v.y - 25f)
          }
        }
        shakeIntensity = 0.42f
        addRing(WORLD_W / 2f, GROUND_Y - 20f, 400f, Color(0xFFFDE047))
        onSound(SoundEffect.LIGHTNING)
      }
      JimatId.AYAM -> {
        units.filter { it.side == TeamSide.ENEMY && it.hp > 0f }.forEach { v ->
          v.stunTimer = max(v.stunTimer, 3f)
        }
        addRing(WORLD_W / 2f, GROUND_Y - 20f, 320f, Color(0xFFC084FC))
        onSound(SoundEffect.KUNTI)
      }
    }
    addCallout(WORLD_W / 2f, 118f, "${jimat.title}!", Color(0xFFFDE047))
    return true
  }

  fun step(dtRaw: Float, speedMultiplier: Int) {
    val simSteps = speedMultiplier.coerceIn(1, 3)
    val dtPerStep = min(0.045f, dtRaw)
    if (!isOver) {
      repeat(simSteps) {
        if (!isOver) updateSimulation(dtPerStep)
      }
    }
    updateEffects(dtPerStep * simSteps)
  }

  private fun updateSimulation(dt: Float) {
    elapsedTime += dt
    val regenMul = if (kemenyanBuff > 0f) 2f else 1f
    souls = min(maxSouls, souls + baseSoulRegen * regenMul * dt)

    kerisBuff = max(0f, kerisBuff - dt)
    kemenyanBuff = max(0f, kemenyanBuff - dt)
    garamBuff = max(0f, garamBuff - dt)
    bossBannerCooldown = max(0f, bossBannerCooldown - dt)
    if (bannerTimer > 0f) {
      bannerTimer -= dt
      if (bannerTimer <= 0f) activeBanner = null
    }

    for (k in GhostId.entries) {
      val cur = cooldowns[k] ?: 0f
      if (cur > 0f) cooldowns[k] = max(0f, cur - dt)
    }

    if (autoSummon) {
      autoSummonCheckTimer -= dt
      if (autoSummonCheckTimer <= 0f) {
        autoSummonCheckTimer = 0.45f
        val ready =
          GhostId.ordered.filter {
            it in progress.ownedGhosts &&
              souls >= it.cost &&
              (cooldowns[it] ?: 0f) <= 0f
          }
        if (ready.isNotEmpty()) {
          // Prefer higher-tier units if affordable, occasionally cheap fast units
          val pick = if (rng.nextFloat() < 0.7f) ready.last() else ready.first()
          summonPlayerGhost(pick)
        }
      }
    }

    spawnTimer -= dt
    if (spawnTimer <= 0f) {
      val enemyGhost = pickWeightedEnemy()
      spawnUnit(TeamSide.ENEMY, enemyGhost)
      val paceScale = max(0.55f, 1f - elapsedTime / 200f)
      spawnTimer = rnd(config.gapMin, config.gapMax) * paceScale
    }

    val snapshot = units.toList()
    for (u in snapshot) {
      if (u.hp <= 0f) continue
      val ghost = u.ghost
      val dir = if (u.side == TeamSide.PLAYER) 1f else -1f
      u.phase += dt * 6f
      u.isMoving = false
      u.slowTimer = max(0f, u.slowTimer - dt)
      u.shieldTimer = max(0f, u.shieldTimer - dt)

      // Smooth parabolic knockback & recoil recovery physics
      if (u.airZ > 0f || u.vz != 0f) {
        u.airZ += u.vz * dt
        u.vz -= 540f * dt
        if (u.airZ <= 0f) {
          if (u.vz < -95f) spawnDust(u.x, u.y, 4)
          u.airZ = 0f
          u.vz = 0f
        }
      }
      if (abs(u.kbVx) > 2f) {
        u.x = (u.x + u.kbVx * dt).coerceIn(PLAYER_FRONT_X, ENEMY_FRONT_X)
        u.kbVx *= kotlin.math.exp(-6.5f * dt)
      } else {
        u.kbVx = 0f
      }
      if (abs(u.recoilTilt) > 0.005f) {
        u.recoilTilt *= kotlin.math.exp(-9f * dt)
      } else {
        u.recoilTilt = 0f
      }

      if (u.charmTimer > 0f) {
        u.charmTimer -= dt
        if (u.charmTimer <= 0f) {
          u.side = if (u.side == TeamSide.PLAYER) TeamSide.ENEMY else TeamSide.PLAYER
        }
      }

      if (u.burnTimer > 0f) {
        u.burnTimer -= dt
        val shieldFactor = if (u.shieldTimer > 0f) 0.55f else 1f
        val burnTick = u.burnDps * shieldFactor * dt
        u.hp -= burnTick
        if (u.side == TeamSide.ENEMY) damageDealt += burnTick
        if (rng.nextFloat() < dt * 14f && particles.size < 180) {
          particles.add(
            ParticleFx(
              x = u.x + rnd(-8f, 8f),
              y = u.y - rnd(10f, 40f),
              vx = 0f,
              vy = -50f,
              life = 0.6f,
              maxLife = 0.6f,
              radius = 2.6f,
              color = Color(0xFFFB923C),
            )
          )
        }
      }

      if (u.stunTimer > 0f) {
        u.stunTimer -= dt
        continue
      }

      u.atkTimer -= dt
      if (u.animTime >= 0f) {
        u.animTime += dt
        if ((ghost == GhostId.PALASIK || ghost == GhostId.BARONG) &&
          u.animTime < ghost.impactTime &&
          rng.nextFloat() < dt * 22f &&
          afterimages.size < 45
        ) {
          afterimages.add(
            AfterimageFx(
              ghost = ghost,
              x = u.x,
              y = u.y - u.airZ,
              facing = dir,
              rotDeg = if (ghost == GhostId.PALASIK) 14f * dir else 0f,
              tint = ghost.accentColor,
              life = 0.24f,
              maxLife = 0.24f,
            )
          )
        }
        if (!u.hitDelivered && u.animTime >= ghost.impactTime) {
          u.hitDelivered = true
          performHit(u)
        }
        if (u.animTime >= ghost.animDuration) {
          u.animTime = -1f
          u.hitDelivered = false
        }
        continue
      }

      val (target, dist) = findNearestTarget(u)
      val baseDist = if (u.side == TeamSide.PLAYER) (ENEMY_FRONT_X - u.x) else (u.x - PLAYER_FRONT_X)
      if ((target != null && dist <= ghost.range) || baseDist <= ghost.range) {
        if (u.atkTimer <= 0f) {
          u.animTime = 0f
          u.hitDelivered = false
          u.atkTimer = ghost.atkInterval
        }
      } else {
        val slowed = u.slowTimer > 0f || (u.side == TeamSide.ENEMY && garamBuff > 0f)
        val spdMul = if (slowed) 0.5f else 1f
        u.x += dir * ghost.speed * spdMul * dt
        u.isMoving = true
        if (ghost == GhostId.PALASIK && rng.nextFloat() < dt * 15f && afterimages.size < 45) {
          afterimages.add(
            AfterimageFx(
              ghost = ghost,
              x = u.x,
              y = u.y - u.airZ,
              facing = dir,
              rotDeg = 10f * dir,
              tint = ghost.accentColor,
              life = 0.22f,
              maxLife = 0.22f,
            )
          )
        }
        if (!ghost.isFlying && rng.nextFloat() < dt * 6f) {
          spawnDust(u.x - dir * 8f, u.y, 1)
        }
      }
    }

    val iterator = units.iterator()
    while (iterator.hasNext()) {
      val u = iterator.next()
      if (u.hp <= 0f) {
        spawnBurst(u.x, u.y - 24f, u.side, 12, 1.4f)
        if (u.side == TeamSide.ENEMY) {
          enemiesDefeated++
          val coinReward = max(1, (u.ghost.cost / 25f * config.coinFactor).roundToInt())
          coinsInMatch += coinReward
          onSound(SoundEffect.COIN)
          if (coinReward > 1) {
            addCallout(u.x, u.y - 70f, "+$coinReward 🪙", Color(0xFFFDE047))
          }
        }
        iterator.remove()
      }
    }

    if (enemyHp <= 0f) {
      enemyHp = 0f
      finishBattle(won = true)
    } else if (playerHp <= 0f) {
      playerHp = 0f
      finishBattle(won = false)
    }
  }

  private fun performHit(u: BattleUnit) {
    val dmg = u.dmg * (if (u.side == TeamSide.PLAYER && kerisBuff > 0f) 1.5f else 1f)
    val ghost = u.ghost
    val dir = if (u.side == TeamSide.PLAYER) 1f else -1f
    val (target, dist) = findNearestTarget(u)
    val baseX = if (u.side == TeamSide.PLAYER) ENEMY_FRONT_X else PLAYER_FRONT_X
    val hitUnit = target != null && dist <= ghost.range + 8f
    if (!hitUnit && abs(baseX - u.x) > ghost.range + 8f) return

    val px = if (hitUnit) target!!.x else baseX
    val py = if (hitUnit) target!!.y - 28f else GROUND_Y - 40f

    if (hitUnit) {
      val shieldMul = if (target!!.shieldTimer > 0f) 0.55f else 1f
      val finalDmg = dmg * shieldMul
      target.hp -= finalDmg
      target.hitFlash = 0.16f
      target.recoilTilt = dir * 0.22f
      if (u.side == TeamSide.PLAYER) damageDealt += finalDmg
    } else {
      if (u.side == TeamSide.PLAYER) {
        enemyHp -= dmg
        enemyBaseFlash = 0.16f
        damageDealt += dmg
      } else {
        playerHp -= dmg
        playerBaseFlash = 0.16f
      }
    }

    spawnBurst(px, py, u.side, 6, 1.2f)

    fun nearbyEnemies(radius: Float): List<BattleUnit> =
      units.filter { it.side != u.side && it.hp > 0f && abs(it.x - px) < radius }

    when (ghost) {
      GhostId.TUYUL -> {
        souls =
          if (u.side == TeamSide.PLAYER) min(maxSouls, souls + 3f)
          else max(0f, souls - 3f)
        addSlashArc(u.x + dir * 18f, u.y - 26f, 26f, dir.toInt(), Color(0xFF4ADE80))
        repeat(4) {
          if (particles.size < 180) {
            particles.add(
              ParticleFx(
                x = px,
                y = py,
                vx = -dir * rnd(40f, 120f),
                vy = rnd(-130f, -50f),
                life = 1f,
                maxLife = 1f,
                radius = 3f,
                color = Color(0xFFFDE047),
              )
            )
          }
        }
        onSound(SoundEffect.HIT_LIGHT)
        if (rng.nextFloat() < 0.5f) {
          addCallout(
            u.x,
            u.y - 50f,
            if (u.side == TeamSide.PLAYER) "+3 curian" else "-3 arwah",
            if (u.side == TeamSide.PLAYER) Color(0xFFFDE047) else Color(0xFFFB7185),
          )
        }
      }
      GhostId.POCONG -> {
        addRing(px, u.y, 52f, Color(0xFFE9E5FF))
        spawnDust(px, u.y, 6)
        nearbyEnemies(50f).forEach { v ->
          if (v !== target) {
            v.hp -= dmg * 0.6f
            if (u.side == TeamSide.PLAYER) damageDealt += dmg * 0.6f
          }
          applyKnockback(v, dir, 105f, 110f)
          v.stunTimer = max(v.stunTimer, 0.35f)
        }
        shakeIntensity = max(shakeIntensity, 0.16f)
        onSound(SoundEffect.SLAM_HEAVY)
        addCallout(px, u.y - 62f, "BRUK!", Color.White)
      }
      GhostId.KUNTI -> {
        addRing(u.x, u.y - 44f, ghost.range * 0.65f, Color(0xFFF0ABFC), dir.toInt())
        projectiles.add(
          ProjectileFx(
            x = u.x + dir * 16f,
            y = u.y - 46f,
            vx = dir * 320f,
            vy = 0f,
            radius = 22f,
            dir = dir.toInt(),
            kind = ProjectileKind.SONIC_WAVE,
            color = Color(0xFFE879F9),
            life = 0.42f,
            maxLife = 0.42f,
          )
        )
        units.forEach { v ->
          val q = (v.x - u.x) * dir
          if (v.side != u.side && v.hp > 0f && q > -10f && q < ghost.range) {
            v.slowTimer = max(v.slowTimer, 2.5f)
            if (v !== target) {
              v.hp -= dmg * 0.4f
              if (u.side == TeamSide.PLAYER) damageDealt += dmg * 0.4f
            }
          }
        }
        onSound(SoundEffect.KUNTI)
        addCallout(u.x, u.y - 74f, "HIHIHI~", Color(0xFFF0ABFC))
      }
      GhostId.GENDRUWO -> {
        addRing(px, u.y, 68f, Color(0xFFFB7185))
        groundCracks.add(GroundCrackFx(px, u.y + 4f, 52f, Color(0xFFFB7185), 1.3f, 1.3f))
        spawnDust(px, u.y, 8)
        nearbyEnemies(60f).forEach { v ->
          if (v !== target) {
            v.hp -= dmg * 0.5f
            if (u.side == TeamSide.PLAYER) damageDealt += dmg * 0.5f
          }
          applyKnockback(v, dir, 140f, 145f)
          v.stunTimer = max(v.stunTimer, 0.5f)
        }
        shakeIntensity = max(shakeIntensity, 0.32f)
        onSound(SoundEffect.SLAM_HEAVY)
        addCallout(px, u.y - 82f, "GRAUUR!", Color(0xFFFDA4AF))
      }
      GhostId.API -> {
        projectiles.add(
          ProjectileFx(
            x = u.x + dir * 16f,
            y = u.y - 42f,
            vx = dir * 240f,
            vy = -12f,
            radius = 12f,
            dir = dir.toInt(),
            kind = ProjectileKind.FIRE_ORB,
            color = Color(0xFFFB923C),
            life = 0.28f,
            maxLife = 0.28f,
          )
        )
        if (hitUnit) {
          target!!.burnTimer = 3f
          target.burnDps = dmg * 0.45f
          onSound(SoundEffect.FIRE)
          addCallout(px, u.y - 72f, "TERBAKAR!", Color(0xFFFB923C))
        } else {
          onSound(SoundEffect.FIRE)
        }
      }
      GhostId.KUYANG -> {
        addSlashArc(px, py, 26f, dir.toInt(), Color(0xFFFB7185))
        u.hp = min(u.maxHp, u.hp + dmg * 0.8f)
        spawnBurst(u.x, u.y - 30f, TeamSide.ENEMY, 5, 1f)
        onSound(SoundEffect.HIT_LIGHT)
        addCallout(u.x, u.y - 78f, "SLURP", Color(0xFFFB7185))
      }
      GhostId.WEWE -> {
        units.forEach { v ->
          if (v.side == u.side && v.hp > 0f && abs(v.x - u.x) < 130f) {
            v.hp = min(v.maxHp, v.hp + 24f)
            spawnBurst(v.x, v.y - 30f, TeamSide.PLAYER, 3, 1f)
            if (v !== u && projectiles.size < 25) {
              val dx = v.x - u.x
              projectiles.add(
                ProjectileFx(
                  x = u.x,
                  y = u.y - 52f,
                  vx = dx * 2.5f,
                  vy = -35f,
                  radius = 9f,
                  dir = if (dx >= 0f) 1 else -1,
                  kind = ProjectileKind.HEAL_ORB,
                  color = Color(0xFF34D399),
                  life = 0.35f,
                  maxLife = 0.35f,
                )
              )
            }
          }
        }
        addRing(u.x, u.y - 20f, 130f, Color(0xFF86EFAC))
        onSound(SoundEffect.HEAL)
        addCallout(u.x, u.y - 88f, "+HP", Color(0xFF86EFAC))
      }
      GhostId.SUNDEL -> {
        if (hitUnit && target!!.charmTimer <= 0f) {
          target.side = u.side
          target.charmTimer = 3.5f
          addRing(target.x, target.y - 30f, 44f, Color(0xFFF9A8D4))
          onSound(SoundEffect.CHARM)
          addCallout(target.x, target.y - 72f, "TERPIKAT 💘", Color(0xFFF9A8D4))
        } else {
          onSound(SoundEffect.HIT_LIGHT)
        }
      }
      GhostId.LEAK -> {
        if (hitUnit) {
          target!!.x = (u.x + dir * 46f).coerceIn(PLAYER_FRONT_X, ENEMY_FRONT_X)
          target.stunTimer = max(target.stunTimer, 0.5f)
          beams.add(TongueBeam(u.x, u.y - 32f, target.x, target.y - 30f, 0.32f))
          onSound(SoundEffect.HIT_LIGHT)
          addCallout(u.x, u.y - 88f, "SRRET!", Color(0xFFF9A8D4))
        } else {
          onSound(SoundEffect.HIT_LIGHT)
        }
      }
      GhostId.PALASIK -> {
        souls =
          if (u.side == TeamSide.PLAYER) min(maxSouls, souls + 8f)
          else max(0f, souls - 8f)
        addSlashArc(px, py, 36f, dir.toInt(), Color(0xFF22D3EE))
        if (hitUnit) {
          target!!.slowTimer = max(target.slowTimer, 2.2f)
          applyKnockback(target, dir, 75f, 65f)
          u.x = (target.x - dir * 24f).coerceIn(PLAYER_FRONT_X, ENEMY_FRONT_X)
        }
        repeat(8) {
          if (particles.size < 180) {
            particles.add(
              ParticleFx(
                x = px + rnd(-10f, 10f),
                y = py + rnd(-12f, 12f),
                vx = -dir * rnd(50f, 150f),
                vy = rnd(-90f, 20f),
                life = 0.65f,
                maxLife = 0.65f,
                radius = rnd(2.5f, 4.5f),
                color = Color(0xFF22D3EE),
              )
            )
          }
        }
        addRing(px, py, 48f, Color(0xFF22D3EE), dir.toInt())
        onSound(SoundEffect.HIT_LIGHT)
        addCallout(
          u.x,
          u.y - 78f,
          if (u.side == TeamSide.PLAYER) "+8 BAYANG!" else "TERKOYAK!",
          Color(0xFF22D3EE),
        )
      }
      GhostId.BARONG -> {
        u.hp = min(u.maxHp, u.hp + 18f)
        u.shieldTimer = max(u.shieldTimer, 4.0f)
        addSlashArc(px, py - 6f, 44f, dir.toInt(), Color(0xFFFBBF24))
        spawnDust(u.x, u.y, 5)
        units.forEach { ally ->
          if (ally.side == u.side && ally.hp > 0f && abs(ally.x - u.x) < 120f) {
            ally.shieldTimer = max(ally.shieldTimer, 4.0f)
          }
        }
        nearbyEnemies(70f).forEach { v ->
          applyKnockback(v, dir, 125f, 130f)
          v.stunTimer = max(v.stunTimer, 0.35f)
        }
        addRing(u.x, u.y - 26f, 118f, Color(0xFFFBBF24))
        shakeIntensity = max(shakeIntensity, 0.18f)
        onSound(SoundEffect.LEVEL_UP)
        addCallout(u.x, u.y - 96f, "PERISAI SAKTI!", Color(0xFFFDE047))
      }
      GhostId.BUTO_IJO -> {
        addRing(px, u.y, 104f, Color(0xFF10B981))
        addSlashArc(px, u.y - 28f, 56f, dir.toInt(), Color(0xFF34D399))
        groundCracks.add(GroundCrackFx(px, u.y + 4f, 84f, Color(0xFF10B981), 1.6f, 1.6f))
        spawnDust(px, u.y, 12)
        nearbyEnemies(95f).forEach { v ->
          val sMul = if (v.shieldTimer > 0f) 0.55f else 1f
          if (v !== target) {
            val splash = dmg * 0.7f * sMul
            v.hp -= splash
            v.hitFlash = 0.16f
            if (u.side == TeamSide.PLAYER) damageDealt += splash
          }
          applyKnockback(v, dir, 175f, 190f)
          v.stunTimer = max(v.stunTimer, 0.8f)
        }
        shakeIntensity = max(shakeIntensity, 0.42f)
        onSound(SoundEffect.SLAM_HEAVY)
        addCallout(px, u.y - 96f, "GEMPA GIJOK!", Color(0xFF34D399))
      }
      GhostId.RANGDA -> {
        val chainTargets =
          units
            .filter { it.side != u.side && it.hp > 0f && abs(it.x - px) < 115f }
            .sortedBy { abs(it.x - px) }
            .take(4)
        if (chainTargets.isEmpty()) {
          addLightningBolt(px, py, Color(0xFFC084FC))
        } else {
          chainTargets.forEach { v ->
            val sMul = if (v.shieldTimer > 0f) 0.55f else 1f
            if (v !== target) {
              val chainDmg = dmg * 0.65f * sMul
              v.hp -= chainDmg
              v.hitFlash = 0.18f
              v.recoilTilt = dir * 0.2f
              if (u.side == TeamSide.PLAYER) damageDealt += chainDmg
            }
            v.burnTimer = max(v.burnTimer, 2.5f)
            v.burnDps = max(v.burnDps, dmg * 0.35f)
            v.slowTimer = max(v.slowTimer, 2.0f)
            addLightningBolt(v.x, v.y - 28f, Color(0xFFC084FC))
          }
        }
        addRing(u.x, u.y - 42f, 92f, Color(0xFFC084FC), dir.toInt())
        shakeIntensity = max(shakeIntensity, 0.22f)
        onSound(SoundEffect.LIGHTNING)
        addCallout(u.x, u.y - 94f, "PETIR HITAM!", Color(0xFFE879F9))
      }
      GhostId.NYI_BLORONG -> {
        addRing(px, u.y - 20f, 95f, Color(0xFF2DD4BF), dir.toInt())
        projectiles.add(
          ProjectileFx(
            x = u.x + dir * 20f,
            y = u.y - 26f,
            vx = dir * 280f,
            vy = 0f,
            radius = 30f,
            dir = dir.toInt(),
            kind = ProjectileKind.TIDAL_WAVE,
            color = Color(0xFF2DD4BF),
            life = 0.45f,
            maxLife = 0.45f,
          )
        )
        nearbyEnemies(90f).forEach { v ->
          val sMul = if (v.shieldTimer > 0f) 0.55f else 1f
          if (v !== target) {
            val waveDmg = dmg * 0.65f * sMul
            v.hp -= waveDmg
            v.hitFlash = 0.15f
            if (u.side == TeamSide.PLAYER) damageDealt += waveDmg
          }
          applyKnockback(v, dir, 145f, 115f)
          v.slowTimer = max(v.slowTimer, 2.5f)
        }
        if (u.side == TeamSide.PLAYER) {
          coinsInMatch += 2
          onSound(SoundEffect.COIN)
          addCallout(u.x, u.y - 94f, "OMBAK +2 🪙!", Color(0xFFFDE047))
        } else {
          onSound(SoundEffect.HEAL)
          addCallout(u.x, u.y - 94f, "OMBAK GAIB!", Color(0xFF2DD4BF))
        }
      }
    }
  }

  private fun findNearestTarget(u: BattleUnit): Pair<BattleUnit?, Float> {
    val dir = if (u.side == TeamSide.PLAYER) 1f else -1f
    var best: BattleUnit? = null
    var bestDist = 1e9f
    for (v in units) {
      if (v.side == u.side || v.hp <= 0f) continue
      val q = (v.x - u.x) * dir
      if (q > -12f && q < bestDist) {
        bestDist = q
        best = v
      }
    }
    return best to bestDist
  }

  private fun spawnUnit(side: TeamSide, ghost: GhostId) {
    if (units.count { it.side == side } >= MAX_UNITS_PER_SIDE) return
    val mul =
      if (side == TeamSide.ENEMY) config.enemyMul
      else 1f + 0.12f * progress.ghostLevel(ghost)
    val hp = ghost.baseHp * mul
    val dmg = ghost.baseDmg * mul
    units.add(
      BattleUnit(
        uid = nextUid++,
        side = side,
        ghost = ghost,
        x = if (side == TeamSide.PLAYER) 125f else WORLD_W - 125f,
        y = GROUND_Y + rnd(-16f, 16f),
        hp = hp,
        maxHp = hp,
        dmg = dmg,
        phase = rnd(0f, 6.28f),
      )
    )
    if (side == TeamSide.ENEMY && ghost.isBossThreat && bossBannerCooldown <= 0f) {
      activeBanner = "PERINGATAN! ${ghost.displayName} Muncul!"
      bannerTimer = 2.6f
      bossBannerCooldown = 14f
    }
  }

  private fun pickWeightedEnemy(): GhostId {
    val total = config.weights.values.sum()
    var r = rng.nextFloat() * total
    var last = GhostId.TUYUL
    for ((g, w) in config.weights) {
      last = g
      r -= w
      if (r <= 0f) return g
    }
    return last
  }

  private fun updateEffects(dt: Float) {
    val rIt = rings.iterator()
    while (rIt.hasNext()) {
      val r = rIt.next()
      r.radius += r.maxRadius * dt * 3f
      r.life -= dt * 3f
      if (r.life <= 0f) rIt.remove()
    }

    val cIt = callouts.iterator()
    while (cIt.hasNext()) {
      val c = cIt.next()
      c.y -= 30f * dt
      c.life -= dt * 1.1f
      if (c.life <= 0f) cIt.remove()
    }

    val pIt = particles.iterator()
    while (pIt.hasNext()) {
      val p = pIt.next()
      p.x += p.vx * dt
      p.y += p.vy * dt
      p.life -= dt * 1.6f
      if (p.life <= 0f) pIt.remove()
    }

    val sIt = slashArcs.iterator()
    while (sIt.hasNext()) {
      val s = sIt.next()
      s.life -= dt
      if (s.life <= 0f) sIt.remove()
    }

    val prIt = projectiles.iterator()
    while (prIt.hasNext()) {
      val pr = prIt.next()
      pr.x += pr.vx * dt
      pr.y += pr.vy * dt
      pr.life -= dt
      if (pr.life <= 0f) prIt.remove()
    }

    val aIt = afterimages.iterator()
    while (aIt.hasNext()) {
      val a = aIt.next()
      a.life -= dt
      if (a.life <= 0f) aIt.remove()
    }

    val gIt = groundCracks.iterator()
    while (gIt.hasNext()) {
      val g = gIt.next()
      g.life -= dt
      if (g.life <= 0f) gIt.remove()
    }

    val bIt = beams.iterator()
    while (bIt.hasNext()) {
      val b = bIt.next()
      b.life -= dt
      if (b.life <= 0f) bIt.remove()
    }

    val lIt = bolts.iterator()
    while (lIt.hasNext()) {
      val l = lIt.next()
      l.life -= dt * 2.6f
      if (l.life <= 0f) lIt.remove()
    }

    for (u in units) {
      u.hitFlash = max(0f, u.hitFlash - dt)
    }
    for (m in ambientMotes) {
      m.x = (m.x + m.vx * dt + WORLD_W) % WORLD_W
      m.y += m.vy * dt
      if (m.y < 30f) m.y = GROUND_Y - 15f
      if (m.y > GROUND_Y) m.y = 35f
    }
    shakeIntensity = max(0f, shakeIntensity - dt)
    playerBaseFlash = max(0f, playerBaseFlash - dt)
    enemyBaseFlash = max(0f, enemyBaseFlash - dt)
  }

  private fun finishBattle(won: Boolean) {
    if (isOver) return
    isOver = true
    val burstX = if (won) ENEMY_BASE_X else PLAYER_BASE_X
    repeat(5) {
      spawnBurst(burstX + rnd(-25f, 25f), GROUND_Y - rnd(0f, 90f), if (won) TeamSide.ENEMY else TeamSide.PLAYER, 10, 2f)
    }
    onSound(if (won) SoundEffect.VICTORY else SoundEffect.DEFEAT)

    var bonus = 0
    var firstClear = false
    var newInfRecord = false
    val msg: String

    when (config.mode) {
      BattleModeType.STAGE -> {
        if (won) {
          bonus += config.rewardCoins
          if (config.stageIndex == progress.clearStage && progress.clearStage < GameCatalog.stages.size) {
            firstClear = true
            bonus += config.rewardCoins
          }
          msg =
            "Stage ${config.stageIndex + 1} (${config.title}) selesai!" +
              if (firstClear) " Stage baru & bonus perdana terbuka!" else ""
        } else {
          msg = "Markasmu hancur. Perkuat hantu & jimat di Toko Dukun."
        }
      }
      BattleModeType.TOURNAMENT -> {
        val t = GameCatalog.tournaments[config.tournamentIndex]
        if (won) {
          bonus += config.rewardCoins
          if (config.tournamentRound >= t.rounds - 1) {
            bonus += t.prize
            msg = "JUARA ${t.name}! Hadiah utama ${t.prize} koin!"
          } else {
            msg = "Babak ${config.tournamentRound + 1} menang! Siap lanjut ke babak berikutnya."
          }
        } else {
          msg = "Kamu tersingkir di babak ${config.tournamentRound + 1}."
        }
      }
      BattleModeType.INFINITY -> {
        if (won) {
          bonus += config.rewardCoins
          if (config.infinityLevel > progress.infinityRecord) newInfRecord = true
          msg =
            "Level ${config.infinityLevel} berhasil ditaklukkan!" +
              if (newInfRecord) " REKOR BARU!" else ""
        } else {
          msg = "Gugur di Level ${config.infinityLevel}. Rekor terbaik: Level ${progress.infinityRecord}."
        }
      }
      BattleModeType.FREE -> {
        if (won) {
          bonus += config.rewardCoins
          msg = "Menang di ${config.title} (${config.freeDifficulty.label})."
        } else {
          msg = "Markasmu hancur di ${config.title}."
        }
      }
    }

    onBattleEnded(
      BattleResultSummary(
        won = won,
        config = config,
        totalCoinsEarned = coinsInMatch + bonus,
        bonusRewardCoins = bonus,
        battleKillCoins = coinsInMatch,
        enemiesDefeated = enemiesDefeated,
        ghostsSummoned = ghostsSummoned,
        damageDealt = damageDealt.roundToInt(),
        durationSec = elapsedTime.roundToInt(),
        message = msg,
        isFirstStageClear = firstClear,
        isNewInfinityRecord = newInfRecord,
      )
    )
  }

  fun hudSnapshot(): BattleHudSnapshot =
    BattleHudSnapshot(
      souls = souls,
      maxSouls = maxSouls,
      soulRegenPerSec = baseSoulRegen * (if (kemenyanBuff > 0f) 2f else 1f),
      fountainLevel = fountainLevel,
      fountainUpgradeCost = fountainUpgradeCost(),
      playerHp = playerHp,
      playerMaxHp = playerMaxHp,
      enemyHp = enemyHp,
      enemyMaxHp = enemyMaxHp,
      coinsInMatch = coinsInMatch,
      elapsedSec = elapsedTime.roundToInt(),
      cooldowns = cooldowns.toMap(),
      maxCooldowns = maxCooldowns,
      kerisRemaining = kerisBuff,
      kemenyanRemaining = kemenyanBuff,
      garamRemaining = garamBuff,
      activeBanner = activeBanner,
      playerUnitCount = units.count { it.side == TeamSide.PLAYER },
      enemyUnitCount = units.count { it.side == TeamSide.ENEMY },
      isOver = isOver,
    )

  private fun spawnBurst(x: Float, y: Float, side: TeamSide, count: Int, velScale: Float) {
    if (particles.size > 180) return
    val color = if (side == TeamSide.PLAYER) Color(0xFFC4B5FD) else Color(0xFFFDA4AF)
    repeat(count) {
      particles.add(
        ParticleFx(
          x = x,
          y = y,
          vx = rnd(-50f, 50f) * velScale,
          vy = rnd(-90f, -20f) * velScale,
          life = 1f,
          maxLife = 1f,
          radius = rnd(1.8f, 3.8f),
          color = color,
        )
      )
    }
  }

  private fun applyKnockback(v: BattleUnit, dir: Float, pushSpeed: Float, liftSpeed: Float) {
    v.kbVx = dir * pushSpeed
    if (liftSpeed > v.vz) v.vz = liftSpeed
    v.airZ = max(v.airZ, 1f)
    v.recoilTilt = dir * 0.28f
  }

  private fun spawnDust(x: Float, y: Float, count: Int) {
    if (particles.size > 175) return
    repeat(count) {
      particles.add(
        ParticleFx(
          x = x + rnd(-10f, 10f),
          y = y + rnd(-4f, 4f),
          vx = rnd(-45f, 45f),
          vy = rnd(-42f, -12f),
          life = 0.45f,
          maxLife = 0.45f,
          radius = rnd(3.2f, 6.2f),
          color = Color(0xFFBDB4D6).copy(alpha = 0.55f),
        )
      )
    }
  }

  private fun addSlashArc(x: Float, y: Float, radius: Float, dir: Int, color: Color) {
    slashArcs.add(SlashArcFx(x, y, radius, dir, color, 0.24f, 0.24f))
  }

  private fun addRing(x: Float, y: Float, maxR: Float, color: Color, dir: Int = 0) {
    rings.add(ShockwaveRing(x, y, 6f, maxR, 1f, color, dir))
  }

  private fun addCallout(x: Float, y: Float, text: String, color: Color) {
    callouts.add(FloatingCallout(x, y, text, color, 1f))
  }

  private fun addLightningBolt(
    targetX: Float,
    targetY: Float,
    color: Color = Color(0xFFFDE047),
  ) {
    val pts = mutableListOf<Pair<Float, Float>>()
    val segs = 7
    for (i in 0..segs) {
      val t = i.toFloat() / segs
      val jitter = if (i == 0 || i == segs) 0f else rnd(-24f, 24f)
      pts.add((targetX + jitter) to (t * targetY))
    }
    bolts.add(LightningBolt(pts, 1f, color))
  }

  private fun rnd(a: Float, b: Float): Float = a + rng.nextFloat() * (b - a)
}

package com.example.ui.battle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.SoundEffect
import com.example.data.GameAssets
import com.example.game.BattleEngine
import com.example.game.BattleHudSnapshot
import com.example.game.BattleResultSummary
import com.example.model.BattleConfig
import com.example.model.BattleModeType
import com.example.model.GameCatalog
import com.example.model.GhostId
import com.example.model.JimatId
import com.example.model.PlayerProgress
import com.example.ui.battle.GhostRenderer.renderBattlefield
import com.example.ui.components.CoinPill
import com.example.ui.components.GameActionButton
import com.example.ui.theme.CursedCrimson
import com.example.ui.theme.CursedCrimsonDeep
import com.example.ui.theme.EctoplasmEmerald
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.MysticPink
import com.example.ui.theme.MysticalBorder
import com.example.ui.theme.MysticalSurface
import com.example.ui.theme.MysticalSurfaceHigh
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SpectralViolet
import com.example.ui.theme.SpectralVioletDeep
import com.example.ui.theme.SpiritCyan
import com.example.ui.theme.SpiritGold
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun BattleScreen(
  config: BattleConfig,
  progress: PlayerProgress,
  assets: GameAssets,
  battleSpeed: Int,
  isMuted: Boolean,
  hapticsEnabled: Boolean,
  onSound: (SoundEffect) -> Unit,
  onConsumeJimat: (JimatId) -> Unit,
  onBattleOutcome: (BattleResultSummary) -> Unit,
  onRetreatCoins: (coins: Int, enemies: Int, summoned: Int) -> Unit,
  onCycleSpeed: () -> Unit,
  onToggleMute: () -> Unit,
  onLaunchNextStage: (Int) -> Unit,
  onLaunchNextTournamentRound: (Int, Int) -> Unit,
  onLaunchNextInfinity: (Int) -> Unit,
  onReplay: () -> Unit,
  onNavigateToStages: () -> Unit,
  onNavigateToTournament: () -> Unit,
  onNavigateToFree: () -> Unit,
  onExitToMenu: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  var sessionId by remember { mutableLongStateOf(1L) }
  var isPaused by remember(config, sessionId) { mutableStateOf(false) }
  var resultSummary by remember(config, sessionId) { mutableStateOf<BattleResultSummary?>(null) }
  var showResultModal by remember(config, sessionId) { mutableStateOf(false) }
  var hintText by remember(config, sessionId) {
    mutableStateOf("Geser kartu di bawah, ketuk untuk memanggil pasukan hantu!")
  }
  var autoSummonEnabled by remember { mutableStateOf(false) }

  // Freeze initial progress snapshot per match start so upgrades stay stable mid-battle,
  // while reading latest jimat inventory dynamically from `progress`.
  val startProgress = remember(config, sessionId) { progress }

  val engine = remember(config, sessionId) {
    BattleEngine(
      config = config,
      progress = startProgress,
      onSound = onSound,
      onBattleEnded = { summary ->
        resultSummary = summary
        onBattleOutcome(summary)
        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      },
    ).apply { autoSummon = autoSummonEnabled }
  }

  var hud by remember(engine) { mutableStateOf(engine.hudSnapshot()) }
  var frameClockSec by remember { mutableFloatStateOf(0f) }

  BackHandler {
    if (resultSummary == null) isPaused = !isPaused else onExitToMenu()
  }

  // Delay result modal slightly so victory/defeat explosion particles are visible
  LaunchedEffect(resultSummary) {
    if (resultSummary != null) {
      kotlinx.coroutines.delay(750)
      showResultModal = true
    } else {
      showResultModal = false
    }
  }

  LaunchedEffect(engine, isPaused, battleSpeed) {
    var lastNanos = 0L
    while (true) {
      withFrameNanos { now ->
        if (lastNanos == 0L) lastNanos = now
        val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastNanos = now
        frameClockSec += dt
        if (!isPaused) {
          engine.step(dt, battleSpeed)
          hud = engine.hudSnapshot()
        }
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      if (!engine.isOver && (engine.coinsInMatch > 0 || engine.enemiesDefeated > 0)) {
        onRetreatCoins(engine.coinsInMatch, engine.enemiesDefeated, engine.ghostsSummoned)
      }
    }
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(listOf(Color(0xFF190E3B), MidnightIndigo, ObsidianNight))
        )
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // 1. TOP TACTICAL BAR
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MysticalSurfaceHigh,
        border = BorderStroke(1.dp, MysticalBorder),
      ) {
        IconButton(
          onClick = { isPaused = true },
          modifier = Modifier.size(48.dp).testTag("battle_pause_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Pause,
            contentDescription = stringResource(R.string.cd_pause),
            tint = InkPrimary,
          )
        }
      }

      // Battle Speed Pill (1x / 2x / 3x)
      Surface(
        onClick = onCycleSpeed,
        shape = RoundedCornerShape(14.dp),
        color = if (battleSpeed > 1) SpectralVioletDeep else MysticalSurfaceHigh,
        border = BorderStroke(1.dp, if (battleSpeed > 1) SpectralViolet else MysticalBorder),
        modifier = Modifier.heightIn(min = 48.dp).testTag("battle_speed_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Speed,
            contentDescription = stringResource(R.string.cd_speed),
            tint = InkPrimary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "${battleSpeed}x",
            style = MaterialTheme.typography.labelMedium,
            color = InkPrimary,
          )
        }
      }

      // Auto-Summon Toggle
      Surface(
        onClick = {
          autoSummonEnabled = !autoSummonEnabled
          engine.autoSummon = autoSummonEnabled
          onSound(SoundEffect.CLICK)
        },
        shape = RoundedCornerShape(14.dp),
        color = if (autoSummonEnabled) Color(0xFF065F46) else MysticalSurfaceHigh,
        border = BorderStroke(1.dp, if (autoSummonEnabled) EctoplasmEmerald else MysticalBorder),
        modifier = Modifier.heightIn(min = 48.dp).testTag("battle_auto_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.AutoMode,
            contentDescription = "Mode panggil otomatis",
            tint = if (autoSummonEnabled) EctoplasmEmerald else InkMuted,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = if (autoSummonEnabled) "AUTO" else "MANUAL",
            style = MaterialTheme.typography.labelSmall,
            color = if (autoSummonEnabled) EctoplasmEmerald else InkMuted,
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      CoinPill(coins = progress.coins + hud.coinsInMatch)

      Surface(
        shape = CircleShape,
        color = MysticalSurfaceHigh,
        border = BorderStroke(1.dp, MysticalBorder),
      ) {
        IconButton(
          onClick = onToggleMute,
          modifier = Modifier.size(48.dp).testTag("battle_mute_button"),
        ) {
          Icon(
            imageVector =
              if (isMuted) Icons.AutoMirrored.Filled.VolumeOff
              else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = stringResource(R.string.cd_sound_toggle),
            tint = if (isMuted) Color(0xFF94A3B8) else SpectralViolet,
          )
        }
      }
    }

    // 2. SOUL BAR (ARWAH) + IN-BATTLE SUMUR BOOST
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val soulRatio by animateFloatAsState(
        targetValue = (hud.souls / hud.maxSouls).coerceIn(0f, 1f),
        label = "soulRatio",
      )
      Box(
        modifier =
          Modifier
            .weight(1f)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(MysticalSurface)
            .border(1.2.dp, SpectralViolet.copy(alpha = 0.55f), RoundedCornerShape(17.dp)),
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxHeight()
              .fillMaxWidth(soulRatio)
              .background(
                Brush.horizontalGradient(listOf(Color(0xFFD97706), SpiritGold, Color(0xFFFDE047)))
              )
        )
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "🔥 Arwah ${hud.souls.toInt()} / ${hud.maxSouls.toInt()}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White,
          )
          Text(
            text = "+${hud.soulRegenPerSec.roundToInt()}/s",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFEF08A),
          )
        }
      }

      // In-battle Sumur Arwah upgrade button
      val canUpgradeFountain =
        !hud.isOver &&
          hud.fountainLevel < BattleEngine.MAX_FOUNTAIN_LEVEL &&
          hud.souls >= hud.fountainUpgradeCost
      Surface(
        onClick = {
          if (engine.upgradeFountain()) {
            hud = engine.hudSnapshot()
            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          }
        },
        enabled = canUpgradeFountain,
        shape = RoundedCornerShape(14.dp),
        color = if (canUpgradeFountain) Color(0xFF0E7490) else MysticalSurface,
        border =
          BorderStroke(
            1.2.dp,
            if (canUpgradeFountain) SpiritCyan else MysticalBorder.copy(alpha = 0.6f),
          ),
        modifier = Modifier.heightIn(min = 48.dp).testTag("fountain_upgrade_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = "Tingkatkan Sumur Arwah",
            tint = if (canUpgradeFountain) Color(0xFFBAE6FD) else InkMuted,
            modifier = Modifier.size(16.dp),
          )
          Column {
            Text(
              text =
                if (hud.fountainLevel >= BattleEngine.MAX_FOUNTAIN_LEVEL) "SUMUR MAKS"
                else "Sumur Lv.${hud.fountainLevel}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (canUpgradeFountain) Color.White else InkMuted,
            )
            if (hud.fountainLevel < BattleEngine.MAX_FOUNTAIN_LEVEL) {
              Text(
                text = "🔥 ${hud.fountainUpgradeCost}",
                style = MaterialTheme.typography.labelSmall,
                color = if (canUpgradeFountain) SpiritGold else InkMuted,
              )
            }
          }
        }
      }
    }

    // 3. HD BATTLEFIELD ARENA + BASE STATUS HUD + WAVE BANNER
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
      val arenaModifier =
        if (maxWidth > 620.dp) {
          Modifier.fillMaxWidth().widthIn(max = 760.dp).aspectRatio(800f / 440f)
        } else {
          Modifier.fillMaxWidth().aspectRatio(800f / 440f)
        }

      Box(
        modifier =
          arenaModifier
            .align(Alignment.Center)
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, SpectralViolet.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          // Force frame invalidation each tick via frameClockSec
          val tick = frameClockSec
          renderBattlefield(engine = engine, assets = assets, clockSec = tick)
        }

        // Active Buff Badges (top-right inside arena)
        Row(
          modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (hud.kerisRemaining > 0f) {
            BuffPill("🗡️ ${ceil(hud.kerisRemaining).toInt()}s", SpiritGold)
          }
          if (hud.kemenyanRemaining > 0f) {
            BuffPill("🕯️ ${ceil(hud.kemenyanRemaining).toInt()}s", SpiritCyan)
          }
          if (hud.garamRemaining > 0f) {
            BuffPill("🧂 ${ceil(hud.garamRemaining).toInt()}s", Color(0xFFE2E8F0))
          }
        }

        // Bottom HUD overlay showing precise Base HP numbers + unit counts
        Row(
          modifier =
            Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .background(Color.Black.copy(alpha = 0.45f))
              .padding(horizontal = 10.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "🏯 ${hud.playerHp.roundToInt()}/${hud.playerMaxHp.roundToInt()} · ${hud.playerUnitCount}/22",
            style = MaterialTheme.typography.labelSmall,
            color = EctoplasmEmerald,
          )
          Text(
            text = "${hud.elapsedSec}d",
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
          )
          Text(
            text = "${hud.enemyUnitCount}/22 · 💀 ${hud.enemyHp.roundToInt()}/${hud.enemyMaxHp.roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = CursedCrimson,
          )
        }

        // Boss / Threat Warning Banner
        androidx.compose.animation.AnimatedVisibility(
          visible = hud.activeBanner != null && !showResultModal,
          enter = slideInVertically { -it } + fadeIn(),
          exit = slideOutVertically { -it } + fadeOut(),
          modifier = Modifier.align(Alignment.TopCenter).padding(top = 34.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = CursedCrimsonDeep.copy(alpha = 0.92f),
            border = BorderStroke(1.2.dp, SpiritGold),
          ) {
            Text(
              text = "⚠️ ${hud.activeBanner ?: ""}",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
          }
        }

        // Victory / Defeat Result Overlay
        if (showResultModal && resultSummary != null) {
          BattleResultOverlay(
            summary = resultSummary!!,
            onNextStage = { onLaunchNextStage(config.stageIndex + 1) },
            onNextTournament = {
              onLaunchNextTournamentRound(config.tournamentIndex, config.tournamentRound + 1)
            },
            onNextInfinity = { onLaunchNextInfinity(config.infinityLevel + 1) },
            onReplay = {
              sessionId++
              onReplay()
            },
            onStages = onNavigateToStages,
            onTournament = onNavigateToTournament,
            onFree = onNavigateToFree,
            onMenu = onExitToMenu,
          )
        }
      }
    }

    // 4. JIMAT CONSUMABLE HOTBAR (only owned jimats shown)
    val ownedJimats = JimatId.entries.filter { progress.jimatCount(it) > 0 }
    if (ownedJimats.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ownedJimats.forEach { j ->
          val qty = progress.jimatCount(j)
          val activeSec =
            when (j) {
              JimatId.KERIS -> hud.kerisRemaining
              JimatId.KEMENYAN -> hud.kemenyanRemaining
              JimatId.GARAM -> hud.garamRemaining
              else -> 0f
            }
          JimatBattleChip(
            jimat = j,
            qty = qty,
            activeSec = activeSec,
            iconBitmap = assets.icons[j.key],
            enabled = !hud.isOver,
            onClick = {
              if (engine.useJimat(j)) {
                onConsumeJimat(j)
                hintText = "${j.title}: ${j.desc}"
                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              }
            },
          )
        }
      }
    }

    // 5. GHOST SUMMON CARD DECK
    val ownedGhosts = GhostId.ordered.filter { it in progress.ownedGhosts }
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ownedGhosts.forEach { ghost ->
        val cdRem = hud.cooldowns[ghost] ?: 0f
        val cdMax = hud.maxCooldowns[ghost] ?: ghost.cooldown
        val affordable = hud.souls >= ghost.cost
        val ready = !hud.isOver && affordable && cdRem <= 0f
        GhostSummonCard(
          ghost = ghost,
          level = progress.ghostLevel(ghost),
          cardBitmap = assets.cards[ghost],
          cooldownRatio = if (cdMax > 0f) (cdRem / cdMax).coerceIn(0f, 1f) else 0f,
          cooldownSec = cdRem,
          ready = ready,
          affordable = affordable,
          onClick = {
            hintText = "${ghost.displayName} (Lv.${progress.ghostLevel(ghost)}): ${ghost.skillSummary}"
            if (engine.summonPlayerGhost(ghost)) {
              hud = engine.hudSnapshot()
              if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
          },
        )
      }
    }

    // 6. TACTICAL HINT LINE
    Text(
      text = hintText,
      style = MaterialTheme.typography.bodySmall,
      color = InkMuted,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
    )
  }

  // PAUSE DIALOG
  if (isPaused && resultSummary == null) {
    AlertDialog(
      onDismissRequest = { isPaused = false },
      containerColor = MysticalSurface,
      titleContentColor = InkPrimary,
      textContentColor = InkMuted,
      title = {
        Text("Pertempuran Dijeda", style = MaterialTheme.typography.headlineMedium)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(config.label, style = MaterialTheme.typography.titleMedium, color = SpectralViolet)
          Text(
            "Koin yang sudah didapat di medan tempur (${hud.coinsInMatch} 🪙) tetap tersimpan jika kamu mundur sekarang.",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      },
      confirmButton = {
        GameActionButton(
          text = "Lanjutkan",
          onClick = { isPaused = false },
          modifier = Modifier.testTag("resume_battle_button"),
        )
      },
      dismissButton = {
        GameActionButton(
          text = "Keluar ke Menu",
          containerColor = CursedCrimsonDeep,
          onClick = {
            isPaused = false
            onRetreatCoins(engine.coinsInMatch, engine.enemiesDefeated, engine.ghostsSummoned)
            onExitToMenu()
          },
          modifier = Modifier.testTag("quit_battle_button"),
        )
      },
    )
  }
}

@Composable
private fun BuffPill(label: String, color: Color) {
  Surface(
    shape = CircleShape,
    color = ObsidianNight.copy(alpha = 0.8f),
    border = BorderStroke(1.dp, color),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

@Composable
private fun JimatBattleChip(
  jimat: JimatId,
  qty: Int,
  activeSec: Float,
  iconBitmap: ImageBitmap?,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(12.dp),
    color = MysticalSurfaceHigh,
    border = BorderStroke(1.2.dp, if (activeSec > 0f) jimat.color else SpiritGold.copy(alpha = 0.55f)),
    modifier = Modifier.heightIn(min = 48.dp).testTag("jimat_btn_${jimat.key}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (iconBitmap != null) {
        Image(
          bitmap = iconBitmap,
          contentDescription = jimat.title,
          contentScale = ContentScale.Crop,
          filterQuality = FilterQuality.High,
          modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)),
        )
      }
      Column {
        Text(
          text = "${jimat.title.substringBefore(' ')} x$qty",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = InkPrimary,
        )
        if (activeSec > 0f) {
          Text(
            text = "${ceil(activeSec).toInt()}d aktif",
            style = MaterialTheme.typography.labelSmall,
            color = jimat.color,
          )
        }
      }
    }
  }
}

@Composable
private fun GhostSummonCard(
  ghost: GhostId,
  level: Int,
  cardBitmap: ImageBitmap?,
  cooldownRatio: Float,
  cooldownSec: Float,
  ready: Boolean,
  affordable: Boolean,
  onClick: () -> Unit,
) {
  val borderColor =
    when {
      ready -> SpiritGold
      affordable -> SpectralViolet.copy(alpha = 0.45f)
      else -> MysticalBorder
    }

  Box(
    modifier =
      Modifier
        .width(86.dp)
        .height(114.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(MysticalSurface)
        .border(if (ready) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
        .alpha(if (ready) 1f else 0.58f)
        .clickable(onClick = onClick)
        .testTag("summon_card_${ghost.key}"),
  ) {
    if (cardBitmap != null) {
      Image(
        bitmap = cardBitmap,
        contentDescription = ghost.displayName,
        contentScale = ContentScale.Crop,
        filterQuality = FilterQuality.High,
        modifier = Modifier.fillMaxSize(),
      )
    }

    // Cooldown vertical veil
    if (cooldownRatio > 0f) {
      Box(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(cooldownRatio)
            .background(ObsidianNight.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center,
      ) {
        if (cooldownSec >= 0.3f) {
          Text(
            text = String.format("%.1fs", cooldownSec),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
        }
      }
    }

    // Cost pill top-left
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = ObsidianNight.copy(alpha = 0.88f),
      border = BorderStroke(1.dp, SpiritGold.copy(alpha = 0.75f)),
      modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
    ) {
      Text(
        text = "🔥${ghost.cost}",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = if (affordable) SpiritGold else CursedCrimson,
        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
      )
    }

    // Level tag top-right if trained
    if (level > 0) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = SpectralVioletDeep.copy(alpha = 0.9f),
        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
      ) {
        Text(
          text = "Lv$level",
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
      }
    }

    // Name bar at bottom
    Box(
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .background(ObsidianNight.copy(alpha = 0.8f))
          .padding(horizontal = 4.dp, vertical = 3.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = ghost.displayName,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = InkPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun BattleResultOverlay(
  summary: BattleResultSummary,
  onNextStage: () -> Unit,
  onNextTournament: () -> Unit,
  onNextInfinity: () -> Unit,
  onReplay: () -> Unit,
  onStages: () -> Unit,
  onTournament: () -> Unit,
  onFree: () -> Unit,
  onMenu: () -> Unit,
) {
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(ObsidianNight.copy(alpha = 0.90f))
        .padding(12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = MysticalSurface),
      border =
        BorderStroke(
          1.5.dp,
          if (summary.won) SpiritGold else CursedCrimson,
        ),
      shape = RoundedCornerShape(18.dp),
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = if (summary.won) "MENANG!" else "KALAH",
          style = MaterialTheme.typography.displaySmall,
          color = if (summary.won) SpiritGold else CursedCrimson,
        )
        Text(
          text = summary.message,
          style = MaterialTheme.typography.bodyMedium,
          color = InkPrimary,
          textAlign = TextAlign.Center,
        )

        // Battle stats breakdown
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MysticalSurfaceHigh)
              .padding(horizontal = 10.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          StatMini("Koin", "+${summary.totalCoinsEarned} 🪙", SpiritGold)
          StatMini("Musuh", "${summary.enemiesDefeated}", CursedCrimson)
          StatMini("Panggil", "${summary.ghostsSummoned}", SpectralViolet)
          StatMini("Durasi", "${summary.durationSec}d", SpiritCyan)
        }

        // Action buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
          when (summary.config.mode) {
            BattleModeType.STAGE -> {
              if (summary.won && summary.config.stageIndex < GameCatalog.stages.lastIndex) {
                GameActionButton(
                  text = "Stage Berikut",
                  onClick = onNextStage,
                  containerColor = EctoplasmEmerald,
                  contentColor = ObsidianNight,
                  modifier = Modifier.weight(1f).testTag("result_next_stage_button"),
                )
              }
              GameActionButton(
                text = "Ulangi",
                onClick = onReplay,
                modifier = Modifier.weight(1f).testTag("result_replay_button"),
              )
              GameActionButton(
                text = "Stage",
                onClick = onStages,
                containerColor = MysticalSurfaceHigh,
                modifier = Modifier.weight(1f).testTag("result_stages_button"),
              )
            }
            BattleModeType.TOURNAMENT -> {
              val t = GameCatalog.tournaments[summary.config.tournamentIndex]
              if (summary.won && summary.config.tournamentRound < t.rounds - 1) {
                GameActionButton(
                  text = "Babak Berikut",
                  onClick = onNextTournament,
                  containerColor = SpiritGold,
                  contentColor = ObsidianNight,
                  modifier = Modifier.weight(1f).testTag("result_next_round_button"),
                )
              } else {
                GameActionButton(
                  text = "Turnamen",
                  onClick = onTournament,
                  modifier = Modifier.weight(1f),
                )
              }
              GameActionButton(
                text = "Menu",
                onClick = onMenu,
                containerColor = MysticalSurfaceHigh,
                modifier = Modifier.weight(1f),
              )
            }
            BattleModeType.INFINITY -> {
              if (summary.won) {
                GameActionButton(
                  text = "Level ${summary.config.infinityLevel + 1}",
                  onClick = onNextInfinity,
                  containerColor = SpiritCyan,
                  contentColor = ObsidianNight,
                  modifier = Modifier.weight(1f).testTag("result_next_inf_button"),
                )
              } else {
                GameActionButton(
                  text = "Ulangi",
                  onClick = onReplay,
                  modifier = Modifier.weight(1f),
                )
              }
              GameActionButton(
                text = "Menu",
                onClick = onMenu,
                containerColor = MysticalSurfaceHigh,
                modifier = Modifier.weight(1f),
              )
            }
            BattleModeType.FREE -> {
              GameActionButton(
                text = "Main Lagi",
                onClick = onReplay,
                modifier = Modifier.weight(1f),
              )
              GameActionButton(
                text = "Pilih Arena",
                onClick = onFree,
                containerColor = MysticalSurfaceHigh,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatMini(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.labelMedium, color = color)
    Text(label, style = MaterialTheme.typography.labelSmall, color = InkMuted)
  }
}

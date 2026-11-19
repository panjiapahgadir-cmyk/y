package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GameAssets
import com.example.game.BattleUnit
import com.example.game.TeamSide
import com.example.model.AchievementCatalog
import com.example.model.GameCatalog
import com.example.model.GhostId
import com.example.model.PlayerProgress
import com.example.ui.battle.GhostRenderer.drawUnit
import com.example.ui.components.CenteredAdaptiveColumn
import com.example.ui.components.CoinPill
import com.example.ui.components.GameActionButton
import com.example.ui.components.MysticBackdrop
import com.example.ui.theme.CursedCrimson
import com.example.ui.theme.EctoplasmEmerald
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.MysticPink
import com.example.ui.theme.MysticalBorder
import com.example.ui.theme.MysticalSurface
import com.example.ui.theme.MysticalSurfaceHigh
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SpectralViolet
import com.example.ui.theme.SpectralVioletDeep
import com.example.ui.theme.SpiritCyan
import com.example.ui.theme.SpiritGold

@Composable
fun HomeScreen(
  progress: PlayerProgress,
  assets: GameAssets,
  isMuted: Boolean,
  onToggleMute: () -> Unit,
  onQuickPlayStage: (Int) -> Unit,
  onOpenStages: () -> Unit,
  onOpenTournament: () -> Unit,
  onOpenInfinity: () -> Unit,
  onOpenFreeMode: () -> Unit,
  onOpenShop: () -> Unit,
  onOpenKitab: () -> Unit,
  onOpenMissions: () -> Unit,
) {
  val claimableMissions =
    remember(progress) {
      AchievementCatalog.all.count { def ->
        def.id !in progress.claimedAchievements &&
          def.progressProvider(progress).let { (done, target) -> done >= target }
      }
    }

  val nextStageIndex = progress.clearStage.coerceIn(0, GameCatalog.stages.lastIndex)
  val nextStage = GameCatalog.stages[nextStageIndex]
  val spotlightGhost =
    remember(progress.ownedGhosts) {
      GhostId.ordered.lastOrNull { it in progress.ownedGhosts } ?: GhostId.POCONG
    }

  MysticBackdrop {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      CenteredAdaptiveColumn {
        // Top Utility Bar: Brand + Coins + Sound
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "PERANG HANTU",
              style = MaterialTheme.typography.displaySmall,
              color = InkPrimary,
            )
            Text(
              text = "Strategi Pasukan Gaib Nusantara",
              style = MaterialTheme.typography.bodySmall,
              color = SpectralViolet,
            )
          }

          CoinPill(coins = progress.coins)

          Surface(
            shape = CircleShape,
            color = MysticalSurfaceHigh,
            border = BorderStroke(1.dp, MysticalBorder),
          ) {
            IconButton(
              onClick = onToggleMute,
              modifier = Modifier.size(48.dp).testTag("home_mute_button"),
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

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Card with Live Animated Spotlight Ghost & Quick Launch
        HeroBannerCard(
          progress = progress,
          spotlightGhost = spotlightGhost,
          nextStageIndex = nextStageIndex,
          nextStageName = nextStage.name,
          assets = assets,
          onQuickPlay = { onQuickPlayStage(nextStageIndex) },
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Section Label: Arena Pertempuran
        Text(
          text = "ARENA PERTEMPURAN",
          style = MaterialTheme.typography.labelLarge,
          color = InkMuted,
          modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
          val twoCols = maxWidth >= 520.dp
          if (twoCols) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeFeatureCard(
                  title = "Mode Cerita",
                  subtitle = "${progress.clearStage}/10 Stage selesai",
                  icon = Icons.Filled.Explore,
                  accent = SpectralViolet,
                  badge = "UTAMA",
                  onClick = onOpenStages,
                  modifier = Modifier.weight(1f).testTag("menu_stages_card"),
                )
                ModeFeatureCard(
                  title = "Turnamen",
                  subtitle = "${progress.totalTrophies} Piala diraih",
                  icon = Icons.Filled.EmojiEvents,
                  accent = SpiritGold,
                  badge = if (progress.totalTrophies > 0) "🏆 ${progress.totalTrophies}" else "3 PIALA",
                  onClick = onOpenTournament,
                  modifier = Modifier.weight(1f).testTag("menu_tournament_card"),
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeFeatureCard(
                  title = "Infinity",
                  subtitle = "Rekor Level ${progress.infinityRecord.coerceAtLeast(1)}",
                  icon = Icons.Filled.AllInclusive,
                  accent = SpiritCyan,
                  badge = "SURVIVAL",
                  onClick = onOpenInfinity,
                  modifier = Modifier.weight(1f).testTag("menu_infinity_card"),
                )
                ModeFeatureCard(
                  title = "Mode Bebas",
                  subtitle = "Latih taktik di 10 arena",
                  icon = Icons.Filled.SportsEsports,
                  accent = EctoplasmEmerald,
                  badge = "BEBAS",
                  onClick = onOpenFreeMode,
                  modifier = Modifier.weight(1f).testTag("menu_free_card"),
                )
              }
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              ModeFeatureCard(
                title = "Mode Cerita (10 Stage)",
                subtitle = "Progres: ${progress.clearStage}/10 Stage ditaklukkan",
                icon = Icons.Filled.Explore,
                accent = SpectralViolet,
                badge = "UTAMA",
                onClick = onOpenStages,
                modifier = Modifier.fillMaxWidth().testTag("menu_stages_card"),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeFeatureCard(
                  title = "Turnamen",
                  subtitle = "${progress.totalTrophies} Piala diraih",
                  icon = Icons.Filled.EmojiEvents,
                  accent = SpiritGold,
                  badge = "3 PIALA",
                  onClick = onOpenTournament,
                  modifier = Modifier.weight(1f).testTag("menu_tournament_card"),
                )
                ModeFeatureCard(
                  title = "Infinity",
                  subtitle = "Rekor Lv.${progress.infinityRecord.coerceAtLeast(1)}",
                  icon = Icons.Filled.AllInclusive,
                  accent = SpiritCyan,
                  badge = "TAHAN",
                  onClick = onOpenInfinity,
                  modifier = Modifier.weight(1f).testTag("menu_infinity_card"),
                )
              }
              ModeFeatureCard(
                title = "Mode Bebas",
                subtitle = "Pilih tingkat kesulitan & arena mana saja untuk eksperimen pasukan",
                icon = Icons.Filled.SportsEsports,
                accent = EctoplasmEmerald,
                badge = "LATIHAN",
                onClick = onOpenFreeMode,
                modifier = Modifier.fillMaxWidth().testTag("menu_free_card"),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section Label: Markas & Pasukan
        Text(
          text = "MARKAS & PASUKAN",
          style = MaterialTheme.typography.labelLarge,
          color = InkMuted,
          modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          UtilityDockTile(
            title = "Toko Dukun",
            subtitle = "Jimat & Upgrade",
            icon = Icons.Filled.Storefront,
            accent = SpiritGold,
            badgeCount = 0,
            onClick = onOpenShop,
            modifier = Modifier.weight(1f).testTag("menu_shop_button"),
          )
          UtilityDockTile(
            title = "Kitab Hantu",
            subtitle = "${progress.ownedGhosts.size}/${GhostId.entries.size} Pasukan",
            icon = Icons.Filled.AutoStories,
            accent = MysticPink,
            badgeCount = 0,
            onClick = onOpenKitab,
            modifier = Modifier.weight(1f).testTag("menu_kitab_button"),
          )
          UtilityDockTile(
            title = "Misi",
            subtitle = if (claimableMissions > 0) "Siap Klaim!" else "Prestasi",
            icon = Icons.Filled.MilitaryTech,
            accent = EctoplasmEmerald,
            badgeCount = claimableMissions,
            onClick = onOpenMissions,
            modifier = Modifier.weight(1f).testTag("menu_missions_button"),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun HeroBannerCard(
  progress: PlayerProgress,
  spotlightGhost: GhostId,
  nextStageIndex: Int,
  nextStageName: String,
  assets: GameAssets,
  onQuickPlay: () -> Unit,
) {
  var clockSec by remember { mutableFloatStateOf(0f) }
  val previewUnit =
    remember(spotlightGhost) {
      BattleUnit(
        uid = 999L,
        side = TeamSide.PLAYER,
        ghost = spotlightGhost,
        x = 0f,
        y = 0f,
        hp = spotlightGhost.baseHp,
        maxHp = spotlightGhost.baseHp,
        dmg = spotlightGhost.baseDmg,
        isMoving = true,
      )
    }

  LaunchedEffect(previewUnit) {
    var last = 0L
    while (true) {
      withFrameNanos { now ->
        if (last == 0L) last = now
        val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
        last = now
        clockSec += dt
        previewUnit.phase += dt * 5f
      }
    }
  }

  val inf = rememberInfiniteTransition(label = "heroGlow")
  val pulse by
    inf.animateFloat(
      initialValue = 0.35f,
      targetValue = 0.72f,
      animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "pulse",
    )

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border = BorderStroke(1.4.dp, SpectralViolet.copy(alpha = 0.65f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              colors = listOf(Color(0xFF28195B), MysticalSurface, Color(0xFF1F1242))
            )
          )
          .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = ObsidianNight.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, SpiritGold.copy(alpha = 0.6f)),
          ) {
            Text(
              text = "⚜️ ${progress.playerRank}",
              style = MaterialTheme.typography.labelSmall,
              color = SpiritGold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }

          Text(
            text =
              if (progress.clearStage >= GameCatalog.stages.size) "Penakluk Alam Gaib"
              else "Stage ${nextStageIndex + 1}: $nextStageName",
            style = MaterialTheme.typography.headlineMedium,
            color = InkPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniHeroStat("Menang", "${progress.totalVictories}", EctoplasmEmerald)
            MiniHeroStat("Piala", "${progress.totalTrophies}", SpiritGold)
            MiniHeroStat("Pasukan", "${progress.ownedGhosts.size}/${GhostId.entries.size}", SpectralViolet)
          }

          Spacer(modifier = Modifier.height(2.dp))

          GameActionButton(
            text =
              if (progress.clearStage >= GameCatalog.stages.size) "Main Stage 10"
              else "Serang Stage ${nextStageIndex + 1}",
            onClick = onQuickPlay,
            containerColor = SpiritGold,
            contentColor = ObsidianNight,
            modifier = Modifier.fillMaxWidth().testTag("quick_play_button"),
          )
        }

        // Live animated ghost showcase pedestal
        Box(
          modifier =
            Modifier
              .size(118.dp, 134.dp)
              .clip(RoundedCornerShape(18.dp))
              .background(ObsidianNight.copy(alpha = 0.55f))
              .border(1.dp, spotlightGhost.accentColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
          contentAlignment = Alignment.BottomCenter,
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val tick = clockSec
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors = listOf(spotlightGhost.accentColor.copy(alpha = pulse * 0.45f), Color.Transparent),
                  center = Offset(size.width * 0.5f, size.height * 0.58f),
                  radius = size.width * 0.55f,
                ),
              radius = size.width * 0.55f,
              center = Offset(size.width * 0.5f, size.height * 0.58f),
            )
            withTransform({
              translate(size.width * 0.5f, size.height * 0.80f)
              scale(0.95f, 0.95f, pivot = Offset.Zero)
            }) {
              drawUnit(
                u = previewUnit,
                assets = assets,
                kerisActive = false,
                clockSec = tick,
              )
            }
          }
          Text(
            text = spotlightGhost.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = InkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
              Modifier
                .fillMaxWidth()
                .background(ObsidianNight.copy(alpha = 0.72f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun MiniHeroStat(label: String, value: String, color: Color) {
  Column {
    Text(value, style = MaterialTheme.typography.labelMedium, color = color)
    Text(label, style = MaterialTheme.typography.labelSmall, color = InkMuted)
  }
}

@Composable
private fun ModeFeatureCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  badge: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border = BorderStroke(1.2.dp, accent.copy(alpha = 0.45f)),
    modifier = modifier.heightIn(min = 86.dp),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(
            Brush.horizontalGradient(listOf(accent.copy(alpha = 0.14f), Color.Transparent))
          )
          .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.18f))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(imageVector = icon, contentDescription = title, tint = accent, modifier = Modifier.size(24.dp))
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = InkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = accent.copy(alpha = 0.18f),
          ) {
            Text(
              text = badge,
              style = MaterialTheme.typography.labelSmall,
              color = accent,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = InkMuted,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Icon(
        imageVector = Icons.Filled.PlayArrow,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun UtilityDockTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  badgeCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurfaceHigh),
    border = BorderStroke(1.1.dp, if (badgeCount > 0) accent else MysticalBorder),
    modifier = modifier.heightIn(min = 92.dp),
  ) {
    Box(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
      if (badgeCount > 0) {
        Surface(
          shape = CircleShape,
          color = CursedCrimson,
          modifier = Modifier.align(Alignment.TopEnd),
        ) {
          Text(
            text = badgeCount.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }

      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(accent.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(imageVector = icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
        }
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = InkPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = if (badgeCount > 0) accent else InkMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GameAssets
import com.example.game.BattleEngine
import com.example.model.FreeDifficulty
import com.example.model.GameCatalog
import com.example.model.PlayerProgress
import com.example.model.StageDef
import com.example.model.TournamentDef
import com.example.ui.battle.GhostRenderer.drawPuraGelapBackground
import com.example.ui.components.AssetArtBox
import com.example.ui.components.CenteredAdaptiveColumn
import com.example.ui.components.GameActionButton
import com.example.ui.components.GameTopBar
import com.example.ui.components.MysticBackdrop
import com.example.ui.theme.EctoplasmEmerald
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.MysticalBorder
import com.example.ui.theme.MysticalSurface
import com.example.ui.theme.MysticalSurfaceHigh
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SpectralViolet
import com.example.ui.theme.SpiritCyan
import com.example.ui.theme.SpiritGold
import kotlin.math.roundToInt

@Composable
fun StageSelectScreen(
  progress: PlayerProgress,
  assets: GameAssets,
  isMuted: Boolean,
  onBack: () -> Unit,
  onToggleMute: () -> Unit,
  onSelectStage: (Int) -> Unit,
) {
  MysticBackdrop {
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
      GameTopBar(
        title = "Pilih Stage",
        coins = progress.coins,
        isMuted = isMuted,
        onBack = onBack,
        onToggleMute = onToggleMute,
      )

      LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(GameCatalog.stages, key = { it.index }) { stage ->
          CenteredAdaptiveColumn {
            val unlocked = stage.index <= progress.clearStage
            val completed = stage.index < progress.clearStage
            StageListCard(
              stage = stage,
              unlocked = unlocked,
              completed = completed,
              assets = assets,
              onClick = { if (unlocked) onSelectStage(stage.index) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StageListCard(
  stage: StageDef,
  unlocked: Boolean,
  completed: Boolean,
  assets: GameAssets,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    enabled = unlocked,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border =
      BorderStroke(
        width = 1.2.dp,
        color = if (unlocked) stage.tint.copy(alpha = 0.65f) else MysticalBorder.copy(alpha = 0.5f),
      ),
    modifier =
      Modifier
        .fillMaxWidth()
        .alpha(if (unlocked) 1f else 0.52f)
        .testTag("stage_card_${stage.index}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Stage Arena Preview Thumbnail
      StageThumbnailBox(
        stage = stage,
        assets = assets,
        modifier = Modifier.size(92.dp, 64.dp),
      )

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = stage.tint.copy(alpha = 0.22f),
          ) {
            Text(
              text = "STAGE ${stage.index + 1}",
              style = MaterialTheme.typography.labelSmall,
              color = stage.tint,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          if (completed) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = "Selesai",
              tint = EctoplasmEmerald,
              modifier = Modifier.size(16.dp),
            )
          } else if (!unlocked) {
            Icon(
              imageVector = Icons.Filled.Lock,
              contentDescription = "Terkunci",
              tint = InkMuted,
              modifier = Modifier.size(15.dp),
            )
          }
        }

        Text(
          text = stage.name,
          style = MaterialTheme.typography.titleMedium,
          color = InkPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "HP ${stage.baseHp.roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
          )
          Text(
            text = "+${stage.reward} 🪙",
            style = MaterialTheme.typography.labelSmall,
            color = SpiritGold,
          )
        }

        // Enemy lineup mini portraits
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          stage.weights.keys.take(5).forEach { ghost ->
            AssetArtBox(
              bitmap = assets.cards[ghost],
              contentDescription = ghost.displayName,
              cornerRadius = 5.dp,
              borderColor = ghost.accentColor.copy(alpha = 0.5f),
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }

      if (unlocked) {
        Box(
          modifier =
            Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(stage.tint.copy(alpha = 0.22f))
              .border(1.dp, stage.tint, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Main Stage ${stage.index + 1}",
            tint = InkPrimary,
          )
        }
      }
    }
  }
}

@Composable
fun TournamentScreen(
  progress: PlayerProgress,
  isMuted: Boolean,
  onBack: () -> Unit,
  onToggleMute: () -> Unit,
  onStartTournament: (Int) -> Unit,
) {
  MysticBackdrop {
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
      GameTopBar(
        title = "Mode Turnamen",
        coins = progress.coins,
        isMuted = isMuted,
        onBack = onBack,
        onToggleMute = onToggleMute,
      )

      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CenteredAdaptiveColumn {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MysticalSurfaceHigh,
            border = BorderStroke(1.dp, SpiritGold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Menangkan seluruh babak beruntun untuk membawa pulang piala dan hadiah koin besar!",
              style = MaterialTheme.typography.bodyMedium,
              color = InkPrimary,
              modifier = Modifier.padding(12.dp),
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GameCatalog.tournaments.forEach { t ->
              TournamentCupCard(
                cup = t,
                wins = progress.trophyCount(t.index),
                canAfford = progress.coins >= t.entryFee,
                onJoin = { onStartTournament(t.index) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TournamentCupCard(
  cup: TournamentDef,
  wins: Int,
  canAfford: Boolean,
  onJoin: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border = BorderStroke(1.4.dp, cup.color.copy(alpha = 0.65f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(Brush.horizontalGradient(listOf(cup.color.copy(alpha = 0.14f), Color.Transparent)))
          .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(54.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(cup.color.copy(alpha = 0.2f))
              .border(1.dp, cup.color, RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Text(cup.badge, style = MaterialTheme.typography.displaySmall)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(cup.name, style = MaterialTheme.typography.titleLarge, color = InkPrimary)
          Text(
            text = "${cup.rounds} Babak · Kekuatan lawan x${cup.startMul}+",
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
          )
          Text(
            text = if (wins > 0) "Juara ${wins}x" else "Belum pernah juara",
            style = MaterialTheme.typography.labelSmall,
            color = if (wins > 0) EctoplasmEmerald else InkMuted,
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text("HADIAH", style = MaterialTheme.typography.labelSmall, color = InkMuted)
          Text("+${cup.prize} 🪙", style = MaterialTheme.typography.titleMedium, color = SpiritGold)
        }
      }

      GameActionButton(
        text =
          if (cup.entryFee == 0) "Ikut Turnamen (Gratis)"
          else "Daftar & Main (${cup.entryFee} 🪙)",
        enabled = canAfford,
        onClick = onJoin,
        containerColor = if (canAfford) cup.color else MysticalSurfaceHigh,
        contentColor = if (canAfford) ObsidianNight else InkMuted,
        modifier = Modifier.fillMaxWidth().testTag("tournament_join_${cup.index}"),
      )
    }
  }
}

@Composable
fun InfinityScreen(
  progress: PlayerProgress,
  isMuted: Boolean,
  onBack: () -> Unit,
  onToggleMute: () -> Unit,
  onStartLevel: (Int) -> Unit,
) {
  MysticBackdrop {
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
      GameTopBar(
        title = "Mode Infinity",
        coins = progress.coins,
        isMuted = isMuted,
        onBack = onBack,
        onToggleMute = onToggleMute,
      )

      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        CenteredAdaptiveColumn {
          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MysticalSurface),
            border = BorderStroke(1.5.dp, SpiritCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier =
                  Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SpiritCyan.copy(alpha = 0.16f))
                    .border(1.5.dp, SpiritCyan, CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.AllInclusive,
                  contentDescription = "Mode Infinity",
                  tint = SpiritCyan,
                  modifier = Modifier.size(36.dp),
                )
              }

              Text(
                text = "Bertahan Tanpa Batas",
                style = MaterialTheme.typography.headlineMedium,
                color = InkPrimary,
              )
              Text(
                text = "Setiap kemenangan membuka level berikutnya dengan gelombang lelembut makin rapat dan hadiah koin makin deras.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
              )

              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MysticalSurfaceHigh,
                border = BorderStroke(1.dp, MysticalBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text("REKOR TERTINGGI", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                    Text(
                      text = "Level ${progress.infinityRecord.coerceAtLeast(0)}",
                      style = MaterialTheme.typography.titleLarge,
                      color = SpiritCyan,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text("HADIAH LEVEL BERIKUT", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                    val nextLv = progress.infinityRecord + 1
                    Text(
                      text = "+${25 + 7 * nextLv} 🪙",
                      style = MaterialTheme.typography.titleMedium,
                      color = SpiritGold,
                    )
                  }
                }
              }

              if (progress.infinityRecord > 0) {
                GameActionButton(
                  text = "Lanjut dari Level ${progress.infinityRecord + 1}",
                  onClick = { onStartLevel(progress.infinityRecord + 1) },
                  containerColor = SpiritCyan,
                  contentColor = ObsidianNight,
                  modifier = Modifier.fillMaxWidth().testTag("infinity_continue_button"),
                )
              }

              GameActionButton(
                text = "Mulai dari Level 1",
                onClick = { onStartLevel(1) },
                containerColor = if (progress.infinityRecord > 0) MysticalSurfaceHigh else SpiritCyan,
                contentColor = if (progress.infinityRecord > 0) InkPrimary else ObsidianNight,
                modifier = Modifier.fillMaxWidth().testTag("infinity_start1_button"),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun FreeModeScreen(
  progress: PlayerProgress,
  selectedDifficulty: FreeDifficulty,
  assets: GameAssets,
  isMuted: Boolean,
  onBack: () -> Unit,
  onToggleMute: () -> Unit,
  onSelectDifficulty: (FreeDifficulty) -> Unit,
  onStartFreeStage: (Int) -> Unit,
) {
  MysticBackdrop {
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
      GameTopBar(
        title = "Mode Bebas",
        coins = progress.coins,
        isMuted = isMuted,
        onBack = onBack,
        onToggleMute = onToggleMute,
      )

      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CenteredAdaptiveColumn {
          Text(
            text = "Tingkat Kesulitan",
            style = MaterialTheme.typography.titleMedium,
            color = InkPrimary,
          )
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FreeDifficulty.entries.forEach { diff ->
              val selected = diff == selectedDifficulty
              FilterChip(
                selected = selected,
                onClick = { onSelectDifficulty(diff) },
                label = {
                  Text(
                    text = diff.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = diff.accent.copy(alpha = 0.24f),
                    selectedLabelColor = diff.accent,
                    containerColor = MysticalSurface,
                    labelColor = InkMuted,
                  ),
                border =
                  FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MysticalBorder,
                    selectedBorderColor = diff.accent,
                  ),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("free_diff_${diff.name}"),
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "HP Markas Lawan ${selectedDifficulty.baseHp.roundToInt()} · Hadiah +${selectedDifficulty.reward} 🪙",
            style = MaterialTheme.typography.labelSmall,
            color = selectedDifficulty.accent,
          )

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Pilih Arena Pertempuran",
            style = MaterialTheme.typography.titleMedium,
            color = InkPrimary,
          )
          Spacer(modifier = Modifier.height(8.dp))

          BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cols = if (maxWidth >= 560.dp) 3 else 2
            val rows = GameCatalog.stages.chunked(cols)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              rows.forEach { rowStages ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  rowStages.forEach { stage ->
                    FreeArenaCard(
                      stage = stage,
                      assets = assets,
                      onClick = { onStartFreeStage(stage.index) },
                      modifier = Modifier.weight(1f),
                    )
                  }
                  if (rowStages.size < cols) {
                    repeat(cols - rowStages.size) { Spacer(modifier = Modifier.weight(1f)) }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FreeArenaCard(
  stage: StageDef,
  assets: GameAssets,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border = BorderStroke(1.2.dp, stage.tint.copy(alpha = 0.55f)),
    modifier = modifier.testTag("free_arena_${stage.index}"),
  ) {
    Column {
      StageThumbnailBox(
        stage = stage,
        assets = assets,
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
      )
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stage.name,
          style = MaterialTheme.typography.labelLarge,
          color = InkPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "Arena #${stage.index + 1}",
          style = MaterialTheme.typography.labelSmall,
          color = stage.tint,
        )
      }
    }
  }
}

@Composable
fun StageThumbnailBox(
  stage: StageDef,
  assets: GameAssets,
  modifier: Modifier = Modifier,
) {
  val bitmap = assets.backgrounds[stage.index]
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(12.dp))
        .background(MysticalSurfaceHigh)
        .border(1.dp, stage.tint.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = stage.name,
        contentScale = ContentScale.Crop,
        filterQuality = FilterQuality.High,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val sx = size.width / BattleEngine.WORLD_W
        val sy = size.height / BattleEngine.WORLD_H
        withTransform({ scale(sx, sy, pivot = Offset.Zero) }) {
          drawPuraGelapBackground(tint = stage.tint, clockSec = 1.2f)
        }
      }
    }
  }
}

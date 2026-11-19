package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.BattleHistoryEntity
import com.example.model.AchievementCatalog
import com.example.model.AchievementDef
import com.example.model.PlayerProgress
import com.example.ui.components.CenteredAdaptiveColumn
import com.example.ui.components.GameActionButton
import com.example.ui.components.GameTopBar
import com.example.ui.components.MysticBackdrop
import com.example.ui.theme.CursedCrimson
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(
  progress: PlayerProgress,
  history: List<BattleHistoryEntity>,
  isMuted: Boolean,
  onBack: () -> Unit,
  onToggleMute: () -> Unit,
  onClaimAchievement: (String) -> Unit,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val tabs = listOf("Misi & Prestasi", "Riwayat Perang")

  MysticBackdrop {
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
      GameTopBar(
        title = "Misi & Riwayat",
        coins = progress.coins,
        isMuted = isMuted,
        onBack = onBack,
        onToggleMute = onToggleMute,
      )

      CenteredAdaptiveColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
        PrimaryTabRow(
          selectedTabIndex = selectedTab,
          containerColor = MysticalSurface,
          contentColor = SpiritGold,
          modifier = Modifier.clip(RoundedCornerShape(14.dp)),
        ) {
          tabs.forEachIndexed { idx, label ->
            Tab(
              selected = selectedTab == idx,
              onClick = { selectedTab = idx },
              text = { Text(label, style = MaterialTheme.typography.labelLarge) },
              selectedContentColor = SpiritGold,
              unselectedContentColor = InkMuted,
              modifier = Modifier.testTag("missions_tab_$idx"),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      if (selectedTab == 0) {
        LazyColumn(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          items(AchievementCatalog.all, key = { it.id }) { def ->
            CenteredAdaptiveColumn {
              AchievementCard(
                def = def,
                progress = progress,
                onClaim = { onClaimAchievement(def.id) },
              )
            }
          }
        }
      } else {
        if (history.isEmpty()) {
          CenteredAdaptiveColumn(modifier = Modifier.padding(24.dp)) {
            Card(
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = MysticalSurface),
              border = BorderStroke(1.dp, MysticalBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text("Belum Ada Catatan", style = MaterialTheme.typography.titleMedium, color = InkPrimary)
                Text(
                  "Selesaikan pertempuran di mode apa pun untuk melihat catatan perangmu di sini.",
                  style = MaterialTheme.typography.bodySmall,
                  color = InkMuted,
                )
              }
            }
          }
        } else {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            items(history, key = { it.id }) { item ->
              CenteredAdaptiveColumn {
                HistoryEntryCard(item = item)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AchievementCard(
  def: AchievementDef,
  progress: PlayerProgress,
  onClaim: () -> Unit,
) {
  val claimed = def.id in progress.claimedAchievements
  val (done, target) = def.progressProvider(progress)
  val ready = !claimed && done >= target
  val ratio = if (target > 0) (done.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 1f

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border =
      BorderStroke(
        width = 1.2.dp,
        color =
          when {
            ready -> SpiritGold
            claimed -> EctoplasmEmerald.copy(alpha = 0.45f)
            else -> MysticalBorder
          },
      ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(def.title, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
        Text(def.description, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          LinearProgressIndicator(
            progress = { ratio },
            color = if (done >= target) EctoplasmEmerald else SpectralViolet,
            trackColor = MysticalSurfaceHigh,
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
          )
          Text(
            text = "$done/$target",
            style = MaterialTheme.typography.labelSmall,
            color = InkMuted,
          )
        }
      }

      GameActionButton(
        text =
          when {
            claimed -> "DIKLAIM"
            ready -> "+${def.rewardCoins} 🪙"
            else -> "+${def.rewardCoins} 🪙"
          },
        enabled = ready,
        onClick = onClaim,
        containerColor =
          when {
            ready -> SpiritGold
            claimed -> EctoplasmEmerald.copy(alpha = 0.22f)
            else -> MysticalSurfaceHigh
          },
        contentColor =
          when {
            ready -> ObsidianNight
            claimed -> EctoplasmEmerald
            else -> InkMuted
          },
        modifier = Modifier.testTag("claim_achievement_${def.id}"),
      )
    }
  }
}

@Composable
private fun HistoryEntryCard(item: BattleHistoryEntity) {
  val accent = if (item.won) EctoplasmEmerald else CursedCrimson
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.18f),
      ) {
        Text(
          text = if (item.won) "MENANG" else "KALAH",
          style = MaterialTheme.typography.labelSmall,
          color = accent,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(item.stageTitle, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
        Text(
          text = "${item.enemiesDefeated} musuh dikalahkan · ${item.durationSec} detik",
          style = MaterialTheme.typography.bodySmall,
          color = InkMuted,
        )
      }

      Text(
        text = "+${item.coinsEarned} 🪙",
        style = MaterialTheme.typography.labelMedium,
        color = if (item.coinsEarned > 0) SpiritGold else SpiritCyan,
      )
    }
  }
}

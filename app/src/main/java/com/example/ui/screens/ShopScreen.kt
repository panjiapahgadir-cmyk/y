package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GameAssets
import com.example.model.GhostId
import com.example.model.JimatId
import com.example.model.PlayerProgress
import com.example.model.UpgradeId
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
import com.example.ui.theme.SpectralVioletDeep
import com.example.ui.theme.SpiritCyan
import com.example.ui.theme.SpiritGold
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
  progress: PlayerProgress,
  assets: GameAssets,
  isMuted: Boolean,
  toastMessage: String?,
  onClearToast: () -> Unit,
  onBack: () -> Unit,
  onToggleMute: () -> Unit,
  onBuyJimat: (JimatId) -> Unit,
  onBuyUpgrade: (UpgradeId) -> Unit,
  onUnlockGhost: (GhostId) -> Unit,
  onTrainGhost: (GhostId) -> Unit,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val tabs = listOf("Jimat", "Upgrade", "Rekrut", "Latih")

  LaunchedEffect(toastMessage) {
    if (toastMessage != null) {
      kotlinx.coroutines.delay(2200)
      onClearToast()
    }
  }

  MysticBackdrop {
    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
      Column(modifier = Modifier.fillMaxSize()) {
        GameTopBar(
          title = "Toko Dukun",
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
                text = {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                selectedContentColor = SpiritGold,
                unselectedContentColor = InkMuted,
                modifier = Modifier.testTag("shop_tab_$idx"),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          when (selectedTab) {
            0 -> {
              items(JimatId.entries, key = { it.key }) { jimat ->
                CenteredAdaptiveColumn {
                  JimatShopCard(
                    jimat = jimat,
                    ownedQty = progress.jimatCount(jimat),
                    canAfford = progress.coins >= jimat.price,
                    assets = assets,
                    onBuy = { onBuyJimat(jimat) },
                  )
                }
              }
            }
            1 -> {
              items(UpgradeId.entries, key = { it.key }) { up ->
                CenteredAdaptiveColumn {
                  UpgradeShopCard(
                    upgrade = up,
                    level = progress.upgradeLevel(up),
                    coins = progress.coins,
                    assets = assets,
                    onUpgrade = { onBuyUpgrade(up) },
                  )
                }
              }
            }
            2 -> {
              items(GhostId.unlockables, key = { it.key }) { ghost ->
                CenteredAdaptiveColumn {
                  UnlockGhostShopCard(
                    ghost = ghost,
                    owned = ghost in progress.ownedGhosts,
                    canAfford = progress.coins >= ghost.unlockPrice,
                    assets = assets,
                    onUnlock = { onUnlockGhost(ghost) },
                  )
                }
              }
            }
            else -> {
              val ownedList = GhostId.ordered.filter { it in progress.ownedGhosts }
              items(ownedList, key = { it.key }) { ghost ->
                CenteredAdaptiveColumn {
                  TrainGhostShopCard(
                    ghost = ghost,
                    level = progress.ghostLevel(ghost),
                    coins = progress.coins,
                    assets = assets,
                    onTrain = { onTrainGhost(ghost) },
                  )
                }
              }
            }
          }
        }
      }

      AnimatedVisibility(
        visible = toastMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = EctoplasmEmerald,
          shadowElevation = 6.dp,
        ) {
          Text(
            text = toastMessage ?: "",
            style = MaterialTheme.typography.labelLarge,
            color = ObsidianNight,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun JimatShopCard(
  jimat: JimatId,
  ownedQty: Int,
  canAfford: Boolean,
  assets: GameAssets,
  onBuy: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border = BorderStroke(1.dp, jimat.color.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AssetArtBox(
        bitmap = assets.icons[jimat.key],
        contentDescription = jimat.title,
        borderColor = jimat.color,
        modifier = Modifier.size(56.dp),
      )

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(jimat.title, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
          if (ownedQty > 0) {
            Surface(shape = RoundedCornerShape(6.dp), color = MysticalSurfaceHigh) {
              Text(
                text = "Dimiliki: $ownedQty",
                style = MaterialTheme.typography.labelSmall,
                color = SpiritGold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        Text(jimat.desc, style = MaterialTheme.typography.bodySmall, color = InkMuted)
      }

      GameActionButton(
        text = "${jimat.price} 🪙",
        enabled = canAfford,
        onClick = onBuy,
        containerColor = if (canAfford) SpiritGold else MysticalSurfaceHigh,
        contentColor = if (canAfford) ObsidianNight else InkMuted,
        modifier = Modifier.testTag("buy_jimat_${jimat.key}"),
      )
    }
  }
}

@Composable
private fun UpgradeShopCard(
  upgrade: UpgradeId,
  level: Int,
  coins: Int,
  assets: GameAssets,
  onUpgrade: () -> Unit,
) {
  val isMax = level >= upgrade.maxLevel
  val price = upgrade.priceForLevel(level)
  val canAfford = !isMax && coins >= price

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border = BorderStroke(1.dp, SpectralViolet.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AssetArtBox(
        bitmap = assets.icons[upgrade.key],
        contentDescription = upgrade.title,
        borderColor = SpectralViolet,
        modifier = Modifier.size(56.dp),
      )

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "${upgrade.title} (Lv.$level/${upgrade.maxLevel})",
          style = MaterialTheme.typography.titleMedium,
          color = InkPrimary,
        )
        Text(upgrade.desc, style = MaterialTheme.typography.bodySmall, color = InkMuted)
        LinearProgressIndicator(
          progress = { level.toFloat() / upgrade.maxLevel.toFloat() },
          color = SpiritCyan,
          trackColor = MysticalSurfaceHigh,
          modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
      }

      GameActionButton(
        text = if (isMax) "MAKS" else "$price 🪙",
        enabled = canAfford,
        onClick = onUpgrade,
        containerColor = if (isMax) EctoplasmEmerald.copy(alpha = 0.25f) else SpectralVioletDeep,
        contentColor = if (isMax) EctoplasmEmerald else Color.White,
        modifier = Modifier.testTag("buy_upgrade_${upgrade.key}"),
      )
    }
  }
}

@Composable
private fun UnlockGhostShopCard(
  ghost: GhostId,
  owned: Boolean,
  canAfford: Boolean,
  assets: GameAssets,
  onUnlock: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border = BorderStroke(1.2.dp, ghost.accentColor.copy(alpha = 0.55f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AssetArtBox(
        bitmap = assets.cards[ghost],
        contentDescription = ghost.displayName,
        borderColor = ghost.accentColor,
        modifier = Modifier.size(62.dp, 76.dp),
      )

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(ghost.displayName, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
          if (ghost.isMythicNew) {
            Surface(shape = RoundedCornerShape(6.dp), color = SpiritGold.copy(alpha = 0.2f)) {
              Text(
                text = "LEGENDARIS",
                style = MaterialTheme.typography.labelSmall,
                color = SpiritGold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        Text(ghost.role, style = MaterialTheme.typography.labelSmall, color = ghost.accentColor)
        Text(ghost.skillSummary, style = MaterialTheme.typography.bodySmall, color = InkMuted)
      }

      GameActionButton(
        text = if (owned) "DIMILIKI" else "${ghost.unlockPrice} 🪙",
        enabled = !owned && canAfford,
        onClick = onUnlock,
        containerColor = if (owned) EctoplasmEmerald.copy(alpha = 0.25f) else SpiritGold,
        contentColor = if (owned) EctoplasmEmerald else ObsidianNight,
        modifier = Modifier.testTag("unlock_ghost_${ghost.key}"),
      )
    }
  }
}

@Composable
private fun TrainGhostShopCard(
  ghost: GhostId,
  level: Int,
  coins: Int,
  assets: GameAssets,
  onTrain: () -> Unit,
) {
  val isMax = level >= 5
  val price = 50 + 45 * level
  val canAfford = !isMax && coins >= price
  val bonusPct = level * 15
  val curHp = (ghost.baseHp * (1f + 0.15f * level)).roundToInt()
  val curDmg = (ghost.baseDmg * (1f + 0.15f * level)).roundToInt()

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MysticalSurface),
    border = BorderStroke(1.dp, ghost.accentColor.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AssetArtBox(
        bitmap = assets.cards[ghost],
        contentDescription = ghost.displayName,
        borderColor = ghost.accentColor,
        modifier = Modifier.size(56.dp, 68.dp),
      )

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = "${ghost.displayName} · Lv.$level/5",
          style = MaterialTheme.typography.titleMedium,
          color = InkPrimary,
        )
        Text(
          text = "HP $curHp · DMG $curDmg (+$bonusPct%)",
          style = MaterialTheme.typography.labelSmall,
          color = EctoplasmEmerald,
        )
        LinearProgressIndicator(
          progress = { level / 5f },
          color = ghost.accentColor,
          trackColor = MysticalSurfaceHigh,
          modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
        )
      }

      GameActionButton(
        text = if (isMax) "MAKS" else "Latih $price 🪙",
        enabled = canAfford,
        onClick = onTrain,
        containerColor = if (isMax) EctoplasmEmerald.copy(alpha = 0.25f) else SpectralVioletDeep,
        contentColor = if (isMax) EctoplasmEmerald else Color.White,
        modifier = Modifier.testTag("train_ghost_${ghost.key}"),
      )
    }
  }
}

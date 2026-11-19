package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameAssets
import com.example.game.BattleUnit
import com.example.game.TeamSide
import com.example.model.GhostId
import com.example.model.PlayerProgress
import com.example.ui.battle.GhostRenderer.drawUnit
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
fun KitabHantuScreen(
  progress: PlayerProgress,
  assets: GameAssets,
  isMuted: Boolean,
  onBack: () -> Unit,
  onToggleMute: () -> Unit,
  onTestAttackSound: (GhostId) -> Unit,
) {
  var selectedGhost by remember { mutableStateOf(GhostId.TUYUL) }
  var clockSec by remember { mutableFloatStateOf(0f) }
  var shockLife by remember { mutableFloatStateOf(0f) }

  val demoUnit =
    remember(selectedGhost) {
      BattleUnit(
        uid = 1L,
        side = TeamSide.PLAYER,
        ghost = selectedGhost,
        x = 0f,
        y = 0f,
        hp = selectedGhost.baseHp,
        maxHp = selectedGhost.baseHp,
        dmg = selectedGhost.baseDmg,
        isMoving = true,
      )
    }

  LaunchedEffect(demoUnit) {
    var last = 0L
    while (true) {
      withFrameNanos { now ->
        if (last == 0L) last = now
        val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
        last = now
        clockSec += dt
        demoUnit.phase += dt * 5f
        if (demoUnit.shieldTimer > 0f) {
          demoUnit.shieldTimer = (demoUnit.shieldTimer - dt).coerceAtLeast(0f)
        }
        if (demoUnit.animTime >= 0f) {
          demoUnit.animTime += dt
          if (demoUnit.animTime >= selectedGhost.animDuration) {
            demoUnit.animTime = -1f
          }
        }
        if (shockLife > 0f) {
          shockLife = (shockLife - dt * 2.0f).coerceAtLeast(0f)
        }
      }
    }
  }

  val owned = selectedGhost in progress.ownedGhosts
  val level = progress.ghostLevel(selectedGhost)
  val mul = 1f + 0.15f * level
  val effHp = (selectedGhost.baseHp * mul).roundToInt()
  val effDmg = (selectedGhost.baseDmg * mul).roundToInt()

  MysticBackdrop {
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
      GameTopBar(
        title = "Kitab Hantu",
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
          // Horizontal roster selector
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            GhostId.ordered.forEach { ghost ->
              val isSelected = ghost == selectedGhost
              val isOwned = ghost in progress.ownedGhosts
              Box(
                modifier =
                  Modifier
                    .width(74.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) MysticalSurfaceHigh else MysticalSurface)
                    .border(
                      width = if (isSelected) 2.dp else 1.dp,
                      color = if (isSelected) ghost.accentColor else MysticalBorder,
                      shape = RoundedCornerShape(14.dp),
                    )
                    .alpha(if (isOwned) 1f else 0.58f)
                    .clickable { selectedGhost = ghost }
                    .padding(6.dp)
                    .testTag("kitab_select_${ghost.key}"),
                contentAlignment = Alignment.Center,
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Box {
                    AssetArtBox(
                      bitmap = assets.cards[ghost],
                      contentDescription = ghost.displayName,
                      cornerRadius = 10.dp,
                      borderColor = ghost.accentColor.copy(alpha = 0.5f),
                      modifier = Modifier.size(56.dp),
                    )
                    if (!isOwned) {
                      Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Belum direkrut",
                        tint = Color.White,
                        modifier =
                          Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(14.dp),
                      )
                    }
                  }
                  Text(
                    text = ghost.displayName.substringBefore(' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) ghost.accentColor else InkPrimary,
                    maxLines = 1,
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Live Animated Showcase + Card Art + "Uji Jurus" button
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MysticalSurface),
            border = BorderStroke(1.4.dp, selectedGhost.accentColor.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                AssetArtBox(
                  bitmap = assets.cards[selectedGhost],
                  contentDescription = selectedGhost.displayName,
                  borderColor = selectedGhost.accentColor,
                  modifier = Modifier.size(76.dp, 96.dp),
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = selectedGhost.displayName,
                      style = MaterialTheme.typography.headlineMedium,
                      color = InkPrimary,
                    )
                    if (selectedGhost.isMythicNew) {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SpiritGold.copy(alpha = 0.22f),
                      ) {
                        Text(
                          text = "BARU",
                          style = MaterialTheme.typography.labelSmall,
                          color = SpiritGold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                      }
                    }
                  }
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = selectedGhost.accentColor.copy(alpha = 0.18f),
                  ) {
                    Text(
                      text = selectedGhost.role,
                      style = MaterialTheme.typography.labelSmall,
                      color = selectedGhost.accentColor,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                  }
                  Text(
                    text =
                      if (owned) "Status: Dimiliki (Lv.$level/5)"
                      else "Status: Terkunci (${selectedGhost.unlockPrice} 🪙 di Toko)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (owned) EctoplasmEmerald else SpiritGold,
                  )
                }
              }

              // Interactive Live Canvas Stage
              Box(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .height(185.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                      Brush.verticalGradient(
                        listOf(Color(0xFF140B2E), Color(0xFF0A0618))
                      )
                    )
                    .border(1.dp, selectedGhost.accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val tick = clockSec
                  val cx = size.width * 0.5f
                  val cy = size.height * 0.78f

                  drawCircle(
                    brush =
                      Brush.radialGradient(
                        colors = listOf(selectedGhost.accentColor.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(cx, cy - 48.dp.toPx()),
                        radius = 110.dp.toPx(),
                      ),
                    radius = 110.dp.toPx(),
                    center = Offset(cx, cy - 48.dp.toPx()),
                  )

                  if (shockLife > 0f) {
                    val r = (1f - shockLife) * 90.dp.toPx() + 18.dp.toPx()
                    drawCircle(
                      color = selectedGhost.accentColor.copy(alpha = shockLife),
                      radius = r,
                      center = Offset(cx, cy - 34.dp.toPx()),
                      style = Stroke(width = 3.dp.toPx()),
                    )
                    if (selectedGhost == GhostId.RANGDA) {
                      for (b in 0..2) {
                        val bx = cx + (b - 1) * 44.dp.toPx()
                        drawLine(
                          color = Color(0xFFC084FC).copy(alpha = shockLife),
                          start = Offset(cx + 12.dp.toPx(), cy - 70.dp.toPx()),
                          end = Offset(bx + 36.dp.toPx(), cy - 8.dp.toPx()),
                          strokeWidth = 3.dp.toPx(),
                        )
                      }
                    }
                  }

                  withTransform({
                    translate(cx, cy)
                    scale(1.35f, 1.35f, pivot = Offset.Zero)
                  }) {
                    drawUnit(
                      u = demoUnit,
                      assets = assets,
                      kerisActive = false,
                      clockSec = tick,
                    )
                  }
                }

                GameActionButton(
                  text = "Uji Jurus",
                  onClick = {
                    demoUnit.animTime = 0f
                    if (selectedGhost == GhostId.BARONG) demoUnit.shieldTimer = 2.6f
                    shockLife = 1f
                    onTestAttackSound(selectedGhost)
                  },
                  containerColor = selectedGhost.accentColor,
                  contentColor = ObsidianNight,
                  minHeight = 48.dp,
                  modifier =
                    Modifier
                      .align(Alignment.BottomEnd)
                      .padding(10.dp)
                      .testTag("kitab_test_attack_button"),
                )
              }

              // Skill & Lore description
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MysticalSurfaceHigh,
                border = BorderStroke(1.dp, MysticalBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "Jurus Khusus: ${selectedGhost.skillSummary}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SpiritGold,
                  )
                  Text(
                    text = selectedGhost.lore,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkPrimary,
                  )
                }
              }

              // Tabular Stats Grid
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                KitabStatChip("HP", "$effHp", EctoplasmEmerald, Modifier.weight(1f))
                KitabStatChip("Serang", "$effDmg", SpiritGold, Modifier.weight(1f))
                KitabStatChip("Arwah", "${selectedGhost.cost}", SpiritCyan, Modifier.weight(1f))
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                KitabStatChip("Jarak", "${selectedGhost.range.roundToInt()}", SpectralViolet, Modifier.weight(1f))
                KitabStatChip("Gerak", "${selectedGhost.speed.roundToInt()}", InkPrimary, Modifier.weight(1f))
                KitabStatChip("Cooldown", "${selectedGhost.cooldown}d", InkMuted, Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun KitabStatChip(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MysticalSurfaceHigh,
    border = BorderStroke(1.dp, MysticalBorder),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(value, style = MaterialTheme.typography.labelLarge, color = color)
      Text(label, style = MaterialTheme.typography.labelSmall, color = InkMuted)
    }
  }
}

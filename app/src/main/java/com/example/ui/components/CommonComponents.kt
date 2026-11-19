package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.MysticalBorder
import com.example.ui.theme.MysticalSurface
import com.example.ui.theme.MysticalSurfaceHigh
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SpectralViolet
import com.example.ui.theme.SpectralVioletDeep
import com.example.ui.theme.SpiritGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MysticBackdrop(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val inf = rememberInfiniteTransition(label = "backdrop")
  val drift by
    inf.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
      label = "drift",
    )

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF1D1042), MidnightIndigo, ObsidianNight),
          )
        )
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Top spectral nebula glow
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(SpectralViolet.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.08f),
            radius = size.width * 0.7f,
          ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.5f, size.height * 0.08f),
      )

      // Floating spirit wisps
      for (i in 0 until 14) {
        val t = (drift + i * 0.071f) % 1f
        val x = ((i * 0.19f + 0.05f * sin(t * 2f * PI.toFloat() + i)) % 1f) * size.width
        val y = (1f - t) * size.height
        val alpha = sin(t * PI.toFloat()) * 0.4f
        val color = if (i % 3 == 0) SpiritGold else SpectralViolet
        drawCircle(
          color = color.copy(alpha = alpha),
          radius = (2.5f + (i % 3) * 1.5f).dp.toPx(),
          center = Offset(x, y),
        )
      }

      // Faint moon crest watermark top-right
      val moonCenter = Offset(size.width * 0.86f, size.height * 0.11f)
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(SpiritGold.copy(alpha = 0.18f), Color.Transparent),
            center = moonCenter,
            radius = 74.dp.toPx(),
          ),
        radius = 74.dp.toPx(),
        center = moonCenter,
      )
      drawCircle(
        color = Color(0xFFFFF4C2).copy(alpha = 0.14f),
        radius = 24.dp.toPx(),
        center = moonCenter,
      )
      drawCircle(
        color = Color(0xFF1D1042),
        radius = 20.dp.toPx(),
        center = Offset(moonCenter.x - 8.dp.toPx(), moonCenter.y - 4.dp.toPx()),
      )
    }
    content()
  }
}

@Composable
fun CoinPill(coins: Int, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.heightIn(min = 34.dp),
    shape = CircleShape,
    color = MysticalSurface,
    border = BorderStroke(1.2.dp, SpiritGold.copy(alpha = 0.65f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text("🪙", style = MaterialTheme.typography.bodyMedium)
      Text(
        text = coins.toString(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = SpiritGold,
      )
    }
  }
}

@Composable
fun GameTopBar(
  title: String,
  coins: Int,
  isMuted: Boolean,
  onBack: (() -> Unit)?,
  onToggleMute: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (onBack != null) {
      Surface(
        shape = CircleShape,
        color = MysticalSurfaceHigh,
        border = BorderStroke(1.dp, MysticalBorder),
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(48.dp).testTag("back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = InkPrimary,
          )
        }
      }
    }

    Text(
      text = title,
      style = MaterialTheme.typography.headlineMedium,
      color = InkPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )

    CoinPill(coins = coins)

    Surface(
      shape = CircleShape,
      color = MysticalSurfaceHigh,
      border = BorderStroke(1.dp, MysticalBorder),
    ) {
      IconButton(
        onClick = onToggleMute,
        modifier = Modifier.size(48.dp).testTag("mute_button"),
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
}

@Composable
fun CenteredAdaptiveColumn(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
      content = content,
    )
  }
}

@Composable
fun GameActionButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  containerColor: Color = SpectralVioletDeep,
  contentColor: Color = Color.White,
  minHeight: Dp = 48.dp,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(14.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = MysticalSurface,
        disabledContentColor = Color(0xFF64748B),
      ),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    border =
      BorderStroke(
        width = 1.dp,
        color = if (enabled) Color.White.copy(alpha = 0.22f) else MysticalBorder.copy(alpha = 0.4f),
      ),
    modifier = modifier.heightIn(min = minHeight),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun AssetArtBox(
  bitmap: ImageBitmap?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 12.dp,
  borderColor: Color = MysticalBorder,
  contentScale: ContentScale = ContentScale.Crop,
  fallbackEmoji: String = "👻",
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(MysticalSurfaceHigh)
        .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
    contentAlignment = Alignment.Center,
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        contentScale = contentScale,
        filterQuality = FilterQuality.High,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Text(fallbackEmoji, style = MaterialTheme.typography.titleLarge)
    }
  }
}

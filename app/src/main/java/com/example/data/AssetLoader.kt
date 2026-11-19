package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.GhostId
import com.example.model.JimatId
import com.example.model.UpgradeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GameAssets(
  val sprites: Map<GhostId, ImageBitmap> = emptyMap(),
  val androidSprites: Map<GhostId, Bitmap> = emptyMap(),
  val cards: Map<GhostId, ImageBitmap> = emptyMap(),
  val icons: Map<String, ImageBitmap> = emptyMap(),
  val backgrounds: Map<Int, ImageBitmap> = emptyMap(),
  val basePlayer: ImageBitmap? = null,
  val baseEnemy: ImageBitmap? = null,
  val foregroundParallax: ImageBitmap? = null,
  val loaded: Boolean = false,
)

class AssetLoader(private val context: Context) {
  @Volatile private var cached: GameAssets? = null

  suspend fun loadAll(): GameAssets {
    cached?.let { return it }
    return withContext(Dispatchers.IO) {
      cached?.let { return@withContext it }
      val spr = mutableMapOf<GhostId, ImageBitmap>()
      val nativeSpr = mutableMapOf<GhostId, Bitmap>()
      val crd = mutableMapOf<GhostId, ImageBitmap>()
      for (g in GhostId.entries) {
        decodeAsset("sprites/${g.key}.webp")?.let { bmp ->
          nativeSpr[g] = bmp
          spr[g] = bmp.asImageBitmap()
        }
        decodeAsset("cards/${g.key}.webp")?.let { crd[g] = it.asImageBitmap() }
      }
      val ics = mutableMapOf<String, ImageBitmap>()
      for (j in JimatId.entries) {
        decodeAsset("icons/${j.key}.webp")?.let { ics[j.key] = it.asImageBitmap() }
      }
      for (u in UpgradeId.entries) {
        decodeAsset("icons/${u.key}.webp")?.let { ics[u.key] = it.asImageBitmap() }
      }
      val bgs = mutableMapOf<Int, ImageBitmap>()
      for (idx in 0..9) {
        decodeAsset("bg/bg_$idx.webp")?.let { bgs[idx] = it.asImageBitmap() }
      }
      val baseP = decodeAsset("bases/base_player.webp")?.asImageBitmap()
      val baseE = decodeAsset("bases/base_enemy.webp")?.asImageBitmap()
      val fg = decodeAsset("fg/fg_parallax.webp")?.asImageBitmap()
      GameAssets(
        sprites = spr,
        androidSprites = nativeSpr,
        cards = crd,
        icons = ics,
        backgrounds = bgs,
        basePlayer = baseP,
        baseEnemy = baseE,
        foregroundParallax = fg,
        loaded = true,
      ).also { cached = it }
    }
  }

  private fun decodeAsset(path: String): Bitmap? =
    runCatching {
      context.assets.open(path).use { stream ->
        BitmapFactory.decodeStream(stream)
      }
    }.getOrNull()
}

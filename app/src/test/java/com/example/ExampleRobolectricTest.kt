package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssetLoader
import com.example.game.BattleUnit
import com.example.game.TeamSide
import com.example.model.GhostId
import com.example.ui.battle.GhostAnimator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Perang Hantu", appName)
  }

  @Test
  fun `assetLoader loads all 10 arenas bases foreground and 14 ghost sprites`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assets = AssetLoader(context).loadAll()
    assertTrue(assets.loaded)
    assertEquals(14, assets.sprites.size)
    assertEquals(14, assets.androidSprites.size)
    assertEquals(10, assets.backgrounds.size)
    for (idx in 0..9) {
      assertNotNull("Missing arena background $idx", assets.backgrounds[idx])
    }
    assertNotNull("Missing player Pendopo base", assets.basePlayer)
    assertNotNull("Missing enemy Citadel base", assets.baseEnemy)
    assertNotNull("Missing foreground parallax strip", assets.foregroundParallax)
  }

  @Test
  fun `ghostAnimator renders multi-pose skeletal mesh cleanly for all 14 ghosts`() {
    val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    val canvasBmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(canvasBmp)

    for (g in GhostId.entries) {
      val unit =
        BattleUnit(
          uid = 1L,
          side = TeamSide.PLAYER,
          ghost = g,
          x = 200f,
          y = 365f,
          hp = g.baseHp,
          maxHp = g.baseHp,
          dmg = g.baseDmg,
        )

      // IDLE
      GhostAnimator.drawAnimatedGhost(canvas, bmp, unit, 80f, g.spriteHeight, 1.0f)
      // MOVE
      unit.isMoving = true
      unit.phase = 2.4f
      GhostAnimator.drawAnimatedGhost(canvas, bmp, unit, 80f, g.spriteHeight, 1.4f)
      // WINDUP
      unit.isMoving = false
      unit.animTime = g.impactTime * 0.6f
      GhostAnimator.drawAnimatedGhost(canvas, bmp, unit, 80f, g.spriteHeight, 1.8f)
      // STRIKE + HIT FLASH
      unit.animTime = g.impactTime + 0.02f
      unit.hitFlash = 0.1f
      unit.airZ = 18f
      GhostAnimator.drawAnimatedGhost(canvas, bmp, unit, 80f, g.spriteHeight, 2.0f)
    }
  }
}

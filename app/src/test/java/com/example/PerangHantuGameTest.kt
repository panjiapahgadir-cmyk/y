package com.example

import com.example.game.BattleEngine
import com.example.game.BattleResultSummary
import com.example.game.TeamSide
import com.example.model.AchievementCatalog
import com.example.model.GameCatalog
import com.example.model.GhostId
import com.example.model.JimatId
import com.example.model.PlayerProgress
import com.example.model.UpgradeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerangHantuGameTest {

  @Test
  fun catalog_containsExpectedStagesTournamentsAnd14Ghosts() {
    assertEquals(10, GameCatalog.stages.size)
    assertEquals(3, GameCatalog.tournaments.size)
    assertEquals(14, GhostId.entries.size)
    assertEquals(5, GhostId.entries.count { it.isMythicNew })
    assertEquals("Kuburan Tua", GameCatalog.stages.first().name)
    assertEquals("Istana Hantu", GameCatalog.stages.last().name)
    assertTrue(GameCatalog.stages[6].hasBakedBg)
  }

  @Test
  fun battleEngine_summonUpgradeFountainAndJimatWorkCorrectly() {
    var endedSummary: BattleResultSummary? = null
    val progress =
      PlayerProgress(
        coins = 500,
        ownedGhosts = GhostId.entries.toSet(),
        ghostLevels = mapOf(GhostId.GENDRUWO to 5, GhostId.BUTO_IJO to 3, GhostId.BARONG to 2),
        upgrades = mapOf(UpgradeId.GUDANG to 2, UpgradeId.SUMUR to 2, UpgradeId.DUKUN to 2),
        inventory = mapOf(JimatId.PETIR to 2, JimatId.KERIS to 1, JimatId.KEMBANG to 1),
      )

    val engine =
      BattleEngine(
        config = GameCatalog.stageConfig(0),
        progress = progress,
        onSound = {},
        onBattleEnded = { endedSummary = it },
      )

    assertEquals(500f, engine.maxSouls, 0.01f)
    assertTrue(engine.summonPlayerGhost(GhostId.TUYUL))
    assertEquals(1, engine.ghostsSummoned)
    assertEquals(1, engine.units.count { it.side == TeamSide.PLAYER })

    // Cooldown prevents immediate second Tuyul summon
    assertFalse(engine.summonPlayerGhost(GhostId.TUYUL))

    // Step simulation so souls regenerate above 80 and fountain can upgrade to Lv2
    repeat(60) {
      engine.step(0.05f, speedMultiplier = 1)
    }
    assertTrue(engine.souls >= 80f)
    assertTrue(engine.upgradeFountain())
    assertEquals(2, engine.fountainLevel)

    // Summon new mythic super-power ghosts when souls allow
    repeat(120) { engine.step(0.05f, speedMultiplier = 1) }
    assertTrue(engine.summonPlayerGhost(GhostId.PALASIK))
    repeat(140) { engine.step(0.05f, speedMultiplier = 1) }
    assertTrue(engine.summonPlayerGhost(GhostId.BARONG))
    repeat(160) { engine.step(0.05f, speedMultiplier = 1) }
    assertTrue(engine.summonPlayerGhost(GhostId.RANGDA))

    // Activate Petir Kyai & Keris Kramat jimats and enable auto-summon until Stage 1 finishes
    engine.autoSummon = true
    assertTrue(engine.useJimat(JimatId.KERIS))
    repeat(40) { engine.step(0.05f, speedMultiplier = 1) }
    assertTrue(engine.useJimat(JimatId.PETIR))
    assertTrue(engine.bolts.isNotEmpty() || engine.particles.isNotEmpty())

    // Run battle at 3x speed until player squad overwhelms Stage 1 enemy base
    var ticks = 0
    while (!engine.isOver && ticks < 2500) {
      engine.step(0.05f, speedMultiplier = 3)
      ticks++
    }
    assertTrue(engine.isOver)
    assertNotNull(endedSummary)
    assertTrue(endedSummary!!.won)
    assertTrue(endedSummary!!.totalCoinsEarned >= 40)
  }

  @Test
  fun achievements_reportProgressAccurately() {
    val p = PlayerProgress(clearStage = 5, ownedGhosts = GhostId.entries.toSet(), trophyGold = 1)
    val collector = AchievementCatalog.all.first { it.id == "collector" }
    val (done, target) = collector.progressProvider(p)
    assertEquals(14, done)
    assertEquals(14, target)

    val mythicDuo = AchievementCatalog.all.first { it.id == "mythic_duo" }
    val (mDone, mTarget) = mythicDuo.progressProvider(p)
    assertEquals(2, mDone)
    assertEquals(2, mTarget)
  }
}

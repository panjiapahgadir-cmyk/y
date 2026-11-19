package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.audio.SoundEffect
import com.example.ui.MainViewModel
import com.example.ui.battle.BattleScreen
import com.example.ui.screens.FreeModeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InfinityScreen
import com.example.ui.screens.KitabHantuScreen
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.StageSelectScreen
import com.example.ui.screens.TournamentScreen
import com.example.ui.theme.PerangHantuTheme

object AppRoutes {
  const val HOME = "home"
  const val STAGES = "stages"
  const val TOURNAMENT = "tournament"
  const val INFINITY = "infinity"
  const val FREE = "free"
  const val SHOP = "shop"
  const val KITAB = "kitab"
  const val MISSIONS = "missions"
  const val BATTLE = "battle"
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )
    setContent {
      PerangHantuTheme {
        PerangHantuApp()
      }
    }
  }
}

@Composable
fun PerangHantuApp(viewModel: MainViewModel = viewModel()) {
  val navController = rememberNavController()
  val progress by viewModel.progress.collectAsStateWithLifecycle()
  val history by viewModel.history.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val assets by viewModel.assets.collectAsStateWithLifecycle()
  val activeBattleConfig by viewModel.activeBattleConfig.collectAsStateWithLifecycle()
  val selectedFreeDifficulty by viewModel.selectedFreeDifficulty.collectAsStateWithLifecycle()
  val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

  NavHost(
    navController = navController,
    startDestination = AppRoutes.HOME,
  ) {
    composable(AppRoutes.HOME) {
      HomeScreen(
        progress = progress,
        assets = assets,
        isMuted = settings.isMuted,
        onToggleMute = viewModel::toggleMute,
        onQuickPlayStage = { stageIdx ->
          viewModel.prepareStageBattle(stageIdx)
          navController.navigate(AppRoutes.BATTLE)
        },
        onOpenStages = {
          viewModel.playClick()
          navController.navigate(AppRoutes.STAGES)
        },
        onOpenTournament = {
          viewModel.playClick()
          navController.navigate(AppRoutes.TOURNAMENT)
        },
        onOpenInfinity = {
          viewModel.playClick()
          navController.navigate(AppRoutes.INFINITY)
        },
        onOpenFreeMode = {
          viewModel.playClick()
          navController.navigate(AppRoutes.FREE)
        },
        onOpenShop = {
          viewModel.playClick()
          navController.navigate(AppRoutes.SHOP)
        },
        onOpenKitab = {
          viewModel.playClick()
          navController.navigate(AppRoutes.KITAB)
        },
        onOpenMissions = {
          viewModel.playClick()
          navController.navigate(AppRoutes.MISSIONS)
        },
      )
    }

    composable(AppRoutes.STAGES) {
      StageSelectScreen(
        progress = progress,
        assets = assets,
        isMuted = settings.isMuted,
        onBack = {
          viewModel.playClick()
          navController.popBackStack()
        },
        onToggleMute = viewModel::toggleMute,
        onSelectStage = { stageIdx ->
          viewModel.prepareStageBattle(stageIdx)
          navController.navigate(AppRoutes.BATTLE)
        },
      )
    }

    composable(AppRoutes.TOURNAMENT) {
      TournamentScreen(
        progress = progress,
        isMuted = settings.isMuted,
        onBack = {
          viewModel.playClick()
          navController.popBackStack()
        },
        onToggleMute = viewModel::toggleMute,
        onStartTournament = { tIdx ->
          viewModel.prepareTournamentRound(tIdx, round = 0, payEntryFee = true)
          navController.navigate(AppRoutes.BATTLE)
        },
      )
    }

    composable(AppRoutes.INFINITY) {
      InfinityScreen(
        progress = progress,
        isMuted = settings.isMuted,
        onBack = {
          viewModel.playClick()
          navController.popBackStack()
        },
        onToggleMute = viewModel::toggleMute,
        onStartLevel = { lv ->
          viewModel.prepareInfinityBattle(lv)
          navController.navigate(AppRoutes.BATTLE)
        },
      )
    }

    composable(AppRoutes.FREE) {
      FreeModeScreen(
        progress = progress,
        selectedDifficulty = selectedFreeDifficulty,
        assets = assets,
        isMuted = settings.isMuted,
        onBack = {
          viewModel.playClick()
          navController.popBackStack()
        },
        onToggleMute = viewModel::toggleMute,
        onSelectDifficulty = viewModel::setFreeDifficulty,
        onStartFreeStage = { stageIdx ->
          viewModel.prepareFreeBattle(stageIdx)
          navController.navigate(AppRoutes.BATTLE)
        },
      )
    }

    composable(AppRoutes.SHOP) {
      ShopScreen(
        progress = progress,
        assets = assets,
        isMuted = settings.isMuted,
        toastMessage = toastMessage,
        onClearToast = viewModel::clearToast,
        onBack = {
          viewModel.playClick()
          navController.popBackStack()
        },
        onToggleMute = viewModel::toggleMute,
        onBuyJimat = viewModel::buyJimat,
        onBuyUpgrade = viewModel::buyUpgrade,
        onUnlockGhost = viewModel::unlockGhost,
        onTrainGhost = viewModel::trainGhost,
      )
    }

    composable(AppRoutes.KITAB) {
      KitabHantuScreen(
        progress = progress,
        assets = assets,
        isMuted = settings.isMuted,
        onBack = {
          viewModel.playClick()
          navController.popBackStack()
        },
        onToggleMute = viewModel::toggleMute,
        onTestAttackSound = { ghost ->
          val sfx =
            when (ghost) {
              com.example.model.GhostId.API -> SoundEffect.FIRE
              com.example.model.GhostId.KUNTI -> SoundEffect.KUNTI
              com.example.model.GhostId.GENDRUWO,
              com.example.model.GhostId.POCONG,
              com.example.model.GhostId.BUTO_IJO -> SoundEffect.SLAM_HEAVY
              com.example.model.GhostId.WEWE -> SoundEffect.HEAL
              com.example.model.GhostId.SUNDEL -> SoundEffect.CHARM
              com.example.model.GhostId.BARONG -> SoundEffect.LEVEL_UP
              com.example.model.GhostId.RANGDA -> SoundEffect.LIGHTNING
              com.example.model.GhostId.NYI_BLORONG -> SoundEffect.COIN
              else -> SoundEffect.HIT_LIGHT
            }
          viewModel.soundEngine.play(sfx)
        },
      )
    }

    composable(AppRoutes.MISSIONS) {
      MissionsScreen(
        progress = progress,
        history = history,
        isMuted = settings.isMuted,
        onBack = {
          viewModel.playClick()
          navController.popBackStack()
        },
        onToggleMute = viewModel::toggleMute,
        onClaimAchievement = viewModel::claimAchievement,
      )
    }

    composable(AppRoutes.BATTLE) {
      BattleScreen(
        config = activeBattleConfig,
        progress = progress,
        assets = assets,
        battleSpeed = settings.battleSpeed,
        isMuted = settings.isMuted,
        hapticsEnabled = settings.hapticsEnabled,
        onSound = { viewModel.soundEngine.play(it) },
        onConsumeJimat = viewModel::consumeJimatInBattle,
        onBattleOutcome = viewModel::commitBattleOutcome,
        onRetreatCoins = viewModel::commitRetreatCoins,
        onCycleSpeed = viewModel::cycleBattleSpeed,
        onToggleMute = viewModel::toggleMute,
        onLaunchNextStage = { nextIdx -> viewModel.prepareStageBattle(nextIdx) },
        onLaunchNextTournamentRound = { tIdx, nextRound ->
          viewModel.prepareTournamentRound(tIdx, nextRound, payEntryFee = false)
        },
        onLaunchNextInfinity = { nextLv -> viewModel.prepareInfinityBattle(nextLv) },
        onReplay = { viewModel.playClick() },
        onNavigateToStages = {
          viewModel.playClick()
          navController.navigate(AppRoutes.STAGES) {
            popUpTo(AppRoutes.HOME) { inclusive = false }
          }
        },
        onNavigateToTournament = {
          viewModel.playClick()
          navController.navigate(AppRoutes.TOURNAMENT) {
            popUpTo(AppRoutes.HOME) { inclusive = false }
          }
        },
        onNavigateToFree = {
          viewModel.playClick()
          navController.navigate(AppRoutes.FREE) {
            popUpTo(AppRoutes.HOME) { inclusive = false }
          }
        },
        onExitToMenu = {
          viewModel.playClick()
          navController.popBackStack(AppRoutes.HOME, inclusive = false)
        },
      )
    }
  }
}

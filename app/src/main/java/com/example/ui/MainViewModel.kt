package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffect
import com.example.audio.SoundEngine
import com.example.data.AppDatabase
import com.example.data.AssetLoader
import com.example.data.BattleHistoryEntity
import com.example.data.GameAssets
import com.example.data.GameRepository
import com.example.data.SettingsStore
import com.example.data.UserSettings
import com.example.game.BattleResultSummary
import com.example.model.AchievementCatalog
import com.example.model.BattleConfig
import com.example.model.BattleModeType
import com.example.model.FreeDifficulty
import com.example.model.GameCatalog
import com.example.model.GhostId
import com.example.model.JimatId
import com.example.model.PlayerProgress
import com.example.model.UpgradeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val db = AppDatabase.getInstance(application)
  private val repository = GameRepository(db.gameDao())
  private val settingsStore = SettingsStore(application)
  private val assetLoader = AssetLoader(application)
  val soundEngine = SoundEngine(application)

  val progress: StateFlow<PlayerProgress> =
    repository.progressFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = PlayerProgress(),
    )

  val history: StateFlow<List<BattleHistoryEntity>> =
    repository.historyFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList(),
    )

  val settings: StateFlow<UserSettings> =
    settingsStore.settingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UserSettings(),
    )

  private val _assets = MutableStateFlow(GameAssets())
  val assets: StateFlow<GameAssets> = _assets.asStateFlow()

  private val _activeBattleConfig = MutableStateFlow(GameCatalog.stageConfig(0))
  val activeBattleConfig: StateFlow<BattleConfig> = _activeBattleConfig.asStateFlow()

  private val _selectedFreeDifficulty = MutableStateFlow(FreeDifficulty.NORMAL)
  val selectedFreeDifficulty: StateFlow<FreeDifficulty> = _selectedFreeDifficulty.asStateFlow()

  private val _toastMessage = MutableStateFlow<String?>(null)
  val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

  init {
    viewModelScope.launch {
      repository.ensureInitialized()
      _assets.value = assetLoader.loadAll()
    }
    viewModelScope.launch {
      settingsStore.settingsFlow.collect { s ->
        soundEngine.isMuted = s.isMuted
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    soundEngine.release()
  }

  fun clearToast() {
    _toastMessage.value = null
  }

  fun playClick() = soundEngine.play(SoundEffect.CLICK)

  fun toggleMute() {
    val next = !settings.value.isMuted
    soundEngine.isMuted = next
    if (!next) soundEngine.play(SoundEffect.CLICK)
    viewModelScope.launch { settingsStore.setMuted(next) }
  }

  fun cycleBattleSpeed() {
    val next =
      when (settings.value.battleSpeed) {
        1 -> 2
        2 -> 3
        else -> 1
      }
    soundEngine.play(SoundEffect.CLICK)
    viewModelScope.launch { settingsStore.setBattleSpeed(next) }
  }

  fun setFreeDifficulty(diff: FreeDifficulty) {
    soundEngine.play(SoundEffect.CLICK)
    _selectedFreeDifficulty.value = diff
  }

  fun prepareStageBattle(stageIndex: Int) {
    soundEngine.play(SoundEffect.CLICK)
    _activeBattleConfig.value = GameCatalog.stageConfig(stageIndex)
  }

  fun prepareTournamentRound(tournamentIndex: Int, round: Int, payEntryFee: Boolean) {
    val t = GameCatalog.tournaments[tournamentIndex]
    if (payEntryFee && progress.value.coins < t.entryFee) return
    soundEngine.play(SoundEffect.CLICK)
    if (payEntryFee && t.entryFee > 0) {
      viewModelScope.launch {
        repository.updateProgress { cur -> cur.copy(coins = (cur.coins - t.entryFee).coerceAtLeast(0)) }
      }
    }
    _activeBattleConfig.value = GameCatalog.tournamentConfig(tournamentIndex, round)
  }

  fun prepareInfinityBattle(level: Int) {
    soundEngine.play(SoundEffect.CLICK)
    _activeBattleConfig.value = GameCatalog.infinityConfig(level)
  }

  fun prepareFreeBattle(stageIndex: Int) {
    soundEngine.play(SoundEffect.CLICK)
    _activeBattleConfig.value = GameCatalog.freeConfig(stageIndex, _selectedFreeDifficulty.value)
  }

  fun consumeJimatInBattle(jimat: JimatId) {
    viewModelScope.launch {
      repository.updateProgress { cur ->
        val count = cur.jimatCount(jimat)
        if (count <= 0) cur
        else cur.copy(inventory = cur.inventory + (jimat to (count - 1)))
      }
    }
  }

  fun commitBattleOutcome(summary: BattleResultSummary) {
    viewModelScope.launch {
      repository.updateProgress { cur ->
        var nextClear = cur.clearStage
        var nextInf = cur.infinityRecord
        var b = cur.trophyBronze
        var s = cur.trophySilver
        var g = cur.trophyGold

        if (summary.won) {
          when (summary.config.mode) {
            BattleModeType.STAGE -> {
              if (summary.config.stageIndex == cur.clearStage && cur.clearStage < GameCatalog.stages.size) {
                nextClear++
              }
            }
            BattleModeType.TOURNAMENT -> {
              val t = GameCatalog.tournaments[summary.config.tournamentIndex]
              if (summary.config.tournamentRound >= t.rounds - 1) {
                when (t.index) {
                  0 -> b++
                  1 -> s++
                  else -> g++
                }
              }
            }
            BattleModeType.INFINITY -> {
              if (summary.config.infinityLevel > nextInf) {
                nextInf = summary.config.infinityLevel
              }
            }
            BattleModeType.FREE -> Unit
          }
        }

        cur.copy(
          coins = cur.coins + summary.totalCoinsEarned,
          clearStage = nextClear,
          infinityRecord = nextInf,
          trophyBronze = b,
          trophySilver = s,
          trophyGold = g,
          totalVictories = cur.totalVictories + if (summary.won) 1 else 0,
          totalEnemiesDefeated = cur.totalEnemiesDefeated + summary.enemiesDefeated,
          totalGhostsSummoned = cur.totalGhostsSummoned + summary.ghostsSummoned,
        )
      }
      repository.recordBattle(
        BattleHistoryEntity(
          modeLabel = summary.config.mode.name,
          stageTitle = summary.config.label,
          won = summary.won,
          coinsEarned = summary.totalCoinsEarned,
          enemiesDefeated = summary.enemiesDefeated,
          durationSec = summary.durationSec,
        )
      )
    }
  }

  fun commitRetreatCoins(coinsInMatch: Int, enemiesDefeated: Int, ghostsSummoned: Int) {
    if (coinsInMatch <= 0 && enemiesDefeated <= 0 && ghostsSummoned <= 0) return
    viewModelScope.launch {
      repository.updateProgress { cur ->
        cur.copy(
          coins = cur.coins + coinsInMatch,
          totalEnemiesDefeated = cur.totalEnemiesDefeated + enemiesDefeated,
          totalGhostsSummoned = cur.totalGhostsSummoned + ghostsSummoned,
        )
      }
    }
  }

  fun buyJimat(jimat: JimatId) {
    val cur = progress.value
    if (cur.coins < jimat.price) return
    soundEngine.play(SoundEffect.COIN)
    viewModelScope.launch {
      repository.updateProgress { p ->
        if (p.coins < jimat.price) p
        else p.copy(
          coins = p.coins - jimat.price,
          inventory = p.inventory + (jimat to (p.jimatCount(jimat) + 1)),
        )
      }
      _toastMessage.value = "Membeli ${jimat.title}!"
    }
  }

  fun buyUpgrade(upgrade: UpgradeId) {
    val cur = progress.value
    val lv = cur.upgradeLevel(upgrade)
    val price = upgrade.priceForLevel(lv)
    if (lv >= upgrade.maxLevel || cur.coins < price) return
    soundEngine.play(SoundEffect.LEVEL_UP)
    viewModelScope.launch {
      repository.updateProgress { p ->
        val curLv = p.upgradeLevel(upgrade)
        val cost = upgrade.priceForLevel(curLv)
        if (curLv >= upgrade.maxLevel || p.coins < cost) p
        else p.copy(
          coins = p.coins - cost,
          upgrades = p.upgrades + (upgrade to (curLv + 1)),
        )
      }
      _toastMessage.value = "${upgrade.title} naik ke Level ${lv + 1}!"
    }
  }

  fun unlockGhost(ghost: GhostId) {
    val cur = progress.value
    if (ghost in cur.ownedGhosts || cur.coins < ghost.unlockPrice) return
    soundEngine.play(SoundEffect.SUMMON)
    viewModelScope.launch {
      repository.updateProgress { p ->
        if (ghost in p.ownedGhosts || p.coins < ghost.unlockPrice) p
        else p.copy(
          coins = p.coins - ghost.unlockPrice,
          ownedGhosts = p.ownedGhosts + ghost,
        )
      }
      _toastMessage.value = "${ghost.displayName} bergabung ke pasukanmu!"
    }
  }

  fun trainGhost(ghost: GhostId) {
    val cur = progress.value
    if (ghost !in cur.ownedGhosts) return
    val lv = cur.ghostLevel(ghost)
    val price = 50 + 45 * lv
    if (lv >= 5 || cur.coins < price) return
    soundEngine.play(SoundEffect.LEVEL_UP)
    viewModelScope.launch {
      repository.updateProgress { p ->
        val curLv = p.ghostLevel(ghost)
        val cost = 50 + 45 * curLv
        if (curLv >= 5 || p.coins < cost) p
        else p.copy(
          coins = p.coins - cost,
          ghostLevels = p.ghostLevels + (ghost to (curLv + 1)),
        )
      }
      _toastMessage.value = "${ghost.displayName} terlatih ke Lv.${lv + 1}!"
    }
  }

  fun claimAchievement(id: String) {
    val def = AchievementCatalog.all.firstOrNull { it.id == id } ?: return
    val cur = progress.value
    if (id in cur.claimedAchievements) return
    val (done, target) = def.progressProvider(cur)
    if (done < target) return
    soundEngine.play(SoundEffect.VICTORY)
    viewModelScope.launch {
      repository.updateProgress { p ->
        if (id in p.claimedAchievements) p
        else p.copy(
          coins = p.coins + def.rewardCoins,
          claimedAchievements = p.claimedAchievements + id,
        )
      }
      _toastMessage.value = "Hadiah ${def.title}: +${def.rewardCoins} koin!"
    }
  }
}

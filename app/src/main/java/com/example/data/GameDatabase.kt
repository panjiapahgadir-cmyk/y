package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.GhostId
import com.example.model.JimatId
import com.example.model.PlayerProgress
import com.example.model.UpgradeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "player_progress")
data class PlayerProgressEntity(
  @PrimaryKey val id: Int = 1,
  val coins: Int = 0,
  val clearStage: Int = 0,
  val infinityRecord: Int = 0,
  val trophyBronze: Int = 0,
  val trophySilver: Int = 0,
  val trophyGold: Int = 0,
  val ownedGhostsCsv: String = "tuyul,pocong,api,kuyang,kunti",
  val ghostLevelsSerialized: String = "",
  val upgradesSerialized: String = "",
  val inventorySerialized: String = "",
  val claimedAchievementsCsv: String = "",
  val totalVictories: Int = 0,
  val totalEnemiesDefeated: Int = 0,
  val totalGhostsSummoned: Int = 0,
)

@Entity(tableName = "battle_history")
data class BattleHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val modeLabel: String,
  val stageTitle: String,
  val won: Boolean,
  val coinsEarned: Int,
  val enemiesDefeated: Int,
  val durationSec: Int,
  val timestampMs: Long = System.currentTimeMillis(),
)

@Dao
interface GameDao {
  @Query("SELECT * FROM player_progress WHERE id = 1")
  fun observeProgress(): Flow<PlayerProgressEntity?>

  @Query("SELECT * FROM player_progress WHERE id = 1")
  suspend fun getProgressOnce(): PlayerProgressEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProgress(entity: PlayerProgressEntity)

  @Query("SELECT * FROM battle_history ORDER BY timestampMs DESC LIMIT 25")
  fun observeHistory(): Flow<List<BattleHistoryEntity>>

  @Insert
  suspend fun insertHistory(entry: BattleHistoryEntity)
}

@Database(
  entities = [PlayerProgressEntity::class, BattleHistoryEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun gameDao(): GameDao

  companion object {
    @Volatile private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "perang_hantu.db",
              )
              .build()
              .also { instance = it }
        }
  }
}

class GameRepository(private val dao: GameDao) {
  val progressFlow: Flow<PlayerProgress> =
    dao.observeProgress().map { entity -> entity?.toDomain() ?: PlayerProgress() }

  val historyFlow: Flow<List<BattleHistoryEntity>> = dao.observeHistory()

  suspend fun ensureInitialized() {
    if (dao.getProgressOnce() == null) {
      dao.upsertProgress(PlayerProgress().toEntity())
    }
  }

  suspend fun updateProgress(transform: (PlayerProgress) -> PlayerProgress) {
    val current = dao.getProgressOnce()?.toDomain() ?: PlayerProgress()
    val updated = transform(current)
    dao.upsertProgress(updated.toEntity())
  }

  suspend fun recordBattle(entry: BattleHistoryEntity) {
    dao.insertHistory(entry)
  }

  private fun PlayerProgressEntity.toDomain(): PlayerProgress {
    val owned =
      ownedGhostsCsv
        .split(",")
        .mapNotNull { GhostId.fromKey(it.trim()) }
        .toMutableSet()
        .apply { addAll(GhostId.entries.filter { it.isStarter }) }
    val ghostLv =
      parseKeyIntMap(ghostLevelsSerialized).mapNotNull { (k, v) ->
        GhostId.fromKey(k)?.let { it to v.coerceIn(0, 5) }
      }.toMap()
    val ups =
      UpgradeId.entries.associateWith { up ->
        (parseKeyIntMap(upgradesSerialized)[up.key] ?: 0).coerceIn(0, up.maxLevel)
      }
    val inv =
      JimatId.entries.associateWith { jm ->
        (parseKeyIntMap(inventorySerialized)[jm.key] ?: 0).coerceAtLeast(0)
      }
    val claimed = claimedAchievementsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    return PlayerProgress(
      coins = coins.coerceAtLeast(0),
      clearStage = clearStage.coerceIn(0, 10),
      infinityRecord = infinityRecord.coerceAtLeast(0),
      trophyBronze = trophyBronze.coerceAtLeast(0),
      trophySilver = trophySilver.coerceAtLeast(0),
      trophyGold = trophyGold.coerceAtLeast(0),
      ownedGhosts = owned,
      ghostLevels = ghostLv,
      upgrades = ups,
      inventory = inv,
      claimedAchievements = claimed,
      totalVictories = totalVictories.coerceAtLeast(0),
      totalEnemiesDefeated = totalEnemiesDefeated.coerceAtLeast(0),
      totalGhostsSummoned = totalGhostsSummoned.coerceAtLeast(0),
    )
  }

  private fun PlayerProgress.toEntity(): PlayerProgressEntity =
    PlayerProgressEntity(
      id = 1,
      coins = coins,
      clearStage = clearStage,
      infinityRecord = infinityRecord,
      trophyBronze = trophyBronze,
      trophySilver = trophySilver,
      trophyGold = trophyGold,
      ownedGhostsCsv = ownedGhosts.joinToString(",") { it.key },
      ghostLevelsSerialized = ghostLevels.entries.joinToString(",") { "${it.key.key}:${it.value}" },
      upgradesSerialized = upgrades.entries.joinToString(",") { "${it.key.key}:${it.value}" },
      inventorySerialized =
        inventory.filterValues { it > 0 }.entries.joinToString(",") { "${it.key.key}:${it.value}" },
      claimedAchievementsCsv = claimedAchievements.joinToString(","),
      totalVictories = totalVictories,
      totalEnemiesDefeated = totalEnemiesDefeated,
      totalGhostsSummoned = totalGhostsSummoned,
    )

  private fun parseKeyIntMap(raw: String): Map<String, Int> {
    if (raw.isBlank()) return emptyMap()
    return raw.split(",").mapNotNull { token ->
      val idx = token.indexOf(':')
      if (idx <= 0) null
      else {
        val k = token.substring(0, idx).trim()
        val v = token.substring(idx + 1).trim().toIntOrNull() ?: 0
        k to v
      }
    }.toMap()
  }
}

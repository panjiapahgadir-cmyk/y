package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "perang_hantu_prefs")

data class UserSettings(
  val isMuted: Boolean = false,
  val battleSpeed: Int = 1, // 1, 2, or 3
  val hapticsEnabled: Boolean = true,
)

class SettingsStore(private val context: Context) {
  private object Keys {
    val IS_MUTED = booleanPreferencesKey("is_muted")
    val BATTLE_SPEED = intPreferencesKey("battle_speed")
    val HAPTICS = booleanPreferencesKey("haptics")
  }

  val settingsFlow: Flow<UserSettings> =
    context.settingsDataStore.data.map { prefs ->
      UserSettings(
        isMuted = prefs[Keys.IS_MUTED] ?: false,
        battleSpeed = (prefs[Keys.BATTLE_SPEED] ?: 1).coerceIn(1, 3),
        hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
      )
    }

  suspend fun setMuted(muted: Boolean) {
    context.settingsDataStore.edit { it[Keys.IS_MUTED] = muted }
  }

  suspend fun setBattleSpeed(speed: Int) {
    context.settingsDataStore.edit { it[Keys.BATTLE_SPEED] = speed.coerceIn(1, 3) }
  }

  suspend fun setHapticsEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.HAPTICS] = enabled }
  }
}

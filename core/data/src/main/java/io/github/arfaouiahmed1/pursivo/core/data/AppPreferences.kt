package io.github.arfaouiahmed1.pursivo.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.arfaouiahmed1.pursivo.core.model.UpdateChannel
import io.github.arfaouiahmed1.pursivo.core.model.UpdatePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pursivoDataStore by preferencesDataStore("pursivo_preferences")

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class AppPreferenceState(
    val onboardingComplete: Boolean = false,
    val dynamicColor: Boolean = true,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val update: UpdatePreference = UpdatePreference(),
)

@Singleton
class AppPreferences @Inject constructor(@param:ApplicationContext private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_complete")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val theme = stringPreferencesKey("theme")
        val updateChannel = stringPreferencesKey("update_channel")
        val dismissedVersion = stringPreferencesKey("dismissed_version")
        val lastChecked = longPreferencesKey("last_checked")
    }

    val state: Flow<AppPreferenceState> = context.pursivoDataStore.data.map { values ->
        AppPreferenceState(
            onboardingComplete = values[Keys.onboarding] ?: false,
            dynamicColor = values[Keys.dynamicColor] ?: true,
            theme = values[Keys.theme]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.SYSTEM,
            update = UpdatePreference(
                channel = values[Keys.updateChannel]?.let(UpdateChannel::valueOf) ?: UpdateChannel.STABLE,
                dismissedVersion = values[Keys.dismissedVersion] ?: "",
                lastCheckedAt = values[Keys.lastChecked] ?: 0,
            ),
        )
    }

    suspend fun completeOnboarding() { context.pursivoDataStore.edit { it[Keys.onboarding] = true } }
    suspend fun setDynamicColor(enabled: Boolean) { context.pursivoDataStore.edit { it[Keys.dynamicColor] = enabled } }
    suspend fun setTheme(theme: ThemePreference) { context.pursivoDataStore.edit { it[Keys.theme] = theme.name } }
    suspend fun setUpdateChannel(channel: UpdateChannel) { context.pursivoDataStore.edit { it[Keys.updateChannel] = channel.name } }
    suspend fun recordUpdateCheck(time: Long) { context.pursivoDataStore.edit { it[Keys.lastChecked] = time } }
    suspend fun dismissVersion(version: String) { context.pursivoDataStore.edit { it[Keys.dismissedVersion] = version } }
}

package com.example.myapplication.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.data.model.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserDataStore @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private val USER_ID = intPreferencesKey("user_id")
        private val FIREBASE_UID = stringPreferencesKey("firebase_uid")
        private val NICKNAME = stringPreferencesKey("nickname")
        private val NAME = stringPreferencesKey("name")
        private val EMAIL = stringPreferencesKey("email")
        private val WALLET_ADDRESS = stringPreferencesKey("wallet_address")
        private val AI_VOICE = booleanPreferencesKey("ai_voice")
        private val IS_SIGN_UP = booleanPreferencesKey("is_sign_up")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }
    
    val userInfo: Flow<UserInfo?> = context.dataStore.data.map { preferences ->
        val id = preferences[USER_ID]
        val firebaseUid = preferences[FIREBASE_UID]
        val nickname = preferences[NICKNAME]
        val name = preferences[NAME]
        val email = preferences[EMAIL]
        
        if (id != null && firebaseUid != null && nickname != null && name != null && email != null) {
            UserInfo(
                id = id,
                firebaseUid = firebaseUid,
                nickname = nickname,
                name = name,
                email = email,
                walletAddress = preferences[WALLET_ADDRESS],
                aiVoice = preferences[AI_VOICE] ?: false,
                isSignUp = preferences[IS_SIGN_UP] ?: false
            )
        } else {
            null
        }
    }
    
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }
    
    suspend fun saveUserInfo(userInfo: UserInfo) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userInfo.id
            preferences[FIREBASE_UID] = userInfo.firebaseUid
            preferences[NICKNAME] = userInfo.nickname
            preferences[NAME] = userInfo.name
            preferences[EMAIL] = userInfo.email
            preferences[WALLET_ADDRESS] = userInfo.walletAddress ?: ""
            preferences[AI_VOICE] = userInfo.aiVoice
            preferences[IS_SIGN_UP] = userInfo.isSignUp
            preferences[IS_LOGGED_IN] = true
        }
    }
    
    suspend fun clearUserInfo() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_ID)
            preferences.remove(FIREBASE_UID)
            preferences.remove(NICKNAME)
            preferences.remove(NAME)
            preferences.remove(EMAIL)
            preferences.remove(WALLET_ADDRESS)
            preferences.remove(AI_VOICE)
            preferences.remove(IS_SIGN_UP)
            preferences[IS_LOGGED_IN] = false
        }
    }
}


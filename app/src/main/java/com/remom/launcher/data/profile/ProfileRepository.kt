package com.remom.launcher.data.profile

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Simple JSON-based local storage for profiles, accounts and settings.
 * Later can be upgraded to Room if needed.
 */
class ProfileRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dataDir: File = File(context.filesDir, "remom_data").also { it.mkdirs() }

    private val profilesFile = File(dataDir, "profiles.json")
    private val accountsFile = File(dataDir, "accounts.json")
    private val settingsFile = File(dataDir, "settings.json")

    // ---------- Profiles ----------

    suspend fun loadProfiles(): ProfileList = withContext(Dispatchers.IO) {
        if (!profilesFile.exists()) {
            val default = ProfileList(
                profiles = listOf(
                    GameProfile(
                        id = UUID.randomUUID().toString(),
                        name = "Vanilla Latest",
                        versionId = "1.21.3",
                        loader = "Vanilla"
                    )
                )
            )
            saveProfiles(default)
            return@withContext default
        }
        try {
            gson.fromJson(profilesFile.readText(), ProfileList::class.java) ?: ProfileList()
        } catch (e: Exception) {
            ProfileList()
        }
    }

    suspend fun saveProfiles(list: ProfileList) = withContext(Dispatchers.IO) {
        profilesFile.writeText(gson.toJson(list))
    }

    suspend fun addProfile(profile: GameProfile) {
        val current = loadProfiles()
        val updated = current.copy(profiles = current.profiles + profile)
        saveProfiles(updated)
    }

    suspend fun deleteProfile(id: String) {
        val current = loadProfiles()
        val updated = current.copy(
            profiles = current.profiles.filter { it.id != id },
            selectedProfileId = if (current.selectedProfileId == id) null else current.selectedProfileId
        )
        saveProfiles(updated)
        // Also delete the profile folder if exists
        val folder = File(context.filesDir, "instances/$id")
        if (folder.exists()) folder.deleteRecursively()
    }

    suspend fun selectProfile(id: String) {
        val current = loadProfiles()
        saveProfiles(current.copy(selectedProfileId = id))
    }

    fun getProfileDir(profileId: String): File {
        return File(context.filesDir, "instances/$profileId").also { it.mkdirs() }
    }

    fun getModsDir(profileId: String): File {
        return File(getProfileDir(profileId), "mods").also { it.mkdirs() }
    }

    fun getResourcePacksDir(profileId: String): File {
        return File(getProfileDir(profileId), "resourcepacks").also { it.mkdirs() }
    }

    fun getShaderPacksDir(profileId: String): File {
        return File(getProfileDir(profileId), "shaderpacks").also { it.mkdirs() }
    }

    // ---------- Accounts ----------

    suspend fun loadAccounts(): AccountList = withContext(Dispatchers.IO) {
        if (!accountsFile.exists()) return@withContext AccountList()
        try {
            gson.fromJson(accountsFile.readText(), AccountList::class.java) ?: AccountList()
        } catch (e: Exception) {
            AccountList()
        }
    }

    suspend fun saveAccounts(list: AccountList) = withContext(Dispatchers.IO) {
        accountsFile.writeText(gson.toJson(list))
    }

    suspend fun addAccount(account: AccountInfo) {
        val current = loadAccounts()
        val updated = current.copy(accounts = current.accounts + account)
        saveAccounts(updated)
    }

    suspend fun removeAccount(id: String) {
        val current = loadAccounts()
        val updated = current.copy(
            accounts = current.accounts.filter { it.id != id },
            selectedAccountId = if (current.selectedAccountId == id) null else current.selectedAccountId
        )
        saveAccounts(updated)
    }

    // ---------- Settings ----------

    suspend fun loadSettings(): LauncherSettings = withContext(Dispatchers.IO) {
        if (!settingsFile.exists()) {
            val default = LauncherSettings()
            saveSettings(default)
            return@withContext default
        }
        try {
            gson.fromJson(settingsFile.readText(), LauncherSettings::class.java) ?: LauncherSettings()
        } catch (e: Exception) {
            LauncherSettings()
        }
    }

    suspend fun saveSettings(settings: LauncherSettings) = withContext(Dispatchers.IO) {
        settingsFile.writeText(gson.toJson(settings))
    }
}

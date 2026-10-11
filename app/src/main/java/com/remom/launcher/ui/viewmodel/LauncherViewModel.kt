package com.remom.launcher.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.remom.launcher.data.modrinth.ModrinthHit
import com.remom.launcher.data.modrinth.ModrinthRepository
import com.remom.launcher.data.modrinth.ModrinthVersion
import com.remom.launcher.data.profile.AccountInfo
import com.remom.launcher.data.profile.GameProfile
import com.remom.launcher.data.profile.LauncherSettings
import com.remom.launcher.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ModsUiState(
    val isLoading: Boolean = false,
    val searchResults: List<ModrinthHit> = emptyList(),
    val installedMods: List<File> = emptyList(),
    val error: String? = null,
    val downloading: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ProfileRepository(application)
    private val modrinth = ModrinthRepository()

    private val _profiles = MutableStateFlow<List<GameProfile>>(emptyList())
    val profiles: StateFlow<List<GameProfile>> = _profiles.asStateFlow()

    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId: StateFlow<String?> = _selectedProfileId.asStateFlow()

    private val _accounts = MutableStateFlow<List<AccountInfo>>(emptyList())
    val accounts: StateFlow<List<AccountInfo>> = _accounts.asStateFlow()

    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _modsState = MutableStateFlow(ModsUiState())
    val modsState: StateFlow<ModsUiState> = _modsState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            val profileList = repo.loadProfiles()
            _profiles.value = profileList.profiles
            _selectedProfileId.value = profileList.selectedProfileId ?: profileList.profiles.firstOrNull()?.id

            val accountList = repo.loadAccounts()
            _accounts.value = accountList.accounts

            _settings.value = repo.loadSettings()

            loadInstalledMods()
        }
    }

    // ---------- Profiles ----------

    fun addProfile(name: String, versionId: String, loader: String = "Vanilla") {
        viewModelScope.launch {
            val profile = GameProfile(
                id = UUID.randomUUID().toString(),
                name = name,
                versionId = versionId,
                loader = loader
            )
            repo.addProfile(profile)
            refreshAll()
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            repo.deleteProfile(id)
            refreshAll()
        }
    }

    fun selectProfile(id: String) {
        viewModelScope.launch {
            repo.selectProfile(id)
            _selectedProfileId.value = id
            loadInstalledMods()
        }
    }

    // ---------- Accounts ----------

    fun addOfflineAccount(username: String) {
        viewModelScope.launch {
            val account = AccountInfo(
                id = UUID.randomUUID().toString(),
                username = username,
                type = "Offline",
                uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray()).toString()
            )
            repo.addAccount(account)
            refreshAll()
        }
    }

    fun removeAccount(id: String) {
        viewModelScope.launch {
            repo.removeAccount(id)
            refreshAll()
        }
    }

    // ---------- Settings ----------

    fun updateSettings(settings: LauncherSettings) {
        viewModelScope.launch {
            repo.saveSettings(settings)
            _settings.value = settings
        }
    }

    // ---------- Mods ----------

    fun loadInstalledMods() {
        viewModelScope.launch {
            val profileId = _selectedProfileId.value ?: return@launch
            val modsDir = repo.getModsDir(profileId)
            val files = modrinth.listInstalledMods(modsDir)
            _modsState.value = _modsState.value.copy(installedMods = files)
        }
    }

    fun searchModrinth(query: String, projectType: String = "mod") {
        if (query.isBlank()) return
        viewModelScope.launch {
            _modsState.value = _modsState.value.copy(isLoading = true, error = null)
            val result = modrinth.search(query, projectType = projectType)
            result.fold(
                onSuccess = { response ->
                    _modsState.value = _modsState.value.copy(
                        isLoading = false,
                        searchResults = response.hits
                    )
                },
                onFailure = { e ->
                    _modsState.value = _modsState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Search failed"
                    )
                }
            )
        }
    }

    fun downloadMod(hit: ModrinthHit, gameVersion: String? = null, loader: String? = null) {
        viewModelScope.launch {
            val profileId = _selectedProfileId.value
            if (profileId == null) {
                _modsState.value = _modsState.value.copy(error = "No profile selected")
                return@launch
            }

            _modsState.value = _modsState.value.copy(downloading = hit.title, error = null)

            val versionsResult = modrinth.getVersions(hit.project_id, gameVersion, loader)
            val versions = versionsResult.getOrNull()
            if (versions.isNullOrEmpty()) {
                _modsState.value = _modsState.value.copy(
                    downloading = null,
                    error = "No compatible version found"
                )
                return@launch
            }

            val best = versions.first()
            val modsDir = repo.getModsDir(profileId)
            val downloadResult = modrinth.downloadVersion(best, modsDir)

            downloadResult.fold(
                onSuccess = {
                    _modsState.value = _modsState.value.copy(downloading = null)
                    loadInstalledMods()
                },
                onFailure = { e ->
                    _modsState.value = _modsState.value.copy(
                        downloading = null,
                        error = e.message ?: "Download failed"
                    )
                }
            )
        }
    }

    fun deleteInstalledMod(file: File) {
        viewModelScope.launch {
            modrinth.deleteMod(file)
            loadInstalledMods()
        }
    }

    fun getModsDir(): File? {
        val id = _selectedProfileId.value ?: return null
        return repo.getModsDir(id)
    }
}

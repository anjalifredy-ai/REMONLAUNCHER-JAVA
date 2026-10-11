package com.remom.launcher.engine

import android.app.Activity
import android.content.Context
import android.util.Log
import com.remom.launcher.data.profile.AccountInfo
import com.remom.launcher.data.profile.GameProfile
import com.remom.launcher.data.profile.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * High-level engine facade used by the UI.
 *
 * Flow:
 * 1. prepare(versionId)  → downloads client + libs (GameInstaller)
 * 2. launch(...)         → builds LaunchConfig → RuntimeBridge.startGame
 *
 * When RuntimeBridge is swapped from Stub → real Amethyst/Mojo backend,
 * the UI does not need to change.
 */
class GameEngine(
    context: Context,
    private val runtime: RuntimeBridge = StubRuntimeBridge()
) {
    private val paths = GamePaths(context)
    private val installer = GameInstaller(paths)
    private val launchBuilder = LaunchBuilder(paths)

    private val _state = MutableStateFlow(LaunchState.IDLE)
    val state: StateFlow<LaunchState> = _state.asStateFlow()

    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    fun isVersionReady(versionId: String): Boolean = installer.isVersionReady(versionId)

    /**
     * Download everything needed for [versionId].
     * Safe to call multiple times (skips existing verified files).
     */
    suspend fun prepare(versionId: String) {
        _state.value = LaunchState.PREPARING
        _lastMessage.value = "Preparing $versionId..."

        installer.prepare(versionId).collect { event ->
            _progress.value = event
            when (event) {
                is DownloadProgress.Stage ->
                    _lastMessage.value = "${event.name}: ${event.detail}"
                is DownloadProgress.Overall ->
                    _lastMessage.value = "Libraries ${event.completedFiles}/${event.totalFiles}"
                is DownloadProgress.Done -> {
                    _state.value = if (event.success) LaunchState.READY else LaunchState.ERROR
                    _lastMessage.value = event.message
                }
                is DownloadProgress.Error -> {
                    _state.value = LaunchState.ERROR
                    _lastMessage.value = event.message
                    Log.e(TAG, event.message, event.cause)
                }
                else -> {}
            }
        }
    }

    /**
     * Full launch: prepare if needed, build config, hand off to runtime.
     */
    suspend fun launch(
        activity: Activity,
        profile: GameProfile,
        account: AccountInfo,
        settings: LauncherSettings
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!installer.isVersionReady(profile.versionId)) {
                prepare(profile.versionId)
                if (_state.value == LaunchState.ERROR) return@withContext false
            }

            _state.value = LaunchState.STARTING
            _lastMessage.value = "Building launch config..."

            val config = launchBuilder.build(profile, account, settings)

            _lastMessage.value = "Starting runtime..."
            val ok = runtime.startGame(activity, config)

            if (ok) {
                _state.value = LaunchState.RUNNING
                _lastMessage.value = "Game running"
            } else {
                _state.value = LaunchState.ERROR
                _lastMessage.value = runtime.getLastError() ?: "Runtime failed to start"
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "Launch failed", e)
            _state.value = LaunchState.ERROR
            _lastMessage.value = e.message ?: "Unknown launch error"
            false
        }
    }

    fun stop() {
        runtime.stopGame()
        _state.value = LaunchState.STOPPED
        _lastMessage.value = "Stopped"
    }

    fun getPaths(): GamePaths = paths

    companion object {
        private const val TAG = "GameEngine"
    }
}

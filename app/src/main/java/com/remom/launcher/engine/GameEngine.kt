package com.remom.launcher.engine

import android.app.Activity
import android.content.Context
import android.util.Log
import com.remom.launcher.data.profile.AccountInfo
import com.remom.launcher.data.profile.GameProfile
import com.remom.launcher.data.profile.LauncherSettings
import com.remom.launcher.engine.loaders.LoaderInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Engine facade: install official Minecraft + optional loader, then hand off to runtime.
 * Default runtime = AmethystRuntimeBridge (playable only after Amethyst linked / installed).
 */
class GameEngine(
    context: Context,
    private val runtime: RuntimeBridge = AmethystRuntimeBridge()
) {
    private val paths = GamePaths(context)
    private val installer = GameInstaller(paths)
    private val loaderInstaller = LoaderInstaller(paths)
    private val launchBuilder = LaunchBuilder(paths)

    private val _state = MutableStateFlow(LaunchState.IDLE)
    val state: StateFlow<LaunchState> = _state.asStateFlow()

    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    fun isVersionReady(versionId: String): Boolean = installer.isVersionReady(versionId)

    suspend fun prepare(versionId: String) {
        _state.value = LaunchState.PREPARING
        _lastMessage.value = "Preparing $versionId..."

        installer.prepare(versionId).collect { event ->
            _progress.value = event
            when (event) {
                is DownloadProgress.Stage ->
                    _lastMessage.value = "${event.name}: ${event.detail}"
                is DownloadProgress.Overall ->
                    _lastMessage.value = "${event.message} (${event.completedFiles}/${event.totalFiles})"
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

    suspend fun prepareLoader(loader: String, gameVersion: String) {
        if (loader.equals("Vanilla", true) || loader.isBlank()) return
        _lastMessage.value = "Installing $loader..."
        loaderInstaller.install(loader, gameVersion).collect { event ->
            _progress.value = event
            if (event is DownloadProgress.Done) _lastMessage.value = event.message
            if (event is DownloadProgress.Error) _lastMessage.value = event.message
        }
    }

    suspend fun launch(
        activity: Activity,
        profile: GameProfile,
        account: AccountInfo,
        settings: LauncherSettings
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseVersion = profile.versionId
                .substringBefore("-fabric")
                .substringBefore("-forge")
                .substringBefore("-neoforge")
                .substringBefore("-quilt")

            if (!installer.isVersionReady(baseVersion) && !installer.isVersionReady(profile.versionId)) {
                prepare(baseVersion)
                if (_state.value == LaunchState.ERROR) return@withContext false
            }

            if (!profile.loader.equals("Vanilla", true)) {
                prepareLoader(profile.loader, baseVersion)
            }

            _state.value = LaunchState.STARTING
            _lastMessage.value = "Building launch config..."

            val opt = AmethystRuntimeBridge.optimizedJvmExtras(settings.allocatedRamMb)
                .joinToString(" ")
            val tuned = settings.copy(
                jvmArgs = (settings.jvmArgs + " " + opt).trim()
            )

            val config = launchBuilder.build(profile, account, tuned)

            _lastMessage.value = "Starting Amethyst runtime bridge..."
            val ok = runtime.startGame(activity, config)

            if (ok) {
                _state.value = LaunchState.RUNNING
                _lastMessage.value = runtime.getLastError() ?: "Runtime / game window started"
            } else {
                _state.value = LaunchState.ERROR
                _lastMessage.value = runtime.getLastError() ?: "Runtime failed"
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

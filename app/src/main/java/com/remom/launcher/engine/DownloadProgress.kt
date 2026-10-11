package com.remom.launcher.engine

/**
 * Progress events emitted while preparing a game for launch.
 */
sealed class DownloadProgress {
    data class Stage(val name: String, val detail: String = "") : DownloadProgress()
    data class FileProgress(
        val fileName: String,
        val downloaded: Long,
        val total: Long
    ) : DownloadProgress() {
        val percent: Int
            get() = if (total > 0) ((downloaded * 100) / total).toInt() else 0
    }
    data class Overall(
        val completedFiles: Int,
        val totalFiles: Int,
        val message: String = ""
    ) : DownloadProgress()
    data class Done(val success: Boolean, val message: String = "") : DownloadProgress()
    data class Error(val message: String, val cause: Throwable? = null) : DownloadProgress()
}

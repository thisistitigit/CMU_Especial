package com.example.reviewapp.utils

inline fun <T> runCatchingLog(tag: String, msg: String, block: () -> T): Result<T> =
    runCatching(block).onFailure { e -> android.util.Log.e(tag, msg, e) }

suspend inline fun <T> runCatchingLogAsync(
    tag: String,
    msg: String,
    crossinline block: suspend () -> T
): Result<T> = try {
    Result.success(block())
} catch (e: Exception) {
    android.util.Log.e(tag, msg, e)
    Result.failure(e)
}

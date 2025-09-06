package com.example.reviewapp.utils



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

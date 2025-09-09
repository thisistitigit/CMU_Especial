package com.example.reviewapp.utils

/**
 * Variante *suspending* de `runCatching` com **logging** em caso de falha.
 *
 * Útil em pipelines assíncronos (upload, rede, FS) onde queremos registar
 * o erro mas continuar com `Result<T>`.
 *
 * @param tag tag para o logcat.
 * @param msg mensagem amigável para diagnóstico.
 * @param block bloco suspenso a executar com captura de exceções.
 * @return `Result.success(T)` ou `Result.failure(Throwable)` (com log).
 */
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

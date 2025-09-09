package com.example.reviewapp.core.ext

/**
 * Extensões numéricas utilitárias para computação de métricas.
 *
 * @since 1.0
 */

/** Converte `null` em `0.0`. Útil para cálculos de média/totais. */
fun Double?.orZero() = this ?: 0.0

/**
 * Calcula a média de uma lista usando um seletor de `Double`.
 *
 * Em listas vazias devolve `0.0` para evitar `NaN`.
 *
 * @param selector função que extrai o valor a promediar.
 * @return média aritmética dos valores selecionados, ou `0.0` se a lista estiver vazia.
 */
fun <T> List<T>.averageBy(selector: (T) -> Double): Double =
    if (isEmpty()) 0.0 else sumOf(selector) / size

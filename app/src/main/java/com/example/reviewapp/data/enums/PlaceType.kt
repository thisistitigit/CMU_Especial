package com.example.reviewapp.data.enums

/**
 * Enumeração dos *types* da Google Places API relevantes para o domínio.
 *
 * O `apiName` corresponde ao valor usado nas queries do Places (Nearby Search).
 *
 * **Grupos utilitários**:
 * - [PASTRY_FOOD] → consumo no local (pastelaria/café/restaurante).
 * - [RETAIL_SWEETS] → retalho que pode vender doçaria/ingredientes.
 * - [DEFAULT] → união dos dois conjuntos anteriores.
 *
 * @property apiName nome do tipo na Google Places API.
 * @since 1.0
 */
enum class PlaceType(val apiName: String) {
    BAKERY("bakery"),
    CAFE("cafe"),
    RESTAURANT("restaurant"),
    GROCERY_OR_SUPERMARKET("grocery_or_supermarket"),
    CONVENIENCE_STORE("convenience_store"),
    STORE("store");

    companion object {
        /** Foco em consumo no local. */
        val PASTRY_FOOD: Set<PlaceType> = setOf(BAKERY, CAFE, RESTAURANT)

        /** Foco em lojas que podem vender doçaria/ingredientes. */
        val RETAIL_SWEETS: Set<PlaceType> = setOf(
            BAKERY, GROCERY_OR_SUPERMARKET, CONVENIENCE_STORE, STORE
        )

        /** Universo padrão de interesse para a app. */
        val DEFAULT: Set<PlaceType> = PASTRY_FOOD + RETAIL_SWEETS
    }
}

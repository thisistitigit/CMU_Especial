package com.example.reviewapp.data.enums

/** Google Places "type" names — 1 por pedido NearbySearch */
enum class PlaceType(val apiName: String) {
    BAKERY("bakery"),
    CAFE("cafe"),
    RESTAURANT("restaurant"),
    GROCERY_OR_SUPERMARKET("grocery_or_supermarket"),
    CONVENIENCE_STORE("convenience_store"),
    STORE("store");

    companion object {
        /** Foco em consumo no local */
        val PASTRY_FOOD: Set<PlaceType> = setOf(
            BAKERY, CAFE,RESTAURANT
        )
        /** Foco em lojas que podem vender doçaria/ingredientes */
        val RETAIL_SWEETS: Set<PlaceType> = setOf(
            BAKERY, GROCERY_OR_SUPERMARKET, CONVENIENCE_STORE, STORE
        )
        /** Tudo o que nos interessa para o teu caso */
        val DEFAULT: Set<PlaceType> = PASTRY_FOOD + RETAIL_SWEETS
    }
}

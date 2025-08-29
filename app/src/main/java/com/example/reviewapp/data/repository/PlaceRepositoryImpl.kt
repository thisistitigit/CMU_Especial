    package com.example.reviewapp.data.repository

    import com.example.reviewapp.data.dao.PlaceDao
    import com.example.reviewapp.data.locals.PlaceEntity
    import com.example.reviewapp.data.models.Place
    import com.example.reviewapp.network.api.GeopifyApi
    import com.example.reviewapp.network.mappers.toPlaces
    import com.google.firebase.firestore.FirebaseFirestore
    import kotlin.math.cos

    // ⚠️ REMOVE estes imports errados/que não precisas:
    // import com.example.reviewapp.network.RetrofitInstance.api
    // import com.google.type.LatLng
    // (se quiseres um overload com LatLng do Maps, importas o correto:
    //  com.google.android.gms.maps.model.LatLng)

    class PlaceRepositoryImpl(
        private val placeDao: PlaceDao,
        private val api: GeopifyApi,
        @javax.inject.Named("GEO_API_KEY") private val geoApiKey: String,
        private val firestore: FirebaseFirestore? = null
    ) : PlaceRepository {

        override suspend fun searchAround(
            lat: Double,
            lng: Double,
            radiusMeters: Int
        ): List<Place> {
            // 1) cache local por bounds
            val dLat = radiusMeters / 111_320.0
            val dLng = radiusMeters / (111_320.0 * cos(Math.toRadians(lat)))
            val minLat = lat - dLat
            val maxLat = lat + dLat
            val minLng = lng - dLng
            val maxLng = lng + dLng

            val cached = placeDao.listInBounds(minLat, maxLat, minLng, maxLng)
                .map { it.toModel() }
            if (cached.isNotEmpty()) return cached.sortedByDescending { it.avgRating }

            // 2) remoto (Geoapify) + cache
            return runCatching {
                val filter = "circle:$lng,$lat,$radiusMeters"
                val resp = api.getPlaces(
                    categories = "catering.cafe,catering.bakery,catering.fast_food",
                    filter = filter,
                    limit = 50,
                    apiKey = geoApiKey
                )
                val fromRemote = resp.toPlaces()

                // cache local
                val now = System.currentTimeMillis()
                placeDao.upsertAll(fromRemote.map { it.toEntity(now) })
                fromRemote
            }.getOrElse { emptyList() }
        }

        // ✅ Convenience: mesmos nomes que queres usar
        // (A interface já fornece um default nearby(..) que chama searchAround(..),
        //  mas deixo estes helpers por simetria/ergonomia.)

        override suspend fun nearby(lat: Double, lng: Double, radiusMeters: Int): List<Place> =
            searchAround(lat, lng, radiusMeters)

        // Se te der jeito passar LatLng do Google Maps:
        suspend fun nearby(
            center: com.google.android.gms.maps.model.LatLng,
            radiusMeters: Int = 250
        ): List<Place> = searchAround(center.latitude, center.longitude, radiusMeters)

        override suspend fun getDetails(placeId: String): Place {
            placeDao.get(placeId)?.let { return it.toModel() }
            throw NoSuchElementException("Place $placeId não encontrado localmente")
        }

        override suspend fun leaderboard(limit: Int): List<Place> =
            placeDao.leaderboard(limit).map { it.toModel() }
    }

    // ---- Mapeadores Room <-> Domain ----
    private fun PlaceEntity.toModel() = Place(
        id = id,
        name = name,
        category = null,
        phone = phone,
        lat = lat, lng = lng,
        address = address,
        avgRating = avgRating,
        ratingsCount = ratingsCount
    )

    private fun Place.toEntity(now: Long) = PlaceEntity(
        id = id,
        name = name,
        phone = phone,
        lat = lat, lng = lng,
        address = address,
        avgRating = avgRating,
        ratingsCount = ratingsCount,
        lastFetchedAt = now
    )

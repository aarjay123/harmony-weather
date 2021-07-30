package hr.dtakac.prognoza.repository.place

import hr.dtakac.prognoza.DEFAULT_PLACE_ID
import hr.dtakac.prognoza.USER_AGENT
import hr.dtakac.prognoza.api.PlaceService
import hr.dtakac.prognoza.coroutines.DispatcherProvider
import hr.dtakac.prognoza.database.dao.PlaceDao
import hr.dtakac.prognoza.database.entity.Place
import hr.dtakac.prognoza.repository.preferences.PreferencesRepository
import kotlinx.coroutines.withContext

class DefaultPlaceRepository(
    private val placeDao: PlaceDao,
    private val placeService: PlaceService,
    private val dispatcherProvider: DispatcherProvider,
    private val preferencesRepository: PreferencesRepository
) : PlaceRepository {
    override suspend fun getSelectedPlace(): Place {
        val place = placeDao.get(preferencesRepository.placeId)
        return if (place == null) {
            val defaultPlace = Place(
                id = DEFAULT_PLACE_ID,
                fullName = "Osijek, Grad Osijek, Osijek-Baranja County, Croatia",
                latitude = 45.55f,
                longitude = 18.69f,
                isSaved = true
            )
            placeDao.insertOrUpdate(defaultPlace)
            defaultPlace
        } else {
            place
        }
    }

    override suspend fun getSavedPlaces(): List<Place> {
        return placeDao.getAll()
    }

    override suspend fun search(query: String): List<Place> {
        val response = placeService.search(
            userAgent = USER_AGENT,
            query = query
        )
        return withContext(dispatcherProvider.default) {
            response.map {
                Place(
                    id = it.id,
                    fullName = it.displayName,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    isSaved = false
                )
            }
        }
    }

    override suspend fun selectPlace(place: Place) {
        placeDao.insertOrUpdate(
            if (!place.isSaved) place.copy(isSaved = true) else place
        )
        preferencesRepository.placeId = place.id
    }
}
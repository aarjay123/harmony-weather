package hr.dtakac.prognoza.domain.usecase

import hr.dtakac.prognoza.domain.repository.PlaceSearcher
import hr.dtakac.prognoza.domain.repository.PlaceSearcherResult
import hr.dtakac.prognoza.entities.Place

class SearchPlaces(private val placeSearcher: PlaceSearcher) {
    suspend operator fun invoke(query: String): SearchPlacesResult {
        val result = placeSearcher.search(query)
        return if (result is PlaceSearcherResult.Success) {
            if (result.places.isEmpty()) {
                SearchPlacesResult.Empty.None
            } else {
                SearchPlacesResult.Success(result.places)
            }
        } else SearchPlacesResult.Empty.Error
    }
}

sealed interface SearchPlacesResult {
    data class Success(val places: List<Place>) : SearchPlacesResult
    sealed interface Empty : SearchPlacesResult {
        object None : Empty
        object Error : Empty
    }
}
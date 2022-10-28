package hr.dtakac.prognoza.domain.usecase

import hr.dtakac.prognoza.domain.repository.PlaceSaver
import hr.dtakac.prognoza.domain.repository.SettingsRepository
import hr.dtakac.prognoza.entities.Place

class SelectPlace(
    private val placeSaver: PlaceSaver,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(place: Place) {
        placeSaver.save(place)
        settingsRepository.setPlace(place)
    }
}
package hr.dtakac.prognoza.entities.forecast.wind

import hr.dtakac.prognoza.entities.forecast.units.Angle
import hr.dtakac.prognoza.entities.forecast.units.Speed

data class Wind(
    val speed: Speed,
    val fromDirection: Angle
)
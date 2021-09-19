package hr.dtakac.prognoza.extensions

import ca.rmen.sunrisesunset.SunriseSunset
import hr.dtakac.prognoza.dbmodel.ForecastMeta
import hr.dtakac.prognoza.dbmodel.ForecastTimeSpan
import hr.dtakac.prognoza.dbmodel.Place
import hr.dtakac.prognoza.repomodel.*
import hr.dtakac.prognoza.uimodel.MeasurementUnit
import hr.dtakac.prognoza.uimodel.RepresentativeWeatherDescription
import hr.dtakac.prognoza.uimodel.WEATHER_ICONS
import hr.dtakac.prognoza.uimodel.cell.DayUiModel
import hr.dtakac.prognoza.uimodel.cell.HourUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.util.*

fun ForecastTimeSpan.toHourUiModel(unit: MeasurementUnit) =
    HourUiModel(
        id = "$placeId-$startTime",
        temperature = instantTemperature,
        feelsLike = if (instantTemperature == null) {
            null
        } else {
            calculateFeelsLikeTemperature(
                instantTemperature,
                instantWindSpeed,
                instantRelativeHumidity
            )
        },
        precipitationAmount = precipitationAmount,
        windSpeed = instantWindSpeed,
        weatherDescription = WEATHER_ICONS[symbolCode],
        time = startTime,
        relativeHumidity = instantRelativeHumidity,
        windFromCompassDirection = instantWindFromDirection?.toCompassDirection(),
        airPressureAtSeaLevel = instantAirPressureAtSeaLevel,
        displayDataInUnit = unit
    )

suspend fun List<ForecastTimeSpan>.toDayUiModel(
    coroutineScope: CoroutineScope,
    unit: MeasurementUnit,
    place: Place
): DayUiModel {
    val weatherIconAsync = coroutineScope.async { representativeWeatherIcon(place) }
    val lowTempAsync = coroutineScope.async { lowestTemperature() }
    val highTempAsync = coroutineScope.async { highestTemperature() }
    val precipitationAsync = coroutineScope.async { totalPrecipitationAmount() }
    val hourWithMaxWindSpeedAsync = coroutineScope.async { hourWithMaxWindSpeed() }
    val maxHumidityAsync = coroutineScope.async { highestRelativeHumidity() }
    val maxPressureAsync = coroutineScope.async { highestPressure() }
    val firstHour = get(0)
    return DayUiModel(
        id = "${firstHour.placeId}-${firstHour.startTime}",
        time = firstHour.startTime,
        representativeWeatherDescription = weatherIconAsync.await(),
        lowTemperature = lowTempAsync.await(),
        highTemperature = highTempAsync.await(),
        totalPrecipitationAmount = precipitationAsync.await(),
        maxWindSpeed = hourWithMaxWindSpeedAsync.await()?.instantWindSpeed,
        windFromCompassDirection = hourWithMaxWindSpeedAsync.await()?.instantWindFromDirection?.toCompassDirection(),
        maxHumidity = maxHumidityAsync.await(),
        maxPressure = maxPressureAsync.await(),
        displayDataInUnit = unit
    )
}

fun List<ForecastTimeSpan>.toForecastResult(
    meta: ForecastMeta?,
    error: ForecastError?
): ForecastResult {
    return if (isNullOrEmpty()) {
        Empty(error)
    } else {
        val success = Success(meta, this)
        if (error == null) {
            success
        } else {
            CachedSuccess(success, error)
        }
    }
}

fun List<ForecastTimeSpan>.highestTemperature(): Double? {
    val max = maxOf { it.airTemperatureMax ?: Double.MIN_VALUE }
    return if (max == Double.MIN_VALUE) {
        null
    } else {
        max
    }
}

fun List<ForecastTimeSpan>.lowestTemperature(): Double? {
    val min = minOf { it.airTemperatureMin ?: Double.MAX_VALUE }
    return if (min == Double.MAX_VALUE) {
        null
    } else {
        min
    }
}

fun List<ForecastTimeSpan>.highestRelativeHumidity(): Double? {
    val max = maxOf { it.instantRelativeHumidity ?: Double.MIN_VALUE }
    return if (max == Double.MIN_VALUE) {
        null
    } else {
        max
    }
}

fun List<ForecastTimeSpan>.highestPressure(): Double? {
    val max = maxOf { it.instantAirPressureAtSeaLevel ?: Double.MIN_VALUE }
    return if (max == Double.MIN_VALUE) {
        null
    } else {
        max
    }
}

fun List<ForecastTimeSpan>.representativeWeatherIcon(place: Place): RepresentativeWeatherDescription? {
    val daySymbolCodes = filter {
        SunriseSunset.isDay(
            GregorianCalendar.from(it.startTime),
            place.latitude,
            place.longitude
        )
    }
        .map { it.symbolCode }
    val mostCommonSymbolCode = if (daySymbolCodes.isEmpty()) {
        map { it.symbolCode }.mostCommon()
    } else {
        daySymbolCodes.mostCommon()
    }
    val weatherIcon = WEATHER_ICONS[mostCommonSymbolCode]
    return if (weatherIcon == null) {
        null
    } else {
        RepresentativeWeatherDescription(
            weatherDescription = weatherIcon,
            isMostly = daySymbolCodes.any { it != mostCommonSymbolCode }
        )
    }
}

fun List<ForecastTimeSpan>.totalPrecipitationAmount(): Double {
    return sumOf { it.precipitationAmount?.toDouble() ?: 0.0 }.toDouble()
}

fun List<ForecastTimeSpan>.hourWithMaxWindSpeed() = maxWithOrNull { o1, o2 ->
    val difference =
        (o1.instantWindSpeed ?: Double.MIN_VALUE) - (o2.instantWindSpeed ?: Double.MIN_VALUE)
    when {
        difference < 0 -> -1
        difference > 0 -> 1
        else -> 0
    }
}
package hr.dtakac.prognoza.entities.forecast

import hr.dtakac.prognoza.entities.forecast.units.*
import hr.dtakac.prognoza.entities.forecast.wind.Wind
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class ForecastDatum(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val temperature: Temperature,
    val precipitation: Length,
    val wind: Wind,
    val airPressureAtSeaLevel: Pressure,
    val description: ForecastDescription,
    val humidity: Percentage
) {
    val feelsLike: Temperature = calculateFeelsLikeTemperature(
        airTemperature = temperature.fahrenheit,
        windSpeed = wind.speed.milesPerHour,
        relativeHumidity = humidity.percent
    )?.let { Temperature(it, TemperatureUnit.F) } ?: temperature

    /**
     * Calculates the "Feels like" temperature. Assumes [airTemperature] in degrees Fahrenheit,
     * [windSpeed] in miles per hour and [relativeHumidity] in percent.
     *
     * @return The "Feels like" temperature in degrees Fahrenheit or null if it can't be calculated with
     * the provided parameters.
     */
    fun calculateFeelsLikeTemperature(
        airTemperature: Double,
        windSpeed: Double?,
        relativeHumidity: Double?
    ): Double? {
        val feelsLikeFahrenheit = if (airTemperature in -50.0..50.0) {
            if (windSpeed == null) {
                null
            } else {
                calculateWindChill(airTemperature, windSpeed)
            }
        } else {
            if (relativeHumidity == null) {
                null
            } else {
                calculateHeatIndex(airTemperature, relativeHumidity)
            }
        }
        return feelsLikeFahrenheit
    }

    /**
     * Calculates wind chill. Assumes [airTemperature] in degrees Fahrenheit and [windSpeed] in miles per hour.
     *
     * Based on [The Wind Chill Equation](https://www.weather.gov/media/epz/wxcalc/windChill.pdf)
     *
     * @return Wind chill temperature in degrees Fahrenheit
     */
    private fun calculateWindChill(
        airTemperature: Double,
        windSpeed: Double
    ): Double {
        return 35.74 + (0.6215 * airTemperature) - (35.75 * windSpeed.pow(0.16)) + (0.4275 * airTemperature * windSpeed.pow(0.16))
    }

    /**
     * Calculates heat index. Assumes [airTemperature] in degrees Fahrenheit and [humidity] in percent.
     *
     * Based on [The Heat Index Equation](https://www.wpc.ncep.noaa.gov/html/heatindex_equation.shtml)
     *
     * @return Heat index temperature in degrees Fahrenheit
     */
    private fun calculateHeatIndex(
        airTemperature: Double,
        humidity: Double
    ): Double {
        val simpleHeatIndex = 0.5 * (airTemperature + 61 + (airTemperature - 68) * 1.2 + humidity * 0.094)
        return if (simpleHeatIndex < 80) {
            simpleHeatIndex
        } else {
            val rothfuszHeatIndex = -42.379 + 2.04901523 * airTemperature + 10.14333127 * humidity -
                    0.22475541 * airTemperature * humidity - 0.00683783 * airTemperature * airTemperature -
                    0.05481717 * humidity * humidity + 0.00122874 * airTemperature * airTemperature * humidity +
                    0.00085282 * airTemperature * humidity * humidity - 0.00000199 * airTemperature * airTemperature * humidity * humidity

            if (humidity < 13 && airTemperature in 80.0..112.0) {
                val adjustment = ((13 - humidity) / 4) * sqrt((17 - abs(airTemperature - 95)) / 17)
                rothfuszHeatIndex - adjustment
            } else if (humidity > 85 && airTemperature in 80.0..87.0) {
                val adjustment = ((humidity - 85) / 10) * ((87 - airTemperature) / 5)
                rothfuszHeatIndex + adjustment
            } else {
                rothfuszHeatIndex
            }
        }
    }
}
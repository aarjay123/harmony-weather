package hr.dtakac.prognoza.data.network.forecast

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ForecastService {
    @GET("locationforecast/2.0/compact")
    suspend fun getCompactLocationForecast(
        @Header("User-Agent") userAgent: String,
        @Header("If-Modified-Since") ifModifiedSince: String?,
        @Query("lat") latitude: String,
        @Query("lon") longitude: String
    ): Response<LocationForecastResponse>
}
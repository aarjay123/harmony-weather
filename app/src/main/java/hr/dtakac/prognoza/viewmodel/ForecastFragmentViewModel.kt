package hr.dtakac.prognoza.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hr.dtakac.prognoza.common.Event
import hr.dtakac.prognoza.extensions.hasExpired
import hr.dtakac.prognoza.extensions.toErrorResourceId
import hr.dtakac.prognoza.dbmodel.ForecastMeta
import hr.dtakac.prognoza.uimodel.forecast.EmptyForecastUiModel
import hr.dtakac.prognoza.uimodel.forecast.ForecastUiModel
import hr.dtakac.prognoza.repomodel.CachedSuccess
import hr.dtakac.prognoza.repomodel.Empty
import hr.dtakac.prognoza.repomodel.ForecastResult
import hr.dtakac.prognoza.repomodel.Success
import hr.dtakac.prognoza.repository.preferences.PreferencesRepository
import hr.dtakac.prognoza.uimodel.MeasurementUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class ForecastFragmentViewModel<T: ForecastUiModel>(
    coroutineScope: CoroutineScope?,
    protected val preferencesRepository: PreferencesRepository
): CoroutineScopeViewModel(coroutineScope) {
    private var currentMeta: ForecastMeta? = null
    private var currentUnit: MeasurementUnit? = null

    protected abstract val _forecast: MutableLiveData<T>
    val forecast: LiveData<T> get() = _forecast

    private val _emptyScreen = MutableLiveData<EmptyForecastUiModel?>()
    val emptyScreen: LiveData<EmptyForecastUiModel?> get() = _emptyScreen

    private val _cachedResultsMessage = MutableLiveData<Event<Int?>>()
    val cachedResultsMessage: LiveData<Event<Int?>> get() = _cachedResultsMessage

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun getForecast() {
        coroutineScope.launch {
            if (isReloadNeeded()) {
                _isLoading.value = true
                when (val result = getNewForecast()) {
                    is Success -> handleSuccess(result)
                    is CachedSuccess -> handleCachedSuccess(result)
                    is Empty -> handleEmpty(result)
                }
                _isLoading.value = false
            }
        }
    }

    protected abstract suspend fun getNewForecast(): ForecastResult

    protected abstract suspend fun mapToForecastUiModel(success: Success, unit: MeasurementUnit): T

    private suspend fun handleSuccess(success: Success) {
        val selectedUnit = preferencesRepository.getSelectedUnit()
        _forecast.value = mapToForecastUiModel(success, selectedUnit)
        currentMeta = success.meta
        currentUnit = selectedUnit
        _emptyScreen.value = null
    }

    private fun handleEmpty(empty: Empty) {
        _emptyScreen.value = EmptyForecastUiModel(empty.reason?.toErrorResourceId())
    }

    private suspend fun handleCachedSuccess(cachedResult: CachedSuccess) {
        handleSuccess(cachedResult.success)
        _cachedResultsMessage.value = Event(cachedResult.reason?.toErrorResourceId())
    }

    private suspend fun isReloadNeeded(): Boolean {
        return currentMeta?.hasExpired() != false ||
                currentMeta?.placeId != preferencesRepository.getSelectedPlaceId() ||
                currentUnit != preferencesRepository.getSelectedUnit() ||
                _forecast.value == null
    }
}
package hr.dtakac.prognoza

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import hr.dtakac.prognoza.di.work.WorkerFactory
import hr.dtakac.prognoza.ui.WidgetRefresher
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PrognozaApplication : Application() {
    @Inject
    lateinit var widgetRefresher: WidgetRefresher

    @Inject
    lateinit var workerFactory: WorkerFactory

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(
            this,
            Configuration.Builder().setWorkerFactory(workerFactory).build()
        )
        if (BuildConfig.DEBUG) {
            widgetRefresher.refresh()
            Timber.plant(Timber.DebugTree())
        }
    }
}
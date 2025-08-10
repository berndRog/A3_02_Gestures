package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.di.appModules
import de.rogallab.mobile.domain.utilities.logInfo
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {

   override fun onCreate() {
      super.onCreate()

      // Initialize Koin dependency injection
      logInfo(TAG, "onCreate(): startKoin{...}")
      startKoin {
         androidLogger(Level.DEBUG)
         // Reference to Android context
         androidContext(androidContext = this@MainApplication)
         // Load modules
         modules(appModules)
      }
   }

   companion object {
      const val IS_INFO = true
      const val IS_DEBUG = true
      const val IS_VERBOSE = true
      private const val TAG = "<-MainApplication"
   }
}
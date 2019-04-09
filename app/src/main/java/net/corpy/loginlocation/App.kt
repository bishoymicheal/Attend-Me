package net.corpy.loginlocation

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.multidex.MultiDexApplication
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.model.Employee


class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        instance = this

//        Utils.setAppLanguage(this, Utils.getCurrentLanguage(this)) {}

    }


    override fun attachBaseContext(base: Context) {
        localeManager = LocaleManager(base)
        super.attachBaseContext(localeManager?.setLocale(base))
        Log.d(TAG, "attachBaseContext")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager?.setLocale(this)
        Log.d(TAG, "onConfigurationChanged: " + newConfig.locale.language)
    }

    companion object {
        fun getInstance(): App {
            if (instance == null)
                instance = App()
            return instance!!
        }

        @JvmStatic
        var localeManager: LocaleManager? = null

        @JvmStatic
        fun localManager(context: Context): LocaleManager {
            if (localeManager == null)
                localeManager = LocaleManager(context)
            return localeManager!!
        }

        val downloadList = ArrayList<Long>()

        private var instance: App? = null
        var currentEmployee: Employee? = null
    }

}
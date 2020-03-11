package es.leocaudete.mistickets.preferences

import android.app.Application

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class SharedApp: Application() {
    companion object{
        lateinit var preferences: Preferences
    }

    override fun onCreate() {
        super.onCreate()
        preferences =
            Preferences(applicationContext)
    }
}
package es.leocaudete.mistickets

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class Preferences(context:Context) {

    val PREFS_NAME = "es.leocaudete.mistickets"
    val LOGIN_PERISTENCE = "keyString"

    val prefs: SharedPreferences =context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

    var login: Boolean
        get()= prefs.getBoolean(LOGIN_PERISTENCE, false)
        set(value) = prefs.edit().putBoolean(LOGIN_PERISTENCE, value).apply()




}
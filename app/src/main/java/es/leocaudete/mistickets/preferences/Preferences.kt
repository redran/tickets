package es.leocaudete.mistickets.preferences

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class Preferences(context:Context) {

    /**
     * Almacena true o false si se quiere guardar o no la sesion
     */
    val PREFS_NAME = "es.leocaudete.mistickets"
    private val LOGIN_PERISTENCE = "login"
    private val LOCAL_DB = "bd"
    private val ID_USU = "id"

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

    /**
     * Almacena true o false si se quiere recordar el usuario o no
     */
    var login: Boolean
        get()= prefs.getBoolean(LOGIN_PERISTENCE, false)
        set(value) = prefs.edit().putBoolean(LOGIN_PERISTENCE, value).apply()

    /**
     * Almacena true o false si se quiere trabajar en local o en la nube
     */
    var bdtype:Boolean
        get()= prefs.getBoolean(LOCAL_DB, false)
        set(value) = prefs.edit().putBoolean(LOCAL_DB, value).apply()

    /**
     * Almacena el id de usuario para ir consultando entre activities
     */
    var usuario_logueado:String
        get()= prefs.getString(ID_USU, "")
        set(value) = prefs.edit().putString(ID_USU, value).apply()
}
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
    private val DIETA="dieta"
    private val OLD_VERSION_APP="oldversionapp"
    private val OLD_VERSION_FIREBASE="oldversionfirebase"
    private val MODO_OPERACION="modooperacion"
    private val AVISO_UNICO_CADUCADOS="avisounico"


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

    /**
     * Almacena true o false si se ha seleccionado el modo dietas o Tickets
     */
    var dieta: Boolean
        get()= prefs.getBoolean(DIETA, false)
        set(value) = prefs.edit().putBoolean(DIETA, value).apply()

    /**
     * Almacena la version actual de nuestro schema de Firebase
     */
    var oldversionapp:String
        get()= prefs.getString(OLD_VERSION_APP, "1.0")
        set(value) = prefs.edit().putString(OLD_VERSION_APP, value).apply()

    /**
     * Almacena la version actual de nuestro schema de Firebase
     */
    var oldversionfirebase:String
        get()= prefs.getString(OLD_VERSION_FIREBASE, "1.0")
        set(value) = prefs.edit().putString(OLD_VERSION_FIREBASE, value).apply()
    /**
     * Define el modo de operación: Lectura o Lectura y Escritura
     */
    var modooperacion:Int
        get()= prefs.getInt(MODO_OPERACION, 1)
        set(value) = prefs.edit().putInt(MODO_OPERACION, value).apply()
    /**
     * Compruebas que se avise solo una vez al entrar y no cada vez que se lance el Main
     */
    var avisounico:Int
        get()= prefs.getInt(AVISO_UNICO_CADUCADOS, 0)
        set(value) = prefs.edit().putInt(AVISO_UNICO_CADUCADOS, value).apply()

}
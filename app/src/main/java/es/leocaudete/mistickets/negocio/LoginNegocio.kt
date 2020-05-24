package es.leocaudete.mistickets.negocio

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import es.leocaudete.mistickets.dao.ApiRest
import es.leocaudete.mistickets.dao.FirestoreDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.presentacion.MainActivity
import es.leocaudete.mistickets.utilidades.ShowMessages

/**
 * @author Leonardo Caudete Palau 2º DAM Semi
 * Esta clase es la que se encarga de intermediar entre la presentacion y los datos
 * Recibe los datos de la capa de datos, los procesa y los devuelve a la capa de presentación.
 * De esta manera si cambiamos de base de datos, no tenemos que tocar ni esta capa ni la de presentación
 */
class LoginNegocio(context: Context, versionApp: String) {

    private var dbFirebase = FirestoreDB(context)
    private var usuarioNegocio=UsuarioNegocio(context)
    private var apirRest=ApiRest(context)


    private lateinit var storageDir: String
    private var context = context
    private val NEW_VERSION_APP = versionApp
    var gestorMensajes = ShowMessages()


    /**
     * Lógica de negocio para el acceso a datos de Cloud
     */
    fun loginOnLine(user: String, password: String, principal:ConstraintLayout,carga:ConstraintLayout) {
        // La logica de negocio la va a ejecutar la capa de datos porque lo hace todo en un thread
        dbFirebase.logeaUsuario(user, password,principal,carga)
    }

    /**
     * Lógica de negocio para el acceso a datos local.
     * Verifica si el usuario existe,
     * si es asi verifica que su contraseña sea la enviada,
     * si es asi graba en la propiedad el valor del id de usuario
     */
    fun loginOffLine(user: String, password: String,principal:ConstraintLayout,carga:ConstraintLayout) {
        carga.visibility=View.VISIBLE
        principal.visibility=View.GONE

        if (usuarioNegocio.buscaUsuario(user)) {
            if (usuarioNegocio.validaPassword(user, password)) {
                SharedApp.preferences.usuario_logueado = usuarioNegocio.buscaIdUsuario(user)
                storageDir =
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + SharedApp.preferences.usuario_logueado
                SharedApp.preferences.oldversionapp = NEW_VERSION_APP
                SharedApp.preferences.modooperacion = 0
                SharedApp.preferences.avisounico = 0
                context.startActivity(Intent(context, MainActivity::class.java))
            } else {
                carga.visibility=View.GONE
                principal.visibility=View.VISIBLE
                gestorMensajes.showAlertOneButton("ERROR", "La contraseña no es correcta", context)
            }
        } else {
            carga.visibility=View.GONE
            principal.visibility=View.VISIBLE
            gestorMensajes.showAlertOneButton("ERROR", "El usuario no es correcto", context)
            SharedApp.preferences.login = false
        }
    }
    /**
     * Devuelve el id de usuario
     */
    fun getIdUsuarioFB():String{
        return dbFirebase.getIdUsuario()
    }

    fun logOutFb(){
        dbFirebase.logOut()
    }


}
package es.leocaudete.mistickets.presentacion

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.negocio.LoginNegocio
import es.leocaudete.mistickets.negocio.TicketsNegocio
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import es.leocaudete.mistickets.utilidades.Utilidades
import kotlinx.android.synthetic.main.login_activity.*
import kotlinx.android.synthetic.main.login_activity.ed_password


/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class Login : AppCompatActivity() {

    lateinit var loginNegocio:LoginNegocio
    var ticketsNegocio=TicketsNegocio(this)
    val utils = Utilidades()

    // Usamos esta constante para controlar actualizaciones
    var NEW_VERSION_APP: String = ""
    var old_version_firebase="" // Si lo instalo nuevo en preferences el valor por defecto sera x si ya lo tenia sera y
    private lateinit var auth: FirebaseAuth


    private val TAG = "DocSnippets"
    var tickets = mutableListOf<Ticket>() // va a lamacenar todos los tickets
    var gestorMensajes = ShowMessages()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        comprobarVersion()
        // Creamos nuestra instancia de la capa de negocio
        loginNegocio=LoginNegocio(this,NEW_VERSION_APP)

        old_version_firebase=SharedApp.preferences.oldversionfirebase
        // Instanciamos la clase que crea la base de datos y tiene nuestro CRUD )

        /*  SharedApp.preferences.usuario_logueado = ""
          SharedApp.preferences.bdtype = swbd.isChecked*/

        val recordar = SharedApp.preferences.login
        val dbcloud = SharedApp.preferences.bdtype
        val userLog = SharedApp.preferences.usuario_logueado

        /**
         * Descomentar esta linea para volver a una version anterior y que se vuelva a ejecutar
         * el update de firebase. No olvidarse de poner el número de versión anterior al que queremos
         * que haga el update.
         */
        //SharedApp.preferences.fbschema=2
        if (recordar) {

            // Si hemos recordado usuario, entonces tenemos que comprobar si es online u offline
            if (dbcloud) {
                // Si la version que ejecutamos es mayor que la que tenemos instalada
                // obligamos a hacer el login para hacer los update necesario
                if (NEW_VERSION_APP.toDouble() > SharedApp.preferences.oldversionapp.toDouble()) {
                    SharedApp.preferences.login = false
                    SharedApp.preferences.usuario_logueado = ""
                } else {
                    if (ticketsNegocio.getIdUsuarioFB() != null) {
                        SharedApp.preferences.avisounico=0
                        utils.delRecFileAndDir(ticketsNegocio.rutaLocalFb())
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }

            } else {
                if (!TextUtils.isEmpty(userLog)) {
                    SharedApp.preferences.avisounico=0
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }

        } else {
            if (dbcloud) {
                loginNegocio.logOutFb()
            } else {
                SharedApp.preferences.usuario_logueado = ""

            }
            // Lo ponemos solo aqui porque si recuerda el usuario nunca va a mostrar la activity
            cb_recordar.isChecked = false
            swbd.isChecked = true
            cambiaEstado()
            pricipal.visibility=View.VISIBLE
            carga.visibility=View.GONE
        }

    }

    /**
     * Comprueba la version de la app.
     * Cuando se ejecuta la app guarda la versión actual en una preferencia.
     * Cuando se ejecuta una actualización, compara la versión de la preferencia con la verión que se acaba de instalar,
     * si son distintas vacia las preferencias de recordar usuario para forzar un llamado a login
     */
    fun comprobarVersion() {
        try {
            var pInfo = applicationContext.packageManager.getPackageInfo(packageName, 0)
            NEW_VERSION_APP = pInfo.versionName

            // Si estamos ejecutando una version nueva
            if (NEW_VERSION_APP.toDouble() > SharedApp.preferences.oldversionapp.toDouble()) {

                // Lo primero es eliminar los valores de preferencias como recordar usuario
                SharedApp.preferences.usuario_logueado = "" // olvidamos usuario SQLite
                SharedApp.preferences.login = false // Que no recuerde usuario logeado

                // Despues de esto continuara con el Login e instanciara SQLiteDB la cual hará el update
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Evento del switch que selecciona On-line y Off-line
     */
    fun clicksw(view: View) {
        cambiaEstado()
    }

    /**
     * Cambia el valor de la preferencia para que se recuerde el tipo de base de datos a la hora de recordar el usuario logeado.
     * También cambia el color del texto para saber que está marcado
     */
    fun cambiaEstado() {
        SharedApp.preferences.bdtype = swbd.isChecked
        if (swbd.isChecked) {
            ed_user.hint = getString(R.string.ed_email)
            online.setTextColor(getColor(R.color.colorPrimary))
            offline.setTextColor(getColor(R.color.colorAccent))
        } else {
            ed_user.hint = getString(R.string.ed_user)
            offline.setTextColor(getColor(R.color.colorPrimary))
            online.setTextColor(getColor(R.color.colorAccent))
        }
    }

    /**
     * Llama a la activity que nos permite recuperar la contraseña
     */
    fun forgotPassword(view: View) {
        startActivity(Intent(this, ForgotPass::class.java))
    }

    /**
     * Llama a la activity que nos permite registrar un usuario
     */
    fun registro(view: View) {

        startActivity(Intent(this, Registro::class.java))
    }

    /**
     * Dependiendo del tipo de conexión marcada llama al metodo login correspondiente.
     * También rellena la preferencia que indica si se desea recordar el usuario
     */
    fun login(view: View) {

        SharedApp.preferences.login = cb_recordar.isChecked

        val user: String = ed_user.text.toString()
        val password: String = ed_password.text.toString()

        if (swbd.isChecked) {
            loginNegocio.loginOnLine(user, password, pricipal,carga)
        } else {
            loginNegocio.loginOffLine(user, password,pricipal,carga)
        }
    }





}

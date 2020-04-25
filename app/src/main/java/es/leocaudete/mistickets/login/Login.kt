package es.leocaudete.mistickets.login

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
import es.leocaudete.mistickets.MainActivity
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.login_activity.*
import kotlinx.android.synthetic.main.login_activity.ed_password
import kotlinx.android.synthetic.main.login_activity.progressBar


/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class Login : AppCompatActivity() {

    // Usamos esta constante para controlar actualizaciones
    var NEW_VERSION_APP: String = ""
    var old_version_firebase="" // Si lo instalo nuevo en preferences el valor por defecto sera x si ya lo tenia sera y
    private lateinit var auth: FirebaseAuth
    lateinit var storageDir: String
    lateinit var dbSQL: SQLiteDB

    private val TAG = "DocSnippets"
    var tickets = mutableListOf<Ticket>() // va a lamacenar todos los tickets
    var gestorMensajes = ShowMessages()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.login_activity)
        old_version_firebase=SharedApp.preferences.oldversionfirebase
        comprobarVersion()
        // Instanciamos la clase que crea la base de datos y tiene nuestro CRUD
        dbSQL = SQLiteDB(this, null)

        auth = FirebaseAuth.getInstance()
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
                    if (auth.currentUser != null) {
                        SharedApp.preferences.avisounico=0
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
                auth.signOut()
            } else {
                SharedApp.preferences.usuario_logueado = ""

            }
            // Lo ponemos solo aqui porque si recuerda el usuario nunca va a mostrar la activity
            cb_recordar.isChecked = false
            swbd.isChecked = true
            cambiaEstado()
        }

    }

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

    override fun onStart() {
        super.onStart()
        // Comprobamos en la preferencias si tenemos recordar a 0 entonces hacemos un singOut


    }

    fun clicksw(view: View) {
        cambiaEstado()
    }

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

    fun forgotPassword(view: View) {
        startActivity(Intent(this, ForgotPass::class.java))
    }

    fun registro(view: View) {

        startActivity(Intent(this, Registro::class.java))
    }

    fun login(view: View) {
        // si el chekbox esta no esta marcado pornemos el valor en prefrencias de recordar a 0
        SharedApp.preferences.login = cb_recordar.isChecked


        val user: String = ed_user.text.toString()
        val password: String = ed_password.text.toString()

        if (swbd.isChecked) {
            loginOnLine(user, password)
        } else {
            loginOffLine(user, password)
        }
    }

    private fun loginOnLine(user: String, password: String) {
        if (!TextUtils.isEmpty(user) && !TextUtils.isEmpty(password)) {
            progressBar.visibility = View.VISIBLE

            auth.signInWithEmailAndPassword(user, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {
                        // upgrafeFirebase()
                        action()
                    } else {
                        Toast.makeText(
                            this,
                            "Error: ususario y/o contraseña incorrectos",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } else {
            Toast.makeText(this, "Rellene todos los campos", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Comprueba que el email de verificación se ha aceptado
     * Si es correcto se actualiza la base de datos en caso de haber cambios en el schema
     * y carga el main
     */
    private fun action() {

        // Solo si ha aceptado el email de verificacion
        if (auth.currentUser!!.isEmailVerified) {
            storageDir =
                getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + auth.currentUser?.uid.toString()
            upgrafeFirebase()
        } else {
            // En este punto tendríamos que verificar si la fecha de ingreso y la actual tienen 1 mes de diferencia y si la tienen eliminar la cuenta de firebase
            gestorMensajes.showAlertOneButton(
                "Alerta",
                "Tienes que aceptar el email de verifiación que se envió a tu correo electrónico",
                this
            )
        }


    }

    /**
     * Verifica si el usuario existe,
     * si es asi verifica que su contraseña sea la enviada,
     * si es asi graba en la propiedad el valor del id de usuario
     */
    private fun loginOffLine(user: String, password: String) {

        val email = ed_user.text.toString()
        if (dbSQL.buscaUsuario(email)) {
            if (dbSQL.validaPassword(email, password)) {
                SharedApp.preferences.usuario_logueado = dbSQL.buscaIdUsuario(email)
                storageDir =
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + SharedApp.preferences.usuario_logueado
                SharedApp.preferences.oldversionapp=NEW_VERSION_APP
                SharedApp.preferences.modooperacion=0
                SharedApp.preferences.avisounico=0
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                gestorMensajes.showAlertOneButton("ERROR", "La contraseña no es correcta", this)
            }
        } else {
            gestorMensajes.showAlertOneButton("ERROR", "El usuario no es correcto", this)
        }
    }


    /**
     * Realiza un upgrade en Firebase para insertar los nuevos
     * campos, dependiendo de la versión
     * Si la version del schema de Firebase es distinto al nuestro, entonces no podemos iniciar sesion
     * y tendremos que actualizar la APP
     *
     */
    fun upgrafeFirebase() {
        val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado
        val rutaTickets = "User/" + userID + "/Tickets"
        var versionFirabaseCloud: String

        // Primero comprobamos que la version de la base de datos coincida con la version de la APP
        //  que estamos usando o nos fallara el mapeo de datos a objetos
        var fbversion = dbRef.collection("Info")
            .document("UUPgCzmmLAk5KJgXB06k")
            .get()
            .addOnSuccessListener { document ->
                if(document==null){
                    Toast.makeText(this,"Error al acceder a la base de datos", Toast.LENGTH_LONG).show()
                }else{
                    versionFirabaseCloud=document["version"].toString()
                    if(old_version_firebase.toDouble()<versionFirabaseCloud.toDouble()){
                        when (versionFirabaseCloud.toDouble()) {
                            2.0 -> {
                                val data = hashMapOf("categoria" to 1, "precio" to 0.00)
                                val ticketsRef = dbRef.collection(rutaTickets)
                                    .get()
                                    .addOnSuccessListener { result ->
                                        for (document in result) {

                                            var ticket=document["idTicket"].toString()
                                            dbRef.collection(rutaTickets).document(ticket).set(data,
                                                SetOptions.merge())
                                        }
                                        // Una vez actualizada, actualizamos la version de la preferencia
                                        // para que no vuelva a hacerla
                                        SharedApp.preferences.oldversionapp=NEW_VERSION_APP
                                        SharedApp.preferences.oldversionfirebase=versionFirabaseCloud
                                        SharedApp.preferences.modooperacion=0
                                        SharedApp.preferences.avisounico=0
                                        startActivity(Intent(this, MainActivity::class.java))
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(
                                            this,
                                            "Se ha producido un error al actualizar la base de datos",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Log.d(TAG, "Error getting documents:", exception)
                                    }
                            }
                            3.0 -> {
                                val data = HashMap<String,Any?>()
                                // Si partimos de dos versiones anteriores entonces tendremos que agregar
                                // todos los campos que se han agregado hasta ahora
                                if(SharedApp.preferences.oldversionfirebase== "1.0"){
                                    data.put("categoria",1)
                                    data.put("precio",0.00)
                                }
                                data.put("isdieta",0)
                                data.put("fecha_envio",null)
                                data.put("metodo_envio",0)
                                data.put("enviado_a",null)
                                data.put("fecha_cobro",null)
                                data.put("metodo_cobro",null)


                                val ticketsRef = dbRef.collection(rutaTickets)
                                    .get()
                                    .addOnSuccessListener { result ->
                                        for (document in result) {

                                            var ticket=document["idTicket"].toString()
                                            dbRef.collection(rutaTickets).document(ticket).set(data,
                                                SetOptions.merge())
                                        }
                                        // Una vez actualizada, actualizamos la version de la preferencia
                                        // para que no vuelva a hacerla
                                        SharedApp.preferences.oldversionapp=NEW_VERSION_APP
                                        SharedApp.preferences.oldversionfirebase=versionFirabaseCloud
                                        SharedApp.preferences.modooperacion=0
                                        SharedApp.preferences.avisounico=0
                                        startActivity(Intent(this, MainActivity::class.java))
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(
                                            this,
                                            "Se ha producido un error al actualizar la base de datos",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Log.d(TAG, "Error getting documents:", exception)
                                    }
                            }
                            else -> {
                                // Tenemos que prohibir que pueda escribir porque si esta ejecutando una version anterior
                                // significa que su modelo es antiguo. Puede leer y mapeará los campos que coincidan
                                // pero si intenta escribir subira el modelo viejo y eso implica que eliminará todos
                                // los campos nuevos y si usa esa cuenta con una app con la version nueva, abra perdido
                                // todos los datos añadidos en versiones posteriores a esta
                                SharedApp.preferences.modooperacion=1 // Se activa el modo lectura
                                startActivity(Intent(this, MainActivity::class.java))
                            }
                        }

                    }else{
                        // Si las versiones son iguales entonces o y ase ha actualizado o la actualización es solo de código y no de BD
                        SharedApp.preferences.modooperacion=0
                        SharedApp.preferences.avisounico=0
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }
            }


    }




}

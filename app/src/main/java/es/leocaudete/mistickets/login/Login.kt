package es.leocaudete.mistickets.login

import android.content.Intent
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
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

    // Usamos esta constante para actualizar el schema de Firebase
    val VERSION_FIREBASE = 3
    private lateinit var auth: FirebaseAuth
    lateinit var storageDir: String
    lateinit var dbSQL: SQLiteDB

    private val TAG = "DocSnippets"
    var tickets = mutableListOf<Ticket>() // va a lamacenar todos los tickets
    var gestorMensajes = ShowMessages()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.login_activity)

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
                //Primero tenemos que ver que no exista una actualizacion en el schema de la base
                // de datos de Firebase (SQLite se encarga el en su DAO).
                // Entonces tendremos que cerrar la sesion y que se loge para actualziar
                if(VERSION_FIREBASE>SharedApp.preferences.fbschema){
                    SharedApp.preferences.login=false
                    SharedApp.preferences.usuario_logueado=""
                }else
                {
                    if (auth.currentUser != null) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }

            } else {
                if (!TextUtils.isEmpty(userLog)) {
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
     *
     */
    fun upgrafeFirebase() {
        val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado
        val rutaTickets = "User/" + userID + "/Tickets"

        if(VERSION_FIREBASE>SharedApp.preferences.fbschema){
            when (VERSION_FIREBASE) {
                2 -> {
                    val data = hashMapOf("categoria" to 0, "precio" to 0.00)
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
                            SharedApp.preferences.fbschema=VERSION_FIREBASE
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
                3 -> {
                    val data = hashMapOf(
                        "isdieta" to 0,
                        "fecha_envio" to "",
                        "metodo_envio" to 0,
                        "enviado_a" to "",
                        "fecha_cobro" to "",
                        "metodo_cobro" to ""
                    )
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
                            SharedApp.preferences.fbschema=VERSION_FIREBASE
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

            }

        }else{
            startActivity(Intent(this, MainActivity::class.java))
        }

    }


}

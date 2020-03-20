package es.leocaudete.mistickets.login

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import es.leocaudete.mistickets.MainActivity
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_registro.*
import kotlinx.android.synthetic.main.login_activity.*
import kotlinx.android.synthetic.main.login_activity.ed_password
import kotlinx.android.synthetic.main.login_activity.progressBar


/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    lateinit var storageDir: String
    lateinit var dbSQL: SQLiteDB

    private val TAG = "DocSnippets"
    var tickets = mutableListOf<Ticket>() // va a lamacenar todos los tickets
    var gestorMensages = ShowMessages()

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

        if (recordar) {
            // Si hemos recordado usuario, entonces tenemos que comprobar si es online u offline
            if (dbcloud) {
                if (auth.currentUser != null) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
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

    // Comprueba si tiene datos y si quiere borrarlos y carga el MainAativity
    private fun action() {

        // Solo si ha aceptado el email de verificacion
        if (auth.currentUser!!.isEmailVerified) {
            storageDir =
                getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + auth.currentUser?.uid.toString()
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // En este punto tendríamos que verificar si la fecha de ingreso y la actual tienen 1 mes de diferencia y si la tienen eliminar la cuenta de firebase
            gestorMensages.showAlertOneButton(
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
                gestorMensages.showAlertOneButton("ERROR", "La contraseña no es correcta", this)
            }
        } else {
            gestorMensages.showAlertOneButton("ERROR", "El usuario no es correcto", this)
        }
    }

    // Este metodo comprueba si hay datos en SQLite
    private fun compruebaDatosOffline() {

        val email = ed_user.text.toString()
        if (dbSQL.buscaUsuario(email)) {
            syncronizaOnLineDeOffLine(email)
        }
    }


    // Se ha encrontrado el usuario en Local y vamos a subir los cambios (si los hubiere) a Firebase
    private fun syncronizaOnLineDeOffLine(email: String) {

        val id_usuario = dbSQL.buscaIdUsuario(email)
        if (!TextUtils.isEmpty(id_usuario)) {
            // Busca todos los tickets de ese usuario
            val args = arrayOf(id_usuario)
            val db: SQLiteDatabase = dbSQL.readableDatabase


            val cursor = db.rawQuery(
                " SELECT * FROM ${SQLiteDB.TABLA_TICKETS} WHERE ${SQLiteDB.USUARIO_TICKET}=?",
                args
            )
        }


    }


}

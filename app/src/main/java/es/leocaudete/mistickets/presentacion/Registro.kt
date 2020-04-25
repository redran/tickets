package es.leocaudete.mistickets.presentacion

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.security.keystore.UserPresenceUnavailableException
import android.text.InputType
import android.text.TextUtils
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Usuario
import es.leocaudete.mistickets.negocio.TicketsNegocio
import es.leocaudete.mistickets.negocio.UsuarioNegocio
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_registro.*

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */

class Registro : AppCompatActivity() {

    var usuarioNegocio = UsuarioNegocio(this)
    var context = this



    private var gestorMensajes = ShowMessages()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Boton atras
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Si usamos cloud
        if (SharedApp.preferences.bdtype) {

            ed_email.hint = getString(R.string.ed_email)
            ed_email.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

            ed_pin.visibility = View.GONE
        } else {
            ed_email.hint = getString(R.string.ed_user)
            ed_email.inputType = InputType.TYPE_CLASS_TEXT
            // Instanciamos la clase que crea la base de datos y tiene nuestro CRUD SQLite
            ed_pin.visibility = View.VISIBLE
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }

    fun register(view: View) {
        val password = ed_password.text.toString()
        val password2 = ed_repeatpassword.text.toString()
        if (TextUtils.equals(password, password2)) {
            creaNuevoUsuario()
        } else {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
        }

    }


    private fun creaNuevoUsuario() {
        val nombre = ed_nombre.text.toString()
        val apellidos = ed_apellidos.text.toString()
        val email = ed_email.text.toString()
        val password = ed_password.text.toString()


        val usuario = Usuario()
        usuario.email = email
        usuario.password = password
        usuario.nombre = nombre
        usuario.apellidos = apellidos


        if (!TextUtils.isEmpty(nombre) && !TextUtils.isEmpty(apellidos) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(
                password
            )
        ) {
            // Camino A Registro de Usuario Firebase
            if (SharedApp.preferences.bdtype) {
                addUserByEmailInFirebase()
            } else { // Camino B Registro de Usuraio Firebase
                val pin = Integer.parseInt(ed_pin.text.toString())
                usuario.pin_de_seguridad = pin
                // Aqui comprobaria y mostraria el campo PIN
                if (pin > 0) {
                    if (usuarioNegocio.buscaUsuario(email)) {
                        gestorMensajes.showAlert(
                            "Información",
                            "Usuario ya registrado. Elija otro usuario",
                            this,
                            { ed_email.text.clear() })
                    } else {
                        val insertados = usuarioNegocio.addUser(usuario)
                        if (insertados < 0) {
                            gestorMensajes.showAlertOneButton(
                                "ERROR",
                                "Error al insertar el nuevo usuario.",
                                this
                            )
                        } else {
                            gestorMensajes.showActionOneButton(
                                "INFO",
                                "Usuario insertado correctamente.",
                                this,
                                {
                                    startActivity(
                                        Intent(
                                            this,
                                            Login::class.java
                                        )
                                    )
                                })
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Debes introducir el PIN para poder recuperar tu password en caso de pérdida",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(this, "Los campos no pueden estar vacios", Toast.LENGTH_LONG).show()
        }
    }

    // Crea un nuevo usuario en Firebase a partir del email y un password
    private fun addUserByEmailInFirebase() {

        val email = ed_email.text.toString()
        val password = ed_password.text.toString()
        val nombre=ed_nombre.text.toString()
        val apellido=ed_apellidos.text.toString()

        if (Patterns.EMAIL_ADDRESS.matcher(ed_email.text).matches()) {
            // Verificacion particular de la longitud del password que Firebase obliga a que no sea menor que 6
            if (password.length >= 6) {
                progressBar.visibility = View.VISIBLE
               var user=Usuario()
                user.email=email
                user.password=password
                user.nombre=nombre
                user.apellidos=apellido

                usuarioNegocio.addUserFb(user)

            } else {
                Toast.makeText(
                    this,
                    "El campo password no puede tener menos de 6 caracteres",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(this, "La dirección de email no parece válida", Toast.LENGTH_LONG).show()
        }

    }

}

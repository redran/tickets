package es.leocaudete.mistickets.presentacion

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.negocio.UsuarioNegocio
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_forgot_pass.*

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class ForgotPass : AppCompatActivity() {
    var usuarioNegocio = UsuarioNegocio(this)

    private var gestoMensajes = ShowMessages()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pass)



        // Boton atras
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if (SharedApp.preferences.bdtype) {
            ed_email.hint = getString(R.string.ed_email)
            ed_pin.visibility = View.GONE
        } else {
            ed_email.hint = getString(R.string.ed_user)
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

    /**
     * Resetea o muestra el password
     */
    fun send(view: View) {

        val email = ed_email.text.toString()
        if (SharedApp.preferences.bdtype) {
            if (!TextUtils.isEmpty(email)) {
                progressBar.visibility = View.VISIBLE
                usuarioNegocio.recuperaPasswordFb(email)
            }
        } else {
            val pin = ed_pin.text.toString()
            // Le pedimos el PIN de seguridad y el email y si lo acierta le mostramos el password
            if (usuarioNegocio.buscaUsuario(email)) {
                var pass = usuarioNegocio.validaPin(Integer.parseInt(pin))
                if (!TextUtils.isEmpty(pass)) {
                    gestoMensajes.showAlertOneButton("PASSWORD", "Tu contraseña es : $pass", this)
                } else {
                    gestoMensajes.showAlertOneButton(
                        "ERROR",
                        "El pin introducido no es valido",
                        this
                    )
                }
            } else {
                gestoMensajes.showAlertOneButton("ERROR", "El usuario especificado no existe", this)
            }
        }
    }

}

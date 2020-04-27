package es.leocaudete.mistickets.presentacion

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.FirestoreDB
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.negocio.TicketsNegocio
import es.leocaudete.mistickets.negocio.UsuarioNegocio
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_sycronizar.*
import java.io.File

class Syncronizar : AppCompatActivity() {

    var ticketsNegocio = TicketsNegocio(this)
    val gestorMensajes = ShowMessages()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sycronizar)



        if (SharedApp.preferences.bdtype) {
            ed_syncro_user.hint = getString(R.string.syncro_user)
        } else {
            ed_syncro_user.hint = getString(R.string.syncro_email)
        }

        pbCargando.visibility = View.GONE
        tv_cargando.visibility = View.GONE

        if (SharedApp.preferences.modooperacion == 1) {
            btn_aceptar.visibility = View.GONE
            gestorMensajes.showActionOneButton(
                "ALERTA",
                "La app está en modo solo lectura y no se permite la sincronización",
                this,
                { startActivity(Intent(this, MainActivity::class.java)) })
        } else {
            btn_aceptar.visibility = View.VISIBLE
        }
    }

    /**
     * Anulamos la opción de volver a tras a través del botón del móvil
     */
    override fun onBackPressed() {
        //
    }

    /**
     * Este metodo sycroniza los tickets de dos cuentas
     */
    fun sycronizar(view: View) {

        val user = ed_syncro_user.text.toString()
        val password = ed_sycnro_pass.text.toString()

        // Si es Cloud
        if (SharedApp.preferences.bdtype) {
            ticketsNegocio.sycronizaConLocal(user, password, pbCargando, tv_cargando)
            // Si es Local
        } else {
            ticketsNegocio.syncronizaConCloud(user, password, pbCargando, tv_cargando)
        }
    }


    fun cancelar(view: View) {
        startActivity(Intent(this, MainActivity::class.java))
    }

}

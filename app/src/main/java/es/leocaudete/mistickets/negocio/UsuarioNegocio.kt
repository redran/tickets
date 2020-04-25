package es.leocaudete.mistickets.negocio

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import es.leocaudete.mistickets.dao.FirestoreDB
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Usuario
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.presentacion.Login
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_forgot_pass.*

/**
 * Logica de negocio que se encarga del  Usuario
 */
class UsuarioNegocio(context: Context) {
    private var dbSQL = SQLiteDB(context, null)
    private var dbFirebase = FirestoreDB(context)
    var context = context
    var auth = FirebaseAuth.getInstance()
    private var gestorMensajes = ShowMessages()

    // OnLine Firebase

    /**
     * Añadir usuario Firebase
     */
    fun addUserFb(usuario: Usuario) {
        dbFirebase.addUserFb(usuario)
    }

    /**
     * Elimina el usuario actual de Firebase, con todos sus datos y las fotos de Firebase Storage
     */
    fun borraUsuarioFb() {
        val auth = FirebaseAuth.getInstance() // Usuario autentificado
        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado
        dbFirebase.borradoCompletoUsuario(userID)
    }

    /**
     * Envia un email para recuperar el password
     */
    fun recuperaPasswordFb(email:String){
        dbFirebase.recuperaPasswordFb(email)
    }

    // OffLine SQLite
    /**
     * Añade un usuario a SQlite
     */
    fun addUser(usuario: Usuario): Long {
        return dbSQL.addUser(usuario)
    }

    /**
     * Borra el usuario logeado de SQLite
     */
    fun borraUsuario() {
        // Elimina el usuario SQLite
        val usuLogin = SharedApp.preferences.usuario_logueado
        // Primero Eliminamos todos los tickets de ese usuario
        val listaTickets = dbSQL.devuelveTickets(usuLogin)
        for (i in listaTickets.indices) {
            dbSQL.deleteTicket(listaTickets[i].idTicket)
        }
        // Luego eliminamos el usuario de la BD y su carpeta local
        dbSQL.deleteUsuario(usuLogin)

        SharedApp.preferences.usuario_logueado = ""
        SharedApp.preferences.login = false
        context.startActivity(Intent(context, Login::class.java))

    }

    /**
     * Busca usuario en SQLite
     */
    fun buscaUsuario(usuario: String): Boolean {
        return dbSQL.buscaUsuario(usuario)
    }

    /**
     * Verifica que el usuario y password son correctos
     */
    fun validaPassword(email: String, password: String): Boolean {
        return dbSQL.validaPassword(email, password)
    }

    /**
     * Busca el id a partir de usuario
     */
    fun buscaIdUsuario(email: String): String {
        return dbSQL.buscaIdUsuario(email)
    }

    /**
     * Verifica que el pin introducido es correcto
     */
    fun validaPin(pin: Int): String {
        return dbSQL.validaPin(pin)
    }
}
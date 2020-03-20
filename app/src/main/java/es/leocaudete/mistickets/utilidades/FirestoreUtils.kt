package es.leocaudete.mistickets.utilidades

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import es.leocaudete.mistickets.login.Login
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.preferences.SharedApp

class FirestoreUtils(context: Context) {

    // Le pasamos el contexto de quien instancia esta clase para poder mostrar los mensajes
    val context = context

    val gestorMensajes = ShowMessages()


    // Borra una foto de usuario y de un ticket pasado por parametro
    fun borraFotoUsuario(foto: String, user: String, ticket: String) {
        var storageRef = FirebaseStorage.getInstance().reference
        var riverRef = storageRef.child("$user/$foto")
        riverRef.delete()
    }

    // Borra todas las fotos de un Ticket
    fun borraFotosTicket(ticket: Ticket) {

        val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
        val auth = FirebaseAuth.getInstance() // Usuario autentificado

        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado

        if (ticket.foto1 != null) {
            borraFotoUsuario(ticket.foto1!!, userID, ticket.idTicket)
        }
        if (ticket.foto2 != null) {
            borraFotoUsuario(ticket.foto2!!, userID, ticket.idTicket)
        }
        if (ticket.foto3 != null) {
            borraFotoUsuario(ticket.foto3!!, userID, ticket.idTicket)
        }
        if (ticket.foto3 != null) {
            borraFotoUsuario(ticket.foto3!!, userID, ticket.idTicket)
        }
    }

    // Borra un ticket
    fun borraTicketsUsuario(ticket: Ticket) {

        val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
        val auth = FirebaseAuth.getInstance() // Usuario autentificado
        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado

        dbRef.collection("User").document(userID)
            .collection("Tickets").document(ticket.idTicket).delete()

    }

    // Elimina el usuario
    fun borraUsuarioFirestore(user: String) {
        val dbRef = FirebaseFirestore.getInstance()
        dbRef.collection("User").document(user).delete()

    }

    // Borra cuenta FireBase
    fun borraUsuarioFirebase() {
        val user = FirebaseAuth.getInstance().currentUser


        user?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    SharedApp.preferences.login=false
                    val intent=Intent(context, Login::class.java)
                    context.startActivity(intent)
                }
            }
            ?.addOnFailureListener {
                gestorMensajes.showActionOneButton("Error", "No se ha podido eliminar el usuario", context, {lanzaLogin()})
            }

    }

    // Borra un usuario, sus tickets y sus fotos
    fun borradoCompletoUsuario(user: String) {

        val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos

        val rutaTickets = "User/" + user + "/Tickets"

        dbRef.collection(rutaTickets)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val ticket = document.toObject(Ticket::class.java)
                    borraFotosTicket(ticket)
                    borraTicketsUsuario(ticket)
                }
                borraUsuarioFirestore(user)
                borraUsuarioFirebase()

            }
            .addOnFailureListener { exception ->
                gestorMensajes.showAlertOneButton(
                    "ERROR",
                    "Se ha producido un error al eliminar el usuario actual",
                    context
                )
            }

    }

    fun lanzaLogin(){
        val intent = Intent(context, Login::class.java)
        context.startActivity(intent)
    }
}
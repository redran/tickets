package es.leocaudete.mistickets.dao

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import es.leocaudete.mistickets.MainActivity
import es.leocaudete.mistickets.login.Login
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class FirestoreDB(context: Context) {

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

    //
    fun insertaTicket(ticket:Ticket){

        val dbRef=FirebaseFirestore.getInstance()

        dbRef.collection("User").document(ticket.idusuario)
            .collection("Tickets").document(ticket.idTicket)
            .set(ticket)
            .addOnSuccessListener {

            }
            .addOnFailureListener{ }

    }

    /**
     *  Necesitamos el ticket que contiene todas las fotos y el nombre del usuario local para saber la ruta local de donde están las fotos
     */
    fun subeFoto_A_Cloud(ticket:Ticket, userId:String){

        // Ruta local de donde estan las fotos
        var storageLocalDir  = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + userId + "/" + ticket.idTicket
        val auth = FirebaseAuth.getInstance() // Usuario autentificado
        var storageRef = FirebaseStorage.getInstance().reference

        if(ticket.foto1!=null)
        {
            var riverRef=storageRef.child(auth.currentUser?.uid.toString() +"/" + ticket.foto1)
            var uri= Uri.fromFile(File("$storageLocalDir/" + ticket.foto1))
            riverRef.putFile(uri)
        }
        if(ticket.foto2!=null)
        {
            var riverRef=storageRef.child(auth.currentUser?.uid.toString() +"/" + ticket.foto2)
            var uri= Uri.fromFile(File("$storageLocalDir/" + ticket.foto2))
            riverRef.putFile(uri)
        }
        if(ticket.foto3!=null)
        {
            var riverRef=storageRef.child(auth.currentUser?.uid.toString() +"/" + ticket.foto3)
            var uri= Uri.fromFile(File("$storageLocalDir/" + ticket.foto3))
            riverRef.putFile(uri)
        }
        if(ticket.foto4!=null)
        {
            var riverRef=storageRef.child(auth.currentUser?.uid.toString() +"/" + ticket.foto4)
            var uri= Uri.fromFile(File("$storageLocalDir/" + ticket.foto4))
            riverRef.putFile(uri)
        }

    }


}
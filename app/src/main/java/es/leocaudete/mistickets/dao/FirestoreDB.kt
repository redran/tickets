package es.leocaudete.mistickets.dao

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import es.leocaudete.mistickets.presentacion.MainActivity
import es.leocaudete.mistickets.presentacion.Login
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.modelo.Usuario
import es.leocaudete.mistickets.negocio.UsuarioNegocio
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import java.io.File

class FirestoreDB(context: Context) {



    // Le pasamos el contexto de quien instancia esta clase para poder mostrar los mensajes
    val context = context
    val auth = FirebaseAuth.getInstance() // Usuario autentificado
    val gestorMensajes = ShowMessages()
    val database = FirebaseFirestore.getInstance() // referencia a la base de datos
    var dbReference = database.collection("User")
    private lateinit var storageDir: String


    /**
     * Borra una foto de usuario y de un ticket pasado por parametro
     */
    fun borraFotoUsuario(foto: String, user: String, ticket: String) {
        var storageRef = FirebaseStorage.getInstance().reference
        var riverRef = storageRef.child("$user/$foto")
        riverRef.delete()
    }

    /**
     * Borra todas las fotos de un Ticket
     */
    fun borraFotosTicket(ticket: Ticket) {

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

    /**
     * Borra un ticket
     */
    fun borraTicketsUsuario(ticket: Ticket) {

        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado

        database.collection("User").document(userID)
            .collection("Tickets").document(ticket.idTicket).delete()

    }

    /**
     * Elimina el usuario
      */
    fun borraUsuarioFirestore(user: String) {

        database.collection("User").document(user).delete()

    }

    /**
     * Borra cuenta FireBase
     */
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

    /**
     * Borra un usuario, sus tickets y sus fotos
     */
    fun borradoCompletoUsuario(user: String) {

        val rutaTickets = "User/" + user + "/Tickets"

        database.collection(rutaTickets)
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

    /**
     * Inserta un nuevo ticket en Firebase
     */
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

    /**
     * VAlida el usuario y contraseña y lanza Main
     */
    fun logeaUsuario(user: String, password: String){
        if (!TextUtils.isEmpty(user) && !TextUtils.isEmpty(password)) {
            auth.signInWithEmailAndPassword(user, password)
                .addOnCompleteListener{ task ->

                    if (task.isSuccessful) {
                        if (auth.currentUser!!.isEmailVerified) {
                            storageDir =
                                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + auth.currentUser?.uid.toString()
                            controlversiones()
                        } else {
                            // En este punto tendríamos que verificar si la fecha de ingreso y la actual tienen 1 mes de diferencia y si la tienen eliminar la cuenta de firebase
                            gestorMensajes.showAlertOneButton(
                                "Alerta",
                                "Tienes que aceptar el email de verifiación que se envió a tu correo electrónico",
                                context
                            )
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Error: ususario y/o contraseña incorrectos",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } else {
            Toast.makeText(context, "Rellene todos los campos", Toast.LENGTH_LONG).show()
        }
    }
    /**
     * Comprueba versiones por si tiene que hacer un upgrade antes de cargar el Main
     */
    fun controlversiones(){

        val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado
        val rutaTickets = "User/" + userID + "/Tickets"
        var versionFirabaseCloud: String
        var  old_version_firebase=SharedApp.preferences.oldversionfirebase
        var pInfo = context.applicationContext.packageManager.getPackageInfo(context.packageName, 0)
        val NEW_VERSION_APP = pInfo.versionName




        // Primero comprobamos que la version de la base de datos coincida con la version de la APP
        //  que estamos usando o nos fallara el mapeo de datos a objetos
        var fbversion = dbRef.collection("Info")
            .document("UUPgCzmmLAk5KJgXB06k")
            .get()
            .addOnSuccessListener { document ->
                if(document==null){
                    Toast.makeText(context,"Error al acceder a la base de datos", Toast.LENGTH_LONG).show()
                }else{
                    versionFirabaseCloud=document["version"].toString()
                    if(old_version_firebase.toDouble()<versionFirabaseCloud.toDouble()){
                        when (versionFirabaseCloud.toDouble()) {
                            2.0 -> {
                                val data = HashMap<String,Any?>()
                                data.put("categoria",1)
                                data.put("precio",0.00)

                                upgradeFirebase(data,NEW_VERSION_APP,versionFirabaseCloud)
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

                                upgradeFirebase(data,NEW_VERSION_APP,versionFirabaseCloud)

                            }
                            else -> {
                                // Tenemos que prohibir que pueda escribir porque si esta ejecutando una version anterior
                                // significa que su modelo es antiguo. Puede leer y mapeará los campos que coincidan
                                // pero si intenta escribir subira el modelo viejo y eso implica que eliminará todos
                                // los campos nuevos y si usa esa cuenta con una app con la version nueva, abra perdido
                                // todos los datos añadidos en versiones posteriores a esta
                                SharedApp.preferences.modooperacion=1 // Se activa el modo lectura
                                context.startActivity(Intent(context, MainActivity::class.java))
                            }
                        }

                    }else{
                        // Si las versiones son iguales entonces o y ase ha actualizado o la actualización es solo de código y no de BD
                        SharedApp.preferences.modooperacion=0
                        SharedApp.preferences.avisounico=0
                        context.startActivity(Intent(context, MainActivity::class.java))
                    }
                }
            }

    }

    /**
     * Realiza los upgrades conrrespondientes en Firebase
     */
    fun upgradeFirebase(datos: HashMap<String,Any?>, versionFirabaseCloud:String, app_version: String){
        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado
        val rutaTickets = "User/" + userID + "/Tickets"

        val ticketsRef = database.collection(rutaTickets)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {

                    var ticket=document["idTicket"].toString()
                    database.collection(rutaTickets).document(ticket).set(datos,
                        SetOptions.merge())
                }
                // Una vez actualizada, actualizamos la version de la preferencia
                // para que no vuelva a hacerla
                SharedApp.preferences.oldversionapp=app_version
                SharedApp.preferences.oldversionfirebase=versionFirabaseCloud
                SharedApp.preferences.modooperacion=0
                SharedApp.preferences.avisounico=0
                context.startActivity(Intent(context, MainActivity::class.java))
            }
            .addOnFailureListener { exception ->
                gestorMensajes.showAlertOneButton("ERROR", "Se ha producido un error al actualizar la base de datos",context)
            }
    }

    /**
     * Añadir usuario Firebase
     */
    fun addUserFb(usuario: Usuario){
        auth.createUserWithEmailAndPassword(usuario.email, usuario.password)
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    verifyEmail(user)

                    // Dentro de User creamos otro documeto con el nombre del uid de Usuario que acaba de asignar
                    // el uid es creado a partir del password y la contraseña
                    var userBD: DocumentReference = dbReference.document(user!!.uid)

                    // añadimos el resto de datos del usuario o bien con un hashMap o con un modelo
                    val usuario = hashMapOf(
                        "nombre" to usuario.nombre,
                        "apellidos" to usuario.apellidos
                    )
                    userBD.set(usuario)
                    context.startActivity(Intent(context, Login::class.java))
                }
            }.addOnFailureListener{
                gestorMensajes.showActionOneButton(
                    "ERROR",
                    "No se ha podido registrar el nuevo usuario. Intentelo más tarde",
                    context,
                    { context.startActivity(Intent(context, Login::class.java)) })
            }

    }

    /**
     * Envia email de verificación
     */
    private fun verifyEmail(user: FirebaseUser?) {
        user?.sendEmailVerification()
            ?.addOnCompleteListener{ task ->

                if (task.isComplete) {
                    Toast.makeText(context, "Email enviado", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Error al enviar email", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Envia un email para recuperar el password
     */
    fun recuperaPasswordFb(email:String){
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener{
                    task ->

                if(task.isSuccessful){
                    context.startActivity(Intent(context,
                        Login::class.java ))
                }
                else
                {
                    Toast.makeText(context, "Error al enviar el email", Toast.LENGTH_LONG).show()
                }
            }
    }

    fun cargaFoto(foto:String?, imgFoto: ImageView){
        var storageRef = FirebaseStorage.getInstance().reference

        var rutaFoto = auth.currentUser?.uid.toString() + "/" + foto
        val pathReference = storageRef.child(rutaFoto)

        pathReference.downloadUrl.addOnSuccessListener {
            Picasso.get()
                .load(it)
                //.resize(400,800)
                .into(imgFoto)
        }
    }

}
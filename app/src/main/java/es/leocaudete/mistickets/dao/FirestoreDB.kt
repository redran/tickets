package es.leocaudete.mistickets.dao

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
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
import es.leocaudete.mistickets.utilidades.Utilidades
import java.io.File

/**
 * @author Leonardo Caudete Palau 2º DAM Semi
 */
class FirestoreDB(context: Context) {


    val utils = Utilidades()
    var gestorMensajes = ShowMessages()
    private var dbSQL = SQLiteDB(context, null)

    // Le pasamos el contexto de quien instancia esta clase para poder mostrar los mensajes
    val context = context
    val auth = FirebaseAuth.getInstance() // Usuario autentificado
    val database = FirebaseFirestore.getInstance() // referencia a la base de datos
    var dbReference = database.collection("User")
    private lateinit var storageDir: String

    /**
     * Consigue la lista de tickets
     */
    fun synTicketsFb(localUser:String,pbCargando:ProgressBar, tv_cargando:TextView){
        val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
        val auth = FirebaseAuth.getInstance()
        val rutaTickets = "User/" + auth.currentUser?.uid.toString() + "/Tickets"
        var listaCloud= mutableListOf<Ticket>()

        dbRef.collection(rutaTickets)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val ticket = document.toObject(Ticket::class.java)
                    listaCloud.add(ticket)
                }
                gestorMensajes.showAlert(
                    "SYNCRONIZACION",
                    "Se van a actualizar la lista de tickets con los datos de la cuenta Local. ¿Está seguro?",
                    context,
                    {syncWithLocal(dbSQL.buscaIdUsuario(localUser),listaCloud,pbCargando,tv_cargando) })


            }
            .addOnFailureListener { exception ->
                gestorMensajes.showAlertOneButton("ERROR","Se ha producido un error al descargar los datos de cloud",context)
            }
    }

    /**
     * Actualiza la lista de tickets de cloud con la lista Local
     */
    fun syncWithLocal(userId: String, list:MutableList<Ticket>,pbCargando:ProgressBar, tv_cargando:TextView  ) {

        pbCargando.visibility = View.VISIBLE
        tv_cargando.visibility = View.VISIBLE

        val auth = FirebaseAuth.getInstance()
        // Tenemos que rellenar dos listas, una local y una de cloud
        var listaCloud = list //firestoreBD.getTicketsCloud(auth.currentUser?.uid.toString())
        var listaLocal = dbSQL.devuelveTickets(userId)

        if(listaLocal.size>0){
            /**
             * Si la lista local tiene datos, buscamos si existen tickets con el mismo id
             * si existen y la fecha de actualización en mas nueva copiamos haciendo un Update
             */

            for(i in listaLocal.indices){
                var ticket = listaLocal[i]
                ticket.idusuario = auth.currentUser?.uid.toString()

                var encontrado=-1
                for(j in listaCloud.indices){

                    if(listaCloud[j].idTicket.equals(listaLocal[i].idTicket)){
                        encontrado=j
                    }
                }
                // Ruta local de donde estan las fotos
                var storageLocalDir =
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + userId + "/" + ticket.idTicket
                if(encontrado>=0){
                    if(listaCloud[encontrado].fecha_modificacion.toLong()<listaLocal[i].fecha_modificacion.toLong()){
                        insertaTicket(ticket,0)
                        subeFoto_A_Cloud(ticket,storageLocalDir)
                    }
                }else{
                    insertaTicket(ticket,0)
                    subeFoto_A_Cloud(ticket,storageLocalDir)
                }

            }

        }else{
            gestorMensajes.showAlertOneButton(
                "INFORMACION",
                "La cuenta local no tiene tickets",
                context)
        }

        pbCargando.visibility = View.GONE
        tv_cargando.visibility = View.GONE

        gestorMensajes.showActionOneButton("INFORMACION","Se ha terminado el proceso de syncronización",context, { context.startActivity(Intent(context, MainActivity::class.java))})
    }

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
     * PostTrabajo indica que tiene que hacer después de borrar el ticket con exito
     */
    fun borraTicketsUsuario(ticket: Ticket, postTrabajo: Int) {

        borraFotosTicket(ticket)

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
                    SharedApp.preferences.login = false
                    val intent = Intent(context, Login::class.java)
                    context.startActivity(intent)
                }
            }
            ?.addOnFailureListener {
                gestorMensajes.showActionOneButton(
                    "Error",
                    "No se ha podido eliminar el usuario",
                    context,
                    { lanzaLogin() })
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
                    // borraFotosTicket(ticket)
                    borraTicketsUsuario(ticket, 0)
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

    fun lanzaLogin() {
        val intent = Intent(context, Login::class.java)
        context.startActivity(intent)
    }

    /**
     * Inserta un nuevo ticket en Firebase
     * @param ticket es ticket a insertar
     * @param opcion indica si carga el main (1) o no (0) después de la inserción
     */
    fun insertaTicket(ticket: Ticket, opcion: Int) {

        val dbRef = FirebaseFirestore.getInstance()

        dbRef.collection("User").document(ticket.idusuario)
            .collection("Tickets").document(ticket.idTicket)
            .set(ticket)
            .addOnSuccessListener {
                if (opcion == 1) {
                    gestorMensajes.showActionOneButton(
                        "TERMINADO",
                        "Se ha insertado el ticket con éxito.",
                        context,
                        { context.startActivity(Intent(context, MainActivity::class.java)) })
                }
            }
            .addOnFailureListener {
                gestorMensajes.showAlertOneButton(
                    "ERROR",
                    "Se ha producido un error al insertar el ticket: " + ticket.titulo,
                    context)
            }

    }

    /**
     *  Necesitamos el ticket que contiene todas las fotos y el nombre del usuario local para saber la ruta local de donde están las fotos
     */
    fun subeFoto_A_Cloud(ticket: Ticket, rutaFoto: String) {


        val auth = FirebaseAuth.getInstance() // Usuario autentificado
        var storageRef = FirebaseStorage.getInstance().reference

        if (ticket.foto1 != null) {
            var riverRef = storageRef.child(auth.currentUser?.uid.toString() + "/" + ticket.foto1)
            var uri = Uri.fromFile(File(rutaFoto+"/" + ticket.foto1))
            riverRef.putFile(uri)
        }
        if (ticket.foto2 != null) {
            var riverRef = storageRef.child(auth.currentUser?.uid.toString() + "/" + ticket.foto2)
            var uri = Uri.fromFile(File(rutaFoto+"/" + ticket.foto2))
            riverRef.putFile(uri)
        }
        if (ticket.foto3 != null) {
            var riverRef = storageRef.child(auth.currentUser?.uid.toString() + "/" + ticket.foto3)
            var uri = Uri.fromFile(File(rutaFoto+"/" + ticket.foto3))
            riverRef.putFile(uri)
        }
        if (ticket.foto4 != null) {
            var riverRef = storageRef.child(auth.currentUser?.uid.toString() + "/" + ticket.foto4)
            var uri = Uri.fromFile(File(rutaFoto+"/" + ticket.foto4))
            riverRef.putFile(uri)
        }

    }

    /**
     * VAlida el usuario y contraseña y lanza Main
     */
    fun logeaUsuario(user: String, password: String, principal: ConstraintLayout, carga: ConstraintLayout) {
        if (!TextUtils.isEmpty(user) && !TextUtils.isEmpty(password)) {
            carga.visibility=View.VISIBLE
            principal.visibility=View.GONE
            auth.signInWithEmailAndPassword(user, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        if (auth.currentUser!!.isEmailVerified) {
                            storageDir =
                                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + auth.currentUser?.uid.toString()
                            utils.delRecFileAndDir(storageDir)
                            controlversiones()
                        } else {
                            carga.visibility=View.GONE
                            principal.visibility=View.VISIBLE
                            // En este punto tendríamos que verificar si la fecha de ingreso y la actual tienen 1 mes de diferencia y si la tienen eliminar la cuenta de firebase
                            gestorMensajes.showAlertOneButton(
                                "Alerta",
                                "Tienes que aceptar el email de verifiación que se envió a tu correo electrónico",
                                context
                            )
                        }
                    } else {
                        carga.visibility=View.GONE
                        principal.visibility=View.VISIBLE
                        Toast.makeText(
                            context,
                            "Error: ususario y/o contraseña incorrectos",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } else {
            carga.visibility=View.GONE
            principal.visibility=View.VISIBLE
            Toast.makeText(context, "Rellene todos los campos", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Comprueba versiones por si tiene que hacer un upgrade antes de cargar el Main
     */
    fun controlversiones() {

        val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado
        val rutaTickets = "User/" + userID + "/Tickets"
        var versionFirabaseCloud: String
        var old_version_firebase = SharedApp.preferences.oldversionfirebase
        var pInfo = context.applicationContext.packageManager.getPackageInfo(context.packageName, 0)
        val NEW_VERSION_APP = pInfo.versionName


        // Primero comprobamos que la version de la base de datos coincida con la version de la APP
        //  que estamos usando o nos fallara el mapeo de datos a objetos
        var fbversion = dbRef.collection("Info")
            .document("UUPgCzmmLAk5KJgXB06k")
            .get()
            .addOnSuccessListener { document ->
                if (document == null) {
                    Toast.makeText(
                        context,
                        "Error al acceder a la base de datos",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    versionFirabaseCloud = document["version"].toString()
                    if (old_version_firebase.toDouble() < versionFirabaseCloud.toDouble()) {
                        when (versionFirabaseCloud.toDouble()) {
                            2.0 -> {
                                val data = HashMap<String, Any?>()
                                data.put("categoria", 1)
                                data.put("precio", 0.00)

                                upgradeFirebase(data, NEW_VERSION_APP, versionFirabaseCloud)
                            }
                            3.0 -> {
                                val data = HashMap<String, Any?>()
                                // Si partimos de dos versiones anteriores entonces tendremos que agregar
                                // todos los campos que se han agregado hasta ahora
                                if (SharedApp.preferences.oldversionfirebase == "1.0") {
                                    data.put("categoria", 1)
                                    data.put("precio", 0.00)
                                }
                                data.put("isdieta", 0)
                                data.put("fecha_envio", null)
                                data.put("metodo_envio", 0)
                                data.put("enviado_a", null)
                                data.put("fecha_cobro", null)
                                data.put("metodo_cobro", null)

                                upgradeFirebase(data, NEW_VERSION_APP, versionFirabaseCloud)

                            }
                            else -> {
                                // Tenemos que prohibir que pueda escribir porque si esta ejecutando una version anterior
                                // significa que su modelo es antiguo. Puede leer y mapeará los campos que coincidan
                                // pero si intenta escribir subira el modelo viejo y eso implica que eliminará todos
                                // los campos nuevos y si usa esa cuenta con una app con la version nueva, abra perdido
                                // todos los datos añadidos en versiones posteriores a esta
                                SharedApp.preferences.modooperacion = 1 // Se activa el modo lectura
                                context.startActivity(Intent(context, MainActivity::class.java))
                            }
                        }

                    } else {
                        // Si las versiones son iguales entonces o y ase ha actualizado o la actualización es solo de código y no de BD
                        SharedApp.preferences.modooperacion = 0
                        SharedApp.preferences.avisounico = 0
                        context.startActivity(Intent(context, MainActivity::class.java))
                    }
                }
            }

    }

    /**
     * Realiza los upgrades conrrespondientes en Firebase
     */
    fun upgradeFirebase(
        datos: HashMap<String, Any?>,
        versionFirabaseCloud: String,
        app_version: String
    ) {
        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado
        val rutaTickets = "User/" + userID + "/Tickets"

        val ticketsRef = database.collection(rutaTickets)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {

                    var ticket = document["idTicket"].toString()
                    database.collection(rutaTickets).document(ticket).set(
                        datos,
                        SetOptions.merge()
                    )
                }
                // Una vez actualizada, actualizamos la version de la preferencia
                // para que no vuelva a hacerla
                SharedApp.preferences.oldversionapp = app_version
                SharedApp.preferences.oldversionfirebase = versionFirabaseCloud
                SharedApp.preferences.modooperacion = 0
                SharedApp.preferences.avisounico = 0
                context.startActivity(Intent(context, MainActivity::class.java))
            }
            .addOnFailureListener { exception ->
                gestorMensajes.showAlertOneButton(
                    "ERROR",
                    "Se ha producido un error al actualizar la base de datos",
                    context
                )
            }
    }

    /**
     * Añadir usuario Firebase
     */
    fun addUserFb(usuario: Usuario) {
        auth.createUserWithEmailAndPassword(usuario.email, usuario.password)
            .addOnCompleteListener { task ->
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
            }.addOnFailureListener {
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
            ?.addOnCompleteListener { task ->

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
    fun recuperaPasswordFb(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    context.startActivity(
                        Intent(
                            context,
                            Login::class.java
                        )
                    )
                } else {
                    Toast.makeText(context, "Error al enviar el email", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Carga una foto en un ImageView sin descargarla
     */
    fun cargaFoto(foto: String?, imgFoto: ImageView) {
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

    /**
     * Descarga de cloud y cuando el hilo termina, entonces asigna la imagen al ImageView
     * Comprueba si exite la imagen en cloud si es asi la descarga y la asigna
     * Sino, comprueba que este en local, porque puede ser que este editando
     * o la acabe de hacer y todavía no este subida
     */
    fun descargaFotoCloudInicial(foto: String, imgView: ImageView) {
        var storageRef = FirebaseStorage.getInstance().reference
        var rutaFoto = auth.currentUser?.uid.toString() + "/" + foto

        var imgRef = storageRef.child(rutaFoto)
        storageDir =
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + auth.currentUser?.uid.toString()
        // Comprobamos si el fichero existe en nuestro directorio
        val home_dir = File(storageDir)
        if (!home_dir.exists()) {
            home_dir.mkdirs()
        }
        var file =
            File(storageDir + "/" + foto)
        if (!file.exists()) {
            file.createNewFile()
            imgRef.getFile(file).addOnSuccessListener {
                imgView.setImageURI(Uri.parse(storageDir + "/" + foto))
            }.addOnFailureListener {}
        } else {
            imgView.setImageURI(Uri.parse(storageDir + "/" + foto))
        }

    }

    /**
     * Descarga todas las fotos de un ticket
     */
    fun descargaFotosTicket(ticket: Ticket, rutalocal:String) {
        if (ticket.foto1 != null) {
            descargaFoto(ticket.foto1.toString(),rutalocal)
        }
        if (ticket.foto2 != null) {
            descargaFoto(ticket.foto2.toString(),rutalocal)
        }
        if (ticket.foto3 != null) {
            descargaFoto(ticket.foto3.toString(),rutalocal)
        }
        if (ticket.foto4 != null) {
            descargaFoto(ticket.foto4.toString(),rutalocal)
        }
    }

    /**
     * Descarga una foto. Si la foto existe la borramos y la volvemos a descargar para tener la última version
     */
    fun descargaFoto(foto: String, rutaLocal:String) {
        var storageRef = FirebaseStorage.getInstance().reference
        var rutaFoto = auth.currentUser?.uid.toString() + "/" + foto
        var imgRef = storageRef.child(rutaFoto)

        // Comprobamos si el fichero existe en nuestro directorio
        val home_dir = File(rutaLocal)
        if (!home_dir.exists()) {
            home_dir.mkdirs()
        }
        var file =
            File(rutaLocal + "/" + foto)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        imgRef.getFile(file)

    }

    /**
     * Devuelve el id de usuario a partir de auth.currentUser=.uid
     */
    fun getIdUsuario(): String {
        return auth.currentUser?.uid.toString()
    }

    /**
     * REaliza un LogOut
     *
     */
    fun logOut(){
        auth.signOut()
    }

}
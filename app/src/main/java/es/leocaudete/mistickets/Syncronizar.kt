package es.leocaudete.mistickets

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.core.view.size
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import es.leocaudete.mistickets.dao.FirestoreDB
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_sycronizar.*
import java.io.File
import java.time.LocalDate

class Syncronizar : AppCompatActivity() {

    val gestorMensajes = ShowMessages()
    val firestoreBD = FirestoreDB(this)
    val sqLiteBD = SQLiteDB(this,null)


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
    }

    // Anulamos la opción de volver a tras a través del botón del móvil
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

            val dbSQL = SQLiteDB(this, null)
            // Verificamos si el usuario existe en la base de datos local
            if (dbSQL.buscaUsuario(user)) {
                if (dbSQL.validaPassword(user, password)) {

                    // Recuperamos la lista de tickets de cloud y cuando finalice la carga entonces llamamos a syncronizar
                    // Lo hacemos asi, porque la carga es en un hilo y tenemos que esperar a que termine
                    var listaCloud= mutableListOf<Ticket>()
                    val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
                    val auth = FirebaseAuth.getInstance()
                    val rutaTickets = "User/" + auth.currentUser?.uid.toString() + "/Tickets"
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
                                this,
                                {syncWithLocal(dbSQL.buscaIdUsuario(user),listaCloud) })

                        }
                        .addOnFailureListener { exception ->
                            gestorMensajes.showAlertOneButton("ERROR","Se ha producido un error al descargar los datos de cloud",this)
                        }
                } else {
                    Toast.makeText(
                        this,
                        "Error: Contraseña incorrectos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "Error: ususario no encontrado",
                    Toast.LENGTH_LONG
                ).show()

            }
            // Si es Local
        } else {
            // Verificamos si la cuenta de CLOUD existe
            val auth = FirebaseAuth.getInstance()
            // Primero comprobamos que la cuenta exista
            auth.signInWithEmailAndPassword(user, password)
                .addOnCompleteListener(this) { task ->
                    // Si existe
                    if (task.isSuccessful) {
                        // Recuperamos la lista de tickets de cloud y cuando finalice la carga entonces llamamos a syncronizar
                        // Lo hacemos asi, porque la carga es en un hilo y tenemos que esperar a que termine
                        var listaCloud= mutableListOf<Ticket>()
                        val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
                        val rutaTickets = "User/" + auth.currentUser?.uid.toString() + "/Tickets"
                        dbRef.collection(rutaTickets)
                            .get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    val ticket = document.toObject(Ticket::class.java)
                                    listaCloud.add(ticket)
                                }
                                gestorMensajes.showAlert(
                                    "SYNCRONIZACION",
                                    "Se van a actualizar la lista de tickets con los datos de la cuenta de CLOUD. ¿Está seguro?",
                                    this,
                                    {syncWithCloud(listaCloud) })

                            }
                            .addOnFailureListener { exception ->
                                gestorMensajes.showAlertOneButton("ERROR","Se ha producido un error al descargar los datos de cloud",this)
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "Error: ususario y/o contraseña incorrectos",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }


    // Acualiza la lista de tickets locales con la lista de Cloud
    fun syncWithCloud(list: MutableList<Ticket>) {

        pbCargando.visibility = View.VISIBLE
        tv_cargando.visibility = View.VISIBLE
        val auth = FirebaseAuth.getInstance()
        val userId=auth.currentUser?.uid.toString()


        val storageLocalDir= getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + SharedApp.preferences.usuario_logueado

        // Tenemos que rellenar dos listas, una local y una de cloud
        var listaCloud = list //firestoreBD.getTicketsCloud(userId)
        var listaLocal = sqLiteBD.devuelveTickets(SharedApp.preferences.usuario_logueado)
        if(listaCloud.size>0){
            /** Independientemente si hay datos o no, copiamos todos los tickets cuyo
             * id no aparezca en nuestra lista
             */
            for(i in listaCloud.indices){

                /**
                 * Primero cambiamos es user id por el local
                 */
                var ticket = listaCloud[i]
                ticket.idusuario = SharedApp.preferences.usuario_logueado

                /**
                 * Si encontramos un id egual entonces comparamos la fecha de modificación
                 */
                var encontrado=-1
                for(j in listaLocal.indices){

                    if(listaLocal[j].idTicket.equals(listaCloud[i].idTicket)){
                        encontrado=j
                    }
                }

                if(encontrado>=0){
                    if(listaLocal[encontrado].fecha_modificacion.toLong() < listaCloud[i].fecha_modificacion.toLong()){
                        sqLiteBD.updateTicket(ticket)
                        //Tambien tenemos que bajarnos las fotos
                        syncWithCloudFoto(ticket,userId)
                    }
                }else{
                    sqLiteBD.addTicket(ticket)
                    //Tambien tenemos que bajarnos las fotos, pero primero creamos el directorio
                    val home_dir = File(storageLocalDir + "/" + ticket.idTicket)
                    if (!home_dir.exists()) {
                        home_dir.mkdirs()
                    }
                    syncWithCloudFoto(ticket,userId)
                }

                gestorMensajes.showActionOneButton("INFORMACION","Se ha terminado el proceso de syncronización",this, {lanzaMain()})

            }
        }else{
            gestorMensajes.showAlertOneButton(
                "INFORMACION",
                "La cuenta de cloud no tiene tickets",
                this)
        }
        pbCargando.visibility = View.GONE
        tv_cargando.visibility = View.GONE



    }

    // Descarga las imagenes de cloud perteneciente a un ticket
    private fun syncWithCloudFoto(ticket: Ticket,userId: String) {

        var storageRef = FirebaseStorage.getInstance().reference
        val storageLocalDir= getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + SharedApp.preferences.usuario_logueado

        var contador=0

        // Find all DownloadTasks under this StorageReference

        for (i in 1..4) {

            // Primero verificamos que el campos foto tenga una foto
            var verificaCampo = false
            when (i) {
                1 -> {
                    if (ticket.foto1 != null) {
                        verificaCampo = true
                    }
                }
                2 -> {
                    if (ticket.foto2 != null) {
                        verificaCampo = true
                    }
                }
                3 -> {
                    if (ticket.foto3 != null) {
                        verificaCampo = true
                    }
                }
                4 -> {
                    if (ticket.foto4 != null) {
                        verificaCampo = true
                    }
                }
            }
            if (verificaCampo) {
                var rutaFoto = userId + "/" + ticket.idTicket + "_foto" + i + ".jpg"
                var imgRef = storageRef.child(rutaFoto)

                // Comprobamos si el fichero existe en nuestro directorio
                var file =
                    File(storageLocalDir + "/" + ticket.idTicket +"/" + ticket.idTicket + "_foto" + i + ".jpg")
                if (!file.exists()) {
                    file.createNewFile()
                }
                imgRef.getFile(file).addOnSuccessListener {
                }.addOnFailureListener {}
            }

        }

    }

    // Actualiza la lista de tickets de cloud con la lista Local
    fun syncWithLocal(userId: String, list:MutableList<Ticket>) {

        pbCargando.visibility = View.VISIBLE
        tv_cargando.visibility = View.VISIBLE

        val auth = FirebaseAuth.getInstance()
        // Tenemos que rellenar dos listas, una local y una de cloud
        var listaCloud = list //firestoreBD.getTicketsCloud(auth.currentUser?.uid.toString())
        var listaLocal = sqLiteBD.devuelveTickets(userId)

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

                if(encontrado>=0){
                    if(listaCloud[encontrado].fecha_modificacion.toLong()<listaLocal[i].fecha_modificacion.toLong()){
                        firestoreBD.insertaTicket(ticket)
                        firestoreBD.subeFoto_A_Cloud(ticket,userId)
                    }
                }else{
                    firestoreBD.insertaTicket(ticket)
                    firestoreBD.subeFoto_A_Cloud(ticket,userId)
                }

            }

        }else{
            gestorMensajes.showAlertOneButton(
                "INFORMACION",
                "La cuenta local no tiene tickets",
                this)
        }

        pbCargando.visibility = View.GONE
        tv_cargando.visibility = View.GONE

        gestorMensajes.showActionOneButton("INFORMACION","Se ha terminado el proceso de syncronización",this, {lanzaMain()})
    }

    fun cancelar(view: View) {
        lanzaMain()
    }

    fun lanzaMain(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}

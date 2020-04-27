package es.leocaudete.mistickets.negocio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.FirestoreDB
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.presentacion.MainActivity
import es.leocaudete.mistickets.utilidades.ShowMessages
import org.jetbrains.anko.doAsync
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * @author Leonardo Caudete Palau 2º DAM Semi
 */
class TicketsNegocio(context: Context) {

    private var dbSQL = SQLiteDB(context, null)
    private var dbFirebase = FirestoreDB(context)
    var auth = FirebaseAuth.getInstance()
    private var gestorMensajes = ShowMessages()

    private lateinit var storageDir: String
    private var context = context

    //Común
    /**
     *  Renombra las fotos en caso de estar en modo edición
     *
     */
    fun renombraFotosTemp(ticket:Ticket, rotaFoto:String){
        for(i in 1..4){
            var strFoto: String = ticket.idTicket + "_foto" + i + ".jpg"
            var file =
                File(rotaFoto + "/" + "edited_" + ticket.idTicket + "_foto" + i + ".jpg")
            if (file.exists()) {
                file.renameTo(File(rotaFoto +"/" + strFoto))
            }
        }

    }
    /**
     * Lanza un hilo que se recibe la lista completa de Tickets asociados a un usuario y
     * busca los que tengan marcada la opción de avisar cuando la garatia este a punto de caducar
     * y luego compara su fecha de caducidad (obtenida de la  fecha de compra y el periodo de garantía)
     * con la fecha actual.
     * Si la fecha que da es menor de 15 días lanza una aviso.
     */
    fun revisaGarantias(tickets: MutableList<Ticket>): MutableList<Ticket> {

        var hayPendientes: Boolean = false
        val CHANNEL_ID = "es.leocaudete.mistickets"
        val notificationId = 123456

        var resultado = mutableListOf<Ticket>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager =
                context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
        // Notificaciones
        var builder = NotificationCompat.Builder(context, CHANNEL_ID)

        builder.apply {
            setSmallIcon(R.mipmap.ic_mistickets_round)
            setContentTitle("Fecha de fin de garantia próxima")
            setContentText("Revisa los ticket que están a punto de caducar")
            priority = NotificationCompat.PRIORITY_DEFAULT
            setAutoCancel(true)
        }



        doAsync {

            for (ticket in tickets) {

                // Cromprobamos si el ticket tiene la opcion de avisar
                if (ticket.avisar_fin_garantia == 1) {

                    // Obtenemos el periodo de garantia
                    val duracionDeLaGarantia: Long =
                        ticket.duracion_garantia.toLong() // 1, 2, 3, 4, 5
                    ticket.periodo_garantia // años, meses

                    //obtenemos la fecha de compra
                    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    val fechaCompra = LocalDate.parse(ticket.fecha_de_compra, formatter)

                    // Calculamos cuando es la fecha de fin de la garantia
                    val fechaFin: LocalDate
                    if (ticket.periodo_garantia == 0) {
                        fechaFin = fechaCompra.plusYears(duracionDeLaGarantia)
                    } else {
                        fechaFin = fechaCompra.plusMonths(duracionDeLaGarantia)
                    }

                    val hoy = LocalDate.now()

                    // Obtenemos la diferencia de dias
                    val diferencia = ChronoUnit.DAYS.between(hoy, fechaFin)

                    // Si cumple las condiciones lo agregamos a la lista de tickets que van a caducar
                    if (diferencia in 1..15) {
                        resultado.add(ticket)
                        hayPendientes = true
                    }
                }
            }
            if (hayPendientes) {
                with(NotificationManagerCompat.from(context.applicationContext)) {
                    notify(notificationId, builder.build())
                }
            }
        }
        return resultado
    }

    /**
     * Muestra unos pocos registros dependiendo de la opcion para nos sobrecargar el sistemas al cargar todas las fotos
     */
    fun listadoFiltrado(opcion: Int, lista: MutableList<Ticket>): MutableList<Ticket> {

        var resultado = mutableListOf<Ticket>()

        when (opcion) {

            // Solo los personales del mes actual

            1 -> {
                var mes = LocalDate.now().monthValue
                var anno = LocalDate.now().year
                resultado = lista.filter {
                    (((LocalDate.parse(
                        it.fecha_de_compra,
                        DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    )).monthValue) == mes) &&
                            (((LocalDate.parse(
                                it.fecha_de_compra,
                                DateTimeFormatter.ofPattern("dd-MM-yyyy")
                            )).year) == anno)
                }.toMutableList()
            }
            // Solo las Dietas
            2 -> resultado = lista.filter { it.isdieta == 1 }.toMutableList()
            // Solo los tickets personales
            3 -> resultado = lista.filter { it.isdieta == 0 }.toMutableList()

        }


        return resultado
    }

    /**
     * Verifica que el usuario de SQLite con el que queremos sincronizar existe
     * y llama a sincronizar
     */
    fun sycronizaConLocal(userLocal:String, passwordLocal:String, pbCargando: ProgressBar, tv_cargando: TextView){
        val dbSQL = SQLiteDB(context, null)
        // Verificamos si el usuario existe en la base de datos local
        if (dbSQL.buscaUsuario(userLocal)) {
            if (dbSQL.validaPassword(userLocal, passwordLocal)) {

                // Recuperamos la lista de tickets de cloud y cuando finalice la carga entonces llamamos a syncronizar
                // Lo hacemos asi, porque la carga es en un hilo y tenemos que esperar a que termine
                dbFirebase.synTicketsFb(userLocal,pbCargando, tv_cargando)
            } else {
                Toast.makeText(
                    context,
                    "Error: Contraseña incorrectos",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Error: ususario no encontrado",
                Toast.LENGTH_LONG
            ).show()

        }
    }

    //**********************  On-Line  ***********************************************************************
    /**
     * Carga la imagen directamente desde Firebase Storage
     */
    fun cargaFoto(foto: String?, imgFoto: ImageView) {
        dbFirebase.cargaFoto(foto, imgFoto)
    }

    /**
     * DEvuelve la ruta de almacenamiento local
     */
    fun rutaLocalFb(): String {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + auth.currentUser?.uid.toString()
    }

    /**
     * Inserta un nuevo ticket en Firebase
     * @param ticket es ticket a insertar
     * @param opcion indica si carga el main (1) o no (0) después de la inserción
     */
    fun insertaTicketFb(ticket:Ticket, opcion:Int){
        dbFirebase.insertaTicket(ticket,opcion)
    }
    /**
     * Borra las fotos de un ticket
     */
    fun borraFotosTicket(ticket:Ticket){
        dbFirebase.borraFotosTicket(ticket)
    }
    /**
     * Borra un Ticket y carga el Main
     */
    fun borraTicket(ticket:Ticket){
        dbFirebase.borraTicketsUsuario(ticket,1)
    }
    /**
     * Descarga de cloud y cuando el hilo termina, entonces asigna la imagen al ImageView
     */
    fun descargaFotoCloudInicial(foto:String, imgView:ImageView){
        dbFirebase.descargaFotoCloudInicial(foto,imgView)
    }

    /**
     * Descarga todas las fotos de un ticket
     */
    fun descargaFotos(ticket:Ticket, rutaLocal:String){
        dbFirebase.descargaFotosTicket(ticket,rutaLocal)
    }

    /**
     * Devuelve el id de usuario
     */
    fun getIdUsuarioFB():String{
        return dbFirebase.getIdUsuario()
    }

    /**
     * Sube las imagenes de un tiack a Cloud.
     * La ruta de las imagenes se le pasa por parámetro ya que pueden ser
     * del propio ticket o de otro usuario a la hora de sincronizar
     */
    fun subeFoto_A_Cloud(ticket: Ticket, userId: String){
        dbFirebase.subeFoto_A_Cloud(ticket,userId)
    }



    //********************************* Off-Line  **************************************************************************
    /**
     * Devuelve la lista de tickets vinculados al usuario pasado como parámetro de una base de datos local
     */
    fun getTicketsOffLine(idUsu: String): MutableList<Ticket> {
        return dbSQL.devuelveTickets(SharedApp.preferences.usuario_logueado)

    }

    /**
     * Devuelve la ruta local donde se almacenarán las fotos
     */
    fun rutaLocal(idTicket:String):String {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + SharedApp.preferences.usuario_logueado + "/" + idTicket
    }

    /**
     * Borra un ticket de la base de datos Local
     */
    fun borraTicketLocal(idTicket:String){
        dbSQL.deleteTicket(idTicket)
    }

    /**
     * Lanza el update y muestra error en caso de fallar
     */
    fun updateTicketLocal(ticket:Ticket){
        if (dbSQL.updateTicket(ticket) > 0) {
            context.startActivity(Intent(context, MainActivity::class.java))
        } else {
            gestorMensajes.showAlertOneButton(
                "ERROR",
                "Error al actualizar el ticket",
                context
            )
        }
    }

    /**
     * Inserta un nuevo Ticket y muestra un error en caso de fallar
     */
    fun insertaTicketLocal(ticket:Ticket){
        if (dbSQL.addTicket(ticket) > 0) {
            context.startActivity(Intent(context, MainActivity::class.java))
        } else {
            gestorMensajes.showAlertOneButton(
                "ERROR",
                "Error al insertar el ticket",
                context
            )
        }
    }



    /**
     * Verifica que el usuario de Cloud Existe
     */
    fun syncronizaConCloud(userCloud:String, passwordCloud:String,pbCargando: ProgressBar, tv_cargando: TextView){
        // Verificamos si la cuenta de CLOUD existe
        val auth = FirebaseAuth.getInstance()
        // Primero comprobamos que la cuenta exista
        auth.signInWithEmailAndPassword(userCloud, passwordCloud)
            .addOnCompleteListener{ task ->
                // Si existe
                if (task.isSuccessful) {
                    // Recuperamos la lista de tickets de cloud y cuando finalice la carga entonces llamamos a syncronizar
                    // Lo hacemos asi, porque la carga es en un hilo y tenemos que esperar a que termine
                    var listaCloud = mutableListOf<Ticket>()
                    val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
                    val rutaTickets = "User/" + dbFirebase.getIdUsuario() + "/Tickets"
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
                                context,
                                { syncWithCloud(listaCloud,pbCargando,tv_cargando) })

                        }
                        .addOnFailureListener { exception ->
                            gestorMensajes.showAlertOneButton(
                                "ERROR",
                                "Se ha producido un error al descargar los datos de cloud",
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
    }

    /**
     * Acualiza la lista de tickets locales con la lista de Cloud
     */
    fun syncWithCloud(list: MutableList<Ticket>,pbCargando: ProgressBar, tv_cargando: TextView) {

        pbCargando.visibility = View.VISIBLE
        tv_cargando.visibility = View.VISIBLE

        // Tenemos que rellenar dos listas, una local y una de cloud
        var listaCloud = list //firestoreBD.getTicketsCloud(userId)
        var listaLocal = dbSQL.devuelveTickets(SharedApp.preferences.usuario_logueado)
        if (listaCloud.size > 0) {
            /** Independientemente si hay datos o no, copiamos todos los tickets cuyo
             * id no aparezca en nuestra lista
             */
            for (i in listaCloud.indices) {

                /**
                 * Primero cambiamos es user id por el local
                 */
                var ticket = listaCloud[i]
                ticket.idusuario = SharedApp.preferences.usuario_logueado

                /**
                 * Si encontramos un id egual entonces comparamos la fecha de modificación
                 */
                var encontrado = -1
                for (j in listaLocal.indices) {

                    if (listaLocal[j].idTicket.equals(listaCloud[i].idTicket)) {
                        encontrado = j
                    }
                }

                if (encontrado >= 0) {
                    if (listaLocal[encontrado].fecha_modificacion.toLong() < listaCloud[i].fecha_modificacion.toLong()) {
                        dbSQL.updateTicket(ticket)
                        //Tambien tenemos que bajarnos las fotos
                        descargaFotos(ticket, rutaLocal(ticket.idTicket))
                    }
                } else {
                    dbSQL.addTicket(ticket)
                    //Tambien tenemos que bajarnos las fotos, pero primero creamos el directorio
                    val home_dir = File(rutaLocal(ticket.idTicket))
                    if (!home_dir.exists()) {
                        home_dir.mkdirs()
                    }
                    descargaFotos(ticket, rutaLocal(ticket.idTicket))
                }
            }
            gestorMensajes.showActionOneButton(
                "INFORMACION",
                "Se ha terminado el proceso de syncronización",
                context,
                { context.startActivity(Intent(context, MainActivity::class.java))})
        } else {
            gestorMensajes.showAlertOneButton(
                "INFORMACION",
                "La cuenta de cloud no tiene tickets",
                context
            )
        }
        pbCargando.visibility = View.GONE
        tv_cargando.visibility = View.GONE


    }




}
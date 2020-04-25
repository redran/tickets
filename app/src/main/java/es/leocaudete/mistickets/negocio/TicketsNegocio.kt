package es.leocaudete.mistickets.negocio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.ImageView
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
import org.jetbrains.anko.doAsync
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TicketsNegocio(context: Context) {

    private var dbSQL = SQLiteDB(context, null)
    private var dbFirebase = FirestoreDB(context)
    var auth = FirebaseAuth.getInstance()

    private lateinit var storageDir: String
    private var context = context

    //On-Line
    /**
     * Carga la imagen directamente desde Firebase Storage
     */
    fun cargaFoto(foto: String?, imgFoto: ImageView) {
        dbFirebase.cargaFoto(foto, imgFoto)
    }

    fun rutaLocalFb(): String {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + auth.currentUser?.uid.toString()
    }

    /**
     * Devuelve la lista de tickets vinculados al usuario pasado como parámetro de una base de datos local
     */
    fun getTicketsOffLine(idUsu: String): MutableList<Ticket> {
        return dbSQL.devuelveTickets(SharedApp.preferences.usuario_logueado)

    }

    fun rutaLocal(idTicket:String):String {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + SharedApp.preferences.usuario_logueado + "/" + idTicket
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


}
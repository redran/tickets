package es.leocaudete.mistickets

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.leocaudete.mistickets.adapters.RecyclerAdapter
import es.leocaudete.mistickets.login.Login
import es.leocaudete.mistickets.modelo.Ticket

import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "DocSnippets"
    private val myAdapter: RecyclerAdapter =
        RecyclerAdapter()
    var tickets = mutableListOf<Ticket>() // va a almacenar todos los tickets
    var ticketsQueVanACaducar = mutableListOf<Ticket>()
    private val CHANNEL_ID = "es.leocaudete.mistickets"
    private val notificationId = 123456
    lateinit var storageLocalDir: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        storageLocalDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
        setSupportActionBar(toolbar)

        getTickets(tickets)

    }

    // Anulamos la opción de volver a tras a través del botón del móvil
    override fun onBackPressed() {
        //
    }

    private fun setUpRecyclerView(reqTickets: MutableList<Ticket>) {


        listadoTickets.setHasFixedSize(true)
        listadoTickets.layoutManager = LinearLayoutManager(this)
        myAdapter.RecyclerAdapter(reqTickets, this)
        listadoTickets.adapter = myAdapter

        pbCargando.visibility = View.INVISIBLE
        tv_cargando.visibility = View.INVISIBLE
    }

    /**
     * Recuperamos los tickets de la base de datos de Firestore
     */
    private fun getTickets(reqTickets: MutableList<Ticket>) {

        pbCargando.visibility = View.VISIBLE
        tv_cargando.visibility = View.VISIBLE


        val dbRef = FirebaseFirestore.getInstance() // referencia a la base de datos
        val auth = FirebaseAuth.getInstance() // Usuario autentificado

        val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado
        val rutaTickets = "User/" + userID + "/Tickets"

        val ticketsRef = dbRef.collection(rutaTickets)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val ticket = document.toObject(Ticket::class.java)
                    tickets.add(ticket)
                }
                setUpRecyclerView(reqTickets)
                revisaGarantias()

            }

            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Se ha producido une error al cargar los datos: ",
                    Toast.LENGTH_LONG
                ).show()
                Log.d(TAG, "Error getting documents:", exception)
            }

    }

    /*
    Infla el menú
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /*
    Acciones sobre los elementos del menú al hacer click
     */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var auth = FirebaseAuth.getInstance()
        return when (item?.itemId) {
            R.id.newticket -> {
                startActivity(Intent(this, NuevoTicket::class.java))
                finish()
                true
            }
            R.id.newticketicon -> {
                startActivity(Intent(this, NuevoTicket::class.java))
                finish()
                true
            }
            R.id.todo -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }
            R.id.caducan -> {
                setUpRecyclerView(ticketsQueVanACaducar)
                true
            }
            R.id.closesission -> {
                auth.signOut()
                startActivity(Intent(this, Login::class.java))
                finish()
                true
            }
            R.id.findticket -> {
                buscar()
                true
            }
            R.id.findticketicon -> {
                buscar()
                true
            }
            R.id.eliminar_usuario ->{

                true
            }


            else -> super.onOptionsItemSelected(item)
        }

    }

    /*
   Lanza la activity que busca tickets
     */
    private fun buscar() {

        // Va a lanzar una activity que nos va a devolver un string con el where
        var intent = Intent(this, Busquedas::class.java)
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {

                var filterdTickets = mutableListOf<Ticket>()
                var conFiltro: Boolean = false
                var reqTicket = data?.getSerializableExtra("where") as Ticket


                // Comprobamos si el objeto que hemos recibido tiene datos para el filtro
                // y vamos filtrando de la lista original y vamos reduciendo poco a por la lista aplicando
                // filtro sucesivos
                if (!TextUtils.isEmpty(reqTicket.titulo)) {

                    filterdTickets =
                        tickets.filter { p -> p.titulo.contains(reqTicket.titulo) }.toMutableList()
                    if (filterdTickets.size > 0) {
                        tickets = filterdTickets
                        conFiltro = true
                    }
                }
                if (!TextUtils.isEmpty(reqTicket.establecimiento)) {
                    filterdTickets =
                        tickets.filter { p -> p.establecimiento.contains(reqTicket.establecimiento) }
                            .toMutableList()
                    if (filterdTickets.size > 0) {
                        tickets = filterdTickets
                        conFiltro = true
                    }
                }
                if (!TextUtils.isEmpty(reqTicket.localidad)) {
                    filterdTickets = tickets.filter { p -> p.localidad.equals(reqTicket.localidad) }
                        .toMutableList()
                    if (filterdTickets.size > 0) {
                        tickets = filterdTickets
                        conFiltro = true
                    }


                }
                if (reqTicket.provincia > 0) {
                    filterdTickets =
                        tickets.filter { p -> p.provincia == reqTicket.provincia }.toMutableList()
                    if (filterdTickets.size > 0) {
                        tickets = filterdTickets
                        conFiltro = true
                    }
                }
                if (!TextUtils.isEmpty(reqTicket.fecha_de_compra)) {
                    filterdTickets =
                        tickets.filter { p -> p.fecha_de_compra.equals(reqTicket.fecha_de_compra) }
                            .toMutableList()
                    if (filterdTickets.size > 0) {
                        tickets = filterdTickets
                        conFiltro = true
                    }

                }

                if (conFiltro) {
                    setUpRecyclerView(tickets) // Si encuentra algo envia el array reducido
                } else {
                    filterdTickets.clear()
                    setUpRecyclerView(filterdTickets) // Si no encuentra nada envia un array vacio
                }


            }

        }
    }

    /*
    Revisa si hay ticket con el aviso de fin de la garantía marcado y si es asi comprueba la fecha de compra
    con el período de garantía y si faltan menos de 15 días para alcanzar la fecha límite, te lanza un aviso
     */
    private fun revisaGarantias() {

        var hayPendientes: Boolean = false

        ticketsQueVanACaducar.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
        // Notificaciones
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)

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
                if (ticket.avisar_fin_garantia) {

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
                        ticketsQueVanACaducar.add(ticket)
                        hayPendientes = true
                    }
                }
            }
            if (hayPendientes) {
                with(NotificationManagerCompat.from(applicationContext)) {
                    notify(notificationId, builder.build())
                }
            }

        }


    }


}

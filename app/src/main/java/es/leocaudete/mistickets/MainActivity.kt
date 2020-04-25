package es.leocaudete.mistickets

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.login.Login
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.dao.FirestoreDB
import es.leocaudete.mistickets.estadisticas.Grafica
import es.leocaudete.mistickets.utilidades.ShowMessages
import es.leocaudete.mistickets.utilidades.Utilidades

import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList

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
    lateinit var dbSQL: SQLiteDB

    val idUsuario = SharedApp.preferences.usuario_logueado

    var gestoMensajes = ShowMessages()
    val fbUtils = FirestoreDB(this)
    val utils = Utilidades()
    val gestorMensajes = ShowMessages()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        storageLocalDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
        setSupportActionBar(toolbar)

        // Instanciamos la clase que crea la base de datos y tiene nuestro CRUD
        dbSQL = SQLiteDB(this, null)

        if (SharedApp.preferences.modooperacion == 1) {
            gestorMensajes.showActionOneButton("ALERTA",
                "Estás ejecutando una versión antigua de la app.\n" +
                        "Para evitar errores pérdida de datos se habilita el acceso como solo lectura.\n" +
                        "Actualice a la última versión para usar todas las características.",
                this,
                { getTickets(tickets) })
        } else {
            getTickets(tickets)
        }

    }


    // Anulamos la opción de volver a tras a través del botón del móvil
    override fun onBackPressed() {
        //
    }

    private fun setUpRecyclerView(reqTickets: MutableList<Ticket>, opcion:Int) {

        var listaFiltrada= mutableListOf<Ticket>()
        // Ahora filtramos lo que queremos que salga por pantalla
        when(opcion){
            // Opcion se recibe todos los tickets y vamos a mostrar solo los del mes actual
            1 -> listaFiltrada=listadoFiltrado(1, reqTickets)
            // Solo los que sean dietas
            2 -> listaFiltrada=listadoFiltrado(2, reqTickets)
            // Solo los personales
            3 -> listaFiltrada=listadoFiltrado(3, reqTickets)

            // Si viene directo de busquedas o del servicio de caducidad mostramos todos los tickets que llegan
            else -> listaFiltrada=reqTickets
        }

        // ordenamos la lista por fecha de compra
        // esto nos devuelve un List, al pasarlo al adapter hay que tranformarlo otra vez en MutableList
        var listaParam = listaFiltrada.sortedByDescending { x ->
            LocalDate.parse(
                x.fecha_de_compra,
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
            )
        }

        listadoTickets.setHasFixedSize(true)
        listadoTickets.layoutManager = LinearLayoutManager(this)
       // Aqui es donde se le pasa el listado que queremos que muestre por pantalla
        myAdapter.RecyclerAdapter(listaParam.toMutableList(), this)
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

        // On-Line. Se carga Firebase
        if (SharedApp.preferences.bdtype) {
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

                    setUpRecyclerView(reqTickets,1)
                    if(SharedApp.preferences.avisounico==0){
                        SharedApp.preferences.avisounico=1
                        revisaGarantias()
                    }


                }

                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Se ha producido une error al cargar los datos: ",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d(TAG, "Error getting documents:", exception)
                }
        } else {
            // Off-Line. Se carga de SQLite
            var ticketsUsu = dbSQL.devuelveTickets(SharedApp.preferences.usuario_logueado)
            if ((ticketsUsu).size > 0) {
                for (i in ticketsUsu.indices) {
                    tickets.add(ticketsUsu[i])
                }
            }
            setUpRecyclerView(reqTickets,1)
            // Solo se avisa una vez después de hacer el login
            if(SharedApp.preferences.avisounico==0){
                SharedApp.preferences.avisounico=1
                revisaGarantias()
            }

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
            R.id.newticket, R.id.newticketicon -> {
                if (SharedApp.preferences.modooperacion == 1) {
                    gestorMensajes.showAlertOneButton(
                        "ALERTA",
                        "La app está en modo solo lectura y no se permite añadir nuevos tickets",
                        this
                    )
                } else {
                    startActivity(Intent(this, NuevoTicket::class.java))
                    finish()
                }

                true
            }
            R.id.personales -> {
              //  startActivity(Intent(this, MainActivity::class.java))
                setUpRecyclerView(tickets,3)
                true
            }
            R.id.caducan -> {
                setUpRecyclerView(ticketsQueVanACaducar,0)
                true
            }
            R.id.dietas->{
                setUpRecyclerView(tickets,2)
                true
            }
            R.id.closesission -> {
                if (SharedApp.preferences.bdtype) {
                    utils.delRecFileAndDir(storageLocalDir + "/" + auth.currentUser?.uid.toString())
                    auth.signOut()
                } else {
                    if (auth != null) {
                        auth.signOut()
                    }
                    SharedApp.preferences.usuario_logueado = ""
                }
                SharedApp.preferences.login = false
                startActivity(Intent(this, Login::class.java))
                finish()
                true
            }
            R.id.findticket, R.id.findticketicon -> {
                buscar()
                true
            }
            R.id.sycronizar -> {
                if (SharedApp.preferences.modooperacion == 1) {
                    gestorMensajes.showAlertOneButton(
                        "ALERTA",
                        "La app está en modo solo lectura y no se permite la sincronización",
                        this
                    )
                } else {
                    startActivity(Intent(this, Syncronizar::class.java))
                    finish()
                }
                true
            }
            R.id.eliminar_usuario -> {
                gestoMensajes.showAlert(
                    "ALERTA",
                    "Va a leminar el usuario actual y todos tu tickets. Esta operación eliminará los datos de forma definitiva. ¿Está seguro?",
                    this,
                    { eliminaUsuarioActual() })
                true
            }
            R.id.estadisticas -> {
                // ArrayList de los tickets ordenados por fecha
                var arrayLisTickets = ArrayList(tickets.sortedByDescending { x ->
                    LocalDate.parse(
                        x.fecha_de_compra,
                        DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    )
                })

                val intent = Intent(this, Grafica::class.java).apply {
                    putExtra("tickets", arrayLisTickets)
                }
                startActivity(intent)

                finish()
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
                var reqTicket = data?.getSerializableExtra("filtrados") as ArrayList<Ticket>
                setUpRecyclerView(reqTicket,0)
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

    // Elimina el usuario actual
    private fun eliminaUsuarioActual() {
        if (SharedApp.preferences.modooperacion == 1) {
            gestorMensajes.showAlertOneButton(
                "ALERTA",
                "La app está en modo solo lectura y no se permite borrar datos",
                this
            )
        } else {
            if (SharedApp.preferences.bdtype) {
                val auth = FirebaseAuth.getInstance() // Usuario autentificado
                val userID = auth.currentUser?.uid.toString() // ID del usuario autentificado
                fbUtils.borradoCompletoUsuario(userID)

            } else {
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
                startActivity(Intent(this, Login::class.java))
            }
        }


    }

    /**
     * Muestra unos pocos registros dependiendo de la opcion para nos sobrecargar el sistemas al cargar todas las fotos
     */
    fun listadoFiltrado(opcion: Int, lista: MutableList<Ticket>): MutableList<Ticket> {

        var resultado = mutableListOf<Ticket>()

        when (opcion) {

            // Solo los personales del mes actual
            1->resultado=lista.filter {((LocalDate.parse(it.fecha_de_compra, DateTimeFormatter.ofPattern("dd-MM-yyyy"))).monthValue)==LocalDate.now().monthValue}.toMutableList()
            // Solo las Dietas
            2->resultado=lista.filter {it.isdieta==1}.toMutableList()
            // Solo los tickets personales
            3->resultado=lista.filter {it.isdieta==0}.toMutableList()

        }


        return resultado
    }


}

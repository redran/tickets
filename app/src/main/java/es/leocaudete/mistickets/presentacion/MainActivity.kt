package es.leocaudete.mistickets.presentacion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.adapters.RecyclerAdapter
import es.leocaudete.mistickets.dao.FirestoreDB
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.negocio.TicketsNegocio
import es.leocaudete.mistickets.negocio.UsuarioNegocio
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import es.leocaudete.mistickets.utilidades.Utilidades
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class MainActivity : AppCompatActivity() {

    var ticketsNegocio = TicketsNegocio(this)
    var usuarioNegocio = UsuarioNegocio(this)
    var context=this

    private val TAG = "DocSnippets"
    private val myAdapter: RecyclerAdapter =
        RecyclerAdapter()
    var tickets = mutableListOf<Ticket>() // va a almacenar todos los tickets
    var ticketsQueVanACaducar = mutableListOf<Ticket>()

    lateinit var storageLocalDir: String

    var gestoMensajes = ShowMessages()

    val utils = Utilidades()
    val gestorMensajes = ShowMessages()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        storageLocalDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
        setSupportActionBar(toolbar)


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

    /**
     * Se encarga de montar el RecyclerView y de pasarle los datos que queremos que muestr
     */
    fun setUpRecyclerView(reqTickets: MutableList<Ticket>, opcion: Int) {

        var listaFiltrada = mutableListOf<Ticket>()
        // Ahora filtramos lo que queremos que salga por pantalla
        when (opcion) {
            // Opcion se recibe todos los tickets y vamos a mostrar solo los del mes actual
            1 -> listaFiltrada = ticketsNegocio.listadoFiltrado(1, reqTickets)
            // Solo los que sean dietas
            2 -> listaFiltrada = ticketsNegocio.listadoFiltrado(2, reqTickets)
            // Solo los personales
            3 -> listaFiltrada = ticketsNegocio.listadoFiltrado(3, reqTickets)

            // Si viene directo de busquedas o del servicio de caducidad mostramos todos los tickets que llegan
            else -> listaFiltrada = reqTickets
        }

        // ordenamos la lista por fecha de compra
        // esto nos devuelve un List, al pasarlo al adapter hay que tranformarlo otra vez en MutableList
        var listaParam = listaFiltrada.sortedByDescending { x ->
            LocalDate.parse(
                x.fecha_de_compra,
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
            )
        }.toMutableList()

        listadoTickets.setHasFixedSize(true)
        listadoTickets.layoutManager = LinearLayoutManager(this)
        // Aqui es donde se le pasa el listado que queremos que muestre por pantalla
        myAdapter.RecyclerAdapter(listaParam, this)
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

                    setUpRecyclerView(reqTickets, 1)
                    if (SharedApp.preferences.avisounico == 0) {
                        SharedApp.preferences.avisounico = 1
                        ticketsQueVanACaducar.clear()
                        ticketsQueVanACaducar = ticketsNegocio.revisaGarantias(tickets)
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
            var ticketsUsu =
                ticketsNegocio.getTicketsOffLine(SharedApp.preferences.usuario_logueado)
            if ((ticketsUsu).size > 0) {
                for (i in ticketsUsu.indices) {
                    tickets.add(ticketsUsu[i])
                }
            }
            setUpRecyclerView(reqTickets, 1)
            // Solo se avisa una vez después de hacer el login
            if (SharedApp.preferences.avisounico == 0) {
                SharedApp.preferences.avisounico = 1
                ticketsQueVanACaducar.clear()
                ticketsQueVanACaducar = ticketsNegocio.revisaGarantias(tickets)
            }

        }


    }

    /**
     * Infla el menú para visualizarlo
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * Define todas las acciones de cada elemento del menú principal
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
                setUpRecyclerView(tickets, 3)
                true
            }
            R.id.caducan -> {
                setUpRecyclerView(ticketsQueVanACaducar, 0)
                true
            }
            R.id.dietas -> {
                setUpRecyclerView(tickets, 2)
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

    /**
     * Lanza la activity que se encarga de las búsquedas
     */
    private fun buscar() {

        // Va a lanzar una activity que nos va a devolver un string con el where
        var intent = Intent(this, Busquedas::class.java)
        startActivityForResult(intent, 1)
    }

    /**
     * Recibe las respuestas de las activitys que se han llamado  con startActivityResult como es la búsqueda
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // El código 1 corresponde a las búsquedas
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                var reqTicket = data?.getSerializableExtra("filtrados") as ArrayList<Ticket>
                setUpRecyclerView(reqTicket, 0)
            }
        }
    }

    /**
     * Elimina el usuario Actual
     */
    private fun eliminaUsuarioActual() {
        if (SharedApp.preferences.modooperacion == 1) {
            gestorMensajes.showAlertOneButton(
                "ALERTA",
                "La app está en modo solo lectura y no se permite borrar datos",
                this
            )
        } else {
            if (SharedApp.preferences.bdtype) {
                usuarioNegocio.borraUsuarioFb()

            } else {
                usuarioNegocio.borraUsuario()
            }
        }

    }



}

package es.leocaudete.mistickets.presentacion

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.negocio.TicketsNegocio
import es.leocaudete.mistickets.preferences.SharedApp
import kotlinx.android.synthetic.main.activity_busquedas.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class Busquedas : AppCompatActivity() {

    var ticketsNegocio = TicketsNegocio(this)
    private val TAG = "DocSnippets"
    var tickets = mutableListOf<Ticket>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_busquedas)

        tvCargando.visibility=View.VISIBLE
        pb_cargando.visibility=View.VISIBLE
        btbuscar.visibility=View.GONE
        getTickets(tickets)

    }


    // Obtenemos la lista de todos los tickets
    private fun getTickets(reqTickets: MutableList<Ticket>) {

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
                    inicializaCampos()
                    tvCargando.visibility=View.GONE
                    pb_cargando.visibility=View.GONE
                    btbuscar.visibility=View.VISIBLE
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
            var ticketsUsu =   ticketsNegocio.getTicketsOffLine(SharedApp.preferences.usuario_logueado)
            if ((ticketsUsu).size > 0) {
                for (i in ticketsUsu.indices) {
                    tickets.add(ticketsUsu[i])
                }
                inicializaCampos()
                tvCargando.visibility=View.GONE
                pb_cargando.visibility=View.GONE
                btbuscar.visibility=View.VISIBLE
            }
        }
    }

    /**
     *  Rellenamos los Spinner y colocamos los valores por defecto
     */
    private fun inicializaCampos() {


        // Rellenamos el Spinner de Provincias lk
        val adapterProvincias = ArrayAdapter.createFromResource(
            this,
            R.array.provincias,
            android.R.layout.simple_spinner_item
        )

        adapterProvincias.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        find_spinner_provincias.adapter = adapterProvincias

        // Rellenamos el Spinner de Categorias
        val adapterCategorias = ArrayAdapter.createFromResource(
            this,
            R.array.categorias,
            android.R.layout.simple_spinner_item
        )

        adapterCategorias.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner_cat.adapter = adapterCategorias

        // Rellenamos el Spinner de Meses
        val adapterMeses = ArrayAdapter.createFromResource(
            this,
            R.array.meses,
            android.R.layout.simple_spinner_item
        )

        adapterMeses.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner_meses.adapter = adapterMeses

        // Rellenamos el Spinner de Años

        var annos: MutableList<Int>
        val setAnnos: LinkedHashSet<String> = linkedSetOf("Años")

        for (ticket in tickets) {
            //Obtenemos el año
            var ticket_anno = (LocalDate.parse(
                ticket.fecha_de_compra,
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
            )).year
            setAnnos.add(ticket_anno.toString())
        }
        val adapterAnnos = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            setAnnos.toMutableList()
        )

        adapterAnnos.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spinner_anno.adapter = adapterAnnos

        cbDietas.isChecked=false

        grpDietas.visibility=View.GONE

    }

    /**
     *  Cancela la busqueda y devuelve RESULT_CANCEL
     */
    fun cancelar(view: View) {
        var intent = Intent(this, MainActivity::class.java)
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    /**
     * Valida que si se ha seleccionado un mes ,también tiene que seleccionar un año
     */
    fun validar():Boolean{
        if(spinner_meses.selectedItemPosition>0){
           if(spinner_anno.selectedItemPosition>0){
               return true
           }else{
               Toast.makeText(this, "Debes seleccionar un año", Toast.LENGTH_LONG).show()
               return false
           }
        }else{
            return true
        }
    }
    /**
     * vamos a crear predicados e iremos filtrando el array de tickets que hemos rellenado al principio
     * Pero antes hay que hacer las validaciones
     */
    fun buscar(view: View) {
        filtrar()
    }

    /**
     * Aplica todo los filtros
     */
    fun filtrar(){
        var listo: Boolean = true
        var resTicket= mutableListOf<Ticket>()


        // Si alguno de los filtros da false, significa que juntos no producen resultados
        if(validar()){
            if (!TextUtils.isEmpty(et_find_tienda.text)) {
                resTicket=tickets.filter{it.establecimiento.contains(et_find_tienda.text.toString().toUpperCase()) }.toMutableList()
                if(resTicket.size==0){
                    listo=false
                }
            }

            if (!TextUtils.isEmpty(et_find_desc.text)) {
                if(resTicket.size>0){
                    resTicket=resTicket.filter{it.titulo.contains(et_find_desc.text.toString().toUpperCase()) }.toMutableList()
                }else{
                    resTicket=tickets.filter{it.titulo.contains(et_find_desc.text.toString().toUpperCase()) }.toMutableList()
                }
                if(resTicket.size==0){
                    listo=false
                }
            }

            if (!TextUtils.isEmpty(find_ed_localidades.text)) {
                if(resTicket.size>0){
                    resTicket=resTicket.filter{it.localidad.equals(find_ed_localidades.text.toString().toUpperCase()) }.toMutableList()
                }else{
                    resTicket=tickets.filter{it.localidad.equals(find_ed_localidades.text.toString().toUpperCase()) }.toMutableList()
                }
                if(resTicket.size==0){
                    listo=false
                }
            }

            if (find_spinner_provincias.selectedItemPosition > 0) {
                if(resTicket.size==0){
                    resTicket=resTicket.filter{it.provincia==(find_spinner_provincias.selectedItemPosition) }.toMutableList()
                }else{
                    resTicket=tickets.filter{it.provincia==(find_spinner_provincias.selectedItemPosition) }.toMutableList()
                }
                if(resTicket.size==0){
                    listo=false
                }
            }

            if (spinner_cat.selectedItemPosition > 0) {
                if(resTicket.size>0){
                    resTicket=resTicket.filter {it.categoria==(spinner_cat.selectedItemPosition) }.toMutableList()
                }else{
                    resTicket=tickets.filter {it.categoria==(spinner_cat.selectedItemPosition) }.toMutableList()
                }
                if(resTicket.size==0){
                    listo=false
                }
            }

            if (spinner_anno.selectedItemPosition>0) {
                if(resTicket.size>0){
                    resTicket=resTicket.filter {((LocalDate.parse(it.fecha_de_compra, DateTimeFormatter.ofPattern("dd-MM-yyyy"))).year)==Integer.parseInt(spinner_anno.selectedItem.toString())  }.toMutableList()
                }else{
                    resTicket=tickets.filter {((LocalDate.parse(it.fecha_de_compra, DateTimeFormatter.ofPattern("dd-MM-yyyy"))).year)==Integer.parseInt(spinner_anno.selectedItem.toString())  }.toMutableList()
                }
                if(resTicket.size==0){
                    listo=false
                }
            }

            if (spinner_meses.selectedItemPosition>0) {
                if(resTicket.size>0){
                    resTicket=resTicket.filter {((LocalDate.parse(it.fecha_de_compra, DateTimeFormatter.ofPattern("dd-MM-yyyy"))).monthValue)==spinner_meses.selectedItemPosition  }.toMutableList()
                }else{
                    resTicket=tickets.filter {((LocalDate.parse(it.fecha_de_compra, DateTimeFormatter.ofPattern("dd-MM-yyyy"))).monthValue)==spinner_meses.selectedItemPosition  }.toMutableList()
                }
                if(resTicket.size==0){
                    listo=false
                }
            }

            // Si marcamos dietas filtramos todos los que tengas el campos isDieta a true
            if(cbDietas.isChecked){
                if(resTicket.size>0){
                    resTicket=resTicket.filter {it.isdieta==1 }.toMutableList()
                }else{
                    resTicket=tickets.filter {it.isdieta==1}.toMutableList()
                }
                if(resTicket.size==0){
                    listo=false
                }
            }
            if(rbCobrado.isChecked){
                if(resTicket.size>0){
                    resTicket=resTicket.filter {it.fecha_cobro!=null}.toMutableList()
                }else{
                    resTicket=tickets.filter {it.fecha_cobro!=null}.toMutableList()
                }
                if(resTicket.size==0){
                    listo=false
                }
            }
            if(rbSinCobrar.isChecked){
                if(resTicket.size>0){
                    resTicket=resTicket.filter {it.fecha_cobro==null}.toMutableList()
                }else{
                    resTicket=tickets.filter {it.fecha_cobro==null}.toMutableList()
                }
                if(resTicket.size==0){
                    listo=false
                }
            }
            if(rbSinPasar.isChecked){
                if(resTicket.size>0){
                    resTicket=resTicket.filter {it.fecha_envio==null}.toMutableList()
                }else{
                    resTicket=tickets.filter {it.fecha_envio==null}.toMutableList()
                }
                if(resTicket.size==0){
                    listo=false
                }
            }



            if (listo && resTicket.size>0) {

                var arrayTickets=ArrayList(resTicket)
                var intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("filtrados", arrayTickets)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Esa busqueda no devuelve ningún resultado", Toast.LENGTH_LONG).show()
                resTicket.clear()

            }


        }
    }

    /**
     * Anulamos la opción de volver a tras a través del botón del móvil
     */
    override fun onBackPressed() {
        //
    }

    /**
     * Comprueba si se ha marcado dietas o no
     */
    fun isDietas(view: View) {
        if(cbDietas.isChecked){
            grpDietas.visibility=View.VISIBLE
        }else{
            grpDietas.visibility=View.GONE
            rbCobrado.isChecked=false
            rbSinCobrar.isChecked=false
            rbSinPasar.isChecked=false
        }
    }
}

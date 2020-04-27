package es.leocaudete.mistickets.presentacion

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.utilidades.Utilidades
import kotlinx.android.synthetic.main.activity_grafica.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.LinkedHashSet

/**
 * @autor Leonardo Caudete Palau 2º DAM Semi
 */
class Grafica : AppCompatActivity() {

    lateinit var listaTickets:ArrayList<Ticket>
    var utilidades=Utilidades()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grafica)

        // Cargamos nuestra toolbar.
        setSupportActionBar(graficaToolBar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        listaTickets = intent.getSerializableExtra("tickets") as ArrayList<Ticket>
        cargaSpinners()
    }

    /**
     * Anulamos la opción de volver a tras a través del botón del móvil
     */
    override fun onBackPressed() {
        //
    }

    /**
     * definimos las acciones para los elementos del menu
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)

        }
    }

    /**
     * Cargamos los espinner con valores entendibles
     */
    fun cargaSpinners(){

        // Rellenamos el Spinner de Categorias
        val adapterCategorias = ArrayAdapter.createFromResource(
            this,
            R.array.categorias,
            android.R.layout.simple_spinner_item
        )

        adapterCategorias.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner_categorias.adapter = adapterCategorias

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

        var annos:MutableList<Int>
        val setAnnos:LinkedHashSet<String> = linkedSetOf("Años")

        for(ticket in listaTickets){
            //Obtenemos el año
            var ticket_anno= (LocalDate.parse(ticket.fecha_de_compra, DateTimeFormatter.ofPattern("dd-MM-yyyy"))).year
            setAnnos.add(ticket_anno.toString())
        }



        val adapterAnnos=ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            setAnnos.toMutableList()
        )

        adapterAnnos.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spinner_anno.adapter=adapterAnnos


    }

    /**
     * Aplicamos los filtros a nuestro Array de Tickets y
     */
    fun mostrarGrafica(view: View) {

        var arrayCategorias = resources.getStringArray(R.array.categorias)

        if(validar()){

            // Tenemos que filtar de nuestra lista todos los que sean del año elegido y del mes en cuestión
            var listaFiltrada=listaTickets
                .filter {(((LocalDate.parse(it.fecha_de_compra, DateTimeFormatter.ofPattern("dd-MM-yyyy"))).year)==Integer.parseInt(spinner_anno.selectedItem.toString()))
                        && (((LocalDate.parse(it.fecha_de_compra, DateTimeFormatter.ofPattern("dd-MM-yyyy"))).monthValue)==spinner_meses.selectedItemPosition)  }




            // Una vez tenemos nuestra listra filtrada
            // ¿Que tenemos que mostrar?
            // Si no hemos elegido categoria, mostraremos la suma de los importes de los tickets agrupados por categoria y las categorias
            var datos=ArrayList<BarEntry>()
            var labels = ArrayList<String>()

            if(spinner_categorias.selectedItemPosition==0){

                // Necesitamos un map de <categorias, sumaPrecios>
               var misdatos= listaFiltrada.groupBy {it.categoria }
                var i=0f
                for((v,k) in  misdatos){
                    datos.add(BarEntry(i,k.sumByDouble { x->x.precio }.toFloat()))
                    // Como no sale todo el nombre, según los resultados mostramos una cantidad de caracteres u otra
                    when(misdatos.size){
                        in 1..2 -> labels.add(utilidades.miSubstring(0,12,arrayCategorias[v]))
                        3 -> labels.add(utilidades.miSubstring(0,8,arrayCategorias[v]))
                        in 4..6 ->  labels.add(utilidades.miSubstring(0,5,arrayCategorias[v]))
                        in 7..100 ->  labels.add(utilidades.miSubstring(0,3,arrayCategorias[v]))
                        else -> labels.add(arrayCategorias[v])
                    }
                    i++
                }

                // Los datos de la columnas
                val barDataSet = BarDataSet(datos, "Categorías")
                barDataSet.setBarBorderWidth(0.9f)
                barDataSet.setColors(ColorTemplate.COLORFUL_COLORS.toMutableList())

                val barData = BarData(barDataSet)
                var xAxis = graficaBarras.xAxis
                xAxis.position=XAxis.XAxisPosition.TOP
                xAxis.mCenteredEntries
                xAxis.granularity=1f
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                graficaBarras.data=barData
                graficaBarras.setFitBars(true)
                graficaBarras.animateXY(5000,5000)
                graficaBarras.invalidate()
                graficaBarras.description.text="Gasto menual por categorías"
                graficaBarras.description.setPosition(0.1f,0.1f)


            // Si hemos elegido categoria, mostraremos la suma de los importes de los tickets de la misma tienda y las tiendas
            }else{

                //Reducimos una vez mas la lista a solamente los de categoría elegida
                var listaTiendasCategoria=listaFiltrada
                    .filter {it.categoria==spinner_categorias.selectedItemPosition}

                // Necesitamos un map de <tienda, sumaPrecios>
                var misdatos= listaTiendasCategoria.groupBy {it.establecimiento }
                var i=0f
                for((v,k) in  misdatos){
                    datos.add(BarEntry(i,k.sumByDouble { x->x.precio }.toFloat()))
                    when(misdatos.size){
                        in 1..2 -> labels.add(utilidades.miSubstring(0,12,k[0].establecimiento))
                        3 -> labels.add(utilidades.miSubstring(0,8,k[0].establecimiento))
                        in 4..6 ->  labels.add(utilidades.miSubstring(0,5,k[0].establecimiento))
                        in 7..100 ->  labels.add(utilidades.miSubstring(0,3,k[0].establecimiento))
                        else -> labels.add(k[0].establecimiento)
                    }
                    i++
                }

                val barDataSet = BarDataSet(datos, "Establecimientos")
                barDataSet.setBarBorderWidth(0.9f)
                barDataSet.setColors(ColorTemplate.COLORFUL_COLORS.toMutableList())

                val barData = BarData(barDataSet)
                var xAxis = graficaBarras.xAxis
                xAxis.position=XAxis.XAxisPosition.TOP
                xAxis.mCenteredEntries
                xAxis.granularity=1f
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)

                graficaBarras.data=barData
                graficaBarras.setFitBars(true)
                graficaBarras.animateXY(5000,5000)
                graficaBarras.invalidate()
                graficaBarras.description.text="Gasto menual por categorías"
                graficaBarras.description.setPosition(5f,-5f)
            }
        }

    }

    /**
     * REglas de validación
     */
    fun validar():Boolean{
        var validado=false

        if(spinner_anno.selectedItemPosition==0){
            Toast.makeText(this, "Debes seleccionar un año", Toast.LENGTH_LONG).show()
        }else if(spinner_meses.selectedItemPosition==0){
            Toast.makeText(this, "Debes seleccionar un mes", Toast.LENGTH_LONG).show()
        }else
        {
            validado=true
        }

        return validado
    }
}

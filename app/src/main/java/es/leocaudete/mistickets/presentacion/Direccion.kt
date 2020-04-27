package es.leocaudete.mistickets.presentacion

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.modelo.Ticket
import kotlinx.android.synthetic.main.activity_direccion.*

/**
 * @author Leonardo Caudete Palau 2º DAM  Semi
 * Esta clase se encarga de todo lo relacionado con la dirección
 * La dejo aparte para dejar mas sitio en Nuevo Ticket y para prepararlo
 * por si en un futuro deseo meter maps
 */
class Direccion : AppCompatActivity() {

    var unTicket = Ticket()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direccion)

        // El ticket que recibimos de NuevoTicke
        unTicket = intent.getSerializableExtra("Ticket") as Ticket
        iniciacampos()
    }
    /**
     * Anulamos la opción de volver a tras a través del botón del móvil
     */
    override fun onBackPressed() {
        //
    }

    /**
     * Ponemos los valores por defecto y si es una edición los rellenamos con los datos del ticket que viene como parametro en el Intent
     */
    fun iniciacampos() {

        ed_direccion.setText(unTicket.direccion)
        ed_localidades.setText(unTicket.localidad)
        // Rellenamos el Spinner de Provincias
        val adapterProvincias = ArrayAdapter.createFromResource(
            this,
            R.array.provincias,
            android.R.layout.simple_spinner_item
        )

        adapterProvincias.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner_provincias.adapter = adapterProvincias
        spinner_provincias.setSelection(unTicket.provincia, false)
    }

    fun guardar(view: View) {
        if (!TextUtils.isEmpty(ed_direccion.text)) {
            unTicket.direccion = ed_direccion.text.toString().toUpperCase()
        }
        if (!TextUtils.isEmpty(ed_localidades.text)) {
            unTicket.localidad = ed_localidades.text.toString().toUpperCase()
        }
        if (spinner_provincias.selectedItemPosition > 0) {
            unTicket.provincia = spinner_provincias.selectedItemPosition
        }
        if(validacion()){
            val myIntent = Intent(this, NuevoTicket::class.java).apply {
                putExtra("unTicket", unTicket)
            }
            setResult(Activity.RESULT_OK, myIntent)
            finish()
        }
    }

    fun cancelar(view: View) {
        val myIntent = Intent(this, NuevoTicket::class.java)
        setResult(Activity.RESULT_CANCELED, myIntent)
        finish()
    }

    fun validacion():Boolean{
        var resultado=true
        if (!TextUtils.isEmpty(ed_localidades.text) && spinner_provincias.selectedItemPosition == 0) {
            Toast.makeText(
                this,
                "Si especificas una localidad tienes que seleccionar una provncia",
                Toast.LENGTH_LONG
            ).show()
            resultado = false
        }
        return resultado
    }

}

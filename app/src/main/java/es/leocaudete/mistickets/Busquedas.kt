package es.leocaudete.mistickets

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_busquedas.*
import java.util.*

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class Busquedas : AppCompatActivity() {

    var findFecha: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_busquedas)
        inicializaCampos()

        find_img_calendar.setOnClickListener {

            val calendar = Calendar.getInstance()
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)

            val dpd = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

                    // Display Selected date in textbox
                    var mes: String = (monthOfYear + 1).toString()
                    if ((monthOfYear + 1) < 10) {
                        mes = "0$mes"
                    }

                    var dia: String = (dayOfMonth).toString()
                    if (dayOfMonth < 10) {
                        dia = "0$dia"
                    }
                    var fecha = "$dia-$mes-$year"

                    find_et_fecha.setText(fecha)
                    findFecha = fecha
                },
                year,
                month,
                day
            )

            dpd.show()
        }

    }

    /**
     *  Rellenamos los Spinner y colocamos los valores por defecto
      */

    private fun inicializaCampos() {


        // Rellenamos el Spinner de Provincias
        val adapterProvincias = ArrayAdapter.createFromResource(
            this,
            R.array.provincias,
            android.R.layout.simple_spinner_item
        )

        adapterProvincias.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        find_spinner_provincias.adapter = adapterProvincias

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
     * va a devolver un objeto de tipo ticket con los datos a buscar.
     * Buscaremos en el array, ya que si buscamos en firebase y trabajamos off-line ya no podemos buscar
     */
    fun buscar(view: View) {

        var listo: Boolean = false
        var resTicket = Ticket()

        if (!TextUtils.isEmpty(et_find_tienda.text)) {
            resTicket.establecimiento = et_find_tienda.text.toString().toUpperCase()
            listo = true
        }
        if (!TextUtils.isEmpty(et_find_desc.text)) {
            resTicket.titulo = et_find_desc.text.toString().toUpperCase()
            listo = true
        }
        if (!TextUtils.isEmpty(find_ed_localidades.text)) {
            resTicket.localidad = find_ed_localidades.text.toString().toUpperCase()
            listo = true
        }
        if (!TextUtils.isEmpty(find_et_fecha.text)) {
            resTicket.fecha_de_compra = findFecha
            listo = true
        }
        if (find_spinner_provincias.selectedItemPosition > 0) {
            resTicket.provincia = find_spinner_provincias.selectedItemPosition
            listo = true
        }

        if (listo) {

            var intent = Intent(this, MainActivity::class.java).apply {
                putExtra("where", resTicket)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else {
            Toast.makeText(this, "Debes de rellenar al menos un filtro", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Anulamos la opción de volver a tras a través del botón del móvil
     */
    override fun onBackPressed() {
        //
    }
}

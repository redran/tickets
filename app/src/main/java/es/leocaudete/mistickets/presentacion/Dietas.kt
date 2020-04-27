package es.leocaudete.mistickets.presentacion

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.modelo.Ticket
import kotlinx.android.synthetic.main.activity_dietas.*
import java.util.*

/**
 * @author Leonardo Caudete Palau 2º DAM Semi
 */
class Dietas : AppCompatActivity() {

    var unTicket = Ticket()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietas)

        unTicket = intent.getSerializableExtra("Ticket") as Ticket
        iniciacampos()

        // Editamos el metodo para que cuando cambiemos de seleccion en el Spinner, mostremos los Edit Text correspondientes
        spinner_metodos?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                comprueba_spinner_metodos()
                ed_enviado_a.setText("")
            }
        }
        setCalEntregado()
        setCalCobrado()
    }

    /**
     * Configura un calendario y lo asociamos a evento click del boton
     */
    fun setCalEntregado() {
        img_entrega.setOnClickListener {

            var MONTHS = arrayOf(
                "Enero",
                "Febrero",
                "Marzo",
                "Abril",
                "Mayo",
                "Junio",
                "Julio",
                "Agosto",
                "Septiembre",
                "Octubre",
                "Noviembre",
                "Diciembre"
            )

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
                        mes = "0" + mes
                    }

                    var dia: String = (dayOfMonth).toString()
                    if (dayOfMonth < 10) {
                        dia = "0$dia"
                    }

                    var fecha = "$dia-$mes-$year"
                    ed_fecha_envio.setText(fecha)
                    unTicket.fecha_envio = fecha
                },
                year,
                month,
                day
            )

            dpd.show()
        }
    }

    fun setCalCobrado() {
        img_cobrado.setOnClickListener {

            var MONTHS = arrayOf(
                "Enero",
                "Febrero",
                "Marzo",
                "Abril",
                "Mayo",
                "Junio",
                "Julio",
                "Agosto",
                "Septiembre",
                "Octubre",
                "Noviembre",
                "Diciembre"
            )

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
                        mes = "0" + mes
                    }

                    var dia: String = (dayOfMonth).toString()
                    if (dayOfMonth < 10) {
                        dia = "0$dia"
                    }

                    var fecha = "$dia-$mes-$year"
                    ed_fecha_cobro.setText(fecha)
                    unTicket.fecha_cobro = fecha
                },
                year,
                month,
                day
            )

            dpd.show()
        }
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

        if (!TextUtils.isEmpty(unTicket.fecha_envio)) {
            ed_fecha_envio.setText(unTicket.fecha_envio)
        }
        // Rellenamos el Spinner de Metodos de Envio
        val adapterMetodos = ArrayAdapter.createFromResource(
            this,
            R.array.enviodietas,
            android.R.layout.simple_spinner_item
        )

        adapterMetodos.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner_metodos.adapter = adapterMetodos

        spinner_metodos.setSelection(unTicket.metodo_envio, false)
        comprueba_spinner_metodos()
        if (!TextUtils.isEmpty(unTicket.enviado_a)) {
            ed_enviado_a.setText(unTicket.enviado_a)
        }
        if (!TextUtils.isEmpty(unTicket.fecha_cobro)) {
            ed_fecha_cobro.setText(unTicket.fecha_cobro)
        }
        if (!TextUtils.isEmpty(unTicket.metodo_cobro)) {
            ed_metodo_crobro.setText(unTicket.metodo_cobro)

        }

    }

    /**
     * Configura las acciones a realizar tras cambiar de selección en el Spinner
     */
    fun comprueba_spinner_metodos() {
        if(spinner_metodos.selectedItemPosition>0){
            if(TextUtils.isEmpty(ed_fecha_envio.text.toString())){
                Toast.makeText(
                    this,
                    "Antes de seleccionar un método, introduza la fecha",
                    Toast.LENGTH_LONG
                ).show()
                spinner_metodos.setSelection(0,false)
                ed_enviado_a.visibility = View.GONE
                ed_enviado_a.setText("")
            }else{
                when (spinner_metodos.selectedItemPosition) {
                    0 -> {
                        ed_enviado_a.visibility = View.GONE
                        ed_enviado_a.setText("")
                    }
                    1 -> {
                        ed_enviado_a.setHint(getString(R.string.alemailde))
                        ed_enviado_a.visibility = View.VISIBLE
                        ed_enviado_a.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

                    }
                    2 -> {
                        ed_enviado_a.setHint(getString(R.string.alwhatsappde))
                        ed_enviado_a.visibility = View.VISIBLE
                        ed_enviado_a.inputType = InputType.TYPE_CLASS_PHONE
                    }
                    3 -> {
                        ed_enviado_a.setHint(getString(R.string.entregadoa))
                        ed_enviado_a.visibility = View.VISIBLE
                        ed_enviado_a.inputType = InputType.TYPE_CLASS_TEXT
                    }
                }

                unTicket.metodo_envio = spinner_metodos.selectedItemPosition
            }
        }else{
            ed_enviado_a.visibility = View.GONE
            ed_enviado_a.setText("")
        }

    }

    /**
     * Vuelve a nuevo ticket
     */
    fun cancelar(view: View) {
        val myIntent = Intent(this, NuevoTicket::class.java)
        setResult(Activity.RESULT_CANCELED, myIntent)
        finish()
    }

    /**
     * Guarda los valores en el objeto ticket y se lo devuelve a la activity que realizó la llamada
     */
    fun guardar(view: View) {
        if (validar()) {
            val myIntent = Intent(this, NuevoTicket::class.java).apply {
                putExtra("unTicket", unTicket)
            }
            setResult(Activity.RESULT_OK, myIntent)
            finish()
        }

    }

    /**
     * comprueba que los campos obligatoris enstén rellenados
     */
    fun validar(): Boolean {

        var validado = false


        // Sino indicas nada se guarda solo que es un ticket de dieta, sin pasar y sin cobrar
        if (TextUtils.isEmpty(ed_fecha_cobro.text.toString())
            && TextUtils.isEmpty(ed_fecha_envio.text.toString())
            && spinner_metodos.selectedItemPosition == 0
            && TextUtils.isEmpty(ed_enviado_a.text.toString())
            && TextUtils.isEmpty(ed_metodo_crobro.text.toString())){
            validado = true
        } else {
            validado= validaEnvio()
        }

        // Sino indicas cobro
        //   Si indicas envio
        //   sino indicas envio ni cobro

        return validado

    }

    fun validaEnvio(): Boolean {
        var resultado = true
        if (!TextUtils.isEmpty(ed_fecha_envio.text.toString())) {
            // Si hemos seleccionado un metodo de envío tenemos que especidifcar en el Edit text, a quién
            if (spinner_metodos.selectedItemPosition > 0) {
                if (TextUtils.isEmpty(ed_enviado_a.text.toString())) {
                    Toast.makeText(
                        this,
                        "Si indicas un método de envio debes especificar a quién",
                        Toast.LENGTH_LONG
                    ).show()
                    resultado = false
                } else {
                    unTicket.enviado_a = ed_enviado_a.text.toString().toUpperCase()
                    // hasta aquí si rellenas todos esos campos entonces se puede guardar
                    resultado = true
                    resultado = validaCobro()
                }
            } else {
                Toast.makeText(
                    this,
                    "Si indicas una fecha de entrega debes indicar un método de entrega",
                    Toast.LENGTH_LONG
                ).show()
                resultado = false
            }
        }else{
            // No se comprueba nada mas porque es el dato imprescincible para continuar
            Toast.makeText(
                this,
                "Debes especificar una fecha de envio",
                Toast.LENGTH_LONG
            ).show()
            resultado = false
        }
        return resultado

    }

    fun validaCobro(): Boolean {
        var resultado = true
        // Si hemos rellenado cobro tenemos que rellenar como lo hemos cobrado
        if (!TextUtils.isEmpty(ed_fecha_cobro.text.toString()) && !TextUtils.isEmpty(ed_fecha_envio.text.toString())) {
            if (TextUtils.isEmpty(ed_metodo_crobro.text.toString())) {
                Toast.makeText(
                    this,
                    "Si indicas una fecha de cobro debes indicar de que manera lo cobraste",
                    Toast.LENGTH_LONG
                ).show()
                // Pero si rellenas además la fecha de cobro entonces debes rellenar el metodo o no se podrá guardar
                resultado = false
            } else {
                unTicket.metodo_cobro = ed_metodo_crobro.text.toString().toUpperCase()
            }

        } else {
            // Sino rellenamos cobro nos aseguramos que no se halla rellenado como lo hemos cobrado
            if (!TextUtils.isEmpty(ed_metodo_crobro.text.toString())) {
                Toast.makeText(
                    this,
                    "Si indicas como lo cobraste debes indicar una fecha de cobro",
                    Toast.LENGTH_LONG
                ).show()
                resultado = false

            }
        }

        return resultado
    }

}

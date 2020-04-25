package es.leocaudete.mistickets

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
import es.leocaudete.mistickets.modelo.Ticket
import kotlinx.android.synthetic.main.activity_dietas.*
import java.util.*

class Dietas : AppCompatActivity() {

    var unTicket = Ticket()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietas)

        unTicket = intent.getSerializableExtra("Ticket") as Ticket
        iniciacampos()

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

    // Anulamos la opción de volver a tras a través del botón del móvil
    override fun onBackPressed() {
        //
    }

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
        if(!TextUtils.isEmpty(unTicket.enviado_a)){
            ed_enviado_a.setText(unTicket.enviado_a)
        }
        if (!TextUtils.isEmpty(unTicket.fecha_cobro)) {
            ed_fecha_cobro.setText(unTicket.fecha_cobro)
        }
        if(!TextUtils.isEmpty(unTicket.metodo_cobro)){
            ed_metodo_crobro.setText(unTicket.metodo_cobro)

        }

    }

    fun comprueba_spinner_metodos() {
        when (spinner_metodos.selectedItemPosition) {
            0 -> {
                ed_enviado_a.visibility = View.GONE
                ed_enviado_a.setText("")
            }
            1 -> {
                ed_enviado_a.setHint(getString(R.string.alemailde))
                ed_enviado_a.visibility = View.VISIBLE
                ed_enviado_a.inputType=InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

            }
            2 -> {
                ed_enviado_a.setHint(getString(R.string.alwhatsappde))
                ed_enviado_a.visibility = View.VISIBLE
                ed_enviado_a.inputType=InputType.TYPE_CLASS_PHONE
            }
            3 -> {
                ed_enviado_a.setHint(getString(R.string.entregadoa))
                ed_enviado_a.visibility = View.VISIBLE
                ed_enviado_a.inputType=InputType.TYPE_CLASS_TEXT
            }
        }
        unTicket.metodo_envio = spinner_metodos.selectedItemPosition
    }

    fun cancelar(view: View) {
        val myIntent = Intent(this, NuevoTicket::class.java)
        setResult(Activity.RESULT_CANCELED, myIntent)
        finish()
    }

    fun guardar(view: View) {
        if(validar()){
            val myIntent = Intent(this, NuevoTicket::class.java).apply {
                putExtra("unTicket", unTicket)
            }
            setResult(Activity.RESULT_OK, myIntent)
            finish()
        }

    }

    fun validar():Boolean {

        var validado=false
        if(!TextUtils.isEmpty(ed_fecha_envio.text.toString())){
            // Si hemos seleccionado un metodo de envío tenemos que especidifcar en el Edit text, a quién
            if (spinner_metodos.selectedItemPosition > 0) {
                if (TextUtils.isEmpty(ed_enviado_a.text.toString())) {
                    Toast.makeText(
                        this,
                        "Si indicas un método de envio debes especificar a quién",
                        Toast.LENGTH_LONG
                    ).show()
                }else{
                    unTicket.enviado_a = ed_enviado_a.text.toString()
                    // hasta aquí si rellenas todos esos campos entonces se puede guardar
                    validado=true
                    if(!TextUtils.isEmpty(ed_fecha_cobro.text.toString())){
                        if(TextUtils.isEmpty(ed_metodo_crobro.text.toString())){
                            Toast.makeText(
                                this,
                                "Si indicas una fecha de cobro debes indicar de que manera lo cobraste",
                                Toast.LENGTH_LONG
                            ).show()
                            // Pero si rellenas además la fecha de cobro entonces debes rellenar el metodo o no se podrá guardar
                            validado=false
                        }else{
                            unTicket.metodo_cobro = ed_metodo_crobro.text.toString()
                        }

                    }
                }
            }
            else{
                Toast.makeText(
                    this,
                    "Si indicas una fecha de entrega debes indicar un método de entrega",
                    Toast.LENGTH_LONG
                ).show()
            }
        }else{
            Toast.makeText(
                this,
                "Debes indicar una fecha de entrega",
                Toast.LENGTH_LONG
            ).show()
        }
        return validado


    }

}

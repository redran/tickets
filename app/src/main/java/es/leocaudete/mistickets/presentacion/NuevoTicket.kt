package es.leocaudete.mistickets.presentacion

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.negocio.TicketsNegocio
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import es.leocaudete.mistickets.utilidades.Utilidades
import kotlinx.android.synthetic.main.activity_nuevo_ticket.*
import java.io.File
import java.util.*


/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class NuevoTicket : AppCompatActivity() {

    var ticketsNegocio = TicketsNegocio(this)
    var utils = Utilidades()
    var gestorMensajes = ShowMessages()

    // Creamos una instancia de nuestro modelo para ir guardando los datos introducidos
    var unTicket = Ticket()
    // enEdicion indica si hemos entrado para editar o para insertar
    var enEdicion: Boolean = false
    // Indica la ruta local donde se guardaran las fotos
    lateinit var storageLocalDir: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nuevo_ticket)

        // le decimos lo que va ha ahacer el bton de calendarioo
        img_calendar.setOnClickListener {

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
                    text_fecha.setText(fecha)
                    unTicket.fecha_de_compra = fecha
                },
                year,
                month,
                day
            )

            dpd.show()
        }

        // Asignamos al nuevo ticket que hemos creado el id de usuario
        // y cargamos la ruta a las imagenes
        if (SharedApp.preferences.bdtype) {
            unTicket.idusuario = ticketsNegocio.getIdUsuarioFB()
            storageLocalDir = ticketsNegocio.rutaLocalFb()
        } else {
            unTicket.idusuario = SharedApp.preferences.usuario_logueado
            storageLocalDir = ticketsNegocio.rutaLocal(unTicket.idTicket)
        }

        // Inicializa los spinner y el resto de campos
        inicializaCampos()


        // Comprueba si ha recibido algo.
        // En caso de edición recibira el ticket para editar
        if (intent.getSerializableExtra("updateTicket") != null) {
            unTicket = intent.getSerializableExtra("updateTicket") as Ticket
            enEdicion = true
            rellenaCampos()
            btn_aceptar.text = getString(R.string.update)
            // Borra las fotos con la marca edited_ que se hubieran quedado en el directorio
            // al cerrar mal la aplicación mientras estabamos editando
            borraFotoTemporal()
        }
        // Asignamos al nuevo ticket que hemos creado el id de usuario
        // y cargamos la ruta a las imagenes
        if (SharedApp.preferences.bdtype) {
            unTicket.idusuario = ticketsNegocio.getIdUsuarioFB()
            storageLocalDir = ticketsNegocio.rutaLocalFb()
        } else {
            unTicket.idusuario = SharedApp.preferences.usuario_logueado
            storageLocalDir = ticketsNegocio.rutaLocal(unTicket.idTicket)
        }
        // Por si acaso todavía no exite, creamos el directorio local del ticket
        // Lo creamos aqui porque si estamos editando, me creará una nueva carpeta con id del nuevo ticket que se crea justo antes
        val home_dir = File(storageLocalDir)
        if (!home_dir.exists()) {
            home_dir.mkdirs()
        }


        // Cargamos nuestra toolbar.
        setSupportActionBar(toolbar)

    }


    /**
     * Habilitamos los campos para edicion y le ponemos el valor del ticket que hemos recibido
     */
    private fun rellenaCampos() {

        ed_descripcion.setText(unTicket.titulo)
        ed_tienda.setText(unTicket.establecimiento)

        text_fecha.setText(unTicket.fecha_de_compra)


        this.spinner_categorias.setSelection(unTicket.categoria, false)
        this.spinner_garantia.setSelection(unTicket.duracion_garantia, false)

        rd_annos.isChecked = unTicket.periodo_garantia == 0
        rd_meses.isChecked = unTicket.periodo_garantia == 1

        check_aviso.isChecked = unTicket.avisar_fin_garantia == 1

        etPrecio.setText(unTicket.precio.toString())

        if (unTicket.isdieta == 0) {
            cb_dietas.isChecked = false
            btn_dietas.visibility = View.GONE
        } else {
            cb_dietas.isChecked = true
            btn_dietas.visibility = View.VISIBLE
        }
    }

    /**
     * Rellenamos los Spinner y colocamos los valores por defecto
     */
    private fun inicializaCampos() {

        // por defecto seleccionamos Años en garantia
        rd_annos.isSelected = true


        // Rellenamos el Spinner de tiempo de garantía
        val adapterGarantia = ArrayAdapter.createFromResource(
            this,
            R.array.annos,
            android.R.layout.simple_spinner_item
        )

        adapterGarantia.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner_garantia.adapter = adapterGarantia


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
        spinner_categorias.adapter = adapterCategorias

        cb_dietas.isChecked = false
        btn_dietas.visibility = View.GONE


    }


    /**
     * Anulamos la opción de volver a tras a través del botón del móvil
     */
    override fun onBackPressed() {
        //
    }

    /**
     * Comprobamos la respuesta de las activity que hemos lanzado con startActivityForResult
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Hemos llamado al gestor de fotos
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                unTicket = data?.getSerializableExtra("unTicket") as Ticket
                enEdicion = data?.getBooleanExtra("updateTicket", false)
            }

        }
        // Hemos llamado al gestor de dietas
        if (requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                unTicket = data?.getSerializableExtra("unTicket") as Ticket
            }
        }
        // Hemos llamado al gestor de direcciones
        if (requestCode == 3) {
            if (resultCode == Activity.RESULT_OK) {
                unTicket = data?.getSerializableExtra("unTicket") as Ticket
            }
        }
    }

    /**
     * Inflamos nuestro menu
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.new_tickets_menu, menu)
        return true
    }

    /**
     * Definimos las acciones para los elementos del menu
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }
            R.id.gestionfotos -> {
                val myIntent = Intent(this, GestorFotos::class.java).apply {
                    putExtra("unTicket", unTicket)
                    if (enEdicion) {
                        putExtra("isEdited", true)
                    }


                }
                startActivityForResult(myIntent, 1)
                true
            }
            R.id.gestiondirecciones -> {
                val myIntent = Intent(this, Direccion::class.java).apply {
                    putExtra("Ticket", unTicket)

                }
                startActivityForResult(myIntent, 3)
                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }

    /**
     * Inserta un nuevo Ticket
     */
    fun insertar(view: View) {

        if (validacion()) {
            rellenaObjeto()
            //
            ticketsNegocio.renombraFotosTemp(unTicket, storageLocalDir)
            if (SharedApp.preferences.bdtype) {
                ticketsNegocio.subeFoto_A_Cloud(unTicket, ticketsNegocio.rutaLocalFb())
                ticketsNegocio.insertaTicketFb(unTicket, 1)
            } else {

                if (enEdicion) {
                    ticketsNegocio.updateTicketLocal(unTicket)
                } else {
                    ticketsNegocio.insertaTicketLocal(unTicket)
                }
            }
        }
    }

    /**
     * Rellenamos nuestra instancia del objeto Ticket con los campos que hallamos rellenado
     */
    fun rellenaObjeto() {


        unTicket.titulo = ed_descripcion.text.toString().toUpperCase()
        unTicket.establecimiento = ed_tienda.text.toString().toUpperCase()


        if (spinner_garantia.selectedItemPosition > 0) {
            unTicket.duracion_garantia = spinner_garantia.selectedItemPosition
        }
        if (rd_annos.isChecked) {
            unTicket.periodo_garantia = 0
        } else {
            unTicket.periodo_garantia = 1
        }

        if (check_aviso.isChecked) {
            unTicket.avisar_fin_garantia = 1
        } else {
            unTicket.avisar_fin_garantia = 0
        }

        unTicket.categoria = spinner_categorias.selectedItemPosition


        // Si estamos editando tenemos que cambiar la referencia a las fotos
        if (enEdicion) {
            if (unTicket.foto1.equals("edited_" + unTicket.idTicket + "_foto1.jpg")) {
                unTicket.foto1 = unTicket.idTicket + "_foto1.jpg"
            }
            if (unTicket.foto2.equals("edited_" + unTicket.idTicket + "_foto2.jpg")) {
                unTicket.foto2 = unTicket.idTicket + "_foto2.jpg"
            }
            if (unTicket.foto3.equals("edited_" + unTicket.idTicket + "_foto3.jpg")) {
                unTicket.foto3 = unTicket.idTicket + "_foto3.jpg"
            }
            if (unTicket.foto4.equals("edited_" + unTicket.idTicket + "_foto4.jpg")) {
                unTicket.foto4 = unTicket.idTicket + "_foto4.jpg"
            }
        }

        unTicket.fecha_modificacion = Timestamp.now()
            .seconds.toString() // Siempre guardamos la fecha de modificación para syncronizar

        // Si tiene algún valor en precio se pone sino por defecto al crear el objeto se pone 0.0
        if (!TextUtils.isEmpty(etPrecio.text)) {
            var precio = String.format("%.2f", etPrecio.text.toString().toDouble())
            var precioCambiado = precio.replace(',', '.').toDouble()
            unTicket.precio = precioCambiado
        }

        if (cb_dietas.isChecked) {
            unTicket.isdieta = 1
        } else {
            unTicket.isdieta = 0
        }
    }

    /**
     * Validamos que todos los campos obligatorios tengan datos y también que los que no son obligatios cumplan con una serie de criterios
     */
    fun validacion(): Boolean {
        var resultado: Boolean = true

        if (TextUtils.isEmpty(ed_descripcion.text)) {
            Toast.makeText(
                this,
                "Debes de poner una breve descripción para facilitar posteriormente las búsquedas",
                Toast.LENGTH_LONG
            ).show()
            resultado = false
        } else {
            if (TextUtils.isEmpty(ed_tienda.text)) {
                Toast.makeText(
                    this,
                    "Debes de poner el nombre del establecimiento para facilitar posteriormente las búsquedas",
                    Toast.LENGTH_LONG
                ).show()
                resultado = false
            } else {
                if (TextUtils.isEmpty(text_fecha.text)) {
                    Toast.makeText(
                        this,
                        "Selecciona una fecha pulsando el calendario",
                        Toast.LENGTH_LONG
                    ).show()
                    resultado = false
                } else {
                    if (check_aviso.isChecked && spinner_garantia.selectedItemPosition == 0) {
                        Toast.makeText(
                            this,
                            "Para activar el aviso de finalización de la garantía, tienes que especificar el tiempo de garantía",
                            Toast.LENGTH_LONG
                        ).show()
                        resultado = false
                    } else {
                        if (spinner_categorias.selectedItemPosition == 0) {
                            Toast.makeText(
                                this,
                                "Debes seleccionar una categoría.",
                                Toast.LENGTH_LONG
                            ).show()
                            resultado = false
                        } else {
                            if (TextUtils.isEmpty(etPrecio.text)) {
                                Toast.makeText(
                                    this,
                                    "Introduce el precio para poder usar otras características de la App.",
                                    Toast.LENGTH_LONG
                                ).show()
                                resultado = false
                            }
                        }
                    }
                }
            }

        }
        return resultado
    }



/**
 *  Cancela la inserccion y elimina las fotos que se hayan creado
 */
fun cancelarInsert(view: View) {

    // Si estamos insertando eliminadmos el directorio local creado y ya
    // porque el ticket esta en memoria y se borrara al salir de la activity
    if (!enEdicion || SharedApp.preferences.bdtype) {
        utils.delRecFileAndDir(storageLocalDir)
    } else {
        // si estamos editando o en Local, borramos las fotos marcadas como edited
        borraFotoTemporal()
    }

    startActivity(Intent(this, MainActivity::class.java))
    finish()

}

/**
 *
 * Elimina las fotos del directorio local donde se ha guardado temporalmente
 */
fun borraFotoTemporal() {

    for (i in 1..4) {
        // Comprobamos si existe en local y lo eliminamos
        if (File(storageLocalDir + "/" + "edited_" + unTicket.idTicket + "_foto" + i + ".jpg").exists()) {
            File(storageLocalDir + "/" + "edited_" + unTicket.idTicket + "_foto" + i + ".jpg").delete()
        }
    }
}

/**
 * Comprueba si se ha marcado dietas o no
 */
fun isDietas(view: View) {
    if (cb_dietas.isChecked) {
        btn_dietas.visibility = View.VISIBLE
    } else {
        btn_dietas.visibility = View.GONE
        // Borramos todos los datos que tuviera
        unTicket.fecha_envio=null
        unTicket.metodo_envio=0
        unTicket.enviado_a=null
        unTicket.fecha_cobro=null
        unTicket.metodo_cobro=null
    }
}

/**
 * Abre la activity Dietas y le pasa el ticket actual
 */
fun gestdietas(view: View) {
    val myIntent = Intent(this, Dietas::class.java).apply {
        putExtra("Ticket", unTicket)

    }
    startActivityForResult(myIntent, 2)
}




}


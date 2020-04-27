package es.leocaudete.mistickets.presentacion

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.negocio.TicketsNegocio
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_visor_ticket.*

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class VisorTicket : AppCompatActivity() {

    var ticketsNegocio = TicketsNegocio(this)

    lateinit var paramTicket: Ticket
    lateinit var storageDir: String
    var gestorMensajes = ShowMessages()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visor_ticket)

        // Cargamos nuestra toolbar.
        setSupportActionBar(visorTicketsToolBar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        paramTicket = intent.getSerializableExtra("TicketVisor") as Ticket
        storageDir = ticketsNegocio.rutaLocal(paramTicket.idTicket)
        cargaImgPortada()
        cargarCampos()


    }

    /**
     * Carga la imagen de portada y descarga todas la imagenes en threads distintos para adelantar tiempo
     */
    fun cargaImgPortada() {

        if (SharedApp.preferences.bdtype) {
            // Descarga todas las imagenes del ticket en la rutaLocal
            ticketsNegocio.descargaFotos(paramTicket, ticketsNegocio.rutaLocalFb())
            if (TextUtils.isEmpty(paramTicket.foto1) || paramTicket.foto1 == null) {
                imgPortada.setBackgroundResource(R.drawable.common_google_signin_btn_icon_light)
            } else {
                ticketsNegocio.cargaFoto(paramTicket.foto1, imgPortada)
            }
        } else {
            //lo hacemos desde local
            imgPortada.setImageBitmap(BitmapFactory.decodeFile(storageDir + "/" + paramTicket.foto1))
        }
    }

    /**
     * inflamos nuestro menu
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.visor_tickets, menu)
        return true
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
            R.id.borrar -> {
                if (SharedApp.preferences.modooperacion == 1) {
                    gestorMensajes.showAlertOneButton(
                        "ALERTA",
                        "La app está en modo solo lectura y no se permite borrar datos",
                        this
                    )
                } else {
                    borrarTicket()
                }

                true
            }
            R.id.editar -> {
                if (SharedApp.preferences.modooperacion == 1) {
                    gestorMensajes.showAlertOneButton(
                        "ALERTA",
                        "La app está en modo solo lectura y no se permite la edición",
                        this
                    )
                } else {
                    var intent = Intent(this, NuevoTicket::class.java).apply {
                        putExtra("updateTicket", paramTicket)
                    }
                    startActivityForResult(intent, 1)
                }

                true
            }

            else -> super.onOptionsItemSelected(item)

        }
    }

    /**
     * Anulamos la opción de volver a tras a través del botón del móvil
     */
    override fun onBackPressed() {
        //
    }

    /**
     * Borra el ticket seleccionado de firebsae
     * Borra las fotos del dispositivo
     * Borra las fotos de Firebase Store
     */
    fun borrarTicket() {
        if (SharedApp.preferences.bdtype) {
            ticketsNegocio.borraTicket(paramTicket)
        } else {
            ticketsNegocio.borraTicketLocal(paramTicket.idTicket)
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * CArga los datos en los campos
     */
    fun cargarCampos() {
        tvTienda.text = paramTicket.establecimiento
        tvDescripcion.text = paramTicket.titulo
        tvFecha.text = paramTicket.fecha_de_compra

        tvUbicacion.text = devuelveUbicacion()
        if (paramTicket.direccion != null) {
            tvDireccion.text = paramTicket.direccion
        } else {
            tvDireccion.text = "No especificada"
        }

        tvCategoria.text = devuelveCategoria()
        tvGarantia.text = devuelveGarantia()

        tvPrecio.text = paramTicket.precio.toString() + " €"
    }

    /**
     * Abre la activity que permite visualizar las imagenes de los tickets
     */
    fun visorFoto(view: View) {
        val intent = Intent(this, VisorFotos::class.java).apply {
            putExtra("unTicket", paramTicket)

        }
        startActivity(intent)
        finish()
    }

    /**
     * Monta un string con las opciones de garantía
     */
    fun devuelveGarantia(): String {
        var garantia: String

        val arrayGarantias = resources.getStringArray(R.array.annos)

        garantia = if (paramTicket.duracion_garantia == 0) {
            "No especificada"
        } else {
            if (paramTicket.periodo_garantia == 0) {
                arrayGarantias[paramTicket.duracion_garantia] + " Años"
            } else {
                arrayGarantias[paramTicket.duracion_garantia] + " Meses"
            }
        }
        return garantia
    }

    /**
     * Devuelve un string con la ubicacion
     */
    fun devuelveUbicacion(): String {
        var ubicacion = "No Especificada"
        val arrayProvincias = resources.getStringArray(R.array.provincias)

        if (paramTicket.provincia > 0) {
            if (paramTicket.localidad != null) {
                ubicacion = arrayProvincias[paramTicket.provincia] + "/" + paramTicket.localidad
            } else {
                ubicacion = arrayProvincias[paramTicket.provincia]
            }

        }

        return ubicacion
    }

    /**
     * Devuelve un String con la Categoría
     */
    fun devuelveCategoria(): String {
        val arrayCategorias = resources.getStringArray(R.array.categorias)
        return arrayCategorias[paramTicket.categoria]
    }
}

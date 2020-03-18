package es.leocaudete.mistickets

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
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_nuevo_ticket.*
import java.io.File
import java.util.*


/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class NuevoTicket : AppCompatActivity() {

    // Creamos una instancia de nuestro modelo para ir guardando los datos introducidos
    var unTicket= Ticket()
    var enEdicion:Boolean=false // Nos indica si hemos entrado para editar o para insertar
    private lateinit var auth: FirebaseAuth
    var gestorMensajes=ShowMessages()

    lateinit var storageLocalDir: String
    lateinit var dbSQL: SQLiteDB


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nuevo_ticket)



        // le decimos lo que va ha ahacer el bton de calendarioo
        img_calendar.setOnClickListener {

            var MONTHS = arrayOf("Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre")

            val calendar = Calendar.getInstance()
            val day=calendar.get(Calendar.DAY_OF_MONTH)
            val month=calendar.get(Calendar.MONTH)
            val year=calendar.get(Calendar.YEAR)

            val dpd = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

                // Display Selected date in textbox
                var mes:String=(monthOfYear+1).toString()
                if((monthOfYear+1)<10){
                    mes="0"+mes
                }

                var dia:String=(dayOfMonth).toString()
                if(dayOfMonth<10){
                    dia="0$dia"
                }

                var fecha= "$dia-$mes-$year"
                text_fecha.setText(fecha)
                unTicket.fecha_de_compra= fecha
            }, year, month, day)

            dpd.show()
        }

        // Asociamos el ticket con el usuario
        auth=FirebaseAuth.getInstance()

        if(SharedApp.preferences.bdtype){
            unTicket.idusuario= auth.currentUser?.uid.toString()
        }else{
            unTicket.idusuario= SharedApp.preferences.usuario_logueado
        }





        inicializaCampos()

        if(intent.getSerializableExtra("updateTicket")!=null){
            unTicket=intent.getSerializableExtra("updateTicket")as Ticket
            enEdicion=true
            rellenaCampos()
            btn_aceptar.text=getString(R.string.update)

        }

        // Ruta de acceso al directorio local donde se guardan las imagenes
        if(SharedApp.preferences.bdtype){
            storageLocalDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + auth.currentUser?.uid.toString()
        }else{
            storageLocalDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + SharedApp.preferences.usuario_logueado + "/" + unTicket.idTicket
        }
        val home_dir = File(storageLocalDir)
        if (!home_dir.exists()) {
            home_dir.mkdirs()
        }

        // Cargamos nuestra toolbar.
        setSupportActionBar(toolbar)

        // Instanciamos la clase que crea la base de datos y tiene nuestro CRUD
        dbSQL = SQLiteDB(this, null)

    }


    // habilitamos los campos para edicion y le ponemos el valor de el ticket que hemos recibido
    private fun rellenaCampos(){
        // Rellenamos nuestra instancia del objeto Ticket con los campos que hallamos rellenado

        ed_descripcion.setText(unTicket.titulo)
        ed_tienda.setText(unTicket.establecimiento)
        ed_direccion.setText(unTicket.direccion)

        ed_localidades.setText(unTicket.localidad)
        text_fecha.setText( unTicket.fecha_de_compra)

        this.spinner_provincias.setSelection(unTicket.provincia,false)
        this.spinner_garantia.setSelection(unTicket.duracion_garantia,false)

        rd_annos.isChecked = unTicket.periodo_garantia==0
        rd_meses.isChecked = unTicket.periodo_garantia==1

        check_aviso.isChecked = unTicket.avisar_fin_garantia==1



    }
    // Rellenamos los Spinner y colocamos los valores por defecto
    private fun inicializaCampos(){

        // por defecto seleccionamos Años en garantia
        rd_annos.isSelected = true

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
    }

    // Anulamos la opción de volver a tras a través del botón del móvil
    override fun onBackPressed() {
        //
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==1){
            if(resultCode== Activity.RESULT_OK){
                unTicket=data?.getSerializableExtra("unTicket") as Ticket
                enEdicion= data?.getBooleanExtra("updateTicket", false)

            }

        }
    }
    // inflamos nuestro menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater=menuInflater
        inflater.inflate(R.menu.new_tickets_menu, menu)
        return true
    }

    // definimos las acciones para los elementos del menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            android.R.id.home ->{
                startActivity(Intent(this,MainActivity::class.java))
                finish()
                true
            }
            R.id.gestionfotos->{
                val myIntent=Intent(this,GestorFotos::class.java).apply{
                    putExtra("unTicket", unTicket)
                    if(enEdicion){
                        putExtra("isEdited", true)
                    }


                }
                startActivityForResult(myIntent, 1)
                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }

    fun insertar(view: View) {

        if(validacion())
        {
            rellenaObjeto()
            if(SharedApp.preferences.bdtype){
                /**
                 * Con firebase se le pasa un ticket
                 * Si es Nuevo habra creado un id_ticket al pulsar sobre nuevoTicket
                 * Si es Aatualizacion se pasa elk ticket con el mismo id y lo chafa
                 */
                val dbRef=FirebaseFirestore.getInstance()
                grabaFoto(1,unTicket.foto1,false)
                grabaFoto(2,unTicket.foto2,false)
                grabaFoto(3,unTicket.foto3,false)
                grabaFoto(4,unTicket.foto4,false)
                val ticketRef = dbRef.collection("User").document(unTicket.idusuario)
                    .collection("Tickets").document(unTicket.idTicket)
                    .set(unTicket)
                    .addOnSuccessListener {
                        startActivity(Intent(this,MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener{ Toast.makeText(this,"No se ha podido registrar el Ticket", Toast.LENGTH_LONG).show()}
            }else{
                /**
                 * Si por el contrario trabajamos co SQLite, al ser una base relacional, tenemos que distinguir y hacer Insert o Update
                 */
                grabaFoto(1,unTicket.foto1,true)
                grabaFoto(2,unTicket.foto2,true)
                grabaFoto(3,unTicket.foto3,true)
                grabaFoto(4,unTicket.foto4,true)
                if(enEdicion){
                    if(dbSQL.updateTicket(unTicket)>0){
                        startActivity(Intent(this,MainActivity::class.java))
                    }else{
                        gestorMensajes.showAlertOneButton("ERROR","Error al actualizar el ticket", this)
                    }
                }else{
                    if(dbSQL.addTicket(unTicket)>0){
                        startActivity(Intent(this,MainActivity::class.java))
                    }else{
                        gestorMensajes.showAlertOneButton("ERROR","Error al insertar el ticket", this)
                    }
                }


            }


        }
    }


    // Graba foto
    fun grabaFoto(numFoto: Int, fotoTicket: String?, local:Boolean){

        // Para ambos casos tenemos que verificar si es edicion para renombrar

        var strFoto:String=unTicket.idTicket + "_foto"+numFoto+".jpg"

        if(enEdicion){
            // Si estamos editando entonces renombramos primero los jpg
            var file=File(storageLocalDir + "/" + "edited_" + unTicket.idTicket + "_foto"+numFoto+".jpg")
            if(file.exists()){
                file.renameTo(File("$storageLocalDir/$strFoto"))
            }

        }

        // Guardamos en la nube, en SQLite se guarda el fichero nada mas realizar la foto
        if(fotoTicket!=null)
        {
            if(!local){
                var storageRef = FirebaseStorage.getInstance().reference
                var riverRef=storageRef.child(auth.currentUser?.uid.toString()+"/" + unTicket.idTicket + "_foto"+numFoto+".jpg")

                // var uri=Uri.parse(storageLocalDir + "/" + unTicket.idTicket + "_foto"+numFoto+".jpg")
                var uri=Uri.fromFile(File("$storageLocalDir/$strFoto"))
                var uploadTask = riverRef.putFile(uri)
            }


        }


    }

    // Rellenamos nuestra instancia del objeto Ticket con los campos que hallamos rellenado
    fun rellenaObjeto(){




        unTicket.titulo=ed_descripcion.text.toString().toUpperCase()
        unTicket.establecimiento=ed_tienda.text.toString().toUpperCase()
        if(!TextUtils.isEmpty(ed_direccion.text)){
            unTicket.direccion=ed_direccion.text.toString().toUpperCase()

        }
        if(spinner_provincias.selectedItemPosition>0){
            unTicket.provincia=spinner_provincias.selectedItemPosition
        }
        if(!TextUtils.isEmpty(ed_localidades.text)){
            unTicket.localidad=ed_localidades.text.toString().toUpperCase()
        }
        if(spinner_garantia.selectedItemPosition>0){
            unTicket.duracion_garantia=spinner_garantia.selectedItemPosition
        }
        if(rd_annos.isChecked){
            unTicket.periodo_garantia=0
        }
        else{
            unTicket.periodo_garantia=1
        }

        if(check_aviso.isChecked){
            unTicket.avisar_fin_garantia = 1
        }else
        {
            unTicket.avisar_fin_garantia = 0
        }


        // Si estamos editando tenemos que cambiar la referencia a las fotos
        if(enEdicion){
            if(unTicket.foto1.equals("edited_" + unTicket.idTicket + "_foto1.jpg")){
               unTicket.foto1=unTicket.idTicket + "_foto1.jpg"
            }
            if(unTicket.foto2.equals("edited_" + unTicket.idTicket + "_foto2.jpg")){
                unTicket.foto2=unTicket.idTicket + "_foto2.jpg"
            }
            if(unTicket.foto3.equals("edited_" + unTicket.idTicket + "_foto3.jpg")){
                unTicket.foto3=unTicket.idTicket + "_foto3.jpg"
            }
            if(unTicket.foto4.equals("edited_" + unTicket.idTicket + "_foto4.jpg")){
                unTicket.foto4=unTicket.idTicket + "_foto4.jpg"
            }
        }

        unTicket.fecha_modificacion= Timestamp.now().seconds.toString() // Siempre guardamos la fecha de modificación para syncronizar


    }

    // Validamos que todos los campos obligatorios tengan datos y también que los que no son obligatios cumplan con una serie de criterios
    fun validacion():Boolean{
        var resultado:Boolean=true

        if(TextUtils.isEmpty(ed_descripcion.text)){
            Toast.makeText(this,"Debes de poner una breve descripción para facilitar posteriormente las búsquedas",Toast.LENGTH_LONG).show()
            resultado=false
        }
        else{
            if(TextUtils.isEmpty(ed_tienda.text)){
                Toast.makeText(this, "Debes de poner el nombre del establecimiento para facilitar posteriormente las búsquedas", Toast.LENGTH_LONG).show()
                resultado=false
            }
            else{
                if(TextUtils.isEmpty(text_fecha.text)){
                    Toast.makeText(this, "Selecciona una fecha pulsando el calendario", Toast.LENGTH_LONG).show()
                    resultado=false
                }
                else{
                    if(check_aviso.isChecked && spinner_garantia.selectedItemPosition==0){
                        Toast.makeText(this, "Para activar el aviso de finalización de la garantía, tienes que especificar el tiempo de garantía", Toast.LENGTH_LONG).show()
                        resultado=false
                    }
                    else
                    {
                        if(!TextUtils.isEmpty(ed_localidades.text) && spinner_provincias.selectedItemPosition==0){
                            Toast.makeText(this, "Si especificas una localidad tienes que seleccionar una provncia", Toast.LENGTH_LONG).show()
                            resultado=false
                        }
                    }
                }

            }

        }


        return resultado
    }

    /*
    Cacela la inserccion y elimina las fotos que se hayan creado
     */
    fun cancelarInsert(view: View) {

        // Si estamo sinsertando eliminado el directorio local creado y ya
        // porque el ticket esta en memoria y se borrara al salir de la activity
        if(!enEdicion){
            dbSQL.utilidades.delRecFileAndDir(storageLocalDir)
        }

        startActivity(Intent(this,MainActivity::class.java))
        finish()

    }

    /*
    Elimina las fotos del directorio local donde se ha guardado temporalmente
     */
    fun borraFoto(numFoto:Int){

        if(enEdicion){
            // Comprobamos si existe en local y lo eliminamos
            if(File(storageLocalDir + "/" + "edited_" + unTicket.idTicket + "_foto"+numFoto+".jpg").exists())
            {
                File(storageLocalDir + "/" + "edited_" + unTicket.idTicket + "_foto"+numFoto+".jpg").delete()
            }
        }else{
            // Comprobamos si existe en local y lo eliminamos
            if(File(storageLocalDir + "/" + unTicket.idTicket + "_foto"+numFoto+".jpg").exists())
            {
                File(storageLocalDir + "/" + unTicket.idTicket + "_foto"+numFoto+".jpg").delete()
            }
        }


    }

}


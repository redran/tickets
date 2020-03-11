package es.leocaudete.mistickets.dao

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.util.Log
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.modelo.Usuario
import es.leocaudete.mistickets.utilidades.Utilidades
import java.io.File

/**
 * Esta clase se encargara de enlazar y tratar con la base de datos local SQLite
 */
class SQLiteDB(context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    val context=context
    lateinit var storageDir: String
    var utilidades= Utilidades()

    companion object {
        val DATABASE_VERSION = 1
        val DATABASE_NAME = "MisTickets.db"

        /** Nombre de la tabla **/
        val TABLA_USUARIO="usuarios"

        /** Nombre de los campos **/
        val ID_USUARIO="_id_usuario"
        val ID_USARIO_FIREBASE="_id_usuario_firebase"
        val EMAIL_USUARIO="email"
        val PASSWORD_USUARIO="password"
        val NOMBRE_USUARIO="nombre"
        val APELLIDOS_USUARIO="apellidos"
        val PIN_USUARIO="pin_de_seguriad"

        /** Nombre de la tabla **/
        val TABLA_TICKETS="tickets"

        /** Nombre de los campos **/
        val ID_TICKET="id_ticket"
        val USUARIO_TICKET="id_usuario_vinculado"
        val DESCRIPCION_COMPRA="titulo"
        val ESTABLECIMIENTO="establecimiento"
        val DIRECCION="direccion"
        val LOCALIDAD="localidad"
        val PROVINCIA="provincia"
        val FECHA_COMPRA="fecha_de_compra"
        val FOTO1="foto1"
        val FOTO2="foto2"
        val FOTO3="foto3"
        val FOTO4="foto4"
        val DURACION_GARANTIA="duracion_garantia"
        val PERIODO_GARANTIA="periodo_garantia"
        val AVISAR_FIN_GARANTIA="avisar_fin_garantia"
        val FECHA_MODIFICACION="fecha_modificacion"

    }

    // Cea las tablas de la base de datos si no existen
    override fun onCreate(db: SQLiteDatabase?) {
        try{
            val createTableUser = "CREATE TABLE ${TABLA_USUARIO}  (${ID_USUARIO} TEXT PRIMARY KEY, " +
                                                                "${ID_USARIO_FIREBASE} TEXT," +
                                                                "${EMAIL_USUARIO} TEXT NOT NULL UNIQUE, " +
                                                                "${PASSWORD_USUARIO} TEXT NOT NULL, " +
                                                                "${NOMBRE_USUARIO} TEXT, " +
                                                                "${APELLIDOS_USUARIO} TEXT," +
                                                                "${PIN_USUARIO} TEXT)"
            db!!.execSQL(createTableUser)

            val createTableTicket = "CREATE TABLE ${TABLA_TICKETS} (${ID_TICKET} TEXT PRIMARY KEY, " +
                                                                 "${USUARIO_TICKET} TEXT NOT NULL, " +
                                                                 "${DESCRIPCION_COMPRA} TEXT, " +
                                                                 "${ESTABLECIMIENTO} TEXT, " +
                                                                 "${DIRECCION} TEXT, " +
                                                                 "${FECHA_COMPRA} TEXT, " +
                                                                 "${PROVINCIA} INTEGER, " +
                                                                 "${LOCALIDAD} TEXT, " +
                                                                 "${DURACION_GARANTIA} INTEGER, " +
                                                                 "${PERIODO_GARANTIA} INTEGER, " +
                                                                 "${AVISAR_FIN_GARANTIA} INTEGER, " +
                                                                 "${FOTO1} TEXT, " +
                                                                 "${FOTO2} TEXT, " +
                                                                 "${FOTO3} TEXT, " +
                                                                 "${FOTO4} TEXT, " +
                                                                 "${FECHA_MODIFICACION} TEXT )"
            db!!.execSQL(createTableTicket)
        }catch (e: SQLiteException){
            Log.e("SQLite(OnCreate)", e.message.toString())
        }
    }

    // Actualiza el esquema de la base de datos
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        Log.d("onOpen","Database opened!!")
    }

    fun addUser(usuario: Usuario):Long{

        // Si no se produce el insert devuelve -1
        var resultado:Long=-1

        // Creamos un ArrayMap<>()
        val data  = ContentValues()
        data.put(ID_USUARIO, usuario.id_usuario)
        data.put(ID_USARIO_FIREBASE,usuario.id_usuario_firebase)
        data.put(EMAIL_USUARIO,usuario.email)
        data.put(PASSWORD_USUARIO,usuario.password)
        data.put(NOMBRE_USUARIO,usuario.nombre)
        data.put(APELLIDOS_USUARIO,usuario.apellidos)
        data.put(PIN_USUARIO, usuario.pin_de_seguridad)

        //Abrimos la BD en modo escritura
        val db = this.writableDatabase
        resultado=db.insert(TABLA_USUARIO,null,data)
        db.close()

        // Una vez añadimos el usuario, creamos su directorio local para almacenar sus fotos

        if(resultado>=0){
            storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + usuario.id_usuario
            val home_dir = File(storageDir)
            if (!home_dir.exists()) {
                home_dir.mkdirs()
            }
        }

        return resultado

    }

    fun addTicket(ticket:Ticket){

        val data = ContentValues()
        data.put(ID_TICKET, ticket.idTicket)
        data.put(USUARIO_TICKET, ticket.idusuario)
        data.put(DESCRIPCION_COMPRA, ticket.titulo)
        data.put(ESTABLECIMIENTO,ticket.establecimiento)
        data.put(FECHA_COMPRA, ticket.fecha_de_compra)
        data.put(DIRECCION,ticket.direccion)
        data.put(PROVINCIA,ticket.provincia)
        data.put(LOCALIDAD,ticket.localidad)
        data.put(DURACION_GARANTIA,ticket.duracion_garantia)
        data.put(PERIODO_GARANTIA,ticket.periodo_garantia)
        data.put(AVISAR_FIN_GARANTIA, ticket.avisar_fin_garantia)
        data.put(FOTO1, ticket.foto1)
        data.put(FOTO2, ticket.foto2)
        data.put(FOTO3, ticket.foto3)
        data.put(FOTO4, ticket.foto4)
        data.put(FECHA_MODIFICACION, ticket.fecha_modificacion)

        //Abrimos la BD en modo escritura
        val db = this.writableDatabase
        db.insert(TABLA_TICKETS,null,data)
        db.close()

    }

    fun deleteUsuario(id_usuario:Int){


        val args = arrayOf(id_usuario)

        val db=this.writableDatabase

        db.execSQL("DELETE FROM $TABLA_USUARIO WHERE $ID_USUARIO=?", args)
        db.close()

        // Borramos recursivamente la carpeta del almacenamiento interno que tiene como nombre el id de Usuario
        utilidades.delRecFileAndDir(storageDir)

    }

    fun deleteTicket(id_ticket:Int){

        val args = arrayOf(id_ticket)
        val db=this.writableDatabase

        db.execSQL("DELETE FROM $TABLA_TICKETS WHERE $ID_TICKET=?", args)
        db.close()

        // Le pasamos la ruta de la carpeta del ticket, que contiene todas las fotos de sus tickets
        // y borramos las foto y la carpeta
        utilidades.delRecFileAndDir("$storageDir/$id_ticket")
    }

    fun updateTicket(id_ticket:Int, ticket:Ticket){

        val args = arrayOf(id_ticket.toString())

        val data = ContentValues()
        data.put(DESCRIPCION_COMPRA, ticket.titulo)
        data.put(ESTABLECIMIENTO,ticket.establecimiento)
        data.put(FECHA_COMPRA, ticket.fecha_de_compra)
        data.put(DIRECCION,ticket.direccion)
        data.put(PROVINCIA,ticket.provincia)
        data.put(LOCALIDAD,ticket.localidad)
        data.put(DURACION_GARANTIA,ticket.duracion_garantia)
        data.put(PERIODO_GARANTIA,ticket.periodo_garantia)
        data.put(AVISAR_FIN_GARANTIA, ticket.avisar_fin_garantia)
        data.put(FOTO1, ticket.foto1)
        data.put(FOTO2, ticket.foto2)
        data.put(FOTO3, ticket.foto3)
        data.put(FOTO4, ticket.foto4)
        data.put(FECHA_MODIFICACION, ticket.fecha_modificacion)

        val db=this.writableDatabase
        db.update(TABLA_TICKETS, data, "$ID_TICKET=?", args)
        db.close()
    }

    /**
     * valida si un usuario existe
     */
    fun buscaUsuario(usuario:String):Boolean{

        var resultado=false

        val args= arrayOf(usuario)
        val db=this.readableDatabase

        val cursor = db.rawQuery(
            " SELECT ${ID_USUARIO} FROM ${TABLA_USUARIO} WHERE ${EMAIL_USUARIO}=?",
            args
        )

        if(cursor.moveToFirst())
        {
            resultado=true
        }
        return resultado
    }

    /**
     * valida si una contraseña existe
     */
    fun validaPassword(email: String, password:String):Boolean{

        var resultado=false

        val args= arrayOf(password,email)
        val db=this.readableDatabase

        val cursor = db.rawQuery(
            " SELECT * FROM ${TABLA_USUARIO} WHERE ${PASSWORD_USUARIO}=? AND ${EMAIL_USUARIO}=?",
            args
        )

        if(cursor.moveToFirst())
        {
            resultado=true
        }
        return resultado
    }

    /**
     * Este metodo devuelve la contraseña para mostraseña en caso de haber olvidado la contraseña
     */
    fun passUsuario(usuario: String):String{

        var resultado:String=""

        val args= arrayOf(usuario)
        val db=this.readableDatabase

        val cursor = db.rawQuery(
            " SELECT ${PASSWORD_USUARIO} FROM ${TABLA_USUARIO} WHERE ${EMAIL_USUARIO}=?",
            args
        )

        if(cursor.moveToFirst())
        {
            resultado=cursor.getString(0)
        }

        return resultado
    }

    /**
     * Busca un usuario por su email y devuelve su id
     */
    fun buscaIdUsuario(email: String):String{

        var resultado:String=""

        val args= arrayOf(email)
        val db=this.readableDatabase

        val cursor = db.rawQuery(
            " SELECT ${ID_USUARIO} FROM ${TABLA_USUARIO} WHERE ${EMAIL_USUARIO}=?",
            args
        )

        if(cursor.moveToFirst())
        {
            resultado=cursor.getString(0)
        }

        return resultado
    }

    /**
     * valida si el PIN introducido es correcto
     */
   fun validaPin(pin: Int): String{
        var resultado=""

        //val args= arrayOf(pin.toString())
        val db=this.readableDatabase

       val columnas = arrayOf(PASSWORD_USUARIO)
       val whereColumns = "${PIN_USUARIO}=?"
       val argsWhere = arrayOf(pin.toString())


        val cursor = db.query(
            //" SELECT ${PASSWORD_USUARIO} FROM ${TABLA_USUARIO} WHERE ${PIN_USUARIO}='" + pin +"'",
            TABLA_USUARIO, // The table to query
            columnas,
            whereColumns,
            argsWhere,
            null,
            null,
            null
        )

        if(cursor.moveToFirst())
        {
            resultado=cursor.getString(0)
        }
       db.close()

        return resultado
    }



}
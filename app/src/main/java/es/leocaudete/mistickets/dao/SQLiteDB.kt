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
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.Utilidades

/**
 * Esta clase se encargara de enlazar y tratar con la base de datos local SQLite
 */
class SQLiteDB(context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    val context = context
    var utilidades = Utilidades()

    companion object {
        val DATABASE_VERSION = 3
        val DATABASE_NAME = "MisTickets.db"

        /** Nombre de la tabla **/
        val TABLA_USUARIO = "usuarios"

        /** Nombre de los campos **/
        val ID_USUARIO = "_id_usuario"
        val ID_USARIO_FIREBASE = "_id_usuario_firebase"
        val EMAIL_USUARIO = "email"
        val PASSWORD_USUARIO = "password"
        val NOMBRE_USUARIO = "nombre"
        val APELLIDOS_USUARIO = "apellidos"
        val PIN_USUARIO = "pin_de_seguriad"

        /** Nombre de la tabla **/
        val TABLA_TICKETS = "tickets"

        /** Nombre de los campos **/
        val ID_TICKET = "id_ticket"
        val USUARIO_TICKET = "id_usuario_vinculado"
        val DESCRIPCION_COMPRA = "titulo"
        val ESTABLECIMIENTO = "establecimiento"
        val DIRECCION = "direccion"
        val LOCALIDAD = "localidad"
        val PROVINCIA = "provincia"
        val FECHA_COMPRA = "fecha_de_compra"
        val FOTO1 = "foto1"
        val FOTO2 = "foto2"
        val FOTO3 = "foto3"
        val FOTO4 = "foto4"
        val DURACION_GARANTIA = "duracion_garantia"
        val PERIODO_GARANTIA = "periodo_garantia"
        val AVISAR_FIN_GARANTIA = "avisar_fin_garantia"
        val FECHA_MODIFICACION = "fecha_modificacion"

        // Updates 11-04-2020
        val CATEGORIA = "categoria"
        val PRECIO = "precio"

        // Update 21-04-2020
        val ISDIETA = "isdieta"
        val FECHA_ENVIO = "fecha_envio"
        val METODO_ENVIO = "metodo_envio"
        val ENVIADO_A = "enviado_a"
        val FECHA_COBRO = "fecha_cobro"
        val METODO_COBRO = "metodo_cobro"


    }

    // Cea las tablas de la base de datos si no existen
    override fun onCreate(db: SQLiteDatabase?) {

        try {
            val createTableUser =
                "CREATE TABLE ${TABLA_USUARIO}  (${ID_USUARIO} TEXT PRIMARY KEY, " +
                        "${ID_USARIO_FIREBASE} TEXT," +
                        "${EMAIL_USUARIO} TEXT NOT NULL UNIQUE, " +
                        "${PASSWORD_USUARIO} TEXT NOT NULL, " +
                        "${NOMBRE_USUARIO} TEXT, " +
                        "${APELLIDOS_USUARIO} TEXT," +
                        "${PIN_USUARIO} TEXT)"
            db!!.execSQL(createTableUser)

            val createTableTicket =
                "CREATE TABLE ${TABLA_TICKETS} (${ID_TICKET} TEXT PRIMARY KEY, " +
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
                        "${FECHA_MODIFICACION} TEXT, " +
                        // Estos campos se añaden para los que hagan la instalación nueva desde la version 2
                        // Los que ya tengan instalada la app con la version 1 haran el Upgrade
                        "${CATEGORIA} INTEGER DEFAULT 1, " +
                        "${PRECIO} REAL DEFAULT 0.0, " +
                        // Estos campos se añaden para los que hagan la instalación nueva desde la version 3
                        // Los que ya tengan instalada la app con la version 1 u 2 haran el Upgrade
                        "${ISDIETA} INTEGER DEFAULT 0, " +
                        "${FECHA_ENVIO} TEXT, " +
                        "${METODO_ENVIO} INTEGER DEFAULT 0, " +
                        "${ENVIADO_A} TEXT, " +
                        "${FECHA_COBRO} TEXT, " +
                        "${METODO_COBRO} TEXT )"

            db!!.execSQL(createTableTicket)
        } catch (e: SQLiteException) {
            Log.e("SQLite(OnCreate)", e.message.toString())
        }
    }

    // Actualiza el esquema de la base de datos
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // ACtualiza el esquema de la tabla si la versión de SQLite cambia
        if (newVersion > oldVersion) {
            when (newVersion) {
                2 -> {
                    db.execSQL("ALTER TABLE ${TABLA_TICKETS} ADD COLUMN ${CATEGORIA} INTEGER DEFAULT 1;")
                    db.execSQL("ALTER TABLE ${TABLA_TICKETS} ADD COLUMN ${PRECIO} REAL DEFAULT 0.0;")
                }
                3 -> {
                    if(oldVersion==1){
                        db.execSQL("ALTER TABLE ${TABLA_TICKETS} ADD COLUMN ${CATEGORIA} INTEGER DEFAULT 1;")
                        db.execSQL("ALTER TABLE ${TABLA_TICKETS} ADD COLUMN ${PRECIO} REAL DEFAULT 0.0;")
                    }
                    db.execSQL("ALTER TABLE ${TABLA_TICKETS} ADD COLUMN ${ISDIETA} INTEGER DEFAULT 0;")
                    db.execSQL("ALTER TABLE ${TABLA_TICKETS} ADD COLUMN ${FECHA_ENVIO} TEXT;")
                    db.execSQL("ALTER TABLE ${TABLA_TICKETS} ADD COLUMN ${METODO_ENVIO} INTEGER DEFAULT 0;")
                    db.execSQL("ALTER TABLE ${TABLA_TICKETS} ADD COLUMN ${ENVIADO_A} TEXT;")
                    db.execSQL("ALTER TABLE ${TABLA_TICKETS} ADD COLUMN ${FECHA_COBRO} TEXT;")
                    db.execSQL("ALTER TABLE ${TABLA_TICKETS} ADD COLUMN ${METODO_COBRO} TEXT;")
                }


            }
        }
    }



    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        Log.d("onOpen", "Database opened!!")
    }


    // UsuarioDAO

    /**
     * Añade un usuario a la base de datos
     */
    fun addUser(usuario: Usuario): Long {

        // Si no se produce el insert devuelve -1
        var resultado: Long = -1

        // Creamos un ArrayMap<>()
        val data = ContentValues()
        data.put(ID_USUARIO, usuario.id_usuario)
        data.put(ID_USARIO_FIREBASE, usuario.id_usuario_firebase)
        data.put(EMAIL_USUARIO, usuario.email)
        data.put(PASSWORD_USUARIO, usuario.password)
        data.put(NOMBRE_USUARIO, usuario.nombre)
        data.put(APELLIDOS_USUARIO, usuario.apellidos)
        data.put(PIN_USUARIO, usuario.pin_de_seguridad)

        //Abrimos la BD en modo escritura
        val db = this.writableDatabase
        resultado = db.insert(TABLA_USUARIO, null, data)
        db.close()

        // Una vez añadimos el usuario, creamos su directorio local para almacenar sus fotos
/*
        if (resultado >= 0) {

            val home_dir = File(storageDir+ "/" + usuario.id_usuario)
            if (!home_dir.exists()) {
                home_dir.mkdirs()
            }
        }*/

        return resultado

    }


    /**
     * Elimina un usuario de la base de datos
     */
    fun deleteUsuario(id_usuario: String) {


        val args = arrayOf(id_usuario)

        val db = this.writableDatabase

        db.execSQL("DELETE FROM $TABLA_USUARIO WHERE $ID_USUARIO=?", args)
        db.close()
        var storageDir =
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + id_usuario
        // Borramos recursivamente la carpeta del almacenamiento interno que tiene como nombre el id de Usuario
        utilidades.delRecFileAndDir(storageDir)

    }

    /**
     * valida si un usuario existe
     */
    fun buscaUsuario(usuario: String): Boolean {

        var resultado = false

        val args = arrayOf(usuario)
        val db = this.readableDatabase

        val cursor = db.rawQuery(
            " SELECT ${ID_USUARIO} FROM ${TABLA_USUARIO} WHERE ${EMAIL_USUARIO}=?",
            args
        )

        if (cursor.moveToFirst()) {
            resultado = true
        }

        cursor.close()
        db.close()

        return resultado
    }

    /**
     * valida si una contraseña existe
     */
    fun validaPassword(email: String, password: String): Boolean {

        var resultado = false

        val args = arrayOf(password, email)
        val db = this.readableDatabase

        val cursor = db.rawQuery(
            " SELECT * FROM ${TABLA_USUARIO} WHERE ${PASSWORD_USUARIO}=? AND ${EMAIL_USUARIO}=?",
            args
        )

        if (cursor.moveToFirst()) {
            resultado = true
        }

        cursor.close()
        db.close()

        return resultado
    }


    /**
     * Busca un usuario por su email y devuelve su id
     */
    fun buscaIdUsuario(email: String): String {

        var resultado: String = ""

        val args = arrayOf(email)
        val db = this.readableDatabase

        val cursor = db.rawQuery(
            " SELECT ${ID_USUARIO} FROM ${TABLA_USUARIO} WHERE ${EMAIL_USUARIO}=?",
            args
        )

        if (cursor.moveToFirst()) {
            resultado = cursor.getString(0)
        }

        cursor.close()
        db.close()

        return resultado
    }

    /**
     * valida si el PIN introducido es correcto y devuelve la contraseña
     */
    fun validaPin(pin: Int): String {
        var resultado = ""

        //val args= arrayOf(pin.toString())
        val db = this.readableDatabase

        val columnas = arrayOf(PASSWORD_USUARIO)
        val whereColumns = "${PIN_USUARIO}=?"
        val argsWhere = arrayOf(pin.toString())


        val cursor = db.query(
            TABLA_USUARIO, // The table to query
            columnas,
            whereColumns,
            argsWhere,
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            resultado = cursor.getString(0)
        }

        cursor.close()
        db.close()

        return resultado
    }



    // TicketDAO

    /**
     * Añade un ticket a la base de datos
     */
    fun addTicket(ticket: Ticket): Long {

        val data = ContentValues()
        data.put(ID_TICKET, ticket.idTicket)
        data.put(USUARIO_TICKET, ticket.idusuario)
        data.put(DESCRIPCION_COMPRA, ticket.titulo)
        data.put(ESTABLECIMIENTO, ticket.establecimiento)
        data.put(FECHA_COMPRA, ticket.fecha_de_compra)
        data.put(DIRECCION, ticket.direccion)
        data.put(PROVINCIA, ticket.provincia)
        data.put(LOCALIDAD, ticket.localidad)
        data.put(DURACION_GARANTIA, ticket.duracion_garantia)
        data.put(PERIODO_GARANTIA, ticket.periodo_garantia)
        data.put(AVISAR_FIN_GARANTIA, ticket.avisar_fin_garantia)
        data.put(FOTO1, ticket.foto1)
        data.put(FOTO2, ticket.foto2)
        data.put(FOTO3, ticket.foto3)
        data.put(FOTO4, ticket.foto4)
        data.put(FECHA_MODIFICACION, ticket.fecha_modificacion)
        data.put(CATEGORIA, ticket.categoria)
        data.put(PRECIO, ticket.precio)
        data.put(ISDIETA, ticket.isdieta)
        data.put(FECHA_ENVIO, ticket.fecha_envio)
        data.put(METODO_ENVIO, ticket.metodo_envio)
        data.put(ENVIADO_A,ticket.enviado_a)
        data.put(FECHA_COBRO,ticket.fecha_cobro)
        data.put(METODO_COBRO,ticket.metodo_cobro)

        //Abrimos la BD en modo escritura

        val db = this.writableDatabase
        val insertados = db.insert(TABLA_TICKETS, null, data)
        db.close()

        return insertados
    }


    /**
     * Borra un ticket de la base de datos
     */
    fun deleteTicket(id_ticket: String) {

        val args = arrayOf(id_ticket)
        val db = this.writableDatabase

        db.execSQL("DELETE FROM $TABLA_TICKETS WHERE $ID_TICKET=?", args)
        db.close()

        // Le pasamos la ruta de la carpeta del ticket, que contiene todas las fotos de sus tickets
        // y borramos las foto y la carpeta
        var storageDir =
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + SharedApp.preferences.usuario_logueado + "/" + id_ticket
        utilidades.delRecFileAndDir(storageDir)


    }

    /**
     * Actualiza el ticket pasado como parámetro en la base de datos
     */
    fun updateTicket(ticket: Ticket): Int {

        val args = arrayOf(ticket.idTicket)

        val data = ContentValues()
        data.put(DESCRIPCION_COMPRA, ticket.titulo)
        data.put(ESTABLECIMIENTO, ticket.establecimiento)
        data.put(FECHA_COMPRA, ticket.fecha_de_compra)
        data.put(DIRECCION, ticket.direccion)
        data.put(PROVINCIA, ticket.provincia)
        data.put(LOCALIDAD, ticket.localidad)
        data.put(DURACION_GARANTIA, ticket.duracion_garantia)
        data.put(PERIODO_GARANTIA, ticket.periodo_garantia)
        data.put(AVISAR_FIN_GARANTIA, ticket.avisar_fin_garantia)
        data.put(FOTO1, ticket.foto1)
        data.put(FOTO2, ticket.foto2)
        data.put(FOTO3, ticket.foto3)
        data.put(FOTO4, ticket.foto4)
        data.put(FECHA_MODIFICACION, ticket.fecha_modificacion)
        data.put(CATEGORIA, ticket.categoria)
        data.put(PRECIO, ticket.precio)
        data.put(ISDIETA, ticket.isdieta)
        data.put(FECHA_ENVIO, ticket.fecha_envio)
        data.put(METODO_ENVIO, ticket.metodo_envio)
        data.put(ENVIADO_A,ticket.enviado_a)
        data.put(FECHA_COBRO,ticket.fecha_cobro)
        data.put(METODO_COBRO,ticket.metodo_cobro)

        val db = this.writableDatabase
        val updateados = db.update(TABLA_TICKETS, data, "$ID_TICKET=?", args)
        db.close()

        return updateados
    }



    /**
     *  Devuelve una lista de Tickets obtenida a partir de un id_usuario
     */
    fun devuelveTickets(idUsu: String): MutableList<Ticket> {

        val args = arrayOf(idUsu)
        val db = this.readableDatabase
        var tickets = mutableListOf<Ticket>()

        val cursor = db.rawQuery(
            " SELECT * FROM ${TABLA_TICKETS} WHERE ${USUARIO_TICKET}=?",
            args
        )

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val ticket = Ticket()
                    ticket.idTicket = cursor.getString(0)
                    ticket.idusuario = cursor.getString(1)
                    ticket.titulo = cursor.getString(2)
                    ticket.establecimiento = cursor.getString(3)
                    ticket.direccion = cursor.getString(4)
                    ticket.fecha_de_compra = cursor.getString(5)
                    ticket.provincia = cursor.getInt(6)
                    ticket.localidad = cursor.getString(7)
                    ticket.duracion_garantia = cursor.getInt(8)
                    ticket.periodo_garantia = cursor.getInt(9)
                    ticket.avisar_fin_garantia = cursor.getInt(10)
                    ticket.foto1 = cursor.getString(11)
                    ticket.foto2 = cursor.getString(12)
                    ticket.foto3 = cursor.getString(13)
                    ticket.foto4 = cursor.getString(14)
                    ticket.fecha_modificacion = cursor.getString(15)
                    ticket.categoria =
                        cursor.getInt(16) // Esta columna es una actualización que se añade a partir de la version 2
                    ticket.precio = cursor.getDouble(17)
                    ticket.isdieta = cursor.getInt(18)
                    ticket.fecha_envio = cursor.getString(19)
                    ticket.metodo_envio = cursor.getInt(20)
                    ticket.enviado_a = cursor.getString(21)
                    ticket.fecha_cobro = cursor.getString(22)
                    ticket.metodo_cobro = cursor.getString(23)


                    tickets.add(ticket)
                } while (cursor.moveToNext())
            }

        }

        cursor.close()
        db.close()

        return tickets
    }


}
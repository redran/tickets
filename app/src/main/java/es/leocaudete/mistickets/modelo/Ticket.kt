package es.leocaudete.mistickets.modelo

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.io.Serializable
import java.time.LocalDate

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class Ticket: Serializable{
    var idTicket: String= Timestamp.now().seconds.toString()
    var idusuario: String=""
    var titulo: String=""
    var establecimiento: String=""
    var direccion: String?=null
    var provincia: Int=0
    var localidad: String?=null

    var fecha_de_compra: String=""
    var duracion_garantia: Int=0
    var periodo_garantia: Int=0 // 0 para Años y 1 para Meses

    var avisar_fin_garantia: Int=0 // 0 es false, desmarcado

    var foto_descritpiva:String?=null
    var foto1: String?=null
    var foto2: String?=null
    var foto3: String?=null
    var foto4: String?=null

    var fecha_modificacion: String=""

    // Añadido de 11-04-2020
    var categoria: Int=0
    var precio: Double=0.0

    // Añadido el 20-04-2020
    var isdieta:Int=0 // por defecto No

    var fecha_envio: String?=null
    var metodo_envio:Int=0
    var enviado_a:String?=null

    var fecha_cobro: String?=null
    var metodo_cobro:String?=null




}
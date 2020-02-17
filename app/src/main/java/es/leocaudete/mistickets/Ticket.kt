package es.leocaudete.mistickets

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

    var avisar_fin_garantia: Boolean=false

    var foto_descritpiva:String?=null
    var foto1: String?=null
    var foto2: String?=null
    var foto3: String?=null
    var foto4: String?=null



}
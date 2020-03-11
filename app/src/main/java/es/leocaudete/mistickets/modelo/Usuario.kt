package es.leocaudete.mistickets.modelo

import com.google.firebase.Timestamp
import java.io.Serializable

class Usuario: Serializable {

    var id_usuario: String= Timestamp.now().seconds.toString()
    var id_usuario_firebase: String=""
    var email: String=""
    var password: String=""
    var nombre: String=""
    var apellidos: String=""
    var pin_de_seguridad:Int=0
}
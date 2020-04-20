package es.leocaudete.mistickets.dietas.modelo

import com.google.firebase.Timestamp
import java.io.Serializable


class Plan : Serializable {

    var idPlan:String=Timestamp.now().seconds.toString()
    var tituloPlan:String="" // Viaje a Madrid por Ramsonware
    var objetivoViaje:String="" // Avanzar software contabilidad

    var lugarTrabajoNegocio="" // Sede Grupo Santander
    var direccionTrabajoNegocio:String="" // Calle Churruca, Nº33, España, Madrid, Madrid
    var personaContacto:String="" // Juanjo Peréz
    var telefonoContactoEmpresa:String="" // 600788963


    var hospedaje:String="" // Hostal Maria Dolores
    var direccionHos:String="" // Calle Veléz, N45, España, Madrid, Leganés
    var telefonoHos:String="" // 91 317 1949
    var reserva:String="" // urlemailreserva, pdf, foto....

    var medioTransporteIda:String="" // Tren
    var momentoIda:String="" // 25-06-2020 18:35
    var desdeIda:String="" // Estación Tren Alicante
    var dirDesdeIda:String="" // Avenida Benalúa, Nº45, España, Alicante, Alicante
    var momentoLlegadaIda:String="" // 25-06-2020 21:30
    var destinoIda:String="" // Estación Atocha, Madrid
    var dirDestinoIda:String="" // Avenida Atocha, s/n, España, Madrid, Madrid
    var billeteIda:String="" // url mail billete, pdf, foto...

    var medioTransporteVuelta:String="" // Autobús
    var momentoVuelta:String="" // 30-06-2020 15:35
    var desdeVuelta:String="" // Estación Autobuses Leganés
    var dirDesdeVuelta:String="" // Calle Jose Maestre, N43, España, Madrid, Leganés
    var momentoLlegadaVuelta:String="" // 30-06-2020 18:35
    var destinoVuelta: String="" // Estación de Autobuses Elda/Petrer
    var dirDestinoVuelta:String="" // Avenida Madrid, N33, España, Alicante, Petrer
    var billeteVuelta:String="" // url mail billete, pdf, foto...

    var policiaDestino:String="" // 96 536 9658
    var taxiDestino:String="" // 96 536 9658

    var limiteDesayuno:Double=0.00 // Maximo a gastar por cena
    var limiteComida:Double=0.00 // Maximo a gastar por cena
    var limiteCena:Double=0.00 // Maximo a gastar por cena
    var limteHospedaje:Double=0.00 // Habitacion/Noche

    var idusuario: String = ""

}
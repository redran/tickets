package es.leocaudete.mistickets.dao


import android.content.Context
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.servicios.ServiceApiRest
import es.leocaudete.mistickets.utilidades.ShowMessages
import es.leocaudete.mistickets.utilidades.Utilidades
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Esta clase tiene los métodos de acceso a datos a través de una Api Rest
 */
class ApiRest(context:Context) {

    val utils = Utilidades()
    var gestorMensajes = ShowMessages()

    fun getRetrofit():Retrofit{
        return  Retrofit.Builder()
            //.baseUrl("http://redran.chickenkiller.com/")
            .baseUrl("http://192.168.100.36:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getService():ServiceApiRest{
        return getRetrofit().create<ServiceApiRest>(ServiceApiRest::class.java)
    }
    /**
     *  Busca una lista de todos los tickets de un usuario y los muestra por panta
     */
    fun getTickets(idUsuario:String) {


    }
}
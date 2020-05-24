package es.leocaudete.mistickets.servicios

import es.leocaudete.mistickets.modelo.Ticket
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ServiceApiRest {

    @GET("tickets/{idUsuario}")
    fun getTickets(@Path("idUsuario") idUsuario:String):Call<List<Ticket>>

    @GET("ticket/{idTicket}")
    fun getTicket(@Path("idTicket") idTicket:String):Call<Ticket>
}
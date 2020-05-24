package es.leocaudete.mistickets.negocio

import android.content.Context
import es.leocaudete.mistickets.dao.ApiRest
import es.leocaudete.mistickets.servicios.ServiceApiRest

class ApiRestNegocio(context:Context) {

    var apiRest=ApiRest(context)

    fun getService():ServiceApiRest{
        return apiRest.getService()
    }

}
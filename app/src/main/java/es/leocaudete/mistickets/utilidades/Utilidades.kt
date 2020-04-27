package es.leocaudete.mistickets.utilidades

import java.io.File

class Utilidades {

    /**
     * Borrado recursivo de Ficheros y Directorios
     */
    fun delRecFileAndDir(ruta:String){
        var dir = File(ruta)

        if(dir.isDirectory){
            var hijos = dir.list()

            // Si el directorio contiene algo, primero lo vaciamos
            if(hijos.size>0){
                for(i in hijos.indices){
                    delRecFileAndDir("$dir/" + hijos[i])
                }
            }

            // Ahora que esta vacio lo borramos
            dir.delete()

        }
        else{
            // Sino es un directorio es un archivo y lo borro
            File(ruta).delete()
        }
    }

    /**
     * Este método es el equivalente a un substring
     * pero si el string original no tiene tantos
     * carateres como quiere sacar el subtring, no da un
     * error de out of range sino que develve el string
     * desde el rango de inicio hasta el tamaño de la cadena
     */
    fun miSubstring(desde:Int, hasta:Int, cadena:String): String{

        if(cadena.length>=hasta){
            return cadena.substring(desde,hasta)
        }else{
            return cadena.substring(desde, cadena.length)
        }
    }






}
package es.leocaudete.mistickets.adapters

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.negocio.TicketsNegocio
import es.leocaudete.mistickets.presentacion.VisorTicket
import es.leocaudete.mistickets.preferences.SharedApp
import java.io.File

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class RecyclerAdapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {



    var tickets: MutableList<Ticket> = mutableListOf()
    lateinit var context: Context

    /**
     * Constructor de la clase RecyvlerAdapter
     * @param tickets La lista con los datos que queremos mostrar
     * @context El contexto en el que queremos mostrar los datos y acciones
     */
    fun RecyclerAdapter(tickets: MutableList<Ticket>, context: Context){
        this.tickets=tickets
        this.context=context
    }

    /**
     * Envia al ViewHolder un solo elemento de la lista
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item=tickets.get(position)
        holder.bind(item)
    }

    /**
     * Infla el elemento con el esquema xml tickets_list y devuelve la vista
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater=LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.tickets_list, parent, false),context)
    }

    override fun getItemCount(): Int {
        return tickets.size
    }

    /**
     * Esto es otra clase declarada dentro de RecyclerAdapter que extiende de RecyclerView.ViewHolder
     * Esta se encarga de asociar los datos de cada ticket con los elementos del xml tickets_list y crea las acciones al clicar sobre los elemntos
     */
    class ViewHolder(view:View, context: Context): RecyclerView.ViewHolder(view){

        var ticketNegocio=TicketsNegocio(context)
        var context=context

        private val tienda=view.findViewById(R.id.tv_tienda) as TextView
        private val fecha=view.findViewById(R.id.tv_fecha) as TextView
        private val titulo=view.findViewById(R.id.tv_descricpion) as TextView
        private val foto=view.findViewById(R.id.iv_descriptiva) as ImageView

        fun bind(ticket: Ticket){

            tienda.text=ticket.establecimiento
            fecha.text=ticket.fecha_de_compra
            titulo.text=ticket.titulo
            // Asociamos la foto al ImageView
            if(ticket.foto1==null)
            {
                foto.setBackgroundResource(R.drawable.common_google_signin_btn_icon_light)
            }
            else
            {
                var rutaFoto:String
                if(SharedApp.preferences.bdtype){
                    ticketNegocio.descargaFotoCloudInicial(ticket.foto1.toString(),foto)
                }else{
                    rutaFoto=ticketNegocio.rutaLocal(ticket.idTicket) +"/"+ticket.foto1
                    foto.setImageBitmap(BitmapFactory.decodeFile(rutaFoto))
                }
            }

            // Definimos el evento onCLick para la imagen
            itemView.setOnClickListener {

                val intent=Intent(context, VisorTicket::class.java).apply {
                    putExtra("TicketVisor", ticket)
                }
                context.startActivity(intent)
            }
        }

    }
}


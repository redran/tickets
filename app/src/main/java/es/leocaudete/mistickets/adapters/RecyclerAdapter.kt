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
import es.leocaudete.mistickets.presentacion.VisorTicket
import es.leocaudete.mistickets.preferences.SharedApp
import java.io.File

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class RecyclerAdapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {



    var tickets: MutableList<Ticket> = mutableListOf()
    lateinit var context: Context

    fun RecyclerAdapter(tickets: MutableList<Ticket>, context: Context){
        this.tickets=tickets
        this.context=context
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item=tickets.get(position)
        holder.bind(item,context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater=LayoutInflater.from(parent.context)
        return ViewHolder(
            layoutInflater.inflate(
                R.layout.tickets_list, parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return tickets.size
    }

    class ViewHolder(view:View): RecyclerView.ViewHolder(view){

        var storageRef = FirebaseStorage.getInstance().reference
        var auth=FirebaseAuth.getInstance()
        lateinit var storageDir: File




        private val tienda=view.findViewById(R.id.tv_tienda) as TextView
        private val fecha=view.findViewById(R.id.tv_fecha) as TextView
        private val titulo=view.findViewById(R.id.tv_descricpion) as TextView
        private val foto=view.findViewById(R.id.iv_descriptiva) as ImageView

        fun bind(ticket: Ticket, context: Context){

            tienda.text=ticket.establecimiento
            fecha.text=ticket.fecha_de_compra
            titulo.text=ticket.titulo
            if(ticket.foto1==null)
            {
                foto.setBackgroundResource(R.drawable.common_google_signin_btn_icon_light)
            }
            else
            {
                var rutaFoto:String;
                if(SharedApp.preferences.bdtype){
                    rutaFoto=auth.currentUser?.uid.toString()+"/"+ticket.foto1
                    val pathReference = storageRef.child(rutaFoto)


                    pathReference.downloadUrl.addOnSuccessListener {
                        Picasso.get().load(it).into(foto)
                    }
                }else{
                    var  storageLocalDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + SharedApp.preferences.usuario_logueado + "/" + ticket.idTicket
                    rutaFoto=storageLocalDir +"/"+ticket.foto1
                    foto.setImageBitmap(BitmapFactory.decodeFile(rutaFoto))
                }


            }


            itemView.setOnClickListener {

                val intent=Intent(context, VisorTicket::class.java).apply {
                    putExtra("TicketVisor", ticket)
                }
                context.startActivity(intent)
            }
        }

    }
}


package es.leocaudete.mistickets

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat.getExternalFilesDirs
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_gestor_fotos.*
import java.io.File

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class RecyclerAdapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {



    var tickets: MutableList<Ticket> = ArrayList()
    lateinit var context: Context

    fun RecyclerAdapter(tickets: MutableList<Ticket>, context: Context){
        this.tickets=tickets
        this.context=context
    }

    override fun onBindViewHolder(holder: RecyclerAdapter.ViewHolder, position: Int) {
        val item=tickets.get(position)
        holder.bind(item,context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.ViewHolder {
        val layoutInflater=LayoutInflater.from(parent.context)
        return ViewHolder(
            layoutInflater.inflate(
                R.layout.tickets_list,parent,false
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
                // Esto era para cargar desde firebase pero va muy lento
                var rutaFoto=auth.currentUser?.uid.toString()+"/"+ticket.foto1
                val pathReference = storageRef.child(rutaFoto)

                pathReference.downloadUrl.addOnSuccessListener {
                    Picasso.get().load(it).into(foto)
                }

              /*  // Cargamos desde local
                storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                foto.setImageBitmap(BitmapFactory.decodeFile(storageDir.toString() + "/" + ticket.foto1))
*/
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


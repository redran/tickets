package es.leocaudete.mistickets

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import es.leocaudete.mistickets.modelo.Ticket
import es.leocaudete.mistickets.preferences.SharedApp
import kotlinx.android.synthetic.main.activity_visor_fotos.*
import java.io.File
/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class VisorFotos : AppCompatActivity() {
    lateinit var storageDir: String
    private lateinit var auth: FirebaseAuth
    var pDownX=0
    var pUpX=0


    var unTicket= Ticket()
    var fotoactual=1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visor_fotos)

        auth= FirebaseAuth.getInstance()
        unTicket = intent.getSerializableExtra("unTicket") as Ticket

        if(SharedApp.preferences.bdtype){
            storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + auth.currentUser?.uid.toString()
        }else{
            storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + SharedApp.preferences.usuario_logueado + "/" + unTicket.idTicket
        }



        cargaFoto(unTicket.foto1)

        imgFoto.setOnTouchListener { v, event ->



            val action = event.action

            when(action){

                MotionEvent.ACTION_DOWN ->{
                    pDownX= event.x.toInt()

                }
                MotionEvent.ACTION_MOVE -> { }

                MotionEvent.ACTION_UP -> {
                    pUpX= event.x.toInt()
                    if(pUpX!=pDownX){
                        // Si se ha movido a la derecha
                        if(pUpX<pDownX)
                        {
                            if(fotoactual<4)
                                fotoactual++
                        }

                        // si se ha movido a la izquierda
                        if(pUpX>pDownX)
                        {
                            if(fotoactual>1)
                                fotoactual--
                        }

                        when(fotoactual){
                            1->cargaFoto(unTicket.foto1)
                            2->cargaFoto(unTicket.foto2)
                            3->cargaFoto(unTicket.foto3)
                            4->cargaFoto(unTicket.foto4)
                        }

                    }
                }

                MotionEvent.ACTION_CANCEL -> {

                }

                else ->{

                }
            }

            true
        }

        btn1.setBackgroundColor(Color.RED)
    }

    fun cargaFoto(foto:String?){
        if(foto!=null && foto!="null"){
            if(SharedApp.preferences.bdtype){
                var storageRef = FirebaseStorage.getInstance().reference

                var rutaFoto=auth.currentUser?.uid.toString()+"/"+  foto
                val pathReference = storageRef.child(rutaFoto)

                pathReference.downloadUrl.addOnSuccessListener {
                    Picasso.get()
                        .load(it)
                        //.resize(400,800)
                        .into(imgFoto)
                }
            }else{
                imgFoto.setImageBitmap(BitmapFactory.decodeFile("$storageDir/$foto"))
            }


        }
        else{
            imgFoto.setImageResource(R.drawable.googleg_disabled_color_18)
         //   imgFoto.setBackgroundResource(R.drawable.googleg_disabled_color_18)
        }
        when(fotoactual) {
            1 -> {
                btn1.setBackgroundColor(Color.RED)
                btn2.setBackgroundColor(Color.GRAY)
                btn3.setBackgroundColor(Color.GRAY)
                btn4.setBackgroundColor(Color.GRAY)
            }
            2 -> {
                btn1.setBackgroundColor(Color.GRAY)
                btn2.setBackgroundColor(Color.RED)
                btn3.setBackgroundColor(Color.GRAY)
                btn4.setBackgroundColor(Color.GRAY)
            }
            3 -> {
                btn1.setBackgroundColor(Color.GRAY)
                btn2.setBackgroundColor(Color.GRAY)
                btn3.setBackgroundColor(Color.RED)
                btn4.setBackgroundColor(Color.GRAY)
            }
            4 -> {
                btn1.setBackgroundColor(Color.GRAY)
                btn2.setBackgroundColor(Color.GRAY)
                btn3.setBackgroundColor(Color.GRAY)
                btn4.setBackgroundColor(Color.RED)
            }
        }


    }

    fun volver(view: View) {

        var intent=Intent(this, VisorTicket::class.java).apply {
            putExtra("TicketVisor", unTicket)
        }
        startActivity(intent)
        finish()
    }
}

package es.leocaudete.mistickets.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.personales.main.MainActivity
import kotlinx.android.synthetic.main.activity_menu.*


class Menu : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        rb_tickets.isChecked=true
    }

    fun salir(view: View) {
        System.exit(0)
    }
    fun entrar(view: View) {

        if(rb_tickets.isChecked){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }else{

        }
    }
}

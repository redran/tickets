package es.leocaudete.mistickets.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_forgot_pass.*
/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class ForgotPass : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var gestoMensajes= ShowMessages()
    lateinit var dbSQL: SQLiteDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pass)

        auth=FirebaseAuth.getInstance()
        // Instanciamos la clase que crea la base de datos y tiene nuestro CRUD
        dbSQL = SQLiteDB(this, null)

        // Boton atras
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if(SharedApp.preferences.bdtype){
            ed_email.hint=getString(R.string.ed_email)
           ed_pin.visibility=View.GONE
        }else{
            ed_email.hint=getString(R.string.ed_user)
            ed_pin.visibility=View.VISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            android.R.id.home ->{
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }

    /**
     * Use sendPasswordResetEmail from Google API to reset the password
     */
    fun send(view: View){

        val email = ed_email.text.toString()
        if(SharedApp.preferences.bdtype){
            if(!TextUtils.isEmpty(email)){
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(this){
                            task ->

                        if(task.isSuccessful){
                            progressBar.visibility=View.VISIBLE

                            startActivity(Intent(this,
                                Login::class.java ))
                        }
                        else
                        {
                            Toast.makeText(this, "Error al enviar el email", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }else{
            val pin = ed_pin.text.toString()
            // Le pedimos el PIN de seguridad y el email y si lo acierta le mostramos el password
            if(dbSQL.buscaUsuario(email)){
                var pass=dbSQL.validaPin(Integer.parseInt(pin))
                if(!TextUtils.isEmpty(pass)){
                    gestoMensajes.showAlertOneButton("PASSWORD","Tu contraseña es : $pass",this)
                }else{
                    gestoMensajes.showAlertOneButton("ERROR","El pin introducido no es valido" ,this)
                }
            }else{
                gestoMensajes.showAlertOneButton("ERROR","El usuario especificado no existe",this)
            }


        }


    }


    fun actualizaPassLocal(usuario: String){

    }

}

package es.leocaudete.mistickets

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.login_activity.*
import java.io.File
/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val TAG = "DocSnippets"
    var tickets = mutableListOf<Ticket>() // va a lamacenar todos los tickets

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        auth=FirebaseAuth.getInstance()


    }

    override fun onStart() {
        super.onStart()
        // Comprobamos en la preferencias si tenemos recordar a 0 entonces hacemos un singOut

         var recordar=SharedApp.preferences.login
        if(recordar)
        {
            if(auth.currentUser!=null){
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        else
        {
            auth.signOut()
            cb_recordar.isChecked=false
        }
    }

    fun forgotPassword(view: View){
        startActivity(Intent(this,ForgotPass::class.java ))
    }
    fun registro(view: View){

        startActivity(Intent(this, Registro::class.java))
    }
    fun login(view: View){
        // si el chekbox esta no esta marcado pornemos el valor en prefrencias de recordar a 0
        SharedApp.preferences.login = cb_recordar.isChecked
        loginUser()
    }

    private fun loginUser(){
        val user: String=ed_user.text.toString()
        val password: String=ed_password.text.toString()

        if(!TextUtils.isEmpty(user) && !TextUtils.isEmpty(password)){
            progressBar.visibility=View.VISIBLE

            auth.signInWithEmailAndPassword(user, password)
                .addOnCompleteListener(this){
                    task ->

                    if(task.isSuccessful){
                        action()
                    }
                    else{
                        Toast.makeText(this, "Error: ususario y/o contraseña incorrectos", Toast.LENGTH_LONG).show()
                    }
                }
        }
        else
        {
            Toast.makeText(this, "Rellene todos los campos", Toast.LENGTH_LONG).show()
        }
    }

    private fun action(){
        // Primero vamos a sincronizar todas las imagenes que falten en nuestro movil con las que tenemos en firebase

        startActivity(Intent(this, MainActivity::class.java))
    }



}

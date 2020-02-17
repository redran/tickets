package es.leocaudete.mistickets

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.widget.EditText
import android.widget.ProgressBar
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_registro.*
/**
 * @author Leonardo Caudete Palau - 2º DAM
 */

class Registro : AppCompatActivity() {

    private lateinit var dbReference: CollectionReference
    private lateinit var database: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        database = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        dbReference = database.collection("User")


        // Boton atras
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
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

    fun register(view: View) {
        val password = ed_password.text.toString()
        val password2 = ed_repeatpassword.text.toString()
        if(TextUtils.equals(password,password2)){
            creaNuevoUsuario()
        }else{
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
        }

    }


    private fun creaNuevoUsuario() {
        val nombre = ed_nombre.text.toString()
        val apellidos = ed_apellidos.text.toString()
        val email = ed_email.text.toString()
        val password = ed_password.text.toString()

        if (!TextUtils.isEmpty(nombre) && !TextUtils.isEmpty(apellidos) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            progressBar.visibility = View.VISIBLE
            auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this){
                    task ->

                    if(task.isComplete){
                        val user:FirebaseUser?=auth.currentUser
                        verifyEmail(user)

                        // Dentro de User creamos otro documeto con el nombre del uid de Usuario que acaba de asignar
                        // el uid es creado a partir del password y la contraseña
                        var userBD:DocumentReference =dbReference.document(user!!.uid)

                        // añadimos el resto de datos del usuario o bien con un hashMap o con un modelo
                        val usuario = hashMapOf(
                            "nombre" to ed_nombre.text.toString(),
                            "apellidos" to ed_apellidos.text.toString()
                        )

                        userBD.set(usuario)

                        action()
                    }
                }
        }
        else
        {
            Toast.makeText(this, "Los campos no pueden estar vacios", Toast.LENGTH_LONG).show()
        }
    }
    private fun action(){
            startActivity(Intent(this,Login::class.java))
    }

    private fun verifyEmail(user:FirebaseUser?){
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this){
                task ->

                if(task.isComplete){
                    Toast.makeText(this, "Email enviado", Toast.LENGTH_LONG).show()
                }else{
                    Toast.makeText(this, "Error al enviar email", Toast.LENGTH_LONG).show()
                }
            }
    }
}

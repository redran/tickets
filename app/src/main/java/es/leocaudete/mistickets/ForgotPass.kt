package es.leocaudete.mistickets

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_forgot_pass.*
/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class ForgotPass : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_pass)

        auth=FirebaseAuth.getInstance()

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

    /**
     * Use sendPasswordResetEmail from Google API to reset the password
     */
    fun send(view: View){

        val email = ed_email.text.toString()

        if(!TextUtils.isEmpty(email)){
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this){
                    task ->

                    if(task.isSuccessful){
                        progressBar.visibility=View.VISIBLE
                        startActivity(Intent(this,Login::class.java ))
                    }
                    else
                    {
                        Toast.makeText(this, "Error al enviar el email", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}

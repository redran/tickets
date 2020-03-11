package es.leocaudete.mistickets.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import es.leocaudete.mistickets.R
import es.leocaudete.mistickets.dao.SQLiteDB
import es.leocaudete.mistickets.modelo.Usuario
import es.leocaudete.mistickets.preferences.SharedApp
import es.leocaudete.mistickets.utilidades.ShowMessages
import kotlinx.android.synthetic.main.activity_registro.*
/**
 * @author Leonardo Caudete Palau - 2º DAM
 */

class Registro : AppCompatActivity() {

    private lateinit var dbReference: CollectionReference
    private lateinit var database: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    lateinit var dbSQL: SQLiteDB
    private var gestorMensajes = ShowMessages()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Instanciamos la clase que crea la base de datos y tiene nuestro CRUD
        dbSQL = SQLiteDB(this, null)

        database = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        dbReference = database.collection("User")


        // Boton atras
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if(SharedApp.preferences.bdtype){
            ed_pin.visibility=View.GONE
        }else{
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


        val usuario=Usuario()
        usuario.email=email
        usuario.password=password
        usuario.nombre=nombre
        usuario.apellidos=apellidos


        if (!TextUtils.isEmpty(nombre) && !TextUtils.isEmpty(apellidos) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            // Camino A Registro de Usuario Firebase
            if(SharedApp.preferences.bdtype){

                // Ahora compruebo que el usuario no exista en local, si exite le aviso que ya existe en local y que se va a clonar el local en la nube
                if(dbSQL.buscaUsuario(email)){
                    gestorMensajes.showAlert("Información","Se ha encontrado este email en la base de datos local. Elija Aceptar para clonar " +
                            "el usuario local en la nube o Cancelar para cambiar el email", this, { addUserByEmailInFirebase(email,dbSQL.passUsuario(email),true)})
                }else{
                    addUserByEmailInFirebase(email,password,false)
                }
            }else{ // Camino B Registro de Usuraio Firebase
                val pin=Integer.parseInt(ed_pin.text.toString())
                usuario.pin_de_seguridad=pin
                // Aqui comprobaria y mostraria el campo PIN
                if(pin>0){
                    if(dbSQL.buscaUsuario(email)){
                        gestorMensajes.showAlert("Información","Se ha encontrado este email en la base de datos local. Elija otro email", this, { ed_email.text.clear() })
                    }else{
                        val insertados = dbSQL.addUser(usuario)
                        if(insertados<0){
                            gestorMensajes.showAlertOneButton("ERROR","Error al insertar el nuevo usuario.",this)
                        }else{
                            gestorMensajes.showActionOneButton("INFO","Usuario insertado correctamente.",this,{startActivity(Intent(this,Login::class.java))})
                        }
                    }
                }
                else
                {
                    Toast.makeText(this, "Debes introducir el PIN para poder recuperar tu password en caso de pérdida", Toast.LENGTH_LONG).show()
                }
            }
        }
        else
        {
            Toast.makeText(this, "Los campos no pueden estar vacios", Toast.LENGTH_LONG).show()
        }
    }

    private fun addUserByEmailInFirebase(email:String, password:String, clonado:Boolean){
        progressBar.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this){
                    task ->

                if(task.isComplete){
                    val user:FirebaseUser?=auth.currentUser
                    verifyEmail(user)

                    // Dentro de User creamos otro documeto con el nombre del uid de Usuario que acaba de asignar
                    // el uid es creado a partir del password y la contraseña
                    var userBD:DocumentReference = dbReference.document(user!!.uid)


                    // añadimos el resto de datos del usuario o bien con un hashMap o con un modelo
                    val usuario = hashMapOf(

                        "nombre" to ed_nombre.text.toString(),
                        "apellidos" to ed_apellidos.text.toString()
                    )

                    // Añadimos usuario en Firebase
                    userBD.set(usuario)

                    // Además de crear el usuario en la nube, lo creamos en Sqlite
                    val usuarioSQlite = Usuario()
                    usuarioSQlite.id_usuario_firebase = user!!.uid
                    usuarioSQlite.email = email
                    usuarioSQlite.password = password
                    usuarioSQlite.nombre = ed_nombre.text.toString()
                    usuarioSQlite.apellidos = ed_apellidos.text.toString()

                    dbSQL.addUser(usuarioSQlite)

                    action(clonado)
                }
            }
    }
    private fun action(clonado:Boolean){
        if(clonado){
            gestorMensajes.showActionOneButton("Información", "El usuario ha sido clonado, recuerde que el password es el mismo que tiene el usuario local", this, {startActivity(Intent(this, Login::class.java))})
        }
        else
        {
            startActivity(Intent(this, Login::class.java))
        }

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

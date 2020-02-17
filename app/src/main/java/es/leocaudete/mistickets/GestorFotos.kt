package es.leocaudete.mistickets


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_gestor_fotos.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * @author Leonardo Caudete Palau - 2º DAM
 */
class GestorFotos : AppCompatActivity() {

    lateinit var unTicket: Ticket
    lateinit var storageDir: File
    var isEdited: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestor_fotos)

        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        // Recuperamos el ticket y seguimos rellenando los datos con las fotos
        unTicket = intent.getSerializableExtra("unTicket") as Ticket
        isEdited = intent.getBooleanExtra("isEdited", false)



        cargaInicial()

        img_foto1.setOnClickListener {
            dispatchTakePictureIntent(1)
        }
        img_foto2.setOnClickListener {
            dispatchTakePictureIntent(2)
        }
        img_foto3.setOnClickListener {
            dispatchTakePictureIntent(3)
        }
        img_foto4.setOnClickListener {
            dispatchTakePictureIntent(4)
        }
    }

    /**
     * Crea una foto temporal mientras se decide que fotos guardar
     */
    @Throws(IOException::class)
    private fun createTemporalFile(numFoto: Int): File{

        var file: File = File(storageDir, "FotoTemporal$numFoto.jpg")
        if (file.exists()) {
            file.delete()
        }

        file.createNewFile()
        return file
    }


    /**
     * Lanza la camara y guarda la foto en un jpg con el numero de foto numFoto en el nombre
     */
    private fun dispatchTakePictureIntent(numfoto: Int) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Creamos el file donde va a ir la foto
                val photoFile: File? = try {
                    createTemporalFile(numfoto)

                } catch (ex: IOException) {
                    // Error occurred while creating the File

                    null
                }
                photoFile?.also {
                    var fotoUri: Uri? = null
                    fotoUri = FileProvider.getUriForFile(
                        this,
                        "es.leocaudete.mistickets",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)

                    startActivityForResult(takePictureIntent, numfoto)

                }
            }
        }
    }


    /**
     * Configuramos las acciones a realizar cuando vuelva de usar la camara
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val strFoto: String
        var bmpImage: Bitmap

        val urlTemporal: String="$storageDir/FotoTemporal$requestCode.jpg"

        // si hemos entrado en modo edicion, las fotos las hemos guardado con otro nombre "edited_"
        // para que no sobreescriban las antiguas, si hubieran, hasta que le demos a Guardar
        if (isEdited) {
            strFoto = "edited_" + unTicket.idTicket + "_foto" + requestCode + ".jpg"
        } else {
            strFoto = unTicket.idTicket + "_foto" + requestCode + ".jpg"
        }

        // Cargamos la foto y la reducimos de tamaño
        val tmpFile=File(urlTemporal) // La foto temporal que pesa mucho
        bmpImage=rotarImagen(BitmapFactory.decodeFile(urlTemporal),90f)
        bmpImage = Bitmap.createScaledBitmap(bmpImage, 800, 800, false)
        bmpImage = resizeBitmap(bmpImage, 800)
        val bytes = ByteArrayOutputStream()
        bmpImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        try {
            val file = File("$storageDir/$strFoto")//createImageFile(requestCode)
            val result: Boolean
            result = file.createNewFile()
            if (result) {
                val fo = FileOutputStream(file)
                fo.write(bytes.toByteArray())
                fo.close()
            }
        } catch (ie: IOException) {
            ie.printStackTrace()
        }


        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {

            img_foto1.setImageBitmap(bmpImage)
            unTicket.foto1 = strFoto
        }
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            img_foto2.setImageBitmap(bmpImage)
            unTicket.foto2 = strFoto
        }
        if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            img_foto3.setImageBitmap(bmpImage)
            unTicket.foto3 = strFoto
        }
        if (requestCode == 4 && resultCode == Activity.RESULT_OK) {
            img_foto4.setImageBitmap(bmpImage)
            unTicket.foto4 = strFoto
        }


       // Borramos la imagen temporal que es la que pesa mucho
        tmpFile.delete()
    }


    /**
     * Rota la imagen porque sale en horizontal
     */
    fun rotarImagen(imagen: Bitmap, grados: Float): Bitmap{
        var matrix=Matrix()
        matrix.postRotate(grados)
        var imgRotada = Bitmap.createBitmap(imagen, 0, 0, imagen.width, imagen.height, matrix, true)
        imagen.recycle()
        return imgRotada
    }
    fun resizeBitmap(getBitmap: Bitmap, maxSize: Int): Bitmap {


        var width = getBitmap?.width
        var height = getBitmap?.height

        val x: Double
        if (width >= height && width > maxSize) {
            x = width / height.toDouble()
            width = maxSize
            height = (maxSize / x).toInt()
        } else if (height >= width && height > maxSize) {
            x = height / width.toDouble()
            height = maxSize
            width = (maxSize / x).toInt()
        }
        return Bitmap.createScaledBitmap(getBitmap, width, height, false)
    }


    /**
     *  Devolvemos las fotos a la activity del nuevo ticket
     */
    fun aceptar(view: View) {



        val myIntent = Intent(this, NuevoTicket::class.java).apply {
            if (isEdited) {
                putExtra("updateTicket", true)
            }
            putExtra("unTicket", unTicket)


        }
        setResult(Activity.RESULT_OK, myIntent)
        finish()


    }

    /**
     * si hay imagen la cargamos
     */
    fun cargaInicial() {


        // No hay imagen
        if (unTicket.foto1 == null) {
            img_foto1.setBackgroundResource(R.drawable.common_google_signin_btn_icon_light)
        } else {
            img_foto1.setImageURI(Uri.parse(storageDir.toString() + "/" + unTicket.foto1))
        }

        if (unTicket.foto2 == null) {
            img_foto2.setBackgroundResource(R.drawable.common_google_signin_btn_icon_light)
        } else {

            img_foto2.setImageURI(Uri.parse(storageDir.toString() + "/" + unTicket.foto2))
        }

        if (unTicket.foto3 == null) {
            img_foto3.setBackgroundResource(R.drawable.common_google_signin_btn_icon_light)
        } else {
            img_foto3.setImageURI(Uri.parse(storageDir.toString() + "/" + unTicket.foto3))
        }

        if (unTicket.foto4 == null) {
            img_foto4.setBackgroundResource(R.drawable.common_google_signin_btn_icon_light)
        } else {
            img_foto4.setImageURI(Uri.parse(storageDir.toString() + "/" + unTicket.foto4))
        }
    }


}

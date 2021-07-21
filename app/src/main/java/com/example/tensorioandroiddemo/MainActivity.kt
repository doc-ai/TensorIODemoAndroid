package com.example.tensorioandroiddemo

import ai.doc.tensorio.core.modelbundle.ModelBundle
import ai.doc.tensorio.core.utilities.ClassificationHelper
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

object ImageUtils {
  lateinit var currentPhotoPath: String

  @Throws(IOException::class)
  fun createImageFile(context: Context): File? {
    // Create an image file name
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "JPEG_${timeStamp}_", /* prefix */
        ".jpg", /* suffix */
        storageDir /* directory */
    ).apply {
      // Save a file: path for use with ACTION_VIEW intents
      currentPhotoPath = absolutePath
    }
  }
}

class MainActivity : AppCompatActivity() {
  var takePicture: ActivityResultLauncher<Uri>? = null;
  var imageUri: Uri? = null;
  var imageFile: File? = null;

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
      if (success) {
        processImage();
      }
    }

    // get reference to button
    val launchCameraButton = findViewById<Button>(R.id.launchCameraButton)
    // set on-click listener
    launchCameraButton.setOnClickListener {
      launchCamera();
    }
  }

  fun processImage() {
    // Load the Model
    val bundle = ModelBundle.bundleWithAsset(applicationContext, "phenomenal-face.tiobundle")
    val model = bundle.newModel()

    // Load an Image
    val stream = imageFile!!.inputStream()
    val bitmap = BitmapFactory.decodeStream(stream)

    // Get the Results
    val output = model.runOn(bitmap)
    val height = (output.get("Height") as FloatArray).get(0)
    val age = (output.get("Age")  as FloatArray).get(0)
    val weight = (output.get("Weight")  as FloatArray).get(0)
    val sex = (output.get("Sex")  as FloatArray).get(0)

    val sexAsString = if (sex > 0.5) "Male" else "Female"

    // Update UI with results
    findViewById<TextView>(R.id.weightText).text = "Weight: %.2f kg".format(weight)
    findViewById<TextView>(R.id.heightText).text = "Height: %.2f cm".format(height)
    findViewById<TextView>(R.id.sexText).text = "Sex: %s".format(sexAsString)
    findViewById<TextView>(R.id.ageText).text = "Age: %.0f years old".format(age)
  }

  fun launchCamera() {
    ImageUtils.createImageFile(applicationContext)?.also {
      imageFile = it;
      imageUri = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
          BuildConfig.APPLICATION_ID + ".provider", it);

      takePicture!!.launch(imageUri)
    }
  }
}
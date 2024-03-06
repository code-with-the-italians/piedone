package it.codewiththeitalians.piedone

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.Images
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.exifinterface.media.ExifInterface
import it.codewiththeitalians.piedone.ui.theme.PiedoneTheme
import timber.log.Timber
import java.io.File

class PickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
                if (uri == null) Timber.d("PhotoPicker: No media selected") else {
                    thisShouldReallyBeOnABgThread(uri)
                }
            }

            PiedoneTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(onClick = { pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }) {
                            Text("PICK ONE")
                        }
                    }
                }
            }
        }
    }

    private fun thisShouldReallyBeOnABgThread(yes: Uri) {
        val imageBytes = contentResolver.openInputStream(yes)!!.use {
            it.readBytes()
        }

        val file = File(cacheDir, "ivanillo")

        file.outputStream().use {
            it.write(imageBytes)
        }

        val metadata = ExifInterface(file)

        metadata.setAttribute(ExifInterface.TAG_ARTIST, "Ivan")
        metadata.setAttribute(ExifInterface.TAG_MODEL, "Pizza")
        metadata.saveAttributes()

        val share = Intent(Intent.ACTION_SEND)
        val type = contentResolver.getType(yes)
        share.setType(type)

        val values = ContentValues()
        values.put(Images.Media.TITLE, "IVAN IS MIGHTY")
        values.put(Images.Media.MIME_TYPE, type)
        val uri = contentResolver.insert(
            Images.Media.EXTERNAL_CONTENT_URI,
            values
        )!!

        contentResolver.openOutputStream(uri)!!.use {
            it.write(file.readBytes())
        }

        share.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(share, "HOT SINGLES IN YOUR AREA"))
    }
}

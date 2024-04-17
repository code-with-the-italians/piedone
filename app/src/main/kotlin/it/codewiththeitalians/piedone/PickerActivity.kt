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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import it.codewiththeitalians.piedone.ui.theme.PiedoneTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class PickerActivity : ComponentActivity() {

    private val currentUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        snapshotFlow { currentUri.value }
            .filterNotNull()
            .onEach {
                Timber.i("IVAN, my thread is: ${Thread.currentThread().name}")
                thisIsRunningOnABGThread(it)
            }
            .onStart { Timber.i("I AM ALIVE") }
            .onCompletion { Timber.i("I AM DED") }
            .flowOn(Dispatchers.Default)
            .launchIn(lifecycleScope)

        setContent {
            MainContent {
                currentUri.value = it
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        Timber.i("New intent received: $intent\n\nData: ${intent.extras.printAll()}")
        if (intent.action != Intent.ACTION_SEND) return

        currentUri.value = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return
    }

    private fun Bundle?.printAll() = buildString {
        if (this@printAll == null) {
            append("null")
            return@buildString
        }

        for (key in keySet()) {
            append(key)
            append(": ")
            appendLine(get(key))
        }
    }

    @Composable
    private fun MainContent(onUriPicked: (Uri) -> Unit) {
        val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { newUri ->
            if (newUri == null) {
                Timber.d("PhotoPicker: No media selected")
            } else {
                onUriPicked(newUri)
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

    private suspend fun thisIsRunningOnABGThread(uri: Uri) {
        val file = withContext(Dispatchers.IO) {
            val myItsyBitsyTempFile = File.createTempFile("piedone-", ".blargh")

            val namingIsHard = contentResolver.openInputStream(uri)!!.use {
                it.readBytes()
            }


            myItsyBitsyTempFile.outputStream().use {
                it.write(namingIsHard)
            }

            val metadata = ExifInterface(myItsyBitsyTempFile)

            withContext(Dispatchers.Default) {
                metadata.setAttribute(ExifInterface.TAG_ARTIST, "Ivan")
                metadata.setAttribute(ExifInterface.TAG_MODEL, "Pizza")
                metadata.clearTags(*sensitiveTags)
            }

            metadata.saveAttributes()
            myItsyBitsyTempFile
        }

        val share = Intent(Intent.ACTION_SEND)
        val type = contentResolver.getType(uri)
        share.setType(type)

        val values = ContentValues()
        values.put(Images.Media.TITLE, "IVAN IS MIGHTY")
        values.put(Images.Media.MIME_TYPE, type)
        val outboundUri = contentResolver.insert(
            Images.Media.EXTERNAL_CONTENT_URI,
            values
        )!!

        withContext(Dispatchers.IO) {
            contentResolver.openOutputStream(outboundUri)!!.use {
                it.write(file.readBytes())
            }
        }

        share.putExtra(Intent.EXTRA_STREAM, outboundUri)
        startActivity(Intent.createChooser(share, "HOT SINGLES IN YOUR AREA"))
    }

    private val sensitiveTags = arrayOf(
        ExifInterface.TAG_GPS_VERSION_ID,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_DIFFERENTIAL,
        ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
    )

    private fun ExifInterface.clearTags(vararg tagNames: String) {
        for (tag in tagNames) {
            setAttribute(tag, null)
        }
    }
}

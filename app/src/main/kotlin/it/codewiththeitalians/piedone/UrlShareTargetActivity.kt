package it.codewiththeitalians.piedone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import it.codewiththeitalians.piedone.ui.theme.PiedoneTheme
import timber.log.Timber

class UrlShareTargetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PiedoneTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(text = "DINGUS IS DINGUSING")
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        Timber.i("New intent received")

        val url = intent.extras?.getString(Intent.EXTRA_TEXT)?.trim()
        if (url == null) {
            Timber.i("No data received")
            finish()
            return
        }

        if (url.matches(IVAN_THE_REGEX)) {
            val newUrl = "https://12ft.io/$url"
            Timber.i("Starting new URL intent: $newUrl")
            val newIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newUrl))
            startActivity(newIntent)
        } else {
            Timber.i("Ignoring non-URL data: $url")
        }

        finish()
    }
}

private val IVAN_THE_REGEX = "^https?://.*$".toRegex(RegexOption.IGNORE_CASE)

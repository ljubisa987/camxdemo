package com.saucelabdemo

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import com.saucelabdemo.ui.theme.SauceLabDemoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val previewView = PreviewView(this)
        val analyzerBitmaps = MutableStateFlow(ImageAnalyzerOutput(null, null))

        val imageAnalyzer = ImageAnalyzer { output ->
            analyzerBitmaps.update { output }
        }

        val camX = CameraXImpl(this, this, previewView, imageAnalyzer)
        camX.startCamera()

        setContent {
            SauceLabDemoTheme {
                enableEdgeToEdge()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Home(modifier = Modifier.padding(innerPadding), previewView, analyzerBitmaps)
                }
            }
        }
    }
}

@Composable
fun Home(
    modifier: Modifier = Modifier,
    view: PreviewView,
    analyzerBitmaps: MutableStateFlow<ImageAnalyzerOutput>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        val innerModifier = Modifier
            .fillMaxWidth()
            .weight(1f)

        Box(modifier = innerModifier.weight(0.5f), contentAlignment = Alignment.Center) {
            analyzerBitmaps.collectAsState().value.bitmap?.let { bitmap ->
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null
                )
            }
            WithText("YUV to Bitmap")
        }

        Box(modifier = innerModifier, contentAlignment = Alignment.Center) {
            analyzerBitmaps.collectAsState().value.let { output ->
                if (output.bitmap != null) {
                    Image(
                        bitmap = output.bitmap.rotate(output.imageInfo!!.rotationDegrees)
                            .asImageBitmap(),
                        contentDescription = null
                    )
                }
            }
            WithText("YUV to Bitmap, rotated")
        }

        Box(modifier = innerModifier, contentAlignment = Alignment.Center) {
            AndroidView(
                modifier = modifier.fillMaxSize(0.4f),
                factory = { view })
            WithText("Camera Preview")
        }
    }
}

@Composable
private fun WithText(text: String) {
    Text(text, color = Color.Green)
}

private fun Bitmap.rotate(degrees: Int): Bitmap =
    Bitmap.createBitmap(
        this,
        0,
        0,
        width,
        height,
        Matrix().apply { postRotate(degrees.toFloat()) },
        true
    )

package com.saucelabdemo

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy

data class ImageAnalyzerOutput(
    val bitmap: Bitmap?,
    val imageInfo: ImageInfo?
)

class ImageAnalyzer(private val onAnalyzerImage: (ImageAnalyzerOutput) -> Unit) :
    ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        onAnalyzerImage.invoke(
            ImageAnalyzerOutput(
                image.toBitmap(),
                imageInfo = image.imageInfo
            )
        )
        image.close()
    }

}

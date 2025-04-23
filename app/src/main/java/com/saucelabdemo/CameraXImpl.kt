package com.saucelabdemo

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OutputImageFormat
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val TAG = "CameraXImpl"
private const val RATIO_16_9 = 16f / 9f
private const val UPPER_RESOLUTION_BOUND_1080P = 1920

class CameraXImpl(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    val previewView: PreviewView,
    val imageAnalyzer: ImageAnalyzer
) : LifecycleObserver {

    data class Config(
        val previewResolutionSelector: ResolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
            .build(),
        val analyzerResolutionSelector: ResolutionSelector = ResolutionSelector.Builder()
            .setResolutionFilter { supportedSizes, _ ->
                Log.d(TAG, "Supported camera resolutions: ${supportedSizes.joinToString(", ")}")
                supportedSizes
                    .filter { size ->
                        val max = max(size.height, size.width)
                        val min = min(size.height, size.width)

                        val ratio = max / min.toFloat()

                        abs(RATIO_16_9 - ratio) < 0.1f && max <= UPPER_RESOLUTION_BOUND_1080P
                    }
                    .sortedByDescending { it.width + it.height }
                    .apply {
                        Log.d(
                            TAG,
                            "Requested camera resolutions: ${this.joinToString(", ")}"
                        )
                    }
            }
            .build(),
        val enableTapToFocus: Boolean = true,
        @OutputImageFormat
        val analyzerFormat: Int = ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
    )

    private var processCameraProvider: ProcessCameraProvider? = null

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            cameraExecutor.shutdown()
        }
    }

    private lateinit var config: Config

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var imagePreview: Preview
        private set

    private lateinit var imageAnalysis: ImageAnalysis

    @SuppressLint("RestrictedApi")
    private fun configure(cameraProviderFuture: ListenableFuture<ProcessCameraProvider>) {
        processCameraProvider = cameraProviderFuture.get()
        val useCases = UseCaseGroup.Builder()

        imagePreview = Preview.Builder()
            .setResolutionSelector(config.previewResolutionSelector)
            .build()

        useCases.addUseCase(imagePreview)

        imageAnalysis = ImageAnalysis.Builder()
            .setOutputImageFormat(config.analyzerFormat)
            .setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(config.analyzerResolutionSelector)
            .build()
            .apply {
                setAnalyzer(cameraExecutor, imageAnalyzer)
            }

        useCases.addUseCase(imageAnalysis)

        val useCaseGroup = useCases.build()
        processCameraProvider!!.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            useCaseGroup
        )

        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        imagePreview.surfaceProvider = previewView.surfaceProvider

        Log.d(
            TAG,
            "Camera initialized, analyzer resolution: ${imageAnalysis.resolutionInfo?.resolution}"
        )
    }

    fun startCamera(config: Config = Config()) {
        with(lifecycleOwner.lifecycle) {
            removeObserver(lifecycleObserver)
            addObserver(lifecycleObserver)
        }

        Log.d(TAG, "Start camera called with: $config")
        this.config = config

        processCameraProvider?.unbindAll()
        ProcessCameraProvider.getInstance(context).apply {
            addListener({ configure(this) }, ContextCompat.getMainExecutor(context))
        }
    }
}

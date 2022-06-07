package com.example.sticker2d

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val finderView: PreviewView,
    private val lifecycleOwner: LifecycleOwner,
    private var cameraSelectorOption: Int = CameraSelector.LENS_FACING_BACK
) {

    private var preview: Preview? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutorService: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private val TAG = "CameraX_TAG"

    init {
        createNewExecutor()
    }

    private fun createNewExecutor() {
            cameraExecutorService = Executors.newSingleThreadExecutor()
    }

    fun startCamera() {
        lifecycleOwner.lifecycleScope.launch {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                // Used to bind the lifecycle of cameras to the lifecycle owner
                cameraProvider = cameraProviderFuture.get()

                // preview
                preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(finderView.surfaceProvider)
                    }

                // setup take photo
                imageCapture = ImageCapture.Builder().build()

                // Select back camera as a default
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraSelectorOption)
                    .build()


                // setup camera config
                setupCameraConfig(cameraProvider, cameraSelector)

            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun setupCameraConfig(
        cameraProvider: ProcessCameraProvider?,
        cameraSelector: CameraSelector
    ) {
        try {
            // Unbind use cases before rebinding
            cameraProvider?.unbindAll()
            // Bind use cases to camera
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.i(TAG, "setupCameraConfig: ${e.message}")
        }
    }

    fun changeCameraSelector() {
        cameraProvider?.unbindAll()
        cameraSelectorOption =
            if (cameraSelectorOption == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        startCamera()
    }

    fun takePhoto(
        name: String = "",
        path: String = "",
        onTakePhotoListener: TakePhotoListener? = null
    ) {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        // Create time stamped name and MediaStore entry.
        var nameImage: String = name
        if (name.isEmpty()) {
            nameImage = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())
        }
        var pathImage: String = path
        if (path.isEmpty()) {
            pathImage = PATH_DEFAULT_PICTURE
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nameImage)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, pathImage)
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.i(TAG, "Photo capture failed: ${exc.message}", exc)
                    onTakePhotoListener?.onError(exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Log.i(TAG, msg)
                    onTakePhotoListener?.onSaveImageSuccess(output)
                }
            })
    }

    interface TakePhotoListener {
        fun onSaveImageSuccess(output: ImageCapture.OutputFileResults)
        fun onError(exc: ImageCaptureException)
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PATH_DEFAULT_PICTURE = "Pictures/CameraX-Image"
    }
}
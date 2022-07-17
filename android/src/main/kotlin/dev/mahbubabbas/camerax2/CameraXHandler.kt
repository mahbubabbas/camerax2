package dev.mahbubabbas.camerax2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.annotation.IntDef
import androidx.annotation.NonNull
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
//import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import io.flutter.plugin.common.*
import io.flutter.view.TextureRegistry
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dev.mahbubabbas.camerax2.Constants.GALLERY_TITLE
import dev.mahbubabbas.camerax2.Constants.MERA_TYPE_JPEG
import java.nio.file.Files.createFile

class CameraXHandler(private val activity: Activity, private val textureRegistry: TextureRegistry) :
    MethodChannel.MethodCallHandler, EventChannel.StreamHandler,
    PluginRegistry.RequestPermissionsResultListener {
    companion object {
        private const val REQUEST_CODE = 19930430
    }

    private var sink: EventChannel.EventSink? = null
    private var listener: PluginRegistry.RequestPermissionsResultListener? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var textureEntry: TextureRegistry.SurfaceTextureEntry? = null

    private var imageCapture: ImageCapture? = null
    lateinit var imgCaptureExecutor: ExecutorService

    @AnalyzeMode
    private var analyzeMode: Int = AnalyzeMode.NONE

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        when (call.method) {
            "state" -> stateNative(result)
            "request" -> requestNative(result)
            "start" -> startNative(call, result)
            "torch" -> torchNative(call, result)
            "analyze" -> analyzeNative(call, result)
            "capture" -> capturePhoto(call, result)
            "stop" -> stopNative(result)
            else -> result.notImplemented()
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        this.sink = events
    }

    override fun onCancel(arguments: Any?) {
        sink = null
    }

    private fun stateNative(result: MethodChannel.Result) {
        // Can't get exact denied or not_determined state without request.
        // Just return not_determined when state isn't authorized
        val state =
            if (
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) 1
            else 0

        result.success(state)
    }

    private fun requestNative(result: MethodChannel.Result) {
        listener = PluginRegistry.RequestPermissionsResultListener { requestCode, _, grantResults ->
            if (requestCode != REQUEST_CODE) {
                false
            } else {
                val authorized = grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED

                result.success(authorized)
                listener = null
                true
            }
        }

        val permissions =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )

        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${activity.applicationContext.packageName}")
                ContextCompat.startActivity(activity, intent, null)
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startNative(call: MethodCall, result: MethodChannel.Result) {
        val future = ProcessCameraProvider.getInstance(activity)
        val executor = ContextCompat.getMainExecutor(activity)

        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        future.addListener({
            cameraProvider = future.get()
            textureEntry = textureRegistry.createSurfaceTexture()
            val textureId = textureEntry!!.id()

            //for image capture
            imageCapture = ImageCapture.Builder().build()

            // Preview
            val surfaceProvider = Preview.SurfaceProvider { request ->
                val resolution = request.resolution
                val texture = textureEntry!!.surfaceTexture()
                texture.setDefaultBufferSize(resolution.width, resolution.height)
                val surface = Surface(texture)
                request.provideSurface(surface, executor, { })
            }

            val preview = Preview.Builder()
                .build()
                .apply { setSurfaceProvider(surfaceProvider) }

            // Analyzer
            val analyzer = ImageAnalysis.Analyzer { imageProxy -> // YUV_420_888 format
                when (analyzeMode) {
                    AnalyzeMode.BARCODE -> {
//                        val mediaImage = imageProxy.image ?: return@Analyzer
//                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//                        val scanner = BarcodeScanning.getClient()
//                        scanner.process(inputImage)
//                                .addOnSuccessListener { barcodes ->
//                                    for (barcode in barcodes) {
//                                        val event = mapOf("name" to "barcode", "data" to barcode.data)
//                                        sink?.success(event)
//                                    }
//                                }
//                                .addOnFailureListener { e -> Log.e(TAG, e.message, e) }
//                                .addOnCompleteListener { imageProxy.close() }
                    }
                    AnalyzeMode.FACE -> {
                        val mediaImage = imageProxy.image ?: return@Analyzer
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        val realTimeOpts = FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                            .setMinFaceSize(0.5f)
                            .build()

                        val scanner = FaceDetection.getClient(realTimeOpts)
                        scanner
                            .process(inputImage)
                            .addOnSuccessListener { faces ->
                                if (faces.size > 0) {
                                    for (face in faces) {
                                        val event = mapOf(
                                            "name" to "face",
                                            "data" to face.data,
                                            "imageSize" to mapOf(
                                                "width" to imageProxy.width.toDouble(),
                                                "height" to imageProxy.height.toDouble()
                                            )
                                        )
                                        sink?.success(event)
                                    }
                                } else {
                                    sink?.success(null)
                                }
                            }
                            .addOnFailureListener { e ->
                                run {
                                    sink?.success(null)
                                    Log.e(TAG, e.message, e)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                    else -> {
                        sink?.success(null)
                        imageProxy.close()
                    }
                }
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().apply { setAnalyzer(executor, analyzer) }

            // Bind to lifecycle.
            val owner = activity as LifecycleOwner
            val selector =
                if (call.arguments == 0) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
            camera =
                cameraProvider!!.bindToLifecycle(owner, selector, preview, analysis, imageCapture)

            camera!!.cameraInfo.torchState.observe(owner) { state ->
                // TorchState.OFF = 0; TorchState.ON = 1
                val event = mapOf("name" to "torchState", "data" to state)
                sink?.success(event)
            }

            // TODO: seems there's not a better way to get the final resolution
            @SuppressLint("RestrictedApi")
            val resolution = preview.attachedSurfaceResolution!!
            val portrait = camera!!.cameraInfo.sensorRotationDegrees % 180 == 0
            val width = resolution.width.toDouble()
            val height = resolution.height.toDouble()
            val size = if (portrait) mapOf(
                "width" to width,
                "height" to height
            ) else mapOf(
                "width" to height,
                "height" to width
            )

            val answer =
                mapOf(
                    "textureId" to textureId,
                    "size" to size,
                    "portrait" to portrait,
                    "torchable" to camera!!.torchable
                )
            result.success(answer)
        }, executor)
    }

    private fun torchNative(call: MethodCall, result: MethodChannel.Result) {
        val state = call.arguments == 1
        camera!!.cameraControl.enableTorch(state)
        result.success(null)
    }

    private fun analyzeNative(call: MethodCall, result: MethodChannel.Result) {
        analyzeMode = call.arguments as Int
        result.success(null)
    }

    private fun stopNative(result: MethodChannel.Result) {
        val owner = activity as LifecycleOwner
        camera!!.cameraInfo.torchState.removeObservers(owner)
        cameraProvider!!.unbindAll()
        textureEntry!!.release()

        analyzeMode = AnalyzeMode.NONE
        camera = null
        textureEntry = null
        cameraProvider = null

        result.success(null)
    }

    //capture photo
    private fun capturePhoto(call: MethodCall, result: MethodChannel.Result) {
        imageCapture?.let {
            val fileName = getFileName()

            var outputFileOptions: ImageCapture.OutputFileOptions? = null

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                // image path
                val imageDir = getOutputDirectory(activity, GALLERY_TITLE)
                val isImageDirCreated: Boolean = imageDir.exists()
                if (isImageDirCreated) {
                    val newImage = createFile(imageDir, fileName)
                    outputFileOptions = ImageCapture.OutputFileOptions.Builder(newImage).build()
                } else {
                    Log.i(TAG, "Failed to create image directory!")
                }
            } else {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, MERA_TYPE_JPEG)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$GALLERY_TITLE")
                }

                outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                    activity.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build()
            }

            it.takePicture(
                outputFileOptions!!,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        result.success(outputFileResults.savedUri?.path + '/' + fileName)
                        Log.i(
                            TAG,
                            "The image has been saved in ${outputFileResults.savedUri?.path}/${fileName}"
                        )
                    }

                    override fun onError(exception: ImageCaptureException) {
                        result.error("IMAGE_CAPTURE_ERROR", exception.message, "")
                        Log.d(TAG, "Error taking photo:$exception")
                    }
                })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        return listener?.onRequestPermissionsResult(requestCode, permissions, grantResults) ?: false
    }

}

@IntDef(AnalyzeMode.NONE, AnalyzeMode.BARCODE, AnalyzeMode.FACE)
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class AnalyzeMode {
    companion object {
        const val NONE = 0
        const val BARCODE = 1
        const val FACE = 2
    }
}
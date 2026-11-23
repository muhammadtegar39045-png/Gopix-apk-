package com.gopix.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentZoom = 1.0f
    private var isNightMode = false
    private var isPortraitMode = false
    private var isHdrMode = true
    private var isLivePhoto = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewView = PreviewView(this)
        setContentView(previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (allPermissionsGranted()) startCamera() else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 10)
    }
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).setFlashMode(ImageCapture.FLASH_MODE_AUTO).build()
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
            videoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture)
                camera?.cameraControl?.setExposureCompensationIndex(1)
                applyZoom(1.0f)
            } catch (e: Exception) { Toast.makeText(this, "Gagal buka kamera: ${e.message}", Toast.LENGTH_SHORT).show() }
        }, ContextCompat.getMainExecutor(this))
    }
    fun applyZoom(zoom: Float) { currentZoom = zoom.coerceIn(0.5f, 10f); camera?.cameraControl?.setZoomRatio(currentZoom) }
    fun zoomWide() = applyZoom(0.5f)
    fun zoom1x() = applyZoom(1.0f)
    fun zoom10x() = applyZoom(10.0f)
    fun setHdr(on: Boolean) { isHdrMode = on }
    fun setNightPortrait(on: Boolean) { isPortraitMode = on; isNightMode = on; if (on) camera?.cameraControl?.setExposureCompensationIndex(2) else camera?.cameraControl?.setExposureCompensationIndex(1) }
    fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(getExternalFilesDir(null), "Gopix_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
        val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(output, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) { Toast.makeText(this@MainActivity, "Foto tersimpan jernih!", Toast.LENGTH_SHORT).show() }
            override fun onError(exc: ImageCaptureException) { Toast.makeText(this@MainActivity, "Gagal: ${exc.message}", Toast.LENGTH_SHORT).show() }
        })
    }
    fun startVideo() {
        val videoCapture = videoCapture ?: return
        val videoFile = File(getExternalFilesDir(null), "Gopix_Video_${System.currentTimeMillis()}.mp4")
        val output = FileOutputOptions.Builder(videoFile).build()
        videoCapture.output.prepareRecording(this, output).withAudioEnabled().start(ContextCompat.getMainExecutor(this)) {}
        Toast.makeText(this, "Rekam Video Stabil...", Toast.LENGTH_SHORT).show()
    }
    fun importConfig(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            if (text.contains("HDR")) setHdr(true)
            if (text.contains("Night")) setNightPortrait(true)
            Toast.makeText(this, "Config berhasil di-import!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(this, "Config gagal", Toast.LENGTH_SHORT).show() }
    }
    override fun onDestroy() { super.onDestroy(); cameraExecutor.shutdown() }
}

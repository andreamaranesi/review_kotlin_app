package com.project.review.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.valueIterator
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.project.review.R
import com.project.review.settings.Settings
import com.project.review.settings.Tools
import java.lang.reflect.Field


/**
 * initializes the back camera to detect a barcode
 */
class ScannerActivity : AppCompatActivity() {

    companion object {
        const val MY_CAMERA_REQUEST_CODE = 100
    }

    private lateinit var cameraSurface: SurfaceView
    private lateinit var cameraSource: CameraSource

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.camera_granted), Toast.LENGTH_LONG).show()
                init(requestedPermissions = true)
            } else {
                Toast.makeText(this, getString(R.string.camera_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    private var start: Float = 0F
    private var top: Float = 0F
    private var end: Float = 0F
    private var bottom: Float = 0F


    /**
     * prompts the user for camera access permission
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.barcode_view)
        initValues()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                MY_CAMERA_REQUEST_CODE
            )
        } else
            init()

    }

    private fun initValues() {
        val widthDivider = ScannerOverlayWidget.widthDivider
        val heightDivider = ScannerOverlayWidget.heightDivider

        val screenWidthPx = Tools.getScreenWidthPx(this)
        val screenHeightPx = Tools.getScreenHeightPx(this)


        start = (screenWidthPx / widthDivider).toFloat()
        top = (screenHeightPx / heightDivider).toFloat()
        end =
            (screenWidthPx / widthDivider + (screenWidthPx - (screenWidthPx / widthDivider) * 2)).toFloat()
        bottom =
            (screenHeightPx / heightDivider + (screenHeightPx - (screenHeightPx / heightDivider) * 2)).toFloat()
    }

    /**
     * when a barcode is detected, it checks that it is contained within one
       determined area, which on a graphic level is represented by the transparent rectangle in the center
       of sight
     */
    private fun checkPosition(barcodes: SparseArray<Barcode>): Barcode? {


        for (barcode: Barcode in barcodes.valueIterator()) {
            val position = barcode.boundingBox

            val rLeft = position.left
            val rRight = position.right

            println(position.top.toString() + " " + position.bottom.toString())
            println("$start $top $end $bottom")
            if (rLeft >= start && rRight <= end) {
                return barcode
            }


        }

        return null
    }

    private fun connectSurfaceAndCamera() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraSource.start(cameraSurface.holder)
        }
    }

    private var flash: Boolean = false

    /**
     *
     * from the CameraSource object goes back to the property that contains the android.hardware.Camera object,
       which contains the methods needed to activate the flash
     *
     * @author Github
     */
    private fun getCamera(cameraSource: CameraSource): android.hardware.Camera? {
        val declaredFields: Array<Field> = CameraSource::class.java.declaredFields
        for (field in declaredFields) {
            if (field.type === android.hardware.Camera::class.java) {
                field.isAccessible = true
                try {
                    val camera: android.hardware.Camera =
                        field.get(cameraSource) as android.hardware.Camera
                    return if (camera != null) {
                        camera
                    } else null
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
                break
            }
        }
        return null
    }

    /**
     * binds the SurfaceView component to the CameraSource object
     *
     * @see cameraSurface
     * @see cameraSource
     */
    private fun init(requestedPermissions: Boolean = false) {

        cameraSurface = findViewById(R.id.cameraSurface)
        val barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.ALL_FORMATS)
            .build()

        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setAutoFocusEnabled(true)
            .setRequestedFps(20.0f)
            .build()

        // CHECKS IF THE CAMERA HAS FLASH
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            this.flash = true

        val flashOn = findViewById<ImageButton>(R.id.flash_on_button)
        val flashOff = findViewById<ImageButton>(R.id.flash_off_button)

        if (this.flash) {
            flashOff.setOnClickListener {
                setFlash(cameraSource, true)
                flashOff.visibility = View.GONE
                flashOn.visibility = View.VISIBLE
            }

            flashOn.setOnClickListener {
                setFlash(cameraSource, false)
                flashOn.visibility = View.GONE
                flashOff.visibility = View.VISIBLE
            }
        } else
            flashOff.visibility = View.GONE


        val context = this

        if (requestedPermissions && !cameraSurface.holder.isCreating)
            connectSurfaceAndCamera()


        cameraSurface.holder.addCallback(object : SurfaceHolder.Callback {


            override fun surfaceCreated(holder: SurfaceHolder) {
                connectSurfaceAndCamera()
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {

            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                cameraSource.stop()
            }


        })
        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {

            }

            /**
             * detects bar codes inside the i-th frame of the camera
             *
             * takes only a qr code and returns it to the calling activity
             *
             * @see com.project.review.MainActivity
             * @see com.project.review.MainActivity.onCameraClick
             */
            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems

                if (barcodes.size() != 0) {

                    val barcode: Barcode? = checkPosition(barcodes)

                    if (barcode != null) {
                        context.runOnUiThread {
                            cameraSource.stop()
                            val intent = Intent()

                            intent.apply {
                                putExtra(Settings.CODE, barcode.displayValue)
                                putExtra(Settings.BARCODE_FORMAT, barcode.format)
                            }
                            setResult(Activity.RESULT_OK, intent)
                            finish()

                        }
                    }

                }

            }
        })


    }

    /**
     * turns the camera flash on or off
     */
    private fun setFlash(cameraSource: CameraSource, flashOn: Boolean) {
        val camera = getCamera(cameraSource)!!
        val param = getCamera(cameraSource)!!.parameters
        param.setFlashMode(
            if (flashOn) android.hardware.Camera.Parameters.FLASH_MODE_TORCH
            else android.hardware.Camera.Parameters.FLASH_MODE_OFF)
        camera.parameters = param
    }

}


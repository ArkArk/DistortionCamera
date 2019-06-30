package jp.ac.titech.itpro.sdl.distortioncamera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.Toast
import com.androidexperiments.shadercam.fragments.CameraFragment
import com.androidexperiments.shadercam.fragments.PermissionsHelper
import com.androidexperiments.shadercam.gl.CameraRenderer

import com.androidexperiments.shadercam.utils.ShaderUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.abs


class MainActivity : FragmentActivity(), CameraRenderer.OnRendererReadyListener, PermissionsHelper.PermissionsListener, SensorEventListener {

    private var cameraFragment: CameraFragment? = null
    private var distortionRenderer: DistortionRenderer? = null

    private lateinit var textureView: TextureView

    private var shouldRestartCamera = false

    private lateinit var permissionsHelper: PermissionsHelper
    private var permissionsSatisfied: Boolean = false

    private var sensorManager: SensorManager? = null
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null

    private var prevGyroscopeTimestamp: Long? = null
    private var angle: Double = 0.0

    private var prevAccelerometerTimestamp: Long? = null
    private var accZ: Double = 0.0
    private var velZ: Double = 0.0
    private var posZ: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.texture_view)

        setupCameraFragment()
        setupSensor()

        if (PermissionsHelper.isMorHigher()) {
            setupPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")

        initSensorValue()
        sensorManager?.registerListener(this, gyroscope!!, SensorManager.SENSOR_DELAY_GAME)
        sensorManager?.registerListener(this, accelerometer!!, SensorManager.SENSOR_DELAY_GAME)

        ShaderUtils.goFullscreen(this.window)

        if (PermissionsHelper.isMorHigher() && !permissionsSatisfied) {
            if (permissionsHelper.checkPermissions()) {
                permissionsSatisfied = true
            } else {
                return
            }
        }

        if (!textureView.isAvailable) {
            textureView.surfaceTextureListener = textureListener
        } else {
            setReady(textureView.surfaceTexture, textureView.width, textureView.height)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause()")

        shutdownCamera(false)
        textureView.surfaceTextureListener = null
        sensorManager?.unregisterListener(this)
    }

    override fun onPermissionsSatisfied() {
        Log.d(TAG, "onPermissionsSatisfied()")

        permissionsSatisfied = true
    }

    override fun onPermissionsFailed(failedPermissions: Array<String>) {
        Log.e(TAG, "onPermissionsFailed(): " + Arrays.toString(failedPermissions))

        permissionsSatisfied = false
        Toast.makeText(
            this, R.string.toast_permissions_failed,
            Toast.LENGTH_LONG
        ).show()
        this.finish()
    }

    override fun onRendererReady() {
        runOnUiThread {
            cameraFragment?.setPreviewTexture(distortionRenderer?.previewTexture)
            cameraFragment?.openCamera()
        }
    }

    override fun onRendererFinished() {
        runOnUiThread {
            if (shouldRestartCamera) {
                setReady(textureView.surfaceTexture, textureView.width, textureView.height)
                shouldRestartCamera = false
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val timestamp = event.timestamp
        when(event.sensor.type) {

            Sensor.TYPE_GYROSCOPE -> {
                val omegaZ = event.values[2]  // z-axis angular velocity [rad/sec]
                prevGyroscopeTimestamp?.let {
                    val dt = (timestamp - it) * 1e-9
                    angle += omegaZ * dt
                }
                distortionRenderer?.angle = angle.toFloat()
                prevGyroscopeTimestamp = timestamp
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                accZ = event.values[2].toDouble()  // z-axis acceleration excluding gravity [m/s^2]
                if (abs(accZ) < 1.0) accZ = 0.0
                prevAccelerometerTimestamp?.let {
                    val dt = (timestamp - it) * 1e-9
                    posZ += velZ*dt
                    velZ += accZ*dt
                    velZ *= 0.9
                }
                distortionRenderer?.posZ = posZ.toFloat()
                prevAccelerometerTimestamp = timestamp
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d(TAG, "onAccuracyChanged: accuracy=$accuracy")
    }

    fun onClickSwapCameraButton(view: View) {
        Log.d(TAG, "onClickSwapCameraButton")
        cameraFragment?.swapCamera()
    }

    fun onClickTakeCameraButton(view: View) {
        Log.d(TAG, "onClickTakeCameraButton")
        saveImageFile()
    }

    fun onClickInitSensorButton(view: View) {
        Log.d(TAG, "onClickInitSensorButton")
        initSensorValue()
    }

    private fun setupPermissions() {
        permissionsHelper = PermissionsHelper.attach(this)
        permissionsHelper.setRequestedPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun getRenderer(surface: SurfaceTexture, width: Int, height: Int): DistortionRenderer {
        return DistortionRenderer(this, surface, width, height)
    }

    private fun setReady(surface: SurfaceTexture, width: Int, height: Int) {
        distortionRenderer = getRenderer(surface, width, height).also {distortionRenderer ->
            distortionRenderer.setCameraFragment(cameraFragment)
            distortionRenderer.setOnRendererReadyListener(this)
            distortionRenderer.start()
        }

        cameraFragment?.configureTransform(width, height)
    }

    private fun setupCameraFragment() {
        if (cameraFragment != null && cameraFragment!!.isAdded) {
            return
        }

        cameraFragment = CameraFragment.getInstance().also { cameraFragment ->
            cameraFragment.setCameraToUse(CameraFragment.CAMERA_PRIMARY)
            cameraFragment.setTextureView(textureView)

            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(cameraFragment, TAG_CAMERA_FRAGMENT)
            transaction.commit()
        }
    }

    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            Toast.makeText(this, R.string.toast_no_sensor_manager, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyroscope == null) {
            Toast.makeText(this, R.string.toast_no_gyroscope, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (accelerometer == null) {
            Toast.makeText(this, R.string.toast_no_linear_acceleration, Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    private fun initSensorValue() {
        prevGyroscopeTimestamp = null
        angle = 0.0

        prevAccelerometerTimestamp = null
        accZ = 0.0
        velZ = 0.0
        posZ = 0.0
    }

    private fun shutdownCamera(shouldRestart: Boolean) {
        if (PermissionsHelper.isMorHigher() && !permissionsSatisfied) return

        cameraFragment?.closeCamera()

        distortionRenderer?.let { renderer ->
            shouldRestartCamera = shouldRestart
            renderer.renderHandler.sendShutdown()
        }
        distortionRenderer = null
    }

    private fun getImageFile(): File {
        val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val fileName = System.currentTimeMillis().toString() + "_" + IMAGE_FILE_NAME + ".jpg"
        return File(directoryPath, fileName)
    }

    private fun saveImageFile() {
        val file = getImageFile()
        val fos = FileOutputStream(file)
        val bitmap = textureView.bitmap
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.close()

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        Toast.makeText(this, "Saved: $file", Toast.LENGTH_LONG).show()
    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            setReady(surface, width, height)
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            cameraFragment?.configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean = true

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val TAG_CAMERA_FRAGMENT = "TAG_CAMERA_FRAGMENT"
        private val IMAGE_FILE_NAME = "DISTORTION_IMAGE"
    }
}
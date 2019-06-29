package jp.ac.titech.itpro.sdl.distortioncamera

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.androidexperiments.shadercam.fragments.PermissionsHelper

import com.androidexperiments.shadercam.fragments.VideoFragment
import com.androidexperiments.shadercam.utils.ShaderUtils
import com.uncorkedstudios.android.view.recordablesurfaceview.RecordableSurfaceView
import java.util.*


class MainActivity : FragmentActivity(), PermissionsHelper.PermissionsListener, SensorEventListener {

    private var videoFragment: VideoFragment? = null

    private lateinit var recordableSurfaceView: RecordableSurfaceView
    private lateinit var renderer: DistortionRenderer
    private lateinit var permissionsHelper: PermissionsHelper

    private var sensorManager: SensorManager? = null
    private var gyroscope: Sensor? = null

    private var prevTimestamp: Double? = null
    private var angle: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        setContentView(R.layout.activity_main)

        if (PermissionsHelper.isMorHigher()) {
            setupPermissions()
        }

        setupSensor()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")

        renderer = DistortionRenderer(this)
        recordableSurfaceView = findViewById(R.id.surface_view)
        setupAngle()
        sensorManager?.registerListener(this, gyroscope!!, SensorManager.SENSOR_DELAY_FASTEST)

        ShaderUtils.goFullscreen(this.window)

        if (PermissionsHelper.isMorHigher()) {
            if (!permissionsHelper.checkPermissions()) {
                return
            } else {
                setupVideoFragment()
                recordableSurfaceView.resume()

                val size = android.graphics.Point()
                windowManager.defaultDisplay.getRealSize(size)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause()")

        shutdownCamera()
        recordableSurfaceView.pause()
        sensorManager?.unregisterListener(this)
    }

    override fun onPermissionsSatisfied() {
        Log.d(TAG, "onPermissionsSatisfied()")
    }

    override fun onPermissionsFailed(failedPermissions: Array<String>) {
        Log.e(TAG, "onPermissionsFailed(): " + Arrays.toString(failedPermissions))
        Toast.makeText(
            this, R.string.toast_permissions_failed,
            Toast.LENGTH_LONG
        ).show()
        this.finish()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val omegaZ = event.values[2]  // z-axis angular velocity (rad/sec)
        val timestamp = event.timestamp
        prevTimestamp?.let {
            val sec = (timestamp - it) * 1e-9
            angle += omegaZ * sec
        }
        renderer.angle = angle
        prevTimestamp = timestamp.toDouble()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d(TAG, "onAccuracyChanged: accuracy=$accuracy")
    }

    fun onClickSwapCameraButton(view: View) {
        videoFragment?.swapCamera()
    }

    private fun setupPermissions() {
        permissionsHelper = PermissionsHelper.attach(this)
        permissionsHelper.setRequestedPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun setupVideoFragment() {
        videoFragment = VideoFragment.getInstance().also { videoFragment ->
            videoFragment.setRecordableSurfaceView(recordableSurfaceView)
            videoFragment.videoRenderer = renderer
            videoFragment.setCameraToUse(VideoFragment.CAMERA_PRIMARY)

            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(videoFragment, TAG_CAMERA_FRAGMENT)
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
    }

    private fun setupAngle() {
        angle = 0.0
        prevTimestamp = null
    }

    private fun shutdownCamera() {
        videoFragment?.let {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.remove(it)
            transaction.commit()
        }
        videoFragment = null
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val TAG_CAMERA_FRAGMENT = "TAG_CAMERA_FRAGMENT"
    }
}
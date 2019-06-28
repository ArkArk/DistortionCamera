package jp.ac.titech.itpro.sdl.distortioncamera

import android.Manifest
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.widget.Toast
import com.androidexperiments.shadercam.fragments.PermissionsHelper

import com.androidexperiments.shadercam.fragments.VideoFragment
import com.androidexperiments.shadercam.gl.VideoRenderer
import com.androidexperiments.shadercam.utils.ShaderUtils
import com.uncorkedstudios.android.view.recordablesurfaceview.RecordableSurfaceView
import java.util.*


class MainActivity : FragmentActivity(), PermissionsHelper.PermissionsListener {

    private var videoFragment: VideoFragment? = null

    private lateinit var recordableSurfaceView: RecordableSurfaceView
    private lateinit var videoRenderer: VideoRenderer
    private lateinit var permissionsHelper: PermissionsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        setContentView(R.layout.activity_main)

        if (PermissionsHelper.isMorHigher()) {
            setupPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")

        videoRenderer = TestRenderer(this)
        recordableSurfaceView = findViewById(R.id.texture_view)

        ShaderUtils.goFullscreen(this.window)

        if (PermissionsHelper.isMorHigher()) {
            if (!permissionsHelper.checkPermissions()) {
                return
            } else {
                setupVideoFragment(videoRenderer)
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
    }

    override fun onPermissionsSatisfied() {
        Log.d(TAG, "onPermissionsSatisfied()")
    }

    override fun onPermissionsFailed(failedPermissions: Array<String>) {
        Log.e(TAG, "onPermissionsFailed(): " + Arrays.toString(failedPermissions))
        Toast.makeText(
            this, "DistortionCamera needs all permissions to function, please try again.",
            Toast.LENGTH_LONG
        ).show()
        this.finish()
    }

    private fun setupPermissions() {
        permissionsHelper = PermissionsHelper.attach(this)
        permissionsHelper.setRequestedPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun setupVideoFragment(renderer: VideoRenderer) {
        videoFragment = VideoFragment.getInstance().also { videoFragment ->
            videoFragment.setRecordableSurfaceView(recordableSurfaceView)
            videoFragment.videoRenderer = renderer
            videoFragment.setCameraToUse(VideoFragment.CAMERA_PRIMARY)

            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(videoFragment, TAG_CAMERA_FRAGMENT)
            transaction.commit()
        }
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
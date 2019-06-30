package jp.ac.titech.itpro.sdl.distortioncamera

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import com.androidexperiments.shadercam.gl.CameraRenderer

class DistortionRenderer(context: Context, previewSurface: SurfaceTexture, width: Int, height: Int) : CameraRenderer(context, previewSurface, width, height, "camera.frag", "camera.vert") {

    var angle: Float = 0f
    var posZ: Float = 0f

    var activeGyroscope: Boolean = false
    var activeAccelerometer: Boolean = false

    override fun setUniformsAndAttribs() {
        super.setUniformsAndAttribs()

        val resolutionHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "resolution")
        GLES20.glUniform3f(resolutionHandle, 1f, 1f, 1f)

        val angleHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "angle")
        GLES20.glUniform1f(angleHandle, angle)

        val posZHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "posZ")
        GLES20.glUniform1f(posZHandle, posZ)

        val activeGyroscopeHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "activeGyroscope")
        GLES20.glUniform1i(activeGyroscopeHandle, if (activeGyroscope) 1 else 0)

        val activeAccelerometerHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "activeAccelerometer")
        GLES20.glUniform1i(activeAccelerometerHandle, if (activeAccelerometer) 1 else 0)
    }
}

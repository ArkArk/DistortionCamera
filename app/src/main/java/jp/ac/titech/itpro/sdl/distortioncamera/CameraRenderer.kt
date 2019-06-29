package jp.ac.titech.itpro.sdl.distortioncamera

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.androidexperiments.shadercam.gl.VideoRenderer

class CameraRenderer(context: Context) : VideoRenderer(context, "camera.frag", "camera.vert") {

    var angle: Double = 0.0

    override fun setUniformsAndAttribs() {
        super.setUniformsAndAttribs()

        val resolutionHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "resolution")
        GLES20.glUniform3f(resolutionHandle, 1f, 1f, 1f)

        val angleHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "angle")
        GLES20.glUniform1f(angleHandle, angle.toFloat())
    }
}

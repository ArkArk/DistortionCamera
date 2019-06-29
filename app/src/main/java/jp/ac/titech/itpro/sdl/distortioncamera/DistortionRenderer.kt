package jp.ac.titech.itpro.sdl.distortioncamera

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import com.androidexperiments.shadercam.gl.CameraRenderer

class DistortionRenderer(context: Context, previewSurface: SurfaceTexture, width: Int, height: Int) : CameraRenderer(context, previewSurface, width, height, "camera.frag", "camera.vert") {

    var angle: Float = 0f

    override fun setUniformsAndAttribs() {
        super.setUniformsAndAttribs()

        val resolutionHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "resolution")
        GLES20.glUniform3f(resolutionHandle, 1f, 1f, 1f)

        val angleHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "angle")
        GLES20.glUniform1f(angleHandle, angle)
    }
}

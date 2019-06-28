package jp.ac.titech.itpro.sdl.distortioncamera

import android.content.Context
import android.opengl.GLES20
import com.androidexperiments.shadercam.gl.VideoRenderer

class TestRenderer(context: Context) : VideoRenderer(context, "test.frag", "test.vert") {

    override fun setUniformsAndAttribs() {
        super.setUniformsAndAttribs()

        val resolutionHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "resolution")
        GLES20.glUniform3f(resolutionHandle, 1f, 1f, 1f)
    }
}

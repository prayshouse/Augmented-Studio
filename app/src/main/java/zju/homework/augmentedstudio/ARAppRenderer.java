package zju.homework.augmentedstudio;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;

import com.vuforia.Device;
import com.vuforia.State;
import com.vuforia.Vuforia;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import zju.homework.augmentedstudio.Activities.ARSceneActivity;
import zju.homework.augmentedstudio.Models.CubeObject;
import zju.homework.augmentedstudio.Shaders.CubeShaders;
import zju.homework.augmentedstudio.Models.MeshObject;
import zju.homework.augmentedstudio.Models.Texture;
import zju.homework.augmentedstudio.Interfaces.ARAppRendererControl;
import zju.homework.augmentedstudio.GL.ARBaseRenderer;
import zju.homework.augmentedstudio.Utils.Util;

/**
 * Created by stardust on 2016/12/12.
 */

public class ARAppRenderer implements GLSurfaceView.Renderer, ARAppRendererControl{


    private static final String LOGTAG = "ARAppRenderer";

    private ARApplicationSession vuforiaAppSession;
    private ARBaseRenderer mSampleAppRenderer;

    private boolean mIsActive = false;

   private Vector<Texture> mTextures = null;
    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;
    Texture texture = null;

    // Constants:
    static final float kObjectScale = 3.f;

//    private Teapot mTeapot;

    // Reference to main activity
    private ARSceneActivity mActivity;

    MeshObject cube;

    private boolean ismIsActive;

    public ARAppRenderer(ARSceneActivity activity, ARApplicationSession session) {
        mActivity = activity;
        vuforiaAppSession = session;

        mSampleAppRenderer = new ARBaseRenderer(this, mActivity, Device.MODE.MODE_AR,
                false, 10f, 5000);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        initRendering();

    }

    void initRendering(){
        Log.i(LOGTAG, "initRendering");

        cube = new CubeObject();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);

        for(Texture t : mTextures){
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = Util.createProgramFromShaderSrc(CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        if( shaderProgramID > 0 ){
            GLES20.glUseProgram(shaderProgramID);
            texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                    "texSampler2D");
            vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                    "vertexPosition");
            textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                    "vertexTexCoord");
            mvpMatrixHandle =GLES20.glGetUniformLocation(shaderProgramID,
                    "modelViewProjectionMatrix");
        }


    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call function to update rendering when render surface
        // parameters have changed:
        mActivity.updateRendering();

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        // Call function to initialize rendering:
        initRendering();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if( !ismIsActive )
            return;
        mSampleAppRenderer.render();
    }

    @Override
    public void renderFrame(State state, float[] projectionMatrix) {

//        Log.i(LOGTAG, "renderFrame");

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        float[] modelViewMatrix = new float[16];
        float[] mvpMatrix = new float[16];

        Matrix.setLookAtM(modelViewMatrix, 0,
                0, 0, 15,
                0, 0, -1,
                0, 1, 0);

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

        GLES20.glUseProgram(shaderProgramID);

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, cube.getVertices());

        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT,
                false, 0, cube.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(0).mTextureID[0]);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                mvpMatrix, 0);
        GLES20.glUniform1i(texSampler2DHandle, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                cube.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                cube.getIndices());

        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);

    }

    public boolean getActive() {
        return ismIsActive;
    }

    public void setActive(boolean ismIsActive) {
        this.ismIsActive = ismIsActive;

        if ( mIsActive ){
//            mSampleAppRenderer.configureVideoBackground();
        }
    }

    public Vector<Texture> getTextures() {
        return mTextures;
    }

    public void setTextures(Vector<Texture> mTextures) {
        this.mTextures = mTextures;
    }
}

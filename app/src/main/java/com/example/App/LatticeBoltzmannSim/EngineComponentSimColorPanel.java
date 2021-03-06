package com.example.App.LatticeBoltzmannSim;

import android.content.Context;
import android.opengl.GLES20;

import com.example.App.GraphicsEngine.Engine.EngineComponent;
import com.example.App.GraphicsEngine.Engine.EngineGLRenderer;
import com.example.App.GraphicsEngine.Utils.Ray3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class EngineComponentSimColorPanel implements EngineComponent.EngineComponentInterface {

    int programHandle;
    int mPositionHandle;
    int mColorHandle;
    int drawListBufferCapacity;
    float Vertices[] =
            {
                    -1.0f, 1.0f, 0.0f,
                    1.0f, 1.0f, 0.0f,
                    -1.0f, -1.0f, 0.0f,
                    1.0f, -1.0f, 0.0f,
            };
    float Colors[] =
            {
                    1.0f, 0.0f, 0.0f, 1.0f,
                    0.0f, 1.0f, 0.0f, 1.0f,
                    0.0f, 1.0f, 0.0f, 1.0f,
                    1.0f, 0.0f, 0.0f, 1.0f,
            };
    int[] dimensions;
    ByteBuffer bb;
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private ShortBuffer drawListBuffer;
    private short drawOrder[] = {0, 1, 2, 1, 2, 3}; // order to draw vertices

    public EngineComponentSimColorPanel(int xDimension, int yDimension) {
        super();

        dimensions = new int[]{xDimension, yDimension};

        String vertexShader =
                "uniform mat4 u_MVPMatrix;" +
                        "attribute vec4 a_v4Position;" +
                        "attribute vec4 a_v4FillColor;" +
                        "varying vec4 v_v4FillColor;" +
                        "void main()" +
                        "{" +
                        "v_v4FillColor = a_v4FillColor;" +
                        "gl_Position = a_v4Position;" +
                        "}";

        String fragmentShader =
                "precision mediump float;" +
                        "varying vec4 v_v4FillColor;" +
                        "void main()" +
                        "{" +
                        "gl_FragColor = v_v4FillColor;" +
                        "}";

        // prepare shaders and OpenGL program
        int vertexShaderHandle = EngineGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShader);

        int fragmentShaderHandle = EngineGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShader);


        programHandle = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(programHandle, vertexShaderHandle);   // add the vertex shader to program
        //GLES20.glAttachShader(mProgram, fragmentShaderOnlyColor); // add the fragment shader to program
        GLES20.glAttachShader(programHandle, fragmentShaderHandle);

        GLES20.glLinkProgram(programHandle);                  // create OpenGL program executables

        // Set program handles. These will later be used to pass in values to the program.
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_v4Position");
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_v4FillColor");

        Vertices = new float[xDimension * yDimension * 3];
        Colors = new float[xDimension * yDimension * 4];

        float xGap = 2 / (float) (xDimension - 1);
        float yGap = 2 / (float) (yDimension - 1);

        /*
        vertices are allocated like this:
        +0  +1  +2  +3
        +4  +5  +6  +7
        +8  +9  +10 +11
        color [0] is for vertice 0, color [1] is for vertice 1 and so on....

        and the triangle is like this:
        0   1   4   4   1   0
        1   2   5   5   2   6
        4   5   8   8   5   9
        6   7   10  10  7   11

        so the algoritm is a bit messy, but this is necessary because later when the color index are
        update from the simulation the method is faster and simpler.
        So more complexity now for much faster update rate later.
         */

        int count = 0;
        for (int y = 0; y < yDimension; y++) {
            for (int x = 0; x < xDimension; x++) {
                Vertices[count * 3] = (xGap * x) - 1;
                Vertices[(count * 3) + 1] = 1 - (yGap * y);
                Vertices[(count * 3) + 2] = 0;

                Colors[count * 4] = 1.0f;
                Colors[count * 4 + 1] = 1.0f;
                Colors[count * 4 + 2] = 1.0f;
                Colors[count * 4 + 3] = 1.0f;
                count++;
            }
        }

        drawOrder = new short[((Vertices.length) / 3 - yDimension) * 6];
        int countX = 0;
        count = 0;
        for (int i = 0; i <= (drawOrder.length / 6 - xDimension); i++) {
            if (countX < (xDimension - 1)) {
                drawOrder[(i * 6)] = (short) (count);
                drawOrder[(i * 6) + 1] = (short) (count + 1);
                drawOrder[(i * 6) + 2] = (short) (count + xDimension);
                drawOrder[(i * 6) + 3] = drawOrder[(i * 6) + 2];
                drawOrder[(i * 6) + 4] = drawOrder[(i * 6) + 1];
                drawOrder[(i * 6) + 5] = (short) (count + xDimension + 1);

                count++;
                countX++;
            } else {
                count++;
                countX = 0;
                i--;
            }
        }

        updateBuffer();
    }


    public void draw(float[] mViewMatrix, float[] mProjectionMatrix, float time) {
        GLES20.glUseProgram(programHandle);

        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
        GLES20.glEnableVertexAttribArray(mColorHandle);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawListBufferCapacity, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);//Draw
    }


    @Override
    public void update(float dt) {

    }

    @Override
    public void doAction(boolean restart) {

    }

    @Override
    public boolean isIntersectingRay(Ray3f ray) {
        return false;
    }

    @Override
    public float getRenderDepth() {
        return 0;
    }

    @Override
    public void setRenderDepth(float rd) {

    }

    @Override
    public void setGL_POINTER(String vertexShader, String fragmentShder) {

    }

    private void updateBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                Vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(Vertices);
        vertexBuffer.position(0);


        bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                Colors.length * 4);
        bb.order(ByteOrder.nativeOrder());
        colorBuffer = bb.asFloatBuffer();
        colorBuffer.put(Colors);
        colorBuffer.position(0);


        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        drawListBufferCapacity = drawListBuffer.capacity();

    }

    public FloatBuffer getColorBufferPointer() {
        //colorBuffer.position(0);
        return colorBuffer;
    }

    @Override
    public void setGL_POINTER(int program) {
    }

    @Override
    public String getID() {
        return "";
    }

    @Override
    public void setID(String id) {

    }

    @Override
    public void loadGLTexture(Context context, int id, boolean LoadNow) {

    }

    @Override
    public int[] getTextureID() {
        return new int[]{0};
    }

    @Override
    public int[] getTextureGL_ID() {
        return new int[]{0};
    }

    @Override
    public void setTextureGL_ID(int id) {

    }

    @Override
    public boolean getCheckForReuse() {
        return false;
    }
}

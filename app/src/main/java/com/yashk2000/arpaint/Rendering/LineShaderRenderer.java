package com.yashk2000.arpaint.Rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import com.yashk2000.arpaint.R;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.vecmath.Vector3f;

/** Renders a point cloud. */
public class LineShaderRenderer {

  private static final String TAG = LineShaderRenderer.class.getSimpleName();
  private static final int FLOATS_PER_POINT = 3; // X,Y,Z.
  private static final int BYTES_PER_FLOAT = 4;
  private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;

  private float[] mModelMatrix = new float[16];
  private float[] mModelViewMatrix = new float[16];
  private float[] mModelViewProjectionMatrix = new float[16];

  private int mPositionAttribute = 0;
  private int mPreviousAttribute = 0;
  private int mNextAttribute = 0;
  private int mSideAttribute = 0;
  private int mWidthAttribte = 0;

  private int mCountersAttribute = 0;

  private int mProjectionUniform = 0;
  private int mModelViewUniform = 0;
  private int mResolutionUniform = 0;
  private int mLineWidthUniform = 0;
  private int mColorUniform = 0;
  private int mOpacityUniform = 0;
  private int mNearUniform = 0;
  private int mFarUniform = 0;
  private int mSizeAttenuationUniform = 0;
  private int mDrawModeUniform = 0;
  private int mNearCutoffUniform = 0;
  private int mFarCutoffUniform = 0;

  private boolean mDrawMode = false;

  private int mVisibility = 0;
  private int mAlphaTest = 0;

  private float[] mPositions;
  private float[] mCounters;
  private float[] mNext;
  private float[] mSide;
  private float[] mWidth;
  private float[] mPrevious;

  private int mPositionAddress;
  private int mPreviousAddress;
  private int mNextAddress;
  private int mSideAddress;
  private int mWidthAddress;
  private int mCounterAddress;

  private int mNumPoints = 0;
  private int mNumBytes = 0;

  private int mVbo = 0;
  private int mVboSize = 0;

  private int mProgramName = 0;
  private float lineWidth = 0;

  private Vector3f mColor;

  public AtomicBoolean bNeedsUpdate = new AtomicBoolean();

  private int mLineDepthScaleUniform;
  private float mLineDepthScale = 10.0f;

  public float mDrawDistance;

  public LineShaderRenderer() {}

  public void createOnGlThread(Context context) {
    ShaderUtil.checkGLError(TAG, "before create");

    int buffers[] = new int[1];
    GLES20.glGenBuffers(1, buffers, 0);
    mVbo = buffers[0];
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
    mVboSize = 0;
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    ShaderUtil.checkGLError(TAG, "buffer alloc");

    int vertexShader =
        ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, R.raw.line_vert);
    int fragmentShader =
        ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, R.raw.line_frag);

    mProgramName = GLES20.glCreateProgram();
    GLES20.glAttachShader(mProgramName, vertexShader);
    GLES20.glAttachShader(mProgramName, fragmentShader);
    GLES20.glLinkProgram(mProgramName);
    GLES20.glUseProgram(mProgramName);

    ShaderUtil.checkGLError(TAG, "program");

    mPositionAttribute = GLES20.glGetAttribLocation(mProgramName, "position");
    mPreviousAttribute = GLES20.glGetAttribLocation(mProgramName, "previous");
    mNextAttribute = GLES20.glGetAttribLocation(mProgramName, "next");
    mSideAttribute = GLES20.glGetAttribLocation(mProgramName, "side");
    mWidthAttribte = GLES20.glGetAttribLocation(mProgramName, "width");
    mCountersAttribute = GLES20.glGetAttribLocation(mProgramName, "counters");
    mProjectionUniform = GLES20.glGetUniformLocation(mProgramName, "projectionMatrix");
    mModelViewUniform = GLES20.glGetUniformLocation(mProgramName, "modelViewMatrix");
    mResolutionUniform = GLES20.glGetUniformLocation(mProgramName, "resolution");
    mLineWidthUniform = GLES20.glGetUniformLocation(mProgramName, "lineWidth");
    mColorUniform = GLES20.glGetUniformLocation(mProgramName, "color");
    mOpacityUniform = GLES20.glGetUniformLocation(mProgramName, "opacity");
    mNearUniform = GLES20.glGetUniformLocation(mProgramName, "near");
    mFarUniform = GLES20.glGetUniformLocation(mProgramName, "far");
    mSizeAttenuationUniform = GLES20.glGetUniformLocation(mProgramName, "sizeAttenuation");
    mVisibility = GLES20.glGetUniformLocation(mProgramName, "visibility");
    mAlphaTest = GLES20.glGetUniformLocation(mProgramName, "alphaTest");
    mDrawModeUniform = GLES20.glGetUniformLocation(mProgramName, "drawMode");
    mNearCutoffUniform = GLES20.glGetUniformLocation(mProgramName, "nearCutOff");
    mFarCutoffUniform = GLES20.glGetUniformLocation(mProgramName, "farCutOff");
    mLineDepthScaleUniform = GLES20.glGetUniformLocation(mProgramName, "lineDepthScale");

    ShaderUtil.checkGLError(TAG, "program  params");

    Matrix.setIdentityM(mModelMatrix, 0);

    mColor = new Vector3f(1f, 1f, 1f);
    lineWidth = 0.5f;
  }

  public void setLineWidth(float width) {
    lineWidth = width;
  }

  public void setDrawDebug(boolean drawDebugMode) {
    mDrawMode = drawDebugMode;
  }

  public void setColor(Vector3f color) {
    mColor = new Vector3f(color);
  }

  public void setDistanceScale(float distanceScale) {
    this.mLineDepthScale = distanceScale;
  }

  public void updateStrokes(ArrayList<ArrayList<Vector3f>> strokes) {
    mNumPoints = 0;
    for (ArrayList<Vector3f> l : strokes) {
      mNumPoints += l.size() * 2 + 2;
    }

    ensureCapacity(mNumPoints);

    int offset = 0;
    for (ArrayList<Vector3f> l : strokes) {
      offset = addLine(l, offset);
    }
    mNumBytes = offset;
  }

  private void ensureCapacity(int numPoints) {
    int count = 1024;
    if (mSide != null) {
      count = mSide.length;
    }

    while (count < numPoints) {
      count += 1024;
    }

    if (mSide == null || mSide.length < count) {
      Log.i(TAG, "alloc " + count);
      mPositions = new float[count * 3];
      mNext = new float[count * 3];
      mPrevious = new float[count * 3];

      mCounters = new float[count];
      mSide = new float[count];
      mWidth = new float[count];
    }
  }

  private int addLine(List<Vector3f> line, int offset) {
    if (line == null || line.size() < 2) return offset;

    int lineSize = line.size();

    int ii = offset;
    for (int i = 0; i < lineSize; i++) {

      int iGood = i;
      if (iGood < 0) iGood = 0;
      if (iGood >= lineSize) iGood = lineSize - 1;

      int i_m_1 = (iGood - 1) < 0 ? iGood : iGood - 1;
      int i_p_1 = (iGood + 1) > (lineSize - 1) ? iGood : iGood + 1;
      float c = ((float) i / lineSize);

      Vector3f current = line.get(iGood);
      Vector3f previous = line.get(i_m_1);
      Vector3f next = line.get(i_p_1);

      if (i == 0) {
        setMemory(ii++, current, previous, next, c, lineWidth, 1f);
      }

      setMemory(ii++, current, previous, next, c, lineWidth, 1f);
      setMemory(ii++, current, previous, next, c, lineWidth, -1f);

      if (i == lineSize - 1) {
        setMemory(ii++, current, previous, next, c, lineWidth, -1f);
      }
    }
    return ii;
  }

  private void setMemory(
      int index,
      Vector3f pos,
      Vector3f prev,
      Vector3f next,
      float counter,
      float width,
      float side) {
    mPositions[index * 3] = pos.x;
    mPositions[index * 3 + 1] = pos.y;
    mPositions[index * 3 + 2] = pos.z;

    mNext[index * 3] = next.x;
    mNext[index * 3 + 1] = next.y;
    mNext[index * 3 + 2] = next.z;

    mPrevious[index * 3] = prev.x;
    mPrevious[index * 3 + 1] = prev.y;
    mPrevious[index * 3 + 2] = prev.z;

    mCounters[index] = counter;
    mSide[index] = side;
    mWidth[index] = width;
  }

  public void clear() {
    bNeedsUpdate.set(true);
  }

  public void upload() {
    bNeedsUpdate.set(false);

    FloatBuffer current = toFloatBuffer(mPositions);
    FloatBuffer next = toFloatBuffer(mNext);
    FloatBuffer previous = toFloatBuffer(mPrevious);

    FloatBuffer side = toFloatBuffer(mSide);
    FloatBuffer width = toFloatBuffer(mWidth);
    FloatBuffer counter = toFloatBuffer(mCounters);

    //        mNumPoints = mPositions.length;

    mPositionAddress = 0;
    mNextAddress = mPositionAddress + mNumBytes * 3 * BYTES_PER_FLOAT;
    mPreviousAddress = mNextAddress + mNumBytes * 3 * BYTES_PER_FLOAT;
    mSideAddress = mPreviousAddress + mNumBytes * 3 * BYTES_PER_FLOAT;

    mWidthAddress = mSideAddress + mNumBytes * BYTES_PER_FLOAT;
    mCounterAddress = mWidthAddress + mNumBytes * BYTES_PER_FLOAT;
    mVboSize = mCounterAddress + mNumBytes * BYTES_PER_FLOAT;

    ShaderUtil.checkGLError(TAG, "before update");

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);

    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);

    GLES20.glBufferSubData(
        GLES20.GL_ARRAY_BUFFER, mPositionAddress, mNumBytes * 3 * BYTES_PER_FLOAT, current);
    GLES20.glBufferSubData(
        GLES20.GL_ARRAY_BUFFER, mNextAddress, mNumBytes * 3 * BYTES_PER_FLOAT, next);
    GLES20.glBufferSubData(
        GLES20.GL_ARRAY_BUFFER, mPreviousAddress, mNumBytes * 3 * BYTES_PER_FLOAT, previous);
    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mSideAddress, mNumBytes * BYTES_PER_FLOAT, side);
    GLES20.glBufferSubData(
        GLES20.GL_ARRAY_BUFFER, mWidthAddress, mNumBytes * BYTES_PER_FLOAT, width);
    GLES20.glBufferSubData(
        GLES20.GL_ARRAY_BUFFER, mCounterAddress, mNumBytes * BYTES_PER_FLOAT, counter);

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    ShaderUtil.checkGLError(TAG, "after update");
  }

  public void draw(
      float[] cameraView,
      float[] cameraPerspective,
      float screenWidth,
      float screenHeight,
      float nearClip,
      float farClip) {

    Matrix.multiplyMM(mModelViewMatrix, 0, cameraView, 0, mModelMatrix, 0);
    Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraPerspective, 0, mModelViewMatrix, 0);

    ShaderUtil.checkGLError(TAG, "Before draw");

    GLES20.glUseProgram(mProgramName);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
    GLES20.glVertexAttribPointer(
        mPositionAttribute,
        FLOATS_PER_POINT,
        GLES20.GL_FLOAT,
        false,
        BYTES_PER_POINT,
        mPositionAddress);
    GLES20.glVertexAttribPointer(
        mPreviousAttribute,
        FLOATS_PER_POINT,
        GLES20.GL_FLOAT,
        false,
        BYTES_PER_POINT,
        mPreviousAddress);
    GLES20.glVertexAttribPointer(
        mNextAttribute, FLOATS_PER_POINT, GLES20.GL_FLOAT, false, BYTES_PER_POINT, mNextAddress);
    GLES20.glVertexAttribPointer(
        mSideAttribute, 1, GLES20.GL_FLOAT, false, BYTES_PER_FLOAT, mSideAddress);
    GLES20.glVertexAttribPointer(
        mWidthAttribte, 1, GLES20.GL_FLOAT, false, BYTES_PER_FLOAT, mWidthAddress);
    GLES20.glVertexAttribPointer(
        mCountersAttribute, 1, GLES20.GL_FLOAT, false, BYTES_PER_FLOAT, mCounterAddress);
    GLES20.glUniformMatrix4fv(mModelViewUniform, 1, false, mModelViewMatrix, 0);
    GLES20.glUniformMatrix4fv(mProjectionUniform, 1, false, cameraPerspective, 0);

    GLES20.glUniform2f(mResolutionUniform, screenWidth, screenHeight);
    GLES20.glUniform1f(mLineWidthUniform, 0.01f);
    GLES20.glUniform3f(mColorUniform, mColor.x, mColor.y, mColor.z);
    GLES20.glUniform1f(mOpacityUniform, 1.0f);
    GLES20.glUniform1f(mNearUniform, nearClip);
    GLES20.glUniform1f(mFarUniform, farClip);
    GLES20.glUniform1f(mSizeAttenuationUniform, 1.0f);
    GLES20.glUniform1f(mVisibility, 1.0f);
    GLES20.glUniform1f(mAlphaTest, 1.0f);
    GLES20.glUniform1f(mDrawModeUniform, mDrawMode ? 1.0f : 0.0f);
    GLES20.glUniform1f(mNearCutoffUniform, mDrawDistance - 0.0075f);
    GLES20.glUniform1f(mFarCutoffUniform, mDrawDistance + 0.0075f);
    GLES20.glUniform1f(mLineDepthScaleUniform, mLineDepthScale);

    GLES20.glEnableVertexAttribArray(mPositionAttribute);
    GLES20.glEnableVertexAttribArray(mPreviousAttribute);
    GLES20.glEnableVertexAttribArray(mNextAttribute);
    GLES20.glEnableVertexAttribArray(mSideAttribute);
    GLES20.glEnableVertexAttribArray(mWidthAttribte);
    GLES20.glEnableVertexAttribArray(mCountersAttribute);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mNumBytes);

    GLES20.glDisableVertexAttribArray(mCountersAttribute);
    GLES20.glDisableVertexAttribArray(mWidthAttribte);
    GLES20.glDisableVertexAttribArray(mSideAttribute);
    GLES20.glDisableVertexAttribArray(mNextAttribute);
    GLES20.glDisableVertexAttribArray(mPreviousAttribute);
    GLES20.glDisableVertexAttribArray(mPositionAttribute);

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    ShaderUtil.checkGLError(TAG, "Draw");
  }

  private FloatBuffer toFloatBuffer(float[] data) {
    FloatBuffer buff;
    ByteBuffer bb = ByteBuffer.allocateDirect(data.length * BYTES_PER_FLOAT);
    bb.order(ByteOrder.nativeOrder());
    buff = bb.asFloatBuffer();
    buff.put(data);
    buff.position(0);
    return buff;
  }
}

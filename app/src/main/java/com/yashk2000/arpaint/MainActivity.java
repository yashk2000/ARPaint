package com.yashk2000.arpaint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.yashk2000.arpaint.Rendering.BackgroundRenderer;
import com.yashk2000.arpaint.Rendering.BiquadFilter;
import com.yashk2000.arpaint.Rendering.LineShaderRenderer;
import com.yashk2000.arpaint.Rendering.LineUtils;
import com.yashk2000.arpaint.Utils.DisplayRotationHelper;
import com.yashk2000.arpaint.Utils.PermissionsHelper;
import com.yashk2000.arpaint.Utils.Settings;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import top.defaults.colorpicker.ColorPickerPopup;


public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer, GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    private String TAG = MainActivity.class.getSimpleName();
    private GLSurfaceView surfaceView;
    private Session session;
    private boolean installRequested;
    private DisplayRotationHelper displayRotationHelper;
    private boolean paused = false;
    private float screenWidth = 0;
    private float screenHeight = 0;
    private BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private Frame frame;
    private AtomicBoolean isTracking = new AtomicBoolean(true);
    private float[] zeroMatrix = new float[16];
    private Vector3f lastPoint;
    private GestureDetectorCompat detector;
    private ArrayList<ArrayList<Vector3f>> strokes;
    private float[] projmtx = new float[16];
    private float[] viewmtx = new float[16];
    private LineShaderRenderer lineShaderRenderer = new LineShaderRenderer();
    private AtomicBoolean touchDown = new AtomicBoolean(false);
    private float[] lastFramePosition;
    private AtomicBoolean newStroke = new AtomicBoolean(false);
    private AtomicReference<Vector2f> lastTouch = new AtomicReference<>();
    private AtomicBoolean reCenterView = new AtomicBoolean(false);
    private AtomicBoolean lineParameters = new AtomicBoolean(false);

    private BiquadFilter biquadFilter;

    private FloatingActionButton fab_main, fab_color, fab_line, fab_clear;
    private Animation fab_open, fab_close, fab_clock, fab_anticlock;
    TextView color, thickness, clear;

    private AtomicBoolean clearDrawing = new AtomicBoolean(false);

    Boolean isOpen = false;

    private SeekBar lineWidthBar;

    private float lineWidthMax = 0.33f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        surfaceView = findViewById(R.id.surfaceview);

        final SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);

        displayRotationHelper = new DisplayRotationHelper(this);
        Matrix.setIdentityM(zeroMatrix, 0);

        lastPoint = new Vector3f(0, 0, 0);
        installRequested = false;

        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        detector = new GestureDetectorCompat(this, this);
        detector.setOnDoubleTapListener(this);
        strokes = new ArrayList<>();

        fab_main = findViewById(R.id.fab);
        fab_color = findViewById(R.id.color);
        fab_line = findViewById(R.id.line);
        fab_clear = findViewById(R.id.clear);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_clock = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_rotate_clock);
        fab_anticlock = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_rotate_anitclock);

        color = findViewById(R.id.textview_color);
        thickness = findViewById(R.id.textview_line_thickness);
        clear = findViewById(R.id.textview_clear);

        fab_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isOpen) {
                    color.setVisibility(View.INVISIBLE);
                    thickness.setVisibility(View.INVISIBLE);
                    clear.setVisibility(View.INVISIBLE);
                    fab_clear.startAnimation(fab_close);
                    fab_line.startAnimation(fab_close);
                    fab_color.startAnimation(fab_close);
                    fab_main.startAnimation(fab_anticlock);
                    fab_clear.setClickable(false);
                    fab_line.setClickable(false);
                    fab_color.setClickable(false);
                    isOpen = false;
                } else {
                    color.setVisibility(View.VISIBLE);
                    thickness.setVisibility(View.VISIBLE);
                    clear.setVisibility(View.VISIBLE);
                    fab_clear.startAnimation(fab_open);
                    fab_line.startAnimation(fab_open);
                    fab_color.startAnimation(fab_open);
                    fab_main.startAnimation(fab_clock);
                    fab_clear.setClickable(true);
                    fab_line.setClickable(true);
                    fab_color.setClickable(true);
                    isOpen = true;
                }

            }
        });


        fab_line.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.dialog_line);
                dialog.show();

                lineWidthBar = dialog.findViewById(R.id.lineWidth);

                lineWidthBar.setProgress(sharedPreferences.getInt("LineWidth", 10));

                lineWidthMax = LineUtils.map((float) lineWidthBar.getProgress(), 0f, 100f, 0.1f, 5f, true);

                SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        if (seekBar == lineWidthBar) {
                            editor.putInt("LineWidth", progress);
                            lineWidthMax = LineUtils.map((float) progress, 0f, 100f, 0.1f, 5f, true);
                        }
                        lineShaderRenderer.bNeedsUpdate.set(true);

                        editor.apply();

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                };

                lineWidthBar.setOnSeekBarChangeListener(seekBarChangeListener);
            }
        });

        fab_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ColorPickerPopup.Builder(getBaseContext())
                        .initialColor(Color.WHITE)
                        .enableBrightness(true)
                        .enableAlpha(true)
                        .okTitle("Choose")
                        .cancelTitle(null)
                        .showIndicator(true)
                        .showValue(false)
                        .build()
                        .show(view, new ColorPickerPopup.ColorPickerObserver() {
                            @Override
                            public void onColorPicked(int color) {
                                Vector3f curColor = new Vector3f(Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f);
                                Settings.setColor(curColor);
                            }
                        });
            }
        });

        fab_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearDrawing.set(true);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                if (!PermissionsHelper.hasCameraPermission(this)) {
                    PermissionsHelper.requestCameraPermission(this);
                    return;
                }

                session = new Session(this);
            } catch (Exception e) {
                exception = e;
            }

            assert session != null;
            Config config = new Config(session);
            if (!session.isSupported(config)) {
                Log.e(TAG, "Exception creating session Device Does Not Support ARCore", exception);
            }
            session.configure(config);
        }
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        surfaceView.onResume();
        displayRotationHelper.onResume();
        paused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (session != null) {
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }

        paused = true;


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
    }

    private void update() {

        if (session == null) {
            return;
        }

        displayRotationHelper.updateSessionIfNeeded(session);

        try {

            session.setCameraTextureName(backgroundRenderer.getTextureId());

            frame = session.update();
            Camera camera = frame.getCamera();

            TrackingState state = camera.getTrackingState();

            if (state == TrackingState.TRACKING && !isTracking.get()) {
                isTracking.set(true);
            } else if (state == TrackingState.STOPPED && isTracking.get()) {
                isTracking.set(false);
                touchDown.set(false);
            }

            camera.getProjectionMatrix(projmtx, 0, Settings.getNearClip(), Settings.getFarClip());
            camera.getViewMatrix(viewmtx, 0);

            float[] position = new float[3];
            camera.getPose().getTranslation(position, 0);

            if (lastFramePosition != null) {
                Vector3f distance = new Vector3f(position[0], position[1], position[2]);
                distance.sub(new Vector3f(lastFramePosition[0], lastFramePosition[1], lastFramePosition[2]));

                if (distance.length() > 0.15) {
                    touchDown.set(false);
                }
            }
            lastFramePosition = position;

            Matrix.multiplyMM(viewmtx, 0, viewmtx, 0, zeroMatrix, 0);


            if (newStroke.get()) {
                newStroke.set(false);
                addStroke(lastTouch.get());
                lineShaderRenderer.bNeedsUpdate.set(true);
            } else if (touchDown.get()) {
                addPoint(lastTouch.get());
                lineShaderRenderer.bNeedsUpdate.set(true);
            }

            if (reCenterView.get()) {
                reCenterView.set(false);
                zeroMatrix = getCalibrationMatrix();
            }

            if (clearDrawing.get()) {
                clearDrawing.set(false);
                clearScreen();
                lineShaderRenderer.bNeedsUpdate.set(true);
            }

            lineShaderRenderer.setDrawDebug(lineParameters.get());
            if (lineShaderRenderer.bNeedsUpdate.get()) {
                lineShaderRenderer.setColor(Settings.getColor());
                lineShaderRenderer.mDrawDistance = Settings.getStrokeDrawDistance();
                float distanceScale = 0.0f;
                lineShaderRenderer.setDistanceScale(distanceScale);
                lineShaderRenderer.setLineWidth(lineWidthMax);
                lineShaderRenderer.clear();
                lineShaderRenderer.updateStrokes(strokes);
                lineShaderRenderer.upload();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearScreen() {
        strokes.clear();
        lineShaderRenderer.clear();
    }

    private void addPoint(Vector2f touchPoint) {
        Vector3f newPoint = LineUtils.GetWorldCoords(touchPoint, screenWidth, screenHeight, projmtx, viewmtx);
        addPoint(newPoint);
    }

    private void addStroke(Vector3f newPoint) {
        float lineSmoothing = 0.1f;
        biquadFilter = new BiquadFilter(lineSmoothing);
        for (int i = 0; i < 1500; i++) {
            biquadFilter.update(newPoint);
        }
        Vector3f p = biquadFilter.update(newPoint);
        lastPoint = new Vector3f(p);
        strokes.add(new ArrayList<Vector3f>());
        strokes.get(strokes.size() - 1).add(lastPoint);
    }

    private void addPoint(Vector3f newPoint) {
        if (LineUtils.distanceCheck(newPoint, lastPoint)) {
            Vector3f p = biquadFilter.update(newPoint);
            lastPoint = new Vector3f(p);
            strokes.get(strokes.size() - 1).add(lastPoint);
        }
    }

    public float[] getCalibrationMatrix() {
        float[] t = new float[3];
        float[] m = new float[16];

        frame.getCamera().getPose().getTranslation(t, 0);
        float[] z = frame.getCamera().getPose().getZAxis();
        Vector3f zAxis = new Vector3f(z[0], z[1], z[2]);
        zAxis.y = 0;
        zAxis.normalize();

        double rotate = Math.atan2(zAxis.x, zAxis.z);

        Matrix.setIdentityM(m, 0);
        Matrix.translateM(m, 0, t[0], t[1], t[2]);
        Matrix.rotateM(m, 0, (float) Math.toDegrees(rotate), 0, 1, 0);
        return m;
    }

    private void addStroke(Vector2f touchPoint) {
        Vector3f newPoint = LineUtils.GetWorldCoords(touchPoint, screenWidth, screenHeight, projmtx, viewmtx);
        addStroke(newPoint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent tap) {
        this.detector.onTouchEvent(tap);

        if (tap.getAction() == MotionEvent.ACTION_DOWN) {
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            touchDown.set(true);
            newStroke.set(true);
            return true;
        } else if (tap.getAction() == MotionEvent.ACTION_MOVE || tap.getAction() == MotionEvent.ACTION_POINTER_DOWN) {
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            touchDown.set(true);
            return true;
        } else if (tap.getAction() == MotionEvent.ACTION_UP || tap.getAction() == MotionEvent.ACTION_CANCEL) {
            touchDown.set(false);
            lastTouch.set(new Vector2f(tap.getX(), tap.getY()));
            return true;
        }

        return super.onTouchEvent(tap);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public void onBackPressed() {
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionsHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (session == null) {
            return;
        }

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        backgroundRenderer.createOnGlThread(this);

        try {

            session.setCameraTextureName(backgroundRenderer.getTextureId());
            lineShaderRenderer.createOnGlThread(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        displayRotationHelper.onSurfaceChanged(width, height);
        screenWidth = width;
        screenHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;

        update();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (frame == null) {
            return;
        }

        backgroundRenderer.draw(frame);

        if (frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            lineShaderRenderer.draw(viewmtx, projmtx, screenWidth, screenHeight, Settings.getNearClip(), Settings.getFarClip());
        }
    }
}
package com.softbankrobotics.pepperapplauncher;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

public class OverlayPopupService extends Service implements View.OnClickListener, Observer {

    private final static int FINGERS_NUMBER = 2;
    private final static long TIME_SHOW_OVERLAY = 10000;
    private WindowManager windowManager;
    private FrameLayout mLeftGestureView;
    private ClosePopupView overlayView;
    private CountDownTimer mTimer;
    private CountDownHandler mCountDownHandler;
    private final String TAG = "MSI_OverlayService";
    private String locale;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        enableMagicCombination();
        ObservableObject.getInstance().addObserver(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locale = intent.getStringExtra("locale");
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        cancelMagicCombination();
        ObservableObject.getInstance().deleteObserver(this);
        super.onDestroy();
    }

    private void cancelMagicCombination() {
        try {
            if (mTimer != null) {
                mTimer.cancel();
            }
            if (mLeftGestureView != null && windowManager != null) {
                windowManager.removeView(mLeftGestureView);
                mLeftGestureView = null;
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void enableMagicCombination() {
        mCountDownHandler = new CountDownHandler();
        mLeftGestureView = new FrameLayout(this);
        View.OnTouchListener onTouchListener = (v, event) -> {
            if(event.getPointerCount() == FINGERS_NUMBER) {
                mCountDownHandler.sendEmptyMessageDelayed(CountDownHandler.SHOW_ACTIVE_CORNER_MSG, 100);
            } else {
                mCountDownHandler.removeMessages(CountDownHandler.SHOW_ACTIVE_CORNER_MSG);
            }
            return false;
        };
        Point screenPoint = new Point();
        windowManager.getDefaultDisplay().getRealSize(screenPoint);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        // Left dead zone
        params.gravity = Gravity.END;
        params.width = 2;
        mLeftGestureView.setOnTouchListener(onTouchListener);
        windowManager.addView(mLeftGestureView, params);
    }

    @Override
    public void onClick(View v) { }

    protected class CountDownHandler extends Handler {
        static final int SHOW_ACTIVE_CORNER_MSG = 1;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_ACTIVE_CORNER_MSG:
                    showActiveCorner();
                    break;
            }
        }
    }

    public void showActiveCorner() {
        if (overlayView == null) {
            Resources res = getApplicationContext().getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.setLocale(new Locale(locale.toLowerCase()));
            res.updateConfiguration(conf, dm);

            Point screenPoint = new Point();
            windowManager.getDefaultDisplay().getRealSize(screenPoint);

            overlayView = new ClosePopupView(this);
            overlayView.setOnClickListener(this);
            overlayView.setFocusable(false);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);

            params.x = 0;
            params.y = 0;
            params.height = 250;
            params.width = 500;
            windowManager.addView(overlayView, params);
            overlayView.findViewById(R.id.button_yes).setOnClickListener((v) -> {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.softbankrobotics.pepperapplauncher");
                if (launchIntent != null) {
                    startActivity(launchIntent);
                    hideActiveCorner();
                }
            });
            overlayView.findViewById(R.id.button_no).setOnClickListener((v) -> hideActiveCorner());

            mTimer = new CountDownTimer(TIME_SHOW_OVERLAY, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    hideActiveCorner();
                }
            };
            mTimer.start();
        }
    }

    public void hideActiveCorner() {
        try {
            overlayView.setVisibility(View.GONE);

            if (windowManager == null) {
                windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            }

            if (windowManager != null) {
                windowManager.removeView(overlayView);
                overlayView = null;
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        if (!(data instanceof Intent)) {
            return;
        }
        Intent intent = (Intent) data;

        if (intent.getAction() == null) {
            return;
        }
        enableMagicCombination();
    }
}
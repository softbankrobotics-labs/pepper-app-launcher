package com.softbankrobotics.pepperapplauncher;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import java.util.Observable;
import java.util.Observer;

public class OverlayBtnCloseService extends Service implements View.OnClickListener, Observer {

    private WindowManager windowManager;
    private CloseButtonView overlayView;
    private final String TAG = "MSI_OverlayService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"create Service");
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        showActiveCorner();
        ObservableObject.getInstance().addObserver(this);
    }

    @Override
    public void onDestroy() {
        ObservableObject.getInstance().deleteObserver(this);
        hideActiveCorner();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) { }

    public void showActiveCorner() {
        if (overlayView == null) {
            Point screenPoint = new Point();
            windowManager.getDefaultDisplay().getRealSize(screenPoint);

            overlayView = new CloseButtonView(this);
            overlayView.setOnClickListener(this);
            overlayView.setFocusable(false);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP;
            params.x = 0;
            params.y = 0;
            params.height = 75;
            params.width = 75;
            Log.i(TAG,"Add View");
            windowManager.addView(overlayView, params);
            overlayView.findViewById(R.id.close).setOnClickListener((v) -> {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.softbankrobotics.pepperapplauncher");
                if (launchIntent != null) {
                    startActivity(launchIntent);
                    hideActiveCorner();
                }
            });
        }
    }

    public void hideActiveCorner() {
        try {
            Log.i(TAG,"hide");
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
        if (!(data instanceof Intent)) return;
        Intent intent = (Intent) data;
        if (intent.getAction() == null) return;
    }
}
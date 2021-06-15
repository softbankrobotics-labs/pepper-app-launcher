package com.softbankrobotics.pepperapplauncher;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class ClosePopupView extends FrameLayout {

    public ClosePopupView(Context context) {
        super(context);
        init();
    }

    public ClosePopupView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClosePopupView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ClosePopupView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.close_popup, null);
        addView(view);
    }
}

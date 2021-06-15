package com.softbankrobotics.pepperapplauncher;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class CloseButtonView extends FrameLayout {

    public CloseButtonView(Context context) {
        super(context);
        init();
    }

    public CloseButtonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CloseButtonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CloseButtonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.close_button, null);
        addView(view);
    }
}

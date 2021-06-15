package com.softbankrobotics.pepperapplauncher;

import android.util.Log;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.object.conversation.BaseQiChatExecutor;

import java.util.List;

public class MyQiChatExecutor extends BaseQiChatExecutor {

    final String TAG = "appanimateExecutor";
    private final QiContext qiContext;
    private final MainActivity ma;

    MyQiChatExecutor(QiContext context, MainActivity mainActivity) {
        super(context);
        this.qiContext = context;
        this.ma = mainActivity;
    }

    @Override
    public void runWith(List<String> params) {
        // This is called when execute is reached in the topic
        Log.i(TAG, "runWith : load dynamic concepts : " + params.get(0));

        ma.startApplication(ma.appLabelVsPackageName.get(params.get(0)));

    }

    @Override
    public void stop() {
        // This is called when chat is canceled or stopped.
        Log.i(TAG, "execute stopped");
    }
}

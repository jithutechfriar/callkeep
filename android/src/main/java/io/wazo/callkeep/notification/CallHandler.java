package io.wazo.callkeep.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallHandler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i("log", "onReceive: broadcast received");
        CallNotificationService.handleNotificationAction(intent);

    }
}

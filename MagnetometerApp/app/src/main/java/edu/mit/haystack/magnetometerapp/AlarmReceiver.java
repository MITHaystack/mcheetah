package edu.mit.haystack.magnetometerapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * @author David Mascharka
 */
public class AlarmReceiver extends BroadcastReceiver {

    /**
     * Start the logger service when a broadcast is received from the alarm
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent dataLogger = new Intent(context, LoggerService.class);
        context.startService(dataLogger);
    }
}

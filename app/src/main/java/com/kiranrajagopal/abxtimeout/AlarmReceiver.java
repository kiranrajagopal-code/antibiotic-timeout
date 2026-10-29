package com.kiranrajagopal.abxtimeout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || "android.intent.action.MY_PACKAGE_REPLACED".equals(a)) {
            Alarms.rescheduleStored(c);
            return;
        }
        String id = i.getStringExtra("id");
        if (id != null) Alarms.show(c, id, i.getStringExtra("title"), i.getStringExtra("text"));
    }
}

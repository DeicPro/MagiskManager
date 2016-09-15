package com.topjohnwu.magisk;

import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.topjohnwu.magisk.utils.Shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class MonitorService extends Service

{

    private static final String TAG = "Magisk";
    private final Handler handler = new Handler();
    private Boolean disableroot;
    private Boolean disablerootprev;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
            checkProcesses.run();

            return START_STICKY;

    }


    private Runnable checkProcesses = new Runnable() {
        @Override
        public void run() {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (prefs.getBoolean("autoRootEnable", false)) {

                Set<String> set = prefs.getStringSet("auto_blacklist", null);

                if (set != null) {
                    disableroot = getStats(set);

                }
                if (disableroot != disablerootprev) {
                    int counter = 0;
                    String rootstatus = (disableroot ? "disabled" : "enabled");
                    if (disableroot) {
                        ForceDisableRoot();
                    } else {
                            counter +=1;
                            if (counter >=3) {
                                Shell.su("setprop magisk.root 1");
                                counter = 0;
                            }
                        }

                    ShowNotification(disableroot);

                }
                disablerootprev = disableroot;
                Log.d(TAG,"Root check completed, set to " + (disableroot ? "disabled" : "enabled"));


            }
            handler.postDelayed(checkProcesses, 1000);
        }

    };

    private void ForceDisableRoot() {
        Shell.su("setprop magisk.root 0");
        if (Shell.sh("which su").contains("su")) {
            Shell.su(("setprop magisk.root 0"));
        }
        if (Shell.sh("which su").contains("su")) {
            Shell.su(("setprop magisk.root 0"));
        }
        if (Shell.sh("which su").contains("su")) {
            Shell.su(("setprop magisk.root 0"));
        }
    }

    private void ShowNotification(boolean rootAction) {
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder;
        mNotifyMgr.cancelAll();
        if (rootAction) {

            Intent intent = new Intent(getApplication(), WelcomeActivity.class);
            intent.putExtra("relaunch", "relaunch");
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(disableroot ? R.drawable.ic_stat_notification_autoroot_off : R.drawable.ic_stat_notification_autoroot_on)
                            .setContentIntent(pendingIntent)
                            .setContentTitle("Auto-root status changed")
                            .setContentText("Auto root has been " + rootAction + "!  Tap to re-enable when done.");

        } else {
            mBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                            .setAutoCancel(true)
                            .setSmallIcon(disableroot ? R.drawable.ic_stat_notification_autoroot_off : R.drawable.ic_stat_notification_autoroot_on)
                            .setContentTitle("Auto-root status changed")
                            .setContentText("Auto root has been " + rootAction + "!");
        }
        // Builds the notification and issues it.
        int mNotificationId = 1;
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    private boolean getStats(Set<String> seti) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean inStats = false;
            if (seti != null) {
                ArrayList<String> statList = new ArrayList<>(seti);
                for (int i = 0; i < statList.size(); i++) {
                    if (isAppForeground(statList.get(i))) {
                        inStats = (isAppForeground(statList.get(i)));
                    }
                }
                return inStats;
            }
            Log.d(TAG, "SDK check failed.");
        }
        return false;
    }

    protected boolean isAppForeground(String packageName) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
        String topPackageName = "";
        if (stats != null) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : stats) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {
                topPackageName = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
            }
        }

        return topPackageName.equals(packageName);
    }

}

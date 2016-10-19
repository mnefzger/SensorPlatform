package au.carrsq.sensorplatformshared;


import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class UStats {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("M-d-yyyy HH:mm:ss");


    private static SortedMap<Long,UsageStats> getUsageStatsList(Context context){
        SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
        List<UsageStats> usageStatsList = new ArrayList<>();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            UsageStatsManager usm = getUsageStatsManager(context);
            Calendar calendar = Calendar.getInstance();
            long endTime = calendar.getTimeInMillis();
            calendar.add(Calendar.MINUTE, -1);
            long startTime = calendar.getTimeInMillis();

            Log.d("USTATS", "Range start:" + dateFormat.format(startTime) );
            Log.d("USTATS", "Range end:" + dateFormat.format(endTime));

            usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST,startTime,endTime);

            if (usageStatsList != null && usageStatsList.size() > 0) {
                mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : usageStatsList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }

            }
        }

        return mySortedMap;
    }

    public static String getForegroundApp(Context c) {
        String app = null;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            SortedMap<Long,UsageStats> ustats = getUsageStatsList(c);

            if (ustats != null && !ustats.isEmpty()) {
                app = ustats.get(ustats.lastKey()).getPackageName();
            }
        }

        return app;
    }

    @SuppressWarnings("ResourceType")
    private static UsageStatsManager getUsageStatsManager(Context context){
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService("usagestats");
        return usm;
    }

}

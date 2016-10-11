package mnefzger.de.sensorplatformshared;


import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UStats {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("M-d-yyyy HH:mm:ss");


    private static List<UsageStats> getUsageStatsList(Context context){
        List<UsageStats> usageStatsList = new ArrayList<>();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            UsageStatsManager usm = getUsageStatsManager(context);
            Calendar calendar = Calendar.getInstance();
            long endTime = calendar.getTimeInMillis();
            calendar.add(Calendar.MINUTE, -1);
            long startTime = calendar.getTimeInMillis();

            Log.d("USTATS", "Range start:" + dateFormat.format(startTime) );
            Log.d("USTATS", "Range end:" + dateFormat.format(endTime));

            usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,startTime,endTime);
        }

        return usageStatsList;
    }

    public static String getForegroundApp(Context c) {
        String app = null;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            List<UsageStats> ustats = getUsageStatsList(c);
            app = ustats.get(0).getPackageName();
        }

        return app;
    }

    @SuppressWarnings("ResourceType")
    private static UsageStatsManager getUsageStatsManager(Context context){
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService("usagestats");
        return usm;
    }

}

package au.carrsq.sensorplatform.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 *  Logs uncaught exceptions to file
 */

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    Thread.UncaughtExceptionHandler defaultUEH;

    public void UncaughtExceptionHandler() {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        final Writer stringBuffSync = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringBuffSync);
        throwable.printStackTrace(printWriter);
        String stacktrace = stringBuffSync.toString();
        printWriter.close();

        File output = new File(IO.baseDir + File.separator + System.currentTimeMillis() + "_error.txt");

        IO.writeFile(output, stacktrace.toString());

        throwable.printStackTrace();

        System.exit(1);
    }
}

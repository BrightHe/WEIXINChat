package robot.com.myapplication.recorder;

import android.text.format.DateFormat;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Administrator on 2019/7/30.
 */

public class FileNameUtils {
    public static String TAG = "Test";
    private static String fileName;

    public static String getFileName() {
        Log.i( TAG, "getFileName: "+fileName );
        return fileName;
    }

    public static void setFileName( ) {
        FileNameUtils.fileName = DateFormat.format("yyyyMMdd_hhmmss", Calendar.getInstance( Locale.CHINA))+".amr";
    }
}

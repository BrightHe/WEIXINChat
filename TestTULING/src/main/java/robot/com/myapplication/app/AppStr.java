package robot.com.myapplication.app;

import android.app.Application;
import android.util.Log;

/**
 * Created by Administrator on 2019/7/30.
 */

public class AppStr extends Application {
    private String TAG = "Test";
    private boolean isCompleted;
    private boolean isDownLoad;

    public boolean IsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(boolean isCompleted) {
        Log.i( TAG, "setIsCompleted: "+isCompleted );
        this.isCompleted = isCompleted;
    }

    public boolean isDownLoad() {
        return isDownLoad;
    }

    public void setDownLoad(boolean downLoad) {
        isDownLoad = downLoad;
    }
}

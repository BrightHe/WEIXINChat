package robot.com.myapplication.tengxunyun;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.File;

/**
 * Created by Administrator on 2019/7/31.
 */

public class NTest {

    //测试网络状态
    public static int getConnectedType(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null && mNetworkInfo.isAvailable()) {
                return mNetworkInfo.getType();
            }
        }
        return -1;
    }

    /**
     * 判断文件是否存在（播放录音前需要检测文件存在）
     */
    public static boolean fileIsExists(String strFile)
    {
        try {
            File f=new File(strFile);
            if(!f.exists())
            {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}

package robot.com.myapplication.recorder;

/**
 * Created by Administrator on 2019/7/29.
 */

public class Recorder {

    public float time;
    public String filePath;

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Recorder(float time, String filePath) {
        this.time = time;
        this.filePath = filePath;
    }

    public Recorder() {
    }
}

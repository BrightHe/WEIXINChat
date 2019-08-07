package robot.com.myapplication.recorder;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import robot.com.myapplication.R;

/**
 * Dialog管理类
 * Created by Administrator on 2019/7/26.
 */

public class DialogManager {
    private Dialog mDialog;

    private ImageView mIcon;
    private ImageView mVoice;

    private TextView mLabel;

    private Context mContext;

    public DialogManager(Context context){
        mContext = context;
    }

    public void showRecordingDialog(){
        mDialog = new Dialog(mContext, R.style.Theme_AudioDialog);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view=inflater.inflate(R.layout.dialog_recorder,null);
        mDialog.setContentView(view);

        mIcon = view.findViewById(R.id.id_recorder_dialog_icon);
        mVoice = view.findViewById(R.id.id_recorder_dialog_voice);
        mLabel = view.findViewById(R.id.id_recorder_dialog_label);

        mDialog.show();
    }

    //正在播放时的状态
    public void recording() {
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.VISIBLE);
            mLabel.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.drawable.recorder);
            mLabel.setText("手指上划，取消发送");
        }
    }

    //想要取消
    public void wantToCancel(){
        if (mDialog != null&&mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLabel.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.drawable.cancel);
            mLabel.setText("松开手指，取消发送");
        }
    }

    //录音时间太短
    public void tooShort() {
        if (mDialog != null&&mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLabel.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.drawable.voice_to_short);
            mLabel.setText("录音时间过短");
        }
    }

    //30秒语音强制发送
    public void forceSend(float time){
        if (mDialog != null&&mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.VISIBLE);
            mLabel.setVisibility(View.VISIBLE);

            mIcon.setImageResource(R.drawable.recorder);
            mLabel.setText("还可以说   "+(30-(int)(time)-1)+"  秒");
        }
    }

    //关闭dialog
    public void dismissDialog(){
        if (mDialog != null&&mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    /**
     * 通过level更新voice上的图片
     *
     * @param level
     */
    public void updateVoiceLevel(int level){
        if (mDialog != null&&mDialog.isShowing()) {
            int resId = mContext.getResources().getIdentifier("v" + level,
                    "drawable", mContext.getPackageName());
            mVoice.setImageResource(resId);
        }
    }
}

package robot.com.myapplication.recorder;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import robot.com.myapplication.R;

/**
 * 自定义按钮 实现录音等功能
 * Created by Administrator on 2019/7/26.
 */

public class AudioRecorderButton extends android.support.v7.widget.AppCompatButton implements AudioManager.AudioStateListener {
    //手指滑动 距离
    private static final int DISTANCE_Y_CANCEL = 50;
    //状态
    private static final int STATE_NORMAL = 1;
    private static final int STATE_RECORDING = 2;
    private static final int STATE_WANT_TO_CANCEL = 3;
    private static final int STATE_FORCE_SEND = 4;
    //当前状态
    private int mCurState = STATE_NORMAL;
    //已经开始录音
    private boolean isRecording = false;

    private DialogManager mDialogManager;
    private AudioManager mAudioManager;

    //记录语音时长
    private float mTime;
    //是否触发onLongClick
    private boolean mReady;
    private boolean isForced = false;//强制发送
    private static int count = 0 ;

    public AudioRecorderButton(Context context) {
        this(context, null);
    }

    public AudioRecorderButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDialogManager = new DialogManager(getContext());
        //偷个懒，并没有判断 是否存在， 是否可读。

        String dir = context.getExternalCacheDir()+"/recorder_audios";

        mAudioManager = new AudioManager(dir);
        mAudioManager.setOnAudioStateListener(this);
        //按钮长按 准备录音 包括start
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mReady = true;
                mAudioManager.prepareAudio();
                return true;
            }
        });
    }

    /**
     * 录音完成后的回调
     */
    public interface AudioFinishRecorderListener{
        //时长 和 文件
        void onFinish(float seconds, String filePath);
    }

    //结束录音
    private AudioFinishRecorderListener mListener;

    public void setAudioFinishRecorderListener (AudioFinishRecorderListener listener){
        mListener = listener;
    }

    /**
     * 记录录音的时长的Runnable
     */
    private Runnable mGetTimeRunnable = new Runnable() {
        @Override
        public void run() {
            while (isRecording) {
                try {
                    Thread.sleep( 100 );
                    mTime += 0.1f;
                    if ( (int) mTime >= 24 && (int)mTime < 30) {
                        mHandler.sendEmptyMessage( MSG_DIALOG_FORCE );
                    }
                    if((int)mTime == 30){
                        mHandler.sendEmptyMessage( MSG_DIALOG_STOP );
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    //获取音量大小的Runnable
    private Runnable mGetVoiceLevelRunnable = new Runnable() {
        @Override
        public void run() {
            while (isRecording) {
                try {
                    Thread.sleep(100);
                    //                    mTime += 0.1f;
                    mHandler.sendEmptyMessage(MSG_VOICE_CHANGED);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private static final int MSG_AUDIO_PREPARED = 0X110;
    private static final int MSG_VOICE_CHANGED = 0X111;
    private static final int MSG_DIALOG_DISMISS = 0X112;
    private static final int MSG_DIALOG_FORCE = 0X113;
    private static final int MSG_DIALOG_STOP = 0X114;

    /**
     * 多线程
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_AUDIO_PREPARED :
                    //真正现实应该在audio end prepared以后
                    mDialogManager.showRecordingDialog();
                    isRecording = true;
                    new Thread(mGetVoiceLevelRunnable).start();
                    new Thread( mGetTimeRunnable ).start();
                    break;
                case MSG_VOICE_CHANGED :
                    mDialogManager.updateVoiceLevel(mAudioManager.getVoiceLevel(7));
                    break;
                case MSG_DIALOG_DISMISS :
                    mDialogManager.dismissDialog();
                    break;
                case MSG_DIALOG_FORCE:
                    changeState( STATE_FORCE_SEND );
                    break;
                case MSG_DIALOG_STOP:
                    isForced = true;
                    isRecording = false;
                    mDialogManager.dismissDialog();
                    mAudioManager.release();
                    break;
            }
        }
    };

    /**
     * 重写的接口方法
     */
    @Override
    public void wellPrepared() {
        mHandler.sendEmptyMessage(MSG_AUDIO_PREPARED);
    }

    /**
     * 触摸事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //                Log.i( "Test", "ACTION_DOWN Event: isRecording is "+isRecording );
                //                Log.i( "Test", "ACTION_DOWN Event: isForced is "+isForced );
                //                Log.i( "Test", "ACTION_DOWN Event: mCurState is "+mCurState );
                //                Log.i( "Test", "ACTION_DOWN Event: mTime is "+mTime );
                isRecording = true;
                changeState(STATE_RECORDING);
                break;
            case MotionEvent.ACTION_MOVE:
                //                Log.i( "Test", "ACTION_MOVE Event: isRecording is "+isRecording );
                //                Log.i( "Test", "ACTION_MOVE Event: isForced is "+isForced );
                //                Log.i( "Test", "ACTION_MOVE Event: mCurState is "+mCurState );
                //                Log.i( "Test", "ACTION_MOVE Event: mTime is "+mTime );
                if(!isForced){
                    if (isRecording) {
                        //根据想x,y的坐标，判断是否想要取消
                        if (wantToCancel(x, y)) {
                            changeState(STATE_WANT_TO_CANCEL);
                        } else {
                            changeState(STATE_RECORDING);
                        }
                    }
                } else{
                    count++;
                    if(count == 1){
                        if (mListener != null) {
                            mListener.onFinish( mTime, mAudioManager.getCurrentFilePath() );
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                //                Log.i( "Test", "ACTION_UP Event: isRecording is "+isRecording );
                //                Log.i( "Test", "ACTION_UP Event: isForced is "+isForced );
                //                Log.i( "Test", "ACTION_UP Event: mCurState is "+mCurState );
                //                Log.i( "Test", "ACTION_UP Event: mTime is "+mTime );
                //如果onLongClick 没触发
                if (!mReady) {
                    reset();
                    return super.onTouchEvent(event);
                }
                //触发了onLongClick 没准备好，但是已经prepared 已经start
                //所以消除文件夹
                if(!isForced && (!isRecording || mTime<0.6f)){
                    mDialogManager.tooShort();
                    mAudioManager.cancel();
                    mHandler.sendEmptyMessageDelayed(MSG_DIALOG_DISMISS, 1300);
                }else if(isRecording && !isForced && mCurState==STATE_RECORDING){//正常录制结束
                    mDialogManager.dismissDialog();
                    mAudioManager.release();
                    if (mListener != null) {
                        mListener.onFinish(mTime,mAudioManager.getCurrentFilePath());
                    }
                } else if (isRecording && !isForced && mCurState == STATE_WANT_TO_CANCEL) {
                    mDialogManager.dismissDialog();
                    mAudioManager.cancel();
                }else if(isForced){ //30秒之后强制发送
                    mDialogManager.dismissDialog();
                    mAudioManager.release();
                }
                reset();
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 恢复状态 标志位
     */
    private void reset() {
        isForced = false;
        isRecording = false;
        mReady = false;
        changeState(STATE_NORMAL);
        mTime = 0;
    }

    /**
     * 取消录音
     */
    private boolean wantToCancel(int x, int y) {
        //如果左右滑出 button
        if (x < 0 || x > getWidth()) {
            return true;
        }
        //如果上下滑出 button  加上我们自定义的距离
        if (y < -DISTANCE_Y_CANCEL || y > getHeight() + DISTANCE_Y_CANCEL) {
            return true;
        }
        return false;
    }

    //改变状态
    private void changeState(int state) {
        if (mCurState != state) {
            mCurState = state;
            switch (state) {
                case STATE_NORMAL:
                    setBackgroundResource( R.drawable.btn_recorder_normal);
                    setText(R.string.str_recorder_normal);
                    break;
                case STATE_RECORDING:
                    setBackgroundResource(R.drawable.btn_recording);
                    setText(R.string.str_recorder_recording);
                    if (isRecording) {
                        mDialogManager.recording();
                    }
                    break;
                case STATE_WANT_TO_CANCEL:
                    setBackgroundResource(R.drawable.btn_recording);
                    setText(R.string.str_recorder_want_cancel);
                    mDialogManager.wantToCancel();
                    break;
                case STATE_FORCE_SEND:
                    mDialogManager.forceSend( mTime );
                    break;
            }
        }
    }
}

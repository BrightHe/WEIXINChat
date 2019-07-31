package robot.com.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import robot.com.myapplication.app.AppStr;
import robot.com.myapplication.mqtt.Constants;
import robot.com.myapplication.mqtt.RePublishClient;
import robot.com.myapplication.mqtt.SubscriptClient;
import robot.com.myapplication.poWindow.CustomPopupWindow;
import robot.com.myapplication.poWindow.CustomPopupWindow.OnItemClickListener;
import robot.com.myapplication.recorder.AudioRecorderButton;
import robot.com.myapplication.recorder.MediaManager;
import robot.com.myapplication.tengxunyun.PostAmr;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnItemClickListener {
    private RePublishClient rePublishClient = new RePublishClient();

    private String fromWho = "HZH";
    private String toUser = "HZH";
    private int infType = ListData.TEXT;
    private String fileAmrPath;
    private String picFilePath;

    private List<ListData> lists; //消息列表
    private ListView lv;    //列表控件
    private EditText et_sendText; //消息输入框
    private TextView btn_send;  //发送
    private ImageView pop_plus;
    private String content_str; //
    private TextAdapter adapter;
    private double currentTime, oldTime = 0;//

    //本地广播
    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private LocalBroadcastManager localBroadcastManager;

    private CustomPopupWindow mPop;
    private AudioRecorderButton mAudioRecorderButton;
    public boolean isPop;

//    private ArrayAdapter<Recorder> mAdapter;
//    private List<Recorder> mDatas = new ArrayList<>();
    private View mAnimView_left,mAnimView_right;

    private String TAG = "Test";
    private String message; //接收的消息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        mPop = new CustomPopupWindow( this );
        mPop.setOnItemClickListener( this );

        initView(); //初始化界面
        rePublishClient.connectMQTTServer(); // 连接MQTT服务

        //订阅
        new Thread( new Runnable() {
            @Override
            public void run() {
                Log.i( TAG, "==============The client begin to start ...." );
                SubscriptClient client = new SubscriptClient( MainActivity.this );
                client.start();
                Log.i( TAG, "==============The client is running...." );
            }
        } ).start();

        intentFilter = new IntentFilter();
        intentFilter.addAction( Constants.MY_MQTT_BROADCAST_NAME );
        localReceiver = new LocalReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance( this );
        //注册本地接收器
        localBroadcastManager.registerReceiver( localReceiver, intentFilter );

        setListViewAdapter();
    }

    /**
     * 广播接收器
     */
    private class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            message = intent.getStringExtra( "message" );
            runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    Log.i( TAG, "MainActivity message is:" + message );
                    ListData listData;
                    Gson gson = new Gson();
                    listData = gson.fromJson( message, ListData.class );
                    lists.add( listData );
                    adapter.notifyDataSetChanged();
                    lv.setAdapter( adapter );
                }
            } );
        }
    }

    /*
     *初始化界面
     */
    private void initView() {
        lists = new ArrayList<ListData>();
        lv = (ListView) findViewById( R.id.lv );
        et_sendText = (EditText) findViewById( R.id.et_sendText );
        mAudioRecorderButton = (AudioRecorderButton) findViewById( R.id.id_recorder_button );
        mAudioRecorderButton.setAudioFinishRecorderListener( new AudioRecorderButton.AudioFinishRecorderListener() {
            @Override
            public void onFinish(float seconds, String filePath) {
                Log.i( TAG, "onFinish: time is " + seconds );
                Log.i( TAG, "onFinish: filePath is " + filePath );
                ListData recorderList = new ListData( seconds,null,fromWho, toUser, ListData.SEND, getTime(), ListData.RECORDER );
                lists.add( recorderList );
                //更新adapter
                adapter.notifyDataSetChanged();
                lv.setAdapter( adapter );
                PostAmr postAmr = new PostAmr();
                AppStr appStr = (AppStr)getApplication();
                appStr.setIsCompleted( false );
                Log.i( TAG, "onFinish: 即将上传amr文件" );
                postAmr.PostPic( MainActivity.this,filePath );
                sendAmr(appStr,postAmr,recorderList);
            }
        } );
        btn_send = (TextView) findViewById( R.id.btn_send );
        pop_plus = (ImageView) findViewById( R.id.pop_plus );
        pop_plus.setOnClickListener( this );
        btn_send.setOnClickListener( this );
        adapter = new TextAdapter( this, lists );
        lv.setAdapter( adapter );
    }

    /**
     * 发布语音信息
     */
    private void sendAmr(final AppStr appStr,final PostAmr postAmr,final ListData recorderList) {
        if(appStr.getIsCompleted() == true){
            String amrFilePath = postAmr.getAmrDir();
            Log.i( TAG, "onFinish: amrFilPath is "+amrFilePath );
            String httpMessage = postAmr.getHttpMessage();
            Log.i( TAG, "onFinish: httpMessage is "+httpMessage );
            if(httpMessage.equals( "OK" ) && amrFilePath != null){
                recorderList.setFilePath( amrFilePath );
                Gson gson = new Gson();
                final String jsonStr = gson.toJson( recorderList,ListData.class );
                Log.i( TAG, "onFinish: jsonStr is "+jsonStr );
                new Thread( new Runnable() {
                    @Override
                    public void run() {
                        rePublishClient.myRepublish( jsonStr );
                    }
                } ).start();
            } else{
                Log.i( TAG, "onFinish: 语音上传失败！" );
            }
        }else{
            new Thread( new Runnable() {
                @Override
                public void run() {
                    sendAmr(appStr,postAmr,recorderList );
                }
            } ).start();
        }
    }

    /**
     * 语音消息的读取
     */
    private void setListViewAdapter() {
        adapter = new TextAdapter(this,lists );
        lv.setAdapter( adapter );
        lv.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ListData finalData = lists.get( position );
                if(finalData.getInfType() == ListData.RECORDER){
                    //如果第一个动画正在运行， 停止第一个播放其他的
                    if (mAnimView_left != null && mAnimView_right != null) {
                        if(finalData.getFlag() == ListData.SEND){
                            mAnimView_right.setBackgroundResource( R.drawable.adj_right );
                        }else {
                            mAnimView_left.setBackgroundResource( R.drawable.adj_left );
                        }
                        mAnimView_left = null;
                        mAnimView_right = null;
                    }
                    //播放动画
                    mAnimView_left = view.findViewById( R.id.id_recorder_anim_left );
                    mAnimView_right = view.findViewById( R.id.id_recorder_anim_right );
                    AnimationDrawable animation;
                    if(finalData.getFlag() == ListData.SEND){
                        mAnimView_right.setBackgroundResource( R.drawable.play_anim_right );
                        animation = (AnimationDrawable) mAnimView_right.getBackground();
                    }else{
                        mAnimView_left.setBackgroundResource( R.drawable.play_anim_left );
                        animation = (AnimationDrawable) mAnimView_left.getBackground();
                    }
                    animation.start();

                    Log.i( TAG, "onItemClick: lists.get( position ).filePath is " +lists.get( position ).filePath);
                    //播放音频  完成后改回原来的background
                    MediaManager.playSound( lists.get( position ).filePath, new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            //取消动画
                            if(finalData.getFlag() == ListData.SEND){
                                mAnimView_right.setBackgroundResource( R.drawable.adj_right );
                            }else{
                                mAnimView_left.setBackgroundResource( R.drawable.adj_left );
                            }
                        }
                    } );
                }else{
                    Log.i( TAG, "onItemClick: you have clicked list position"+position );
                }
            }
        } );
    }

    /**
     * 根据生命周期 管理播放录音
     */
    @Override
    protected void onPause() {
        super.onPause();
        MediaManager.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MediaManager.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaManager.release();
    }

    /*
     *设置时间
     */
    private String getTime() {
        currentTime = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
        Date curDate = new Date();
        String str = format.format( curDate );
        if (currentTime - oldTime >= 5 * 60 * 1000) {
            oldTime = currentTime;
            return str;
        } else {
            return "";
        }
    }

    /*
     *点击事件的处理
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                sentAndRepublish();
                break;
            case R.id.pop_plus:
                if (isPop == true) {
                    pop_plus.setImageResource( R.drawable.plus_normal );
                    isPop = false;
                    mPop.dismiss();
                    Log.i( TAG, "onClick: 3---------" + isPop );
                } else {
                    isPop = true;
                    pop_plus.setImageResource( R.drawable.plus_picked );
                    //设置PopWindow中的位置
                    mPop.showAtLocation( findViewById( R.id.LinearLayout1 ), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0 );
                    Log.i( TAG, "onClick: 2 -------" + isPop );
                }
                break;
        }
    }

    /**
     * 至发布
     */
    private void sentAndRepublish() {
        content_str = et_sendText.getText().toString();
        et_sendText.setText( "" );
        //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //        String time = sdf.format(new Date());
        ListData listData;
        listData = new ListData( fromWho, toUser, content_str, ListData.SEND, getTime(), infType );
        lists.add( listData );

        Log.i( TAG, "----------content_str=" + content_str );
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        } );

        Log.i( TAG, "----------content_str=" + content_str );
        Gson gson = new Gson();
        final String jsonStr = gson.toJson( listData, ListData.class );
        Log.i( TAG, "myRepublish: jsonStr is " + jsonStr );

        if (lists.size() > 30) {
            for (int i = 0; i < lists.size(); i++) {
                lists.remove( i );
            }
        }

        new Thread( new Runnable() {
            @Override
            public void run() {
                rePublishClient.myRepublish( jsonStr );
            }
        } ).start();
    }

    /**
     * 重写popupWindow接口的方法
     *
     * @param v
     */
    @Override
    public void setOnItemClick(View v) {
        switch (v.getId()) {
            case R.id.vChat_re:
                infType = ListData.RECORDER;
                et_sendText.setVisibility( View.GONE );
                mAudioRecorderButton.setVisibility( View.VISIBLE );
                Toast.makeText( this, "你点击了发语音", Toast.LENGTH_SHORT ).show();
                Log.i( TAG, "setOnItemClick: 你点击了发语音" );
                break;
            case R.id.camera_re:
                infType = ListData.IMAGE;
                Toast.makeText( this, "你点击了拍照", Toast.LENGTH_SHORT ).show();
                Log.i( TAG, "setOnItemClick: 你点击了拍照" );
                break;
            case R.id.pictures_re:
                infType = ListData.IMAGE;
                Toast.makeText( this, "你点击了相册", Toast.LENGTH_SHORT ).show();
                Log.i( TAG, "setOnItemClick: 你点击了相册" );
                break;
        }
    }
}

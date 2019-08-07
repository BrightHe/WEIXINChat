package robot.com.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import robot.com.myapplication.recorder.AudioRecorderButton;
import robot.com.myapplication.recorder.MediaManager;
import robot.com.myapplication.tengxunyun.NTest;
import robot.com.myapplication.tengxunyun.PostObj;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private RePublishClient rePublishClient = new RePublishClient();

    private String fromWho = "HZH";
    private String toUser = "HZH";
    private int infType = ListData.TEXT;
//    private String fileAmrPath;
//    private String picFilePath;

    private List<ListData> lists; //消息列表
    private ListView lv;    //列表控件
    private EditText et_sendText; //消息输入框
    private Button  btn_send;  //发送
    private ImageView bt_voice,bt_keyboard,bt_emoji,pop_plus,camera_img,pictures_img;
    private LinearLayout others;
    private String content_str;
    private TextAdapter adapter;
    private double currentTime, oldTime = 0;

    //本地广播
    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private LocalBroadcastManager localBroadcastManager;

    //自定义button
    private AudioRecorderButton mAudioRecorderButton;
    public boolean isPop;

    private View mAnimView_left,mAnimView_right;

    private String TAG = "Test";
    private String message; //接收的消息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        // 启动activity时不自动弹出软键盘
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        initView(); //初始化界面
        setDefaultState();
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

        //list点击
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

    /**
     *初始化界面
     */
    private void initView() {
        lists = new ArrayList<ListData>();
        lv = (ListView) findViewById( R.id.lv );
        bt_voice = (ImageView) findViewById( R.id.bt_voice );
        bt_voice.setOnClickListener( this );
        bt_keyboard = (ImageView) findViewById( R.id.bt_keyboard );
        bt_keyboard.setOnClickListener( this );
        bt_emoji = (ImageView) findViewById( R.id.bt_emoji );
        bt_emoji.setOnClickListener( this );
        et_sendText = (EditText) findViewById( R.id.et_sendText );
        et_sendText.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.length() != 0){
                    btn_send.setVisibility( View.VISIBLE );
                    pop_plus.setVisibility( View.GONE );
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() == 0){
                    btn_send.setVisibility( View.GONE );
                    pop_plus.setVisibility( View.VISIBLE );
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() != 0){
                    //结束输入时，发送显示
                    btn_send.setVisibility( View.VISIBLE );
                    pop_plus.setVisibility( View.GONE );
                }
            }
        } );
        mAudioRecorderButton = (AudioRecorderButton) findViewById( R.id.id_recorder_button );
        mAudioRecorderButton.setAudioFinishRecorderListener( new AudioRecorderButton.AudioFinishRecorderListener() {
            @Override
            public void onFinish(float seconds, String filePath) {
                ListData recorderList = new ListData( seconds,null,fromWho, toUser, ListData.SEND, getTime(), ListData.RECORDER );
                recorderList.setAmrFilePath( filePath );
                lists.add( recorderList );
                //更新adapter
                adapter.notifyDataSetChanged();
                lv.setAdapter( adapter );
                //测试网络状况
                if(NTest.getConnectedType( MainActivity.this ) == 1){
                    PostObj postAmr = new PostObj();
                    AppStr appStr = (AppStr)getApplication();
                    appStr.setIsCompleted( false );
                    Log.i( TAG, "onFinish: 即将上传amr文件" );
                    postAmr.PostObject( MainActivity.this,filePath,ListData.SEND );
                    sendAmr(appStr,postAmr,recorderList);
                }else{
                    Toast.makeText( MainActivity.this, "网络连接不可用，请稍后重试！", Toast.LENGTH_SHORT ).show();
                }
            }
        } );
        btn_send = (Button) findViewById( R.id.bt_send );
        btn_send.setOnClickListener( this );
        pop_plus = (ImageView) findViewById( R.id.pop_plus );
        pop_plus.setOnClickListener( this );
        others = (LinearLayout)findViewById( R.id.others );
        camera_img = (ImageView)findViewById( R.id.camera_img );
        camera_img.setOnClickListener( this );
        pictures_img = (ImageView) findViewById( R.id.pictures_img );
        pictures_img.setOnClickListener( this );
        adapter = new TextAdapter( this, lists );
        lv.setAdapter( adapter );
    }

    /**
     * 发布语音信息
     */
    private void sendAmr(final AppStr appStr, final PostObj postAmr, final ListData recorderList) {
        if(appStr.IsCompleted() == true){
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
        lv.setSelection( adapter.getCount()-1 );
        lv.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ListData finalData = lists.get( position );
                if(finalData.getInfType() == ListData.RECORDER){
                    //如果第一个动画正在运行， 停止第一个播放其他的
                    if(mAnimView_right != null) {
                        mAnimView_right.setBackgroundResource( R.drawable.adj_right );
                    }
                    if(mAnimView_left != null) {
                        mAnimView_left.setBackgroundResource( R.drawable.adj_left );
                    }
                    mAnimView_right = null;
                    mAnimView_left =  null;

                    //播放动画
                    mAnimView_left = view.findViewById( R.id.id_recorder_anim_left );
                    mAnimView_right = view.findViewById( R.id.id_recorder_anim_right );
                    AnimationDrawable animation = null;
                    AppStr appStr = (AppStr)getApplicationContext();
                    if(finalData.getFlag() == ListData.SEND){
                        //检测文件
                        if(lists.get( position ).getAmrFilePath() == null){
                            Toast.makeText( MainActivity.this, "录音文件地址为空，无法播放！", Toast.LENGTH_SHORT ).show();
                            return;
                        }

                        if(!NTest.fileIsExists( lists.get( position ).getAmrFilePath() )){
                            Toast.makeText( MainActivity.this, "录音文件不存在，无法播放！", Toast.LENGTH_SHORT ).show();
                            return;
                        }
                        mAnimView_right.setBackgroundResource( R.drawable.play_anim_right );
                        animation = (AnimationDrawable) mAnimView_right.getBackground();
                        appStr.setDownLoad( true );
                    }else{
                        if(lists.get( position ).getAmrFilePath() == null){
                            //判断网络状态，要进行网络加载工作
                            if(NTest.getConnectedType( MainActivity.this ) == 1){
                                appStr.setIsCompleted( false );
                                PostObj downLoad  = new PostObj();
                                Log.i( TAG, "onItemClick: 即将下载录音文件" );
                                downLoad.PostObject( MainActivity.this,finalData.getFilePath(),ListData.RECEIVE );
                                appStr.setDownLoad( false );
                                downLoadRecorder(appStr,downLoad,position);
                                //                                mAnimView_left.setBackgroundResource( R.drawable.play_anim_left );
                                //                                animation = (AnimationDrawable) mAnimView_left.getBackground();
                            }else {
                                Toast.makeText( MainActivity.this, "亲，当前网络断开了哦！无法播放", Toast.LENGTH_SHORT ).show();
                            }
                        }else{
                            if(!NTest.fileIsExists( lists.get( position ).getAmrFilePath() )){
                                Toast.makeText( MainActivity.this, "录音文件不存在，无法播放！", Toast.LENGTH_SHORT ).show();
                                return;
                            }
                            //                            mAnimView_left.setBackgroundResource( R.drawable.play_anim_left );
                            //                            animation = (AnimationDrawable) mAnimView_left.getBackground();
                        }
                        mAnimView_left.setBackgroundResource( R.drawable.play_anim_left );
                        animation = (AnimationDrawable) mAnimView_left.getBackground();
                    }
                    animation.start();
                    //播放录音
                    playRecorder(appStr,finalData,position,mAnimView_left,mAnimView_right);
                } else {
                    if(isPop){
                        others.setVisibility( View.GONE );
                        isPop = false;
                        pop_plus.setImageResource( R.drawable.plus_normal );
                    }
                }
            }
        } );
    }

    /**
     * 播放语音
     */
    private void playRecorder(final AppStr appStr, final ListData finalData, final int position, final View mAnimView_left, final View mAnimView_right) {
        if(appStr.isDownLoad() == true){
            //检测文件
            if(lists.get( position ).getAmrFilePath() == null){
                Toast.makeText( MainActivity.this, "录音文件地址为空，无法播放！", Toast.LENGTH_SHORT ).show();
                return;
            }

            if(NTest.fileIsExists( lists.get( position ).getAmrFilePath() ) == false){
                Toast.makeText( MainActivity.this, "录音文件不存在，无法播放！", Toast.LENGTH_SHORT ).show();
                return;
            }

            //播放音频  完成后改回原来的background
            MediaManager.playSound( lists.get( position ).getAmrFilePath(), new MediaPlayer.OnCompletionListener() {
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
            new Thread( new Runnable() {
                @Override
                public void run() {
                    playRecorder(appStr,finalData,position,mAnimView_left,mAnimView_right);
                }
            } ).start();
        }
    }

    /**
     * 下载录音文件
     */
    @Nullable
    private void downLoadRecorder(final AppStr appStr, final PostObj downLoad, final int position) {
        //网络下载需要时间，需要等待下载成功结束拿到本地地址
        if(appStr.IsCompleted() == true){
            String httpMessage = downLoad.getHttpMessage();
            String amrPath = downLoad.getAmrDir();
            if(httpMessage.equals( "OK" )){
                Log.i( TAG, "downLoadRecorder: amrPath is "+amrPath );
                lists.get( position ).setAmrFilePath( amrPath );
                appStr.setDownLoad( true );
            }else {
                Toast.makeText( MainActivity.this, "下载语音文件失败！", Toast.LENGTH_SHORT ).show();
                appStr.setDownLoad( true );
            }
        }else {
            new Thread( new Runnable() {
                @Override
                public void run() {
                    downLoadRecorder( appStr,downLoad,position );
                }
            } ).start();
        }
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

    /**
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

    /**
     *点击事件的处理
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_voice:
                bt_voice.setVisibility( View.GONE );
                bt_keyboard.setVisibility( View.VISIBLE );
                et_sendText.setVisibility( View.GONE );
                mAudioRecorderButton.setVisibility( View.VISIBLE );
                btn_send.setVisibility( View.GONE );
                bt_emoji.setVisibility( View.VISIBLE );
                others.setVisibility( View.GONE );
                break;
            case R.id.bt_keyboard:
                bt_keyboard.setVisibility( View.GONE );
                bt_voice.setVisibility( View.VISIBLE );
                et_sendText.setVisibility( View.VISIBLE );
                mAudioRecorderButton.setVisibility( View.GONE );
                bt_emoji.setVisibility( View.VISIBLE );
                pop_plus.setVisibility( View.VISIBLE );
                others.setVisibility( View.GONE );
                break;
            case R.id.bt_emoji:
                bt_keyboard.setVisibility( View.GONE );
                bt_voice.setVisibility( View.VISIBLE );
                et_sendText.setVisibility( View.VISIBLE );
                mAudioRecorderButton.setVisibility( View.GONE );
                bt_emoji.setVisibility( View.VISIBLE );
                pop_plus.setVisibility( View.VISIBLE );
                others.setVisibility( View.GONE );
                break;
            case R.id.bt_send:
                sentAndRepublish();
                break;
            case R.id.pop_plus:
                if (isPop == true) {
                    pop_plus.setImageResource( R.drawable.plus_normal );
                    isPop = false;
//                    mPop.dismiss();
                    others.setVisibility( View.GONE );
                    Log.i( TAG, "onClick: 3---------" + isPop );
                } else {
                    isPop = true;
                    pop_plus.setImageResource( R.drawable.plus_picked );
                    others.setVisibility( View.VISIBLE );
                    //设置PopWindow中的位置
//                    mPop.showAtLocation( findViewById( R.id.LinearLayout1 ), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0 );
                    Log.i( TAG, "onClick: 2 -------" + isPop );
                }
                break;
//            case R.id.vChat_img:
//                infType = ListData.RECORDER;
//                et_sendText.setVisibility( View.GONE );
//                mAudioRecorderButton.setVisibility( View.VISIBLE );
//                Toast.makeText( this, "你点击了发语音", Toast.LENGTH_SHORT ).show();
//                Log.i( TAG, "setOnItemClick: 你点击了发语音" );
//                break;
            case R.id.camera_img:
                infType = ListData.IMAGE;
                Toast.makeText( this, "你点击了拍照", Toast.LENGTH_SHORT ).show();
                Log.i( TAG, "setOnItemClick: 你点击了拍照" );
                break;
            case R.id.pictures_img:
                infType = ListData.IMAGE;
                Toast.makeText( this, "你点击了相册", Toast.LENGTH_SHORT ).show();
                Log.i( TAG, "setOnItemClick: 你点击了相册" );
                break;
        }
    }

    /**
     * 底部输入的初始状态
     */
    public void setDefaultState( ){
        bt_keyboard.setVisibility( View.GONE );
        bt_voice.setVisibility( View.VISIBLE );
        et_sendText.setVisibility( View.VISIBLE );
        mAudioRecorderButton.setVisibility( View.GONE );
        bt_emoji.setVisibility( View.VISIBLE );
        pop_plus.setVisibility( View.VISIBLE );
        btn_send.setVisibility( View.GONE );
        others.setVisibility( View.GONE );
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
}

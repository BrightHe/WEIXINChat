package robot.com.myapplication.mqtt;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Created by Administrator on 2019/7/28.
 */

public class RePublishClient {
    private String TAG = "Test";
    private MqttClient client;
    private String host = "tcp://47.105.185.251:61613"; //主机的ip(tcp连接)
    private String userName = "admin";    // MQTT的server的用户名
    private String passWord = "password"; // MQTT的server的密码
    private MqttTopic topic;
    private MqttMessage message;

    private String myTopic = "HZH/HZH";     //   发布消息主题
    private String myClientID = "13791156728@163.com"; //  发布消息的ID , 可以是任意唯一字符串 （比如：邮箱，手机号，UUID等）

    /*
     *连接MQTT服务
     */
    public void connectMQTTServer(){
        try {
            Log.i(TAG, "=================begin to connect MQTT server====================");
            client = new MqttClient(host, myClientID, new MemoryPersistence()); // myClientID 是客户端ID，可以是任何唯一的标识

            // 连接 MQTT服务器
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            }).start();

            Log.i(TAG, "=================connect MQTT server end=========================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接MQTT服务器
     */
    private void connect() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setUserName(userName);
        options.setPassword(passWord.toCharArray());
        // 设置超时时间
        options.setConnectionTimeout(10);
        // 设置会话心跳时间
        options.setKeepAliveInterval(20);
        try {
            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {
                    Log.i(TAG, "connectionLost-----------");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.i(TAG, "deliveryComplete---------" + token.isComplete());
                }

                @Override
                public void messageArrived(String topic, MqttMessage arg1)
                        throws Exception {
                    Log.i(TAG, "messageArrived----------");

                }
            });

            topic = client.getTopic(myTopic);
            client.connect(options);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     *发布消息
     */
    public void myRepublish(String jsonStr) {
        try {
            message = new MqttMessage();
            message.setQos(2); // 可以有三种值（0,1,2），分别代表消息发送情况(保证消息传递的次数)：最多一次(即<=1);至少一次(即>=1);一次(即=1)
            message.setRetained(false);// 发布保留标识，表示服务器要保留这次推送的信息，如果有新的订阅者出现，就把这消息推送给它，如果设有那么推送至当前订阅者后释放。
            Log.i(TAG, message.isRetained() + "------retained状态");

            //设置负载，即消息内容
            message.setPayload(jsonStr.getBytes());

            Log.i(TAG, "message is :" + new String(message.getPayload()));
            MqttDeliveryToken token = topic.publish(message);
            token.waitForCompletion();
            Log.i(TAG, token.isComplete() + "======================token.isComplete()=========================");
        } catch (Exception e) {
            e.printStackTrace();
            Log.i( TAG, "myRepublish: "+message.toString() );
        }
    }

}

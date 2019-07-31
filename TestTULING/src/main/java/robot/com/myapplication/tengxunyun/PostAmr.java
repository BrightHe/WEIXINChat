package robot.com.myapplication.tengxunyun;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlProgressListener;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.transfer.COSXMLUploadTask;
import com.tencent.cos.xml.transfer.TransferConfig;
import com.tencent.cos.xml.transfer.TransferManager;
import com.tencent.cos.xml.transfer.TransferState;
import com.tencent.cos.xml.transfer.TransferStateListener;
import com.tencent.qcloud.core.auth.QCloudCredentialProvider;
import com.tencent.qcloud.core.auth.ShortTimeCredentialProvider;

import robot.com.myapplication.app.AppStr;
import robot.com.myapplication.recorder.FileNameUtils;

/**
 * Created by Administrator on 2019/7/30.
 */

public class PostAmr {
    private String TAG = "Test";
    private static String amrDir,httpMessage;

    public String getAmrDir() {
        return amrDir;
    }

    public String getHttpMessage() {
        return httpMessage;
    }

    public void PostPic(Context context, String filePath){
        String region = "ap-chengdu";//存储桶所在的地域
        String secretId = "AKIDTfdNr6E5KK5dvvnv54oOnJYVIyPe5S3T"; //永久密钥 secretId
        String secretKey ="djfVFBQWYaVu7C6qTxKeV6WkjVGrRnRv"; //永久密钥 secretKey

        //创建 CosXmlServiceConfig 对象，根据需要修改默认的配置参数
        CosXmlServiceConfig serviceConfig = new CosXmlServiceConfig.Builder()
                .setRegion(region)
                .isHttps(true) // 使用 https 请求, 默认 http 请求
                .setDebuggable(true)
                .builder();

        /**
         * 初始化 {@link QCloudCredentialProvider} 对象，来给 SDK 提供临时密钥。
         */
        QCloudCredentialProvider credentialProvider = new ShortTimeCredentialProvider(secretId,
                secretKey, 300);
        //初始化CosXmlService 服务类，用来操作各种 COS 服务
        CosXmlService cosXmlService = new CosXmlService(context , serviceConfig, credentialProvider);
        uploadObject(cosXmlService,context,filePath);
    }

    //上传对象
    public void uploadObject(CosXmlService cosXmlService, final Context context, String filePath){
        // 初始化 TransferConfig
//        TransferConfig transferConfig = new TransferConfig.Builder().build();
        /**/
//         若有特殊要求，则可以如下进行初始化定制。如限定当对象 >= 2M 时，启用分片上传，且分片上传的分片大小为 1M, 当源对象大于 5M 时启用分片复制，且分片复制的大小为 5M。
         TransferConfig transferConfig = new TransferConfig.Builder()
         .setDividsionForCopy(5 * 1024 * 1024) // 是否启用分片复制的最小对象大小
         .setSliceSizeForCopy(5 * 1024 * 1024) //分片复制时的分片大小
         .setDivisionForUpload(2 * 1024 * 1024) // 是否启用分片上传的最小对象大小
         .setSliceSizeForUpload(1024 * 1024) //分片上传时的分片大小
         .build();

        //初始化 TransferManager
        TransferManager transferManager = new TransferManager(cosXmlService, transferConfig);

        String bucket = "pic-001-1259665619"; //存储桶名称
        String cosPath = "vDemo/"+ FileNameUtils.getFileName(); //即对象到 COS 上的绝对路径, 格式如 cosPath = "text.txt";           "对象键"
        String srcPath = null;
        if(filePath == null){
            Toast.makeText( context, "文件地址为空！", Toast.LENGTH_SHORT ).show();
            return ;
        }else{
            srcPath = filePath; // 如 srcPath=Environment.getExternalStorageDirectory().getPath() + "/text.txt";  "本地文件的绝对路径"
        }
        String uploadId = null; //若存在初始化分片上传的 UploadId，则赋值对应 uploadId 值用于续传，否则，赋值 null。
        //上传对象
        COSXMLUploadTask cosxmlUploadTask = transferManager.upload(bucket, cosPath, srcPath, uploadId);

        /**
         * 若是上传字节数组，则可调用 TransferManager 的 upload(string, string, byte[]) 方法实现;
         * byte[] bytes = "this is a test".getBytes(Charset.forName("UTF-8"));
         * cosxmlUploadTask = transferManager.upload(bucket, cosPath, bytes);
         */

        /**
         * 若是上传字节流，则可调用 TransferManager 的 upload(String, String, InputStream) 方法实现；
         * InputStream inputStream = new ByteArrayInputStream("this is a test".getBytes(Charset.forName("UTF-8")));
         * cosxmlUploadTask = transferManager.upload(bucket, cosPath, inputStream);
         */


        //设置上传进度回调
        cosxmlUploadTask.setCosXmlProgressListener(new CosXmlProgressListener() {
            @Override
            public void onProgress(long complete, long target) {
                float progress = 1.0f * complete / target * 100;
                Log.d(TAG,  String.format("progress = %d%%", (int)progress));
            }
        });
        //设置返回结果回调
        cosxmlUploadTask.setCosXmlResultListener(new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                COSXMLUploadTask.COSXMLUploadTaskResult cOSXMLUploadTaskResult = (COSXMLUploadTask.COSXMLUploadTaskResult)result;
                //对象地址
                httpMessage = cOSXMLUploadTaskResult.httpMessage;
                Log.i( TAG, "onSuccess: flag is "+httpMessage );
                amrDir = cOSXMLUploadTaskResult.accessUrl;
                Log.i( TAG, "onSuccess: vDir is "+amrDir );
                AppStr appStr = (AppStr)context.getApplicationContext();
                appStr.setIsCompleted( true );
            }

            @Override
            public void onFail(CosXmlRequest request, CosXmlClientException exception, CosXmlServiceException serviceException) {
                AppStr appStr = (AppStr)context.getApplicationContext();
                appStr.setIsCompleted( true );
                Log.d(TAG,  "Failed: " + (exception == null ? serviceException.getMessage() : exception.toString()));
            }
        });
        //设置任务状态回调, 可以查看任务过程
        cosxmlUploadTask.setTransferStateListener(new TransferStateListener() {
            @Override
            public void onStateChanged(TransferState state) {
//                Log.d(TAG, "Task state:" + state.toString());
                Log.i( TAG, "onStateChanged: "+state.name() );
            }
        });

        /**
         若有特殊要求，则可以如下操作：
         PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, cosPath, srcPath);
         putObjectRequest.setRegion(region); //设置存储桶所在的地域
         putObjectRequest.setNeedMD5(true); //是否启用 Md5 校验
         COSXMLUploadTask cosxmlUploadTask = transferManager.upload(putObjectRequest, uploadId);
         */

     /*   //取消上传
        cosxmlUploadTask.cancel();


        //暂停上传
        cosxmlUploadTask.pause();

        //恢复上传
        cosxmlUploadTask.resume();*/
    }

}

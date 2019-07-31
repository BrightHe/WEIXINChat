package robot.com.myapplication;

import android.os.AsyncTask;
import android.util.Log;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HttpData extends AsyncTask<String, Void, String> {

	private HttpClient httpClient;
	private HttpGet httpGet;
	private HttpResponse httpResponse;
	private HttpEntity httpEntity;
	private InputStream in;
	
	private HttpGetDataListener listener;
	
	private String url;
	public HttpData(String url ,HttpGetDataListener listener) {
		this.url = url;
		this.listener = listener;
	}
	
	@Override
	protected String doInBackground(String... params) {
		try {
			httpClient = new DefaultHttpClient();
			httpGet =  new HttpGet(url);
			httpResponse = httpClient.execute(httpGet);
			httpEntity = httpResponse.getEntity();
			
			Log.i("haha1", "----------"+httpEntity.toString());
			
			in = httpEntity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			Log.i("haha2", "----------"+br.toString());
			
			String line = null;
			StringBuffer sb = new StringBuffer();
			while((line  = br.readLine()) != null){
				sb.append(line);
			}
			
			Log.i("haha3", "----------"+sb.toString());
			
			return sb.toString();
			
		} catch (Exception e) {
		}
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		listener.getDataUrl(result);
		
		Log.i("haha4", "----------"+result);
		
		super.onPostExecute(result);
	}
	
	

}

package com.example.lint;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class PostService extends IntentService {

    private Exception mException;
    private HttpURLConnection httpConnection;
    private URL mUrl;

    public PostService() {
        super("Service_Post");

        Log.i("LOG_SERVICE","Constructor Service_Post().");
    }

    public void onCreate(){

        super.onCreate();

        Log.i("LOG_SERVICE","Service onCreate()");
    }

    protected void onHandleIntent(Intent intent){
        try{
            String url=intent.getExtras().getString("url");
            JSONObject datosJson=new JSONObject(intent.getExtras().getString(("datosJson")));
            String token=intent.getExtras().getString("token");
            String operacion=intent.getExtras().getString("operacion");

            ejecutarPost(url,datosJson,token,operacion);

            Log.i("LOG_SERVICE","Se ejecuto onHandleIntent()");
        }
        catch(Exception Exc)
        {
            Log.e("LOG_SERVICE","Service Error: "+Exc.getMessage());
        }
    }

    protected void ejecutarPost(String url,JSONObject datosJson,String token, String operacion)
    {
        String action="";

        //Se retorna distinta accion para el broadcast receiver dependiendo el post
        if(url.equals("http://so-unlam.net.ar/api/api/register"))
            action="com.example.intentservice.intent.action.RESPUESTA_REGISTER";

        if(url.equals("http://so-unlam.net.ar/api/api/login"))
            action="com.example.intentservice.intent.action.RESPUESTA_LOGIN";

        if(url.equals("http://so-unlam.net.ar/api/api/event"))
            action="com.example.intentservice.intent.action.RESPUESTA_EVENT";

        if(url.equals("http://so-unlam.net.ar/api/api/refresh"))
            action="com.example.intentservice.intent.action.RESPUESTA_TOKEN";

        String result=Post(url,datosJson,token,operacion);

        Intent i=new Intent(action);
        i.putExtra("datosJson",result);

        sendBroadcast(i);
    }

    private String Post(String url,JSONObject datosJson, String token, String operacion)
    {
        HttpURLConnection urlConnection=null;
        String result="";
        String line="";

        try
        {
            URL mUrl=new URL(url);

            urlConnection=(HttpURLConnection) mUrl.openConnection();
            urlConnection.setRequestProperty("Content-Type","application/json; charset=UTF-8");

            //solo para el registro de evento
            if(token!=null)
                urlConnection.setRequestProperty("Authorization","Bearer "+token);

            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setConnectTimeout(5000);
            urlConnection.setRequestMethod(operacion);

            DataOutputStream wr=new DataOutputStream(urlConnection.getOutputStream());

            wr.write(datosJson.toString().getBytes("UTF-8"));

            Log.i("LOG_SERVICE","Se va a enviar al servidor: "+datosJson.toString());

            wr.flush();
            wr.close();

            urlConnection.connect();
            int responseCode=urlConnection.getResponseCode();

            BufferedReader bufferedReader;

            if((responseCode==HttpURLConnection.HTTP_OK)||(responseCode==HttpURLConnection.HTTP_CREATED))
            {
                bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            }
            else
            {
                bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
            }

            while ((line = bufferedReader.readLine()) != null) {
                result += line + "\n";
            }
            bufferedReader.close();


            Log.i("LOG_SERVICE","ResponseCode: "+ Integer.toString(responseCode));
            Log.i("LOG_SERVICE","Result: "+ result);

            mException=null;
            urlConnection.disconnect();
            return result;
        }
        catch(Exception Exc)
        {
            Log.e("LOG_SERVICE","Service Error: "+Exc.getMessage());
            return null;
        }

    }

}

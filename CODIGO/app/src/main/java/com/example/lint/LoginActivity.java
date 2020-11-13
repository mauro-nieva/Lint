package com.example.lint;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class LoginActivity extends AppCompatActivity {

    private Button   btnLogin;
    private Button   btnRegister;
    private TextView txtEmail;
    private TextView txtPassword;
    private ImageView imgInternet;

    //Variables de Broadcast Receiver para Post
    public IntentFilter filtro;
    private LoginActivity.ReceptorOperacion receiver;

    //variables de conexion a internet
    private Handler handlerInternet;
    private Timer timerInternet;
    private boolean internetOn;

    //variable clase SharedPreferences
    public ArchivoPermanente archivoPermanente;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtEmail=(TextView) findViewById(R.id.txtEmail);
        txtPassword=(TextView) findViewById(R.id.txtPassword);
        imgInternet=(ImageView) findViewById(R.id.imgInternet);
        btnLogin=(Button) findViewById(R.id.btnLogin);
        btnRegister=(Button) findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(OnClickLoguearse);
        btnRegister.setOnClickListener(OnClickRegistrarse);

        receiver=new LoginActivity.ReceptorOperacion();
        internetOn=true;

        archivoPermanente=new ArchivoPermanente();

        ActivaTimerInternet();
        configurarBroadcastreceiver();

        //se guarda la ultima activity
        archivoPermanente.ultimaActivity="Login";
        archivoPermanente.escribeUltActivity(this);

        //Oculta la barra
        getSupportActionBar().hide();


        //Solicita permiso para la camara en tiempo de ejecucion
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.CAMERA}, 50);
        }

        Log.i("LOG_LOGIN","OnCreate");
    }

    //Activa Timer que Chequea Conexion a Internet
    //----------------------------------------------------------------------------------------------
    public void ActivaTimerInternet()
    {
        try
        {
            //Se activa el timer que consulta conexion de internet
            handlerInternet = new Handler();
            timerInternet = new Timer();
            timerInternet.schedule(timerTaskInternet, 0, 2000);
        }
        catch(Exception e)
        {
            Log.e("LOG_LOGIN","Error Activar Timer Internet:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //OnClickListener del boton de Registrarse
    //----------------------------------------------------------------------------------------------
    View.OnClickListener OnClickRegistrarse=new View.OnClickListener()
    {
        @Override
        public void onClick(View v) {
            Intent intent=new Intent(LoginActivity.this,RegisterActivity.class);
            startActivity(intent);
        }
    };
    //----------------------------------------------------------------------------------------------

    //OnClickListener del boton de Loguearse
    //----------------------------------------------------------------------------------------------
    View.OnClickListener OnClickLoguearse=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {

                if(internetOn) {
                    JSONObject obj = new JSONObject();

                    obj.put("email", txtEmail.getText());
                    obj.put("password", txtPassword.getText());

                    Intent i = new Intent(LoginActivity.this, PostService.class);

                    i.putExtra("url", "http://so-unlam.net.ar/api/api/login");
                    i.putExtra("datosJson", obj.toString());
                    i.putExtra("operacion", "POST");

                    startService(i);

                    Log.i("LOG_LOGIN","Se ejecuta POST LOGIN.");
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Sin conexion a internet", Toast.LENGTH_LONG).show();
                    Log.e("LOG_LOGIN","POST LOGIN Sin conexion a internet");
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("LOG_LOGIN","Error Login: "+e.getMessage());
            }
        }
    };
    //----------------------------------------------------------------------------------------------

    //Configuracion de BroadcastReceiver
    //----------------------------------------------------------------------------------------------
    private void configurarBroadcastreceiver(){

        filtro=new IntentFilter("com.example.intentservice.intent.action.RESPUESTA_LOGIN");

        filtro.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(receiver,filtro);

    }
    //----------------------------------------------------------------------------------------------

    //BroadcastReceiver para el POST de Login
    //----------------------------------------------------------------------------------------------
    public class ReceptorOperacion extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent){
            try{
                String datosJsonString= intent.getStringExtra("datosJson");
                JSONObject datosJson = new JSONObject(datosJsonString);
                String success = datosJson.getString("success");

                Log.i("LOG_LOGIN","Datos Json Login Thread:"+datosJsonString);

                if(success.equals("true"))
                {
                    Toast.makeText(getApplicationContext(), "Ingreso correcto", Toast.LENGTH_LONG).show();

                    Log.i("LOG_LOGIN","Token:"+datosJson.get("token"));
                    Log.i("LOG_LOGIN","Token Refresh:"+datosJson.get("token_refresh"));

                    String token=datosJson.get("token").toString();
                    String tokenRefresh=datosJson.get("token_refresh").toString();

                    //Se escriben los token en el Shared Preferences
                    archivoPermanente.token=token;
                    archivoPermanente.tokenRefresh=tokenRefresh;
                    archivoPermanente.escribeToken(context);

                    Intent Int_Linterna=new Intent(LoginActivity.this,LinternaActivity.class);
                    startActivity(Int_Linterna);
                }
                else
                {
                    String msg=datosJson.getString("msg");
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }
            }
            catch(Exception e)
            {
                Log.e("LOG_LOGIN","Error Json Login Thread:"+e.getMessage());
            }

        }
    }
    //----------------------------------------------------------------------------------------------

    //onResume
    //----------------------------------------------------------------------------------------------
    protected void onResume() {
        super.onResume();

        Log.i("LOG_LOGIN","OnResume");

    }
    //----------------------------------------------------------------------------------------------

    //Libera el Timer de Conexion a Internet
    //----------------------------------------------------------------------------------------------
    public void LiberaTimerInternet()
    {
        try
        {
            //Se cancela el timer que consulta conexion de internet
            timerTaskInternet.cancel();
            timerInternet.cancel();
        }
        catch(Exception e)
        {
            Log.e("LOG_LOGIN","Error Liberar Timer Internet:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //onDestroy
    //----------------------------------------------------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i("LOG_LOGIN","OnDestroy");

        LiberaTimerInternet();
        finish();
    }
    //----------------------------------------------------------------------------------------------

    //TimerTask de conexion a internet
    //----------------------------------------------------------------------------------------------
    public TimerTask timerTaskInternet = new TimerTask() {
        @Override
        public void run() {
            handlerInternet.post(new Runnable() {
                public void run() {
                    try {
                        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                        if (networkInfo != null && networkInfo.isConnected())
                        {
                            imgInternet.setImageResource(R.drawable.internet_on);
                            internetOn=true;
                        }
                        else {
                            imgInternet.setImageResource(R.drawable.internet_off);
                            internetOn=false;
                        }

                    } catch (Exception e) {
                        Log.e("LOG_LOGIN","Error Conexion Internet:"+e.getMessage());
                    }
                }
            });


        }
    };
    //----------------------------------------------------------------------------------------------

    //onPause
    //----------------------------------------------------------------------------------------------
    protected void onPause() {
        super.onPause();

        Log.i("LOG_LOGIN","OnPause");
    }
    //----------------------------------------------------------------------------------------------

}

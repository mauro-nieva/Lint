package com.example.lint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class RegisterActivity extends AppCompatActivity {

    private Button btnRegister;
    private TextView txtName;
    private TextView txtLastname;
    private TextView txtDni;
    private TextView txtEmail;
    private TextView txtPassword;
    private TextView txtCommission;
    private ImageView imgInternet;

    public IntentFilter filtro;
    private ReceptorOperacion receiver;

    //variables de conexion de internet
    private Handler handlerInternet;
    private Timer timerInternet;
    private boolean internetOn;

    //variable clase SharedPreferences
    public ArchivoPermanente archivoPermanente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnRegister=(Button) findViewById(R.id.btnRegister);
        txtName=(TextView) findViewById(R.id.txtName);
        txtLastname=(TextView) findViewById(R.id.txtLastname);
        txtDni=(TextView) findViewById(R.id.txtDni);
        txtEmail=(TextView) findViewById(R.id.txtEmail);
        txtPassword=(TextView) findViewById(R.id.txtPassword);
        txtCommission=(TextView) findViewById(R.id.txtCommission);
        imgInternet=(ImageView) findViewById(R.id.imgInternet);

        receiver=new ReceptorOperacion();
        archivoPermanente=new ArchivoPermanente();

        btnRegister.setOnClickListener(OnClickRegistrarse);

        ActivaTimerInternet();
        configurarBroadcastreceiver();

        getSupportActionBar().hide();
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
            Log.e("LOG_REGISTRO","Error Activar Timer Internet:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------


    //OnClickListener del boton de Registrarse
    //----------------------------------------------------------------------------------------------
    private View.OnClickListener OnClickRegistrarse=(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            JSONObject obj = new JSONObject();

            try {

                if(internetOn)
                {
                    obj.put("env", "PROD");
                    obj.put("name", txtName.getText());
                    obj.put("lastname", txtLastname.getText());
                    obj.put("dni", txtDni.getText());
                    obj.put("email", txtEmail.getText());
                    obj.put("password", txtPassword.getText());
                    obj.put("commission", txtCommission.getText());

                    Intent i = new Intent(RegisterActivity.this, PostService.class);

                    i.putExtra("url", "http://so-unlam.net.ar/api/api/register");
                    i.putExtra("datosJson", obj.toString());
                    i.putExtra("operacion", "POST");

                    startService(i);

                    Log.i("LOG_REGISTRO","Se ejecuta POST REGISTRO.");

                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Sin conexion a internet", Toast.LENGTH_LONG).show();
                    Log.e("LOG_REGISTRO","POST REGISTRO Sin conexion a internet");
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("LOG_REGISTRO","Error Registro: "+e.getMessage());
            }
        }
    }) ;
    //----------------------------------------------------------------------------------------------

    //Configuracion de BroadcastReceiver
    //----------------------------------------------------------------------------------------------
    private void configurarBroadcastreceiver(){

        filtro=new IntentFilter("com.example.intentservice.intent.action.RESPUESTA_REGISTER");

        filtro.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(receiver,filtro);

    }
    //----------------------------------------------------------------------------------------------

    //BroadcastReceiver de Registro
    //----------------------------------------------------------------------------------------------
    public class ReceptorOperacion extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent){
            try{
                String datosJsonString= intent.getStringExtra("datosJson");
                JSONObject datosJson = new JSONObject(datosJsonString);
                String success = datosJson.getString("success");


                Log.i("LOG_REGISTRO","Datos Json Main Thread:"+datosJsonString);


                if(success.equals("true"))
                {
                    archivoPermanente.escribeLog("REGISTRO", context);
                    //escribeLog();

                    Toast.makeText(getApplicationContext(), "Usuario registrado correctamente", Toast.LENGTH_LONG).show();
                    Log.i("LOG_REGISTRO","Usuario registrado correctamente");
                    finish();
                }
                else
                {
                    String msg=datosJson.getString("msg");
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }
            }
            catch(Exception e)
            {
                Log.e("LOG_REGISTRO","Error Json Main Thread:"+e.getMessage());
            }

        }
    }
    //----------------------------------------------------------------------------------------------

    //onResume
    //----------------------------------------------------------------------------------------------
    protected void onResume() {
        super.onResume();

        Log.i("LOG_REGISTRO","OnResume");
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

        Log.i("LOG_REGISTRO","OnPause");
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
            Log.e("LOG_REGISTRO","Error Liberar Timer Internet:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //onDestroy
    //----------------------------------------------------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i("LOG_REGISTRO","OnDestroy");

        LiberaTimerInternet();
    }
    //----------------------------------------------------------------------------------------------
}

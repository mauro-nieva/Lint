package com.example.lint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;


public class LinternaActivity extends AppCompatActivity {

    private ImageView imgLinterna;
    private ImageView imgModo;
    private ImageView imgInternet;
    private ImageView imgLog;
    private ImageView imgEnviarValores;
    private Button btnLinterna;
    private TextView txtBateria ;
    private TextView txtX ;
    private TextView txtY ;
    private TextView txtZ ;
    private TextView txtLuminosidad ;
    private TextView txtProximidad ;

    //Variables de Broadcast Receiver para Registro de evento
    public IntentFilter filtro;
    private LinternaActivity.ReceptorOperacion receiver;

    //Variables de Broadcast Receiver para Token Refresh
    private IntentFilter filtroTokenRefresh;
    private BReceiverTokenRefresh receiverTokenRefresh;

    //variables para verificar bateria
    private IntentFilter filtroBateria;
    private BReceiverBateria receiverBateria;

    //variables para el sensor acelerometro
    private SensorManager sManagerAcelerometro;
    private Sensor sensorAcelerometro;
    private AsyncTaskAcelerometro aTaskAcelerometro;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 1000;

    //variables para el sensor de luminosidad
    private SensorManager sManagerLuminosidad;
    private Sensor sensorLuminosidad;
    private float maxValue;
    private AsyncTaskLuminosidad aTaskLuminosidad;
    private boolean isLuminosidad;

    //variables para el sensor de proximidad
    private SensorManager sManagerProximidad;
    private Sensor sensorProximidad;
    private boolean enBolsillo;
    private AsyncTaskProximidad aTaskProximidad;
    private boolean isProximidad;

    //variables de conexion de internet
    private Handler handlerInternet;
    private Timer timerInternet;
    private boolean internetOn;

    //variables para el flash
    Camera camara;
    Camera.Parameters parametrosCamara;
    private boolean isOn;
    private boolean isFlash;

    //variables funcionales
    private boolean isAuto;
    private String token;
    private String tokenRefresh;
    private String ultimaActivity;
    private String modo;

    //variables para renovar el token
    private Handler handlerTokenRefresh;
    private Timer timerTokenRefresh;

    //variable clase SharedPreferences
    public ArchivoPermanente archivoPermanente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linterna);

        imgLinterna=(ImageView) findViewById(R.id.imgLinterna);
        imgModo=(ImageView) findViewById(R.id.imgModo);
        imgInternet=(ImageView) findViewById(R.id.imgInternet);
        imgLog=(ImageView) findViewById(R.id.imgLog);
        imgEnviarValores=(ImageView) findViewById(R.id.imgEnviarValores);
        btnLinterna=(Button) findViewById(R.id.btnLinterna);
        txtBateria=(TextView) findViewById(R.id.txtBateria);
        txtX=(TextView) findViewById(R.id.txtX);
        txtY=(TextView) findViewById(R.id.txtY);
        txtZ=(TextView) findViewById(R.id.txtZ);
        txtLuminosidad=(TextView) findViewById(R.id.txtLuminosidad);
        txtProximidad=(TextView) findViewById(R.id.txtProximidad);

        isOn=false;
        isFlash=false;
        isAuto=false;
        enBolsillo=false;
        isLuminosidad=true;
        isProximidad=true;
        internetOn=true;

        receiver=new ReceptorOperacion();
        receiverBateria=new BReceiverBateria();
        receiverTokenRefresh=new BReceiverTokenRefresh();

        btnLinterna.setOnClickListener(OnClickLinterna);
        imgModo.setOnClickListener(OnClickModo);
        imgLog.setOnClickListener(OnClickLog);
        imgEnviarValores.setOnClickListener(OnClickValores);

        //Obtiene variables del archivo SharedPreferences
        ObtieneVariablesArchivo();

        ChequeaCamara();
        ActivaTimerInternet();
        ActivaTimerIokenRefresh();

        InicializaSensorAcelerometro();
        InicializaSensorLuminosidad();
        InicializaSensorProximidad();

        ConfigBReceiverBateria();
        configurarBroadcastreceiver();
        configurarBreceiverToken();

        getSupportActionBar().hide();

        //mantener pantalla encendida
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(ultimaActivity=="Login") {
            //Se registra el evento Login
            RegistraEvento("LOGIN", "Login a traves de api.");
            //Se establece el modo manual por defecto en el archivo Shared Preferences
            archivoPermanente.escribeModo("manual",this);
            modo="manual";
        }
        else
        {
            //Si la ultima activity no fue Login chequea el modo que tenia
            if(modo=="auto") {
                imgModo.setImageResource(R.drawable.auto);
                ejecutarAuto();
                isAuto=true;
            }
        }

        Log.i("LOG_LINTERNA","OnCreate");

    }

    //Obtiene variables del archivo Shared Preferences
    //----------------------------------------------------------------------------------------------
    public void ObtieneVariablesArchivo()
    {
        archivoPermanente=new ArchivoPermanente();
        archivoPermanente.ObtieneToken(this);
        archivoPermanente.ObtieneUltimaActivity(this);
        archivoPermanente.ObtieneModo(this);
        token=archivoPermanente.token;
        tokenRefresh=archivoPermanente.tokenRefresh;
        ultimaActivity=archivoPermanente.ultimaActivity;
        modo=archivoPermanente.modo;
    }

    //----------------------------------------------------------------------------------------------


    //Configuracion de BroadcastReceiver de Registro
    //----------------------------------------------------------------------------------------------
    private void configurarBroadcastreceiver(){

        filtro=new IntentFilter("com.example.intentservice.intent.action.RESPUESTA_EVENT");

        filtro.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(receiver,filtro);

    }
    //----------------------------------------------------------------------------------------------

    //Configuracion de BroadcastReceiver de Bateria
    //----------------------------------------------------------------------------------------------
    private void ConfigBReceiverBateria(){

        filtroBateria = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(receiverBateria,filtroBateria);

    }
    //----------------------------------------------------------------------------------------------

    //Configuracion de BroadcastReceiver de TokenRefresh
    //----------------------------------------------------------------------------------------------
    private void configurarBreceiverToken(){

        filtroTokenRefresh=new IntentFilter("com.example.intentservice.intent.action.RESPUESTA_TOKEN");

        filtroTokenRefresh.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(receiverTokenRefresh,filtroTokenRefresh);

    }
    //----------------------------------------------------------------------------------------------

    //BroadcastReceiver de Bateria
    //----------------------------------------------------------------------------------------------
    public class BReceiverBateria extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent){
            try{
                context.unregisterReceiver(this);
                int currentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
                int level = -1;
                if (currentLevel >=0 && scale > 0){
                    level= (currentLevel * 100)/scale;
                }
                txtBateria.setText(level+"%");
            }
            catch(Exception e)
            {
                Log.e("LOG_LINTERNA","Error Broadcast Receiver de Bateria:"+e.getMessage());
            }

        }
    }
    //----------------------------------------------------------------------------------------------


    //Reistra el evento a traves de un post
    //----------------------------------------------------------------------------------------------
    public void RegistraEvento(String evento,String descripcion)
    {
        try
        {
            archivoPermanente.escribeLog(evento,this);

            if(internetOn)
            {
                JSONObject obj = new JSONObject();

                obj.put("env", "PROD");
                obj.put("type_events", evento);
                obj.put("description", descripcion);

                Intent i = new Intent(LinternaActivity.this, PostService.class);

                i.putExtra("url", "http://so-unlam.net.ar/api/api/event");
                i.putExtra("datosJson", obj.toString());
                i.putExtra("token",token);
                i.putExtra("operacion", "POST");

                startService(i);

                Log.i("LOG_LINTERNA","Se ejecuta POST REGISTRO EVENTO.");
            }
            else
                {
                    archivoPermanente.escribeLog("Sin conexion a internet.",this);
                    Log.e("LOG_LINTERNA","POST REGISTRO EVENTO Sin conexion a internet");
                }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
            Log.e("LOG_LINTERNA","Error Registro Evento: "+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //Actualiza el token a traves de un put
    //----------------------------------------------------------------------------------------------
    public void ActualizaToken()
    {
        try
        {
            if(internetOn)
            {
                JSONObject obj = new JSONObject();

                Intent i = new Intent(LinternaActivity.this, PostService.class);

                i.putExtra("url", "http://so-unlam.net.ar/api/api/refresh");
                i.putExtra("datosJson", obj.toString());
                i.putExtra("token",tokenRefresh);
                i.putExtra("operacion", "PUT");

                startService(i);

                Log.i("LOG_LINTERNA","Se ejecuta PUT TOKEN REFRESH.");
            }
            else
            {
                //archivoPermanente.escribeLog("Sin conexion a internet.",this);
                Log.e("LOG_LINTERNA","PUT TOKEN REFRESH Sin conexion a internet");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.e("LOG_LINTERNA","Error Put Token Refresh: "+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //Receptor de BroadcastReceiver
    //----------------------------------------------------------------------------------------------
    public class ReceptorOperacion extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent){
            try{
                String datosJsonString= intent.getStringExtra("datosJson");
                JSONObject datosJson = new JSONObject(datosJsonString);
                String success = datosJson.getString("success");


                Log.i("LOG_LINTERNA","Datos Json Login Thread:"+datosJsonString);


                if(success.equals("true"))
                {
                    Log.i("LOG_LINTERNA","Se Registro Evento.");
                }
                else
                {
                    String msg=datosJson.getString("msg");
                    Log.e("LOG_LINTERNA","Error en Registro Evento. "+msg);
                }

                archivoPermanente.escribeLog(datosJsonString,context);
            }
            catch(Exception e)
            {
                Log.e("LOG_LINTERNA","Error Json Login Thread:"+e.getMessage());
            }

        }
    }
    //----------------------------------------------------------------------------------------------

    //Broadcast Receiver para Token Refresh
    //----------------------------------------------------------------------------------------------
    public class BReceiverTokenRefresh extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent){
            try{
                String datosJsonString= intent.getStringExtra("datosJson");
                JSONObject datosJson = new JSONObject(datosJsonString);
                String success = datosJson.getString("success");

                Log.i("LOG_LINTERNA","Datos Json Actualizar Token Thread:"+datosJsonString);


                if(success.equals("true"))
                {
                    Log.i("LOG_LINTERNA","Se Actualizo el Token.");
                    Log.i("LOG_LINTERNA","Token:"+datosJson.get("token"));
                    Log.i("LOG_LINTERNA","Token Refresh:"+datosJson.get("token_refresh"));

                    token=datosJson.getString("token");
                    tokenRefresh=datosJson.getString("token_refresh");

                    //Se guardan los nuevos token en el shared preferences
                    archivoPermanente.escribeToken(context);

                    Toast.makeText(getApplicationContext(), "Token Actualizado", Toast.LENGTH_LONG).show();

                }
                else
                {
                    String msg=datosJson.getString("msg");
                    Log.e("LOG_LINTERNA","Error en Actualizar Token. "+msg);
                }

                RegistraEvento("TOKEN REFRESH","Actualizacion de token ");
            }
            catch(Exception e)
            {
                Log.e("LOG_LINTERNA","Error Json Actualizar Token Thread:"+e.getMessage());
            }

        }
    }
    //----------------------------------------------------------------------------------------------

    //Chequea si el dispositivo posee camara y la abre
    //----------------------------------------------------------------------------------------------
    public void ChequeaCamara()
    {
        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            camara=Camera.open();
            parametrosCamara=camara.getParameters();
            isFlash=true;
        }
        else {
            Log.e("LOG_LINTERNA","El dispositivo no cuenta con Flash.");
        }
    }
    //----------------------------------------------------------------------------------------------

    //Activa el Timer de Conexion a Internet
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
            Log.e("LOG_LINTERNA","Error Activar Timer Internet:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //Activa el Timer de Token Refresh
    //----------------------------------------------------------------------------------------------
    public void ActivaTimerIokenRefresh()
    {
        try
        {
            //Se activa el timer que renueva el token
            handlerTokenRefresh = new Handler();
            timerTokenRefresh = new Timer();
            //Cada 25 minutos
            timerTokenRefresh.schedule(timerTaskTokenRefresh, 0, 1500000);
        }
        catch(Exception e)
        {
            Log.e("LOG_LINTERNA","Error Activar Timer Token Refresh:"+e.getMessage());
        }
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
            Log.e("LOG_LINTERNA","Error Liberar Timer Internet:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //Libera el Timer que renueva el token
    //----------------------------------------------------------------------------------------------
    public void LiberaTimerToken()
    {
        try
        {
            //Se cancela el timer y timertask que renueva el token
            timerTaskTokenRefresh.cancel();
            timerTokenRefresh.cancel();
        }
        catch(Exception e)
        {
            Log.e("LOG_LINTERNA","Error Liberar Timer Token:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //Inicializacion de Sensor Acelerometro
    //----------------------------------------------------------------------------------------------
    public void InicializaSensorAcelerometro()
    {
        try
        {
            sManagerAcelerometro = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            sensorAcelerometro = sManagerAcelerometro.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            aTaskAcelerometro=new AsyncTaskAcelerometro();
            aTaskAcelerometro.execute(sManagerAcelerometro, sensorAcelerometro);

        }
        catch(Exception e)
        {
            Log.e("LOG_LINTERNA","Error Inicializar Acelerometro:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //Inicializacion de Sensor Luminosidad
    //----------------------------------------------------------------------------------------------
    public void InicializaSensorLuminosidad()
    {
        try
        {
            sManagerLuminosidad = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensorLuminosidad = sManagerLuminosidad.getDefaultSensor(Sensor.TYPE_LIGHT);

            if (sensorLuminosidad == null) {
                Toast.makeText(this, "El dispositivo no tiene sensor de luminosidad!", Toast.LENGTH_SHORT).show();
                Log.e("LOG_LINTERNA","El dispositivo no tiene sensor de luminosidad!");
                isLuminosidad=false;
            }
            else
            {
                // maximo valor del sensor de luminosidad
                maxValue = sensorLuminosidad.getMaximumRange();
            }
        }
        catch(Exception e)
        {
            Log.e("LOG_LINTERNA","Error Inicializar Sensor de Luminosidad:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //Inicializacion de Sensor Acelerometro
    //----------------------------------------------------------------------------------------------
    public void InicializaSensorProximidad()
    {
        try
        {
            sManagerProximidad = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensorProximidad = sManagerProximidad.getDefaultSensor(Sensor.TYPE_PROXIMITY);

            if (sensorProximidad == null) {
                Toast.makeText(this, "El dispositivo no tiene sensor de proximidad!", Toast.LENGTH_SHORT).show();
                Log.e("LOG_LINTERNA","El dispositivo no tiene sensor de proximidad!");
                isProximidad=false;
            }

        }
        catch(Exception e)
        {
            Log.e("LOG_LINTERNA","Error Inicializar Sensor de Proximidad:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //OnClickListener del boton de Modo
    //----------------------------------------------------------------------------------------------
    View.OnClickListener OnClickModo=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(isAuto)
            {
                imgModo.setImageResource(R.drawable.manual);
                cancelarAuto();
                isAuto=false;
                archivoPermanente.escribeModo("manual",LinternaActivity.this);

                RegistraEvento("MODO MANUAL","Se cambio aplicacion a modo manual.");
                Toast.makeText(getApplicationContext(), "Modo Manual", Toast.LENGTH_LONG).show();
            }
            else
            {
                imgModo.setImageResource(R.drawable.auto);
                ejecutarAuto();
                isAuto=true;
                archivoPermanente.escribeModo("auto",LinternaActivity.this);

                RegistraEvento("MODO AUTO","Se cambio aplicacion a modo automatico.");
                Toast.makeText(getApplicationContext(), "Modo Auto", Toast.LENGTH_LONG).show();
            }

        }
    };
    //----------------------------------------------------------------------------------------------

    //OnClickListener del boton de enviar valores
    //----------------------------------------------------------------------------------------------
    View.OnClickListener OnClickValores=new View.OnClickListener() {
        @Override
        public void onClick(View v) {

                String modo = "Manual";

                if (isAuto)
                    modo = "Auto";

                String msg = "Modo: " + modo +
                        " " + txtX.getText() +
                        " " + txtY.getText() +
                        " " + txtZ.getText() +
                        " " + txtLuminosidad.getText() +
                        " " + txtProximidad.getText() +
                        " Bateria: " + txtBateria.getText();

                RegistraEvento("VALORES", msg);

                Toast.makeText(getApplicationContext(), "Valores Enviados", Toast.LENGTH_LONG).show();

        }
    };
    //----------------------------------------------------------------------------------------------

    //OnClickListener del boton de Log
    //----------------------------------------------------------------------------------------------
    View.OnClickListener OnClickLog=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent=new Intent(LinternaActivity.this,LogActivity.class);
            startActivity(intent);
            finish();
        }
    };
    //----------------------------------------------------------------------------------------------

    //Enciende el flash y actualiza la imagen de fondo
    //----------------------------------------------------------------------------------------------
    public void enciendeFlash()
    {
        if(isFlash) {
            imgLinterna.setImageResource(R.drawable.linterna_on);
            parametrosCamara.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camara.setParameters(parametrosCamara);
            camara.startPreview();
            isOn = true;
        }
    }
    //----------------------------------------------------------------------------------------------

    //Apaga el flash y actualiza la imagen de fondo
    //----------------------------------------------------------------------------------------------
    public void apagaFlash()
    {
        if(isFlash) {
            imgLinterna.setImageResource(R.drawable.linterna_off);
            parametrosCamara.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camara.setParameters(parametrosCamara);
            camara.stopPreview();
            isOn = false;
        }
    }
    //----------------------------------------------------------------------------------------------

    //OnClickListener del boton de Linterna
    //----------------------------------------------------------------------------------------------
    View.OnClickListener OnClickLinterna=new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if(isAuto==false)
            {
                if (isOn)
                {
                    apagaFlash();
                }
                else
                    {
                    enciendeFlash();
                }
            }
        }
    };
    //----------------------------------------------------------------------------------------------

    //Set de la variable enBolsillo
    //----------------------------------------------------------------------------------------------
    public void setBolsillo (Boolean b) {
        enBolsillo=b;
    }
    //----------------------------------------------------------------------------------------------

    //Get de la variable enBolsillo
    //----------------------------------------------------------------------------------------------
    public Boolean getBolsillo () {
        return enBolsillo;
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

    //TimerTask de Token Refresh
    //----------------------------------------------------------------------------------------------
    public TimerTask timerTaskTokenRefresh = new TimerTask() {
        @Override
        public void run() {
            handlerInternet.post(new Runnable() {
                public void run() {
                    try {
                        ActualizaToken();
                    } catch (Exception e) {
                        Log.e("LOG_LINTERNA","Error Token Refresh:"+e.getMessage());
                    }
                }
            });
        }
    };
    //----------------------------------------------------------------------------------------------

    //AsyncTask Acelerometro
    //----------------------------------------------------------------------------------------------
    public class AsyncTaskAcelerometro extends AsyncTask {

        SensorManager sm;
        Sensor s;

        @Override
        protected Object doInBackground(Object[] objects) {
            sm=(SensorManager) objects[0];
            s=(Sensor) objects[1];
            sm.registerListener(SensorEventListenerAcel, s, sm.SENSOR_DELAY_NORMAL);
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);

            float x=(float)values[0];
            float y=(float)values[1];
            float z=(float)values[2];
            boolean shake=(boolean)values[3];

            txtX.setText("X: "+x);
            txtY.setText("Y: "+y);
            txtZ.setText("Z: "+z);

            if(shake)
            {
                if(isAuto==false) {
                    if (isOn) {
                        apagaFlash();
                    } else {
                        enciendeFlash();
                    }
                }
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            sm.unregisterListener(SensorEventListenerAcel);
        }

        //SensorEventListener del Acelerometro
        //------------------------------------------------------------------------------------------
        public SensorEventListener SensorEventListenerAcel=new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                synchronized (this) {
                    if (sensorAcelerometro.getType() == Sensor.TYPE_ACCELEROMETER) {
                        float x = sensorEvent.values[0];
                        float y = sensorEvent.values[1];
                        float z = sensorEvent.values[2];
                        boolean shake;

                        long curTime = System.currentTimeMillis();

                        if ((curTime - lastUpdate) > 100) {

                            long diffTime = (curTime - lastUpdate);
                            lastUpdate = curTime;

                            float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                            if (speed > SHAKE_THRESHOLD)
                                shake = true;
                            else
                                shake = false;

                            publishProgress(x, y, z, shake);

                            last_x = x;
                            last_y = y;
                            last_z = z;
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        //------------------------------------------------------------------------------------------
    }
    //----------------------------------------------------------------------------------------------

    //AsyncTask Luminosidad
    //----------------------------------------------------------------------------------------------
    public class AsyncTaskLuminosidad extends AsyncTask {

        SensorManager sm;
        Sensor s;

        @Override
        protected Object doInBackground(Object[] objects) {
            sm=(SensorManager) objects[0];
            s=(Sensor) objects[1];
            sm.registerListener(lightEventListener, s, SensorManager.SENSOR_DELAY_FASTEST);
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);

            float luminosidad=(float)values[0];

            txtLuminosidad.setText("L: "+luminosidad+" lx");

            //Si la luminosidad es nula, el flash esta apagado y no esta en el bolsillo->enciende
            if(luminosidad==0.0 && isOn==false && getBolsillo()==false)
            {
                enciendeFlash();
            }

            //Si la luminosidad no es nula y el flash esta encendido->apaga
            if(luminosidad!=0.0 && isOn==true)
            {
                apagaFlash();
            }
        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
            sm.unregisterListener(lightEventListener);
        }

        //SensorEventListener del Sensor de Luminosidad
        //------------------------------------------------------------------------------------------
        public SensorEventListener lightEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float value = sensorEvent.values[0];

                publishProgress(value);

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        //------------------------------------------------------------------------------------------
    }
    //----------------------------------------------------------------------------------------------

    //AsyncTask Proximidad
    //----------------------------------------------------------------------------------------------
    public class AsyncTaskProximidad extends AsyncTask {

        SensorManager sm;
        Sensor s;

        @Override
        protected Object doInBackground(Object[] objects) {
            sm=(SensorManager) objects[0];
            s=(Sensor) objects[1];
            sm.registerListener(proximidadEventListener, s, SensorManager.SENSOR_DELAY_FASTEST);
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);

            float proximidad=(float)values[0];

            txtProximidad.setText("P: "+proximidad);

            if(proximidad < s.getMaximumRange()) {
                // Detected something nearby
                setBolsillo(true);
            } else {
                // Nothing is nearby
                setBolsillo(false);
            }

            //el flash esta encendido y esta en el bolsillo->apaga
            if(isOn==true && getBolsillo()==true)
            {
                apagaFlash();
            }

            if(getBolsillo()==false && txtLuminosidad.getText().subSequence(3,6).equals("0.0"))
            {
                enciendeFlash();
            }

        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
            sm.unregisterListener(proximidadEventListener);
        }

        //SensorEventListener del Sensor de Proximidad
        //------------------------------------------------------------------------------------------
        public SensorEventListener proximidadEventListener = new SensorEventListener()
        {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float value = sensorEvent.values[0];

                publishProgress(value);

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        //------------------------------------------------------------------------------------------
    }
    //----------------------------------------------------------------------------------------------

    //Ejecuta los AsyncTask del Modo Automatico
    //----------------------------------------------------------------------------------------------
    protected void ejecutarAuto()
    {
        aTaskProximidad=new AsyncTaskProximidad();
        aTaskLuminosidad=new AsyncTaskLuminosidad();
        aTaskLuminosidad.execute(sManagerLuminosidad, sensorLuminosidad);
        aTaskProximidad.execute(sManagerProximidad, sensorProximidad);
    }
    //----------------------------------------------------------------------------------------------

    //onResume
    //----------------------------------------------------------------------------------------------
    protected void onResume() {
        super.onResume();

        Log.i("LOG_LINTERNA","OnResume()");
    }
    //----------------------------------------------------------------------------------------------

    //onStart
    //----------------------------------------------------------------------------------------------
    protected void onStart() {
        super.onStart();

        aTaskAcelerometro=new AsyncTaskAcelerometro();
        aTaskAcelerometro.execute(sManagerAcelerometro, sensorAcelerometro);

        if(isAuto && isLuminosidad && isProximidad)
            ejecutarAuto();

        Log.i("LOG_LINTERNA","OnStart()");
    }
    //----------------------------------------------------------------------------------------------

    //Cancela los AsyncTask del Modo Automatico
    //----------------------------------------------------------------------------------------------
    protected  void cancelarAuto()
    {
        aTaskLuminosidad.cancel(true);
        aTaskProximidad.cancel(true);

        txtLuminosidad.setText("L: 0 lx");
        txtProximidad.setText("P: 0");
    }
    //----------------------------------------------------------------------------------------------

    //onPause
    //----------------------------------------------------------------------------------------------
    protected void onPause()
    {
        super.onPause();

        Log.i("LOG_LINTERNA","OnPause()");
    }
    //----------------------------------------------------------------------------------------------

    //onStop
    //----------------------------------------------------------------------------------------------
    protected void onStop()
    {
        super.onStop();
        aTaskAcelerometro.cancel(true);

        if(isAuto && isProximidad && isLuminosidad) {
            cancelarAuto();
            apagaFlash();
        }

        if(isOn)
            apagaFlash();

        Log.i("LOG_LINTERNA","OnStop()");
    }
    //----------------------------------------------------------------------------------------------

    //Libera la Camara
    //----------------------------------------------------------------------------------------------
    public void LiberaCamara()
    {
        try
        {
            camara.release();
            camara=null;
        }
        catch(Exception e)
        {
            Log.e("LOG_LINTERNA","Error Liberar Camara:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //onDestroy
    //----------------------------------------------------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i("LOG_LINTERNA","OnDestroy()");

        LiberaCamara();
        LiberaTimerInternet();
        LiberaTimerToken();
        aTaskAcelerometro.cancel(true);
    }
    //----------------------------------------------------------------------------------------------


}

package com.example.lint;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ArchivoPermanente {
    String token;
    String tokenRefresh;
    String Log;
    String nombre;
    String ultimaActivity;
    String modo;

    public ArchivoPermanente()
    {
        token="";
        tokenRefresh="";
        Log="";
        ultimaActivity="";
        modo="";
        nombre="Historial";
    }

    //Escribe el token y el token refresh en el archivo Shared Preferences
    //----------------------------------------------------------------------------------------------
    public void escribeToken(Context context)
    {
        try {

            SharedPreferences preferences = context.getSharedPreferences("Historial", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            editor.putString("token", token);
            editor.putString("token_refresh", tokenRefresh);

            editor.commit();

            android.util.Log.i("LOG_ARCHIVO","Token Shared Preferences: "+token);
            android.util.Log.i("LOG_ARCHIVO","Token Refresh Shared Preferences: "+tokenRefresh);
        }
        catch(Exception e)
        {
            android.util.Log.e("LOG_ARCHIVO","Error Token Shared Preferences:"+e.getMessage());
        }

    }
    //----------------------------------------------------------------------------------------------

    //Escribe la ultima activity en el archivo Shared Preferences
    //----------------------------------------------------------------------------------------------
    public void escribeUltActivity(Context context)
    {
        try {

            SharedPreferences preferences = context.getSharedPreferences("Historial", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            editor.putString("activity", ultimaActivity);

            editor.commit();

            android.util.Log.i("LOG_ARCHIVO","Ultima Activity Shared Preferences: "+ultimaActivity);
        }
        catch(Exception e)
        {
            android.util.Log.e("LOG_ARCHIVO","Error Ultima Activity Shared Preferences:"+e.getMessage());
        }

    }
    //----------------------------------------------------------------------------------------------

    //Escribe modo en el archivo Shared Preferences
    //----------------------------------------------------------------------------------------------
    public void escribeModo(String modo_activo, LinternaActivity context)
    {
        try {

            SharedPreferences preferences = context.getSharedPreferences("Historial", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("modo", modo_activo);

            editor.commit();

            android.util.Log.i("LOG_ARCHIVO","Modo Shared Preferences: "+modo_activo);
        }
        catch(Exception e)
        {
            android.util.Log.e("LOG_ARCHIVO","Error Modo Shared Preferences:"+e.getMessage());
        }

    }
    //----------------------------------------------------------------------------------------------

    //Escribe el log en el archivo Shared Preferences
    //----------------------------------------------------------------------------------------------
    public void escribeLog(String linea, Context context)
    {
        try {
            SharedPreferences preferences = context.getSharedPreferences("Historial", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = preferences.edit();

            String contenido = preferences.getString("log", "");

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = df.format(c.getTime());

            String agregado = formattedDate + " " + linea;

            editor.putString("log", contenido + agregado + "\n");

            editor.commit();

            android.util.Log.i("LOG_ARCHIVO","Log Shared Preferences: "+agregado);
        }
        catch(Exception e)
        {
            android.util.Log.e("LOG_ARCHIVO","Error Log Shared Preferences:"+e.getMessage());
        }

    }
    //----------------------------------------------------------------------------------------------

    //Obtiene el token y token refresh del archivo Shared Preferences
    //----------------------------------------------------------------------------------------------
    public void ObtieneToken(Context context)
    {
        try
        {
            SharedPreferences preferences=context.getSharedPreferences("Historial", Context.MODE_PRIVATE);
            token=preferences.getString("token","");
            tokenRefresh=preferences.getString("token_refresh","");

            android.util.Log.i("LOG_ARCHIVO","Token Shared Preferences:"+token);
            android.util.Log.i("LOG_ARCHIVO","Token Refresh Shared Preferences:"+tokenRefresh);
        }
        catch(Exception e)
        {
            android.util.Log.e("LOG_ARCHIVO","Error Token Shared Preferences:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //Obtiene modo del archivo Shared Preferences
    //----------------------------------------------------------------------------------------------
    public void ObtieneModo(Context context)
    {
        try
        {
            SharedPreferences preferences=context.getSharedPreferences("Historial", Context.MODE_PRIVATE);
            modo=preferences.getString("modo","");

            android.util.Log.i("LOG_ARCHIVO","Modo Shared Preferences:"+modo);
        }
        catch(Exception e)
        {
            android.util.Log.e("LOG_ARCHIVO","Error Modo Shared Preferences:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

    //Obtiene ultima activity del archivo Shared Preferences
    //----------------------------------------------------------------------------------------------
    public void ObtieneUltimaActivity(Context context)
    {
        try
        {
            SharedPreferences preferences=context.getSharedPreferences("Historial", Context.MODE_PRIVATE);
            ultimaActivity=preferences.getString("activity","");

            android.util.Log.i("LOG_ARCHIVO","Ultima Activity Shared Preferences:"+ultimaActivity);
        }
        catch(Exception e)
        {
            android.util.Log.e("LOG_ARCHIVO","Error Ultima Activity Shared Preferences:"+e.getMessage());
        }
    }
    //----------------------------------------------------------------------------------------------

}

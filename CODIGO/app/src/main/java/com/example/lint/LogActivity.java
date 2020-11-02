package com.example.lint;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class LogActivity extends AppCompatActivity {

    private TextView txtHistorial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        txtHistorial=(TextView) findViewById(R.id.txtHistorial);

        SharedPreferences preferences=getSharedPreferences("Historial", Context.MODE_PRIVATE);
        String contenido=preferences.getString("log","");
        txtHistorial.setText(contenido);

        EscribeUltimaActivity();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LogActivity.this, LinternaActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    //Escribe la ultima Activity en el archivo Shared Preferences
    //----------------------------------------------------------------------------------------------
    private void EscribeUltimaActivity()
    {
        try {
            SharedPreferences preferences = getSharedPreferences("Historial", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = preferences.edit();

            editor.putString("activity", "Log");

            editor.commit();

            Log.i("LOG_LOG","Activity Shared Preferences: Log");
        }
        catch(Exception e)
        {
            Log.e("LOG_LOG","Error Activity Shared Preferences:"+e.getMessage());
        }

    }
    //----------------------------------------------------------------------------------------------
}

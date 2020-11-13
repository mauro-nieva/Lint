package com.example.lint;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class LogActivity extends AppCompatActivity {

    private TextView txtHistorial;

    //variable clase SharedPreferences
    public ArchivoPermanente archivoPermanente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        txtHistorial=(TextView) findViewById(R.id.txtHistorial);
        archivoPermanente=new ArchivoPermanente();

        SharedPreferences preferences=getSharedPreferences("Historial", Context.MODE_PRIVATE);
        String contenido=preferences.getString("log","");
        txtHistorial.setText(contenido);

        //se guarda la ultima activity
        archivoPermanente.ultimaActivity="Log";
        archivoPermanente.escribeUltActivity(this);

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

}

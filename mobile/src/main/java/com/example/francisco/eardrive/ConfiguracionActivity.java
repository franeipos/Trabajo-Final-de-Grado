package com.example.francisco.eardrive;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.ToggleButton;

public class ConfiguracionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Configuracion EarDrive");

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //REcuperamos las preferencias del usuario
        SharedPreferences prefs = getSharedPreferences("Preferencias", Context.MODE_PRIVATE);
        Boolean automatico = prefs.getBoolean("quieto", false);
        Boolean vibrar = prefs.getBoolean("vibrar",false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

            /*Detección Actividades*/
            Switch quieto = null;
            quieto = (Switch) this.findViewById(R.id.quieto);
            quieto.setChecked(automatico);
            quieto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences prefs = getSharedPreferences("Preferencias", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("quieto", isChecked);
                    editor.commit();
                    Log.i("Info", "Se han guardado QUIETO: " + isChecked);
                }
            });

            /*Activar Vibracion*/
            Switch vibracion = null;
            vibracion = (Switch) findViewById(R.id.vibrar);
            vibracion.setChecked(vibrar);
            vibracion.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences prefs = getSharedPreferences("Preferencias", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("vibrar", isChecked);
                    editor.commit();
                    Log.i("Info", "Se han guardado VIBRAR : " + isChecked);
                }
            });
        }
        else{
            /*Deteccion Actividades*/
            ToggleButton quieto = (ToggleButton) this.findViewById(R.id.quieto);
            quieto.setChecked(automatico);

            quieto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences prefs = getSharedPreferences("Preferencias", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("quieto", isChecked);
                    editor.commit();
                    Log.i("Info", "Se han guardado en las prefs : " + isChecked);
                }
            });

            /*Activar vibracion*/
            ToggleButton vibracion = null;
            vibracion = (ToggleButton) findViewById(R.id.vibrar);
            vibracion.setChecked(vibrar);
            vibracion.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences prefs = getSharedPreferences("Preferencias", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("vibrar", isChecked);
                    editor.commit();
                    Log.i("Info", "Se han guardado VIBRAR : " + isChecked);
                }
            });

        }


    }



}

package com.example.francisco.eardrive;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

public class InicioActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private TextView mTextView;
    private GoogleApiClient mApiClient;
    private String tipoAlerta = "";
    private final String ALERTA = "/alerta";
    private ImageView alertaIco = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                alertaIco = (ImageView) stub.findViewById(R.id.alertaIco);
                seleccionarAlerta();
            }
        });

        vibrar();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            tipoAlerta = extras.getString("alertaTipo");
        }
        Log.i("WEAR", "TIPO ALERTA : " + tipoAlerta);

        Log.i("WEAR", "Created");
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();

        mApiClient.connect();

        Log.i("WEAR", "Final initGoogleClient");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void vibrar() {
        // Get instance of Vibrator from current Context
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        int dot = 200;      // Length of a Morse Code "dot" in milliseconds
        int dash = 500;     // Length of a Morse Code "dash" in milliseconds
        int short_gap = 200;    // Length of Gap Between dots/dashes
        int medium_gap = 500;   // Length of Gap Between Letters
        int long_gap = 1000;    // Length of Gap Between Words
        long[] pattern = {
                0,  // Start immediately
                dot, short_gap, dot, short_gap, dot,    // s
                medium_gap,
                dash, short_gap, dash, short_gap, dash, // o
                medium_gap,
                dot, short_gap, dot, short_gap, dot,    // s
                long_gap
        };

// Only perform this pattern one time (-1 means "do not repeat")
        v.vibrate(pattern, -1);

    }

    private void seleccionarAlerta() {
        switch (tipoAlerta) {
            case "1":
                alertaIco.setImageResource(R.drawable.earphone);
                break;
            case "2":
                alertaIco.setImageResource(R.drawable.siren);
                break;
            default:
                 alertaIco.setImageResource(R.drawable.earphone);
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}

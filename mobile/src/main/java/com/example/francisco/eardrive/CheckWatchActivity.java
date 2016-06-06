package com.example.francisco.eardrive;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class CheckWatchActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks{
    private GoogleApiClient mApiClient;
    private ImageView paired = null;
    private TextView tPaired = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_watch);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        paired = (ImageView) findViewById(R.id.paired);
        tPaired =(TextView) findViewById(R.id.pairedText);

        initGoogleApiClient();
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks( this )
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i("CHECK WATCH", "connected");
        new Thread( new Runnable() {
            @Override
            public void run() {
                final List<Node> connectedNodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await().getNodes();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!connectedNodes.isEmpty()){
                            paired.setImageResource(R.drawable.paired);
                            tPaired.setText("CONECTADO");
                        }
                        else{
                            paired.setImageResource(R.drawable.dispaired);
                            tPaired.setText("NO CONECTADO");
                        }
                    }
                });
            }

        }).start();
    }
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        if ( mApiClient != null ) {
            if ( mApiClient.isConnected() ) {
                mApiClient.disconnect();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if( mApiClient != null )
            mApiClient.unregisterConnectionCallbacks( this );
        super.onDestroy();
    }
}

package com.example.francisco.eardrive;

import android.Manifest;
//import android.app.Fragment;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;


public class MensajeActivity extends AppCompatActivity  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    private static final int MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 1;
    private static final int POLL_INTERVAL = 24 ; // variable para controlar cada cuanto analizamos el volumen del sonido.
    private static final String RUTA = "/start_activity"; //ruta que se manda al wearable

    private static String mFileName = null;

    private MediaRecorder mRecorder = null;

    private SoundMeter mSensor = null;


    private MediaPlayer mPlayer = null;

    private boolean mIsLargeLayout = false;
    private DialogFragment newFragment = null;

    private ImageView microfono = null;
    private boolean recording = true;
    private boolean mostrandoAlerta = false;
    /*Metodos para llamar el JNI y ejecutar el codigo en C*/
    //Se carga la libreria que hemos definido en el gradle.build
//    static {
//        System.loadLibrary("hello-android-jni");
//    }
//
//    //Metodo al que llamamos de nuestra libreria en C
//    public native String getResultJNI(int param);

    //Atributos para ejecutar el ActivityRecognition
    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private TextView mDetectedActivityTextView;
    private ActivityDetectionBroadcastReceiver mBroadcastReceiver;

    //Variable para acceder a las preferencias del usuario
    private SharedPreferences prefs = null;
    private Boolean automatico = false;
    private Boolean vibracion = false;

    private Boolean grabandoAmbiente =  false;
    private int contAmbiente = 0;
    private double ambiente = 0;
    private Handler mHandler = new Handler();
    // Creamos un nuevo hilo para controlar en segundo plano el volumen de la voz
    private Runnable mPollTask = new Runnable() {
        public void run() {

            double amp = mSensor.getAmplitud();
            int tipoAlerta = mSensor.comprobarAlerta();
            if (grabandoAmbiente) {
                Log.i("Noise", "Amplitud : " + amp);
                ambiente += amp;
                contAmbiente++;
                if (contAmbiente >= 15) {
                    grabandoAmbiente = false;
                    ambiente = ambiente / 15;
                    contAmbiente = 0;
                    cambiarIco();
                    Log.i("Noise", "AMBIENTE GRABADO : " +ambiente);
                }
            } else if(!mostrandoAlerta) {
                if ((amp - ambiente) > 120) {

                    Log.i("Noise", "SUPERADO EL UMBRAL : " + "amplitud : " + amp + "  Ambiente : " + ambiente);
                    darAlerta(2);

                }
                else if (tipoAlerta != -1){
                    darAlerta(tipoAlerta);
                }
            }

            // Runnable(mPollTask) will again execute after POLL_INTERVAL
            mHandler.postDelayed(mPollTask, POLL_INTERVAL);
        }
    };

    private void darAlerta(int tipo){
        sendMessage(RUTA, Integer.toString(tipo));
        vibrar();
        showDialog(Integer.toString(tipo));
        mostrandoAlerta = true;
    }


    private void startRecording() {
        mSensor.start();
        recording = true;
        startAnimation();

        mHandler.postDelayed(mPollTask, POLL_INTERVAL);
    }

    private void stopRecording() {
        mSensor.stop();
        mHandler.removeCallbacks(mPollTask);
        recording = false;
        stopAnimation();
    }


    public void startAnimation() {
        ImageView image = (ImageView) findViewById(R.id.fondoAnimado);
        TextView text = (TextView) findViewById(R.id.detectando);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.animation);
        image.startAnimation(animation);
        text.setVisibility(View.VISIBLE);

        cambiarIco();
    }

    public void cambiarIco() {
        TextView text = (TextView) findViewById(R.id.detectando);
        if(grabandoAmbiente == true){
            text.setText("Grabando ambiente");
            microfono.setImageResource(R.drawable.ambiente);
        }
        else{
            text.setText("Detectando ...");
            //Cambiamos la imagen del micro
            microfono.setImageResource(R.drawable.microflat);
        }
    }

    public void stopAnimation(){
        ImageView image = (ImageView) findViewById(R.id.fondoAnimado);
        TextView text = (TextView) findViewById(R.id.detectando);
        image.clearAnimation();
        text.setVisibility(View.INVISIBLE);

        //Cambiamos la imagen del micro
        microfono.setImageResource(R.drawable.mute);

    }

    public void showDialog(String tipoAlerta) {
        //FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("alert_dialog");

        if(prev!=null){
            ft.remove(prev);
        }

        //LLamamos al JNI para que nos de un tipo de Alerta
        //String msg = getResultJNI(2);
        newFragment= CustomDialogFragment.newInstance(tipoAlerta);
        newFragment.show(ft, "alert_dialog");

        new CountDownTimer(3000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                dismissDialog();
                mostrandoAlerta = false;
            }
        }.start();

//        if (mIsLargeLayout) {
//            // The device is using a large layout, so show the fragment as a dialog
//            newFragment.show(fragmentManager, "dialog");
//        } else {
//            // The device is smaller, so show the fragment fullscreen
//            FragmentTransaction transaction = fragmentManager.beginTransaction();
//            // For a little polish, specify a transition animation
//            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//            // To make it fullscreen, use the 'content' root view as the container
//            // for the fragment, which is always the root view for the activity
//            transaction.add(android.R.id.content, newFragment)
//                    .addToBackStack(null).commit();
//        }
    }

    public void dismissDialog(){
        newFragment.dismiss();
    }

    private void vibrar() {

        if(vibracion) {
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

    }

    /******************* BORRAR *******************/
    public void Boton (View view){
        sendMessage("/start_activity", "2");
        vibrar();
    }

    //********************************************/

    private void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), path, text.getBytes()).await();

                    if(result.getStatus().isSuccess()){
                        Log.i("WEAR","MENSAJE ENTREGADO CORRECTAMENTE");
                    }
                }

            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mensaje);
        setTitle("EarDrive");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayout layout = (LinearLayout) findViewById(R.id.layoutMesaje);

        mSensor = new SoundMeter();
        recording = true;
        microfono = (ImageView) findViewById(R.id.micro);
        microfono.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = MotionEventCompat.getActionMasked(event);

                switch (action) {
                    case (MotionEvent.ACTION_DOWN):
                        return true;
                    case (MotionEvent.ACTION_UP):
                        if (recording) {
                            stopRecording();
                        } else {
                            startRecording();

                        }
                        return true;
                    default:
                        return true;
                }
            }
        });


        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();



        //Cogemos las preferencias del usuario por si desea utilizar la detección automática.
        prefs = getSharedPreferences("Preferencias", Context.MODE_PRIVATE);
        automatico = prefs.getBoolean("quieto", false);
        vibracion = prefs.getBoolean("vibrar",false);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApiIfAvailable(Wearable.API)
                .build();


    }

    /**
     * Metodo con el que controlamos cuando el usuario deja en un segundo plano nuestra app.
     */
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        if(recording ==true){
            stopRecording();
        }

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }

        //Elimina el registro del BroadcastReciver usado en la detección de actividad.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }


    /**
     * Metodo que se llama cada vez que la activity se pone activa, ya sea la primera vez o cuando se entra desde segundo plano
     */
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE )){

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);


                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        //Registra el broadcastReciver para la deteccion de actividad
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.STRING_ACTION));

        //Comenzamos a grabar el sonido ambiente
        grabandoAmbiente = true;
        startRecording();

    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if(automatico) {
            requestActivityUpdates();
        }
        Log.i(TAG, "Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();

            mGoogleApiClient.connect();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public String getDetectedActivity(int detectedActivityType) {
        Toast.makeText(this, "Detected", Toast.LENGTH_SHORT).show();
        Resources resources = this.getResources();
        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.on_bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.on_foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.walking);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity.UNKNOWN:
                return resources.getString(R.string.unknown);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }

    public void requestActivityUpdates() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "GoogleApiClient not yet connected", Toast.LENGTH_SHORT).show();
        } else {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 3000, getActivityDetectionPendingIntent()).setResultCallback(this);
        }
    }

    public void removeActivityUpdates(View view) {
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, getActivityDetectionPendingIntent()).setResultCallback(this);
    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, ActivitiesIntentService.class);

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void onResult(Status status) {
        if (status.isSuccess()) {
            Log.e(TAG, "Successfully added activity detection.");
            Toast.makeText(this, "Successfully added activity detection.", Toast.LENGTH_SHORT).show();

        } else {
            Log.e(TAG, "Error: " + status.getStatusMessage());
            Toast.makeText(this, "Error: "+status.getStatusMessage() , Toast.LENGTH_SHORT).show();

        }
    }

    //Comprobamos si debemos dejar de grabar cuando el usuario esta quieto
    public void comprobarStop(){

        if(recording == true){
            //Recogemos la configuración del usuario para este campo
            Boolean automatico = prefs.getBoolean("quieto", false);

            //Si el usuario ha establecido que quiere que se pare automaticamente paramos de detectar.
            if(automatico == true) {
                stopRecording();
                Toast.makeText(this, "Se ha detenido la detección aumtomaticamente", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> detectedActivities = intent.getParcelableArrayListExtra(Constants.STRING_EXTRA);
            String activityString = "";
            for(DetectedActivity activity: detectedActivities){
                activityString +=  "Activity: " + getDetectedActivity(activity.getType()) + ", Confidence: " + activity.getConfidence() + "%\n";

                //Si la actividad es 'Quieto' y hay más de un 80% de confianza se comprueba si se debe dejar de grabar
                if(activity.getType() == DetectedActivity.STILL && activity.getConfidence() > 80){
                    comprobarStop();
                    Log.i(TAG,"Stop comprobado");
                }
            }
            Log.i(TAG,activityString);
            //mDetectedActivityTextView.setText(activityString);

        }
    }
    /******************************/
    /*CLASE DEL DIALOGO DE ALERTA*/
    /*****************************/
    public static class CustomDialogFragment extends DialogFragment {
        /** The system calls this to get the DialogFragment's layout, regardless
         of whether it's being displayed as a dialog or an embedded fragment. */

        private String alert_type = null;

        public static CustomDialogFragment newInstance(String title) {
            CustomDialogFragment frag = new CustomDialogFragment();
            Bundle args = new Bundle();
            args.putString("title", title);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout to use as dialog or embedded fragment
            View v = inflater.inflate(R.layout.alert_dialog, container, false);
            dibujarTipoAlerta(alert_type ,v);
            return v;
        }

        /** The system calls this only when creating the layout in a dialog. */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            // The only reason you might override this method when using onCreateView() is
            // to modify any dialog characteristics. For example, the dialog includes a
            // title by default, but your custom layout might not need it. So here you can
            // remove the dialog title, but you must call the superclass to get the Dialog.
            super.onCreate(savedInstanceState);
            alert_type = getArguments().getString("title");
        }

        private void dibujarTipoAlerta(String alert, View v) {
            ImageView img = (ImageView) v.findViewById(R.id.alertimg);
            TextView text = (TextView) v.findViewById(R.id.alert_type);
            switch (alert){
                case "1":
                    img.setImageResource(R.drawable.earphone);
                    text.setText("Peligro detectado!");
                    break;
                case "2":
                    img.setImageResource(R.drawable.siren);
                    text.setText("Sirena detectada!");
                    break;
                default:
                    img.setImageResource(R.drawable.earphone);
                    text.setText("Peligro detectado!");
            }
        }
    }

}

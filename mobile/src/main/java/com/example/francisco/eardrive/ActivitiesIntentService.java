package com.example.francisco.eardrive;

/**
 * Created by Francisco on 23/05/2016.
 */
import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class ActivitiesIntentService extends IntentService {

    private static final String TAG = "ActivitiesIntentService";

    public ActivitiesIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        Intent i = new Intent(Constants.STRING_ACTION);

        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        i.putExtra(Constants.STRING_EXTRA, detectedActivities);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }
}

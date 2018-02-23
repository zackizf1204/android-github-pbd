package com.anjasnfriends.android.smarthome;

import android.app.Service;
import android.util.Log;

/**
 * Created by Hishamlazuardi on 23/02/2018.
 */

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

public class SmartJobService extends JobService {

    private static final String TAG = "SmartJobService";

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Performing long running task in scheduled job");
        // TODO(developer): add long running task here.
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

}
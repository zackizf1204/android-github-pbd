package com.anjasnfriends.android.smarthome;

/**
 * Created by ZackiZulfikarFauzi on 20/02/2018.
 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class LocationService extends Service
{
    private static final String TAG = "GPS SERVICE";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;
    public static final String DISTANCE_MATRIX = "distance_matrix";

    String home;

    private class LocationListener implements android.location.LocationListener{
        TextView NewText;
        Location mLastLocation;


        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }
        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
            sendBroadcastMessage(location, home);
        }
        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }
        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }
    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };
    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        home = intent.getStringExtra("alamat");
        Log.d("ALAMAT SERVICE",home);
        return START_STICKY;
    }
    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }
    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }
    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void sendBroadcastMessage(Location location, String homeadd) {
        if (location != null) {
            Geocoder geocoder = new Geocoder(getApplicationContext());
            List<Address> address = null;
            try {
                address = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            String addressString =address.get(0).getAddressLine(0);
            String alamat =addressString.replaceAll(" ","+");

            try {
                sendGetDistance(alamat,homeadd.replaceAll(" ","+"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void sendGetDistance(final String address, final String homeadd) throws Exception {
        Log.e("alamat", address);
        class LongOperation extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... params) {
                String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins="+address+"&destinations="+homeadd+"&key=AIzaSyCRh-gFfBei5c99S6d_HcAe9pvFl_3ZCiw";
                String responseString = null;
                URL obj = null;
                try {
                    obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    // optional default is GET
                    con.setRequestMethod("GET");


                    int responseCode = con.getResponseCode();
                    System.out.println("\nSending 'GET' request to URL : " + url);
                    System.out.println("Response Code : " + responseCode);

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    responseString =  response.toString();
                    Log.e("Distance", responseString);
                    in.close();

                    //print result
                    System.out.println(response.toString());

                    Intent intent = new Intent("DISTANCE_UPDATED");
                    intent.putExtra(DISTANCE_MATRIX, response.toString());

                    sendBroadcast(intent);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e){
                    e.printStackTrace();
                }
                return "Executed";
            }

            @Override
            protected void onPostExecute(String result) {}

            @Override
            protected void onPreExecute() {}

            @Override
            protected void onProgressUpdate(Void... values) {}
        }
        new LongOperation().execute();
    }


}

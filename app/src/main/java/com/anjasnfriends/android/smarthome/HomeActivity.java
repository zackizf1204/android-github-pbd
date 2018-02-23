package com.anjasnfriends.android.smarthome;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.anjasnfriends.android.smarthome.MainActivity.userName;

public class HomeActivity extends AppCompatActivity implements SensorEventListener {
    private final static String TAG = "HomeActivity";
    private final String urlPath = "https://smarthome-server-api.herokuapp.com";
    private final int PROXIMITY_NEAR = 0;
    private final String TOGGLE_SWITCH = "Value_Changed";

    private int doorSwitchValue = 0;
    private int lamp1SwitchValue = 0;
    private int lamp2SwitchValue = 0;
    private int lamp3SwitchValue = 0;
    private int alarmSwitchValue = 0;

    public static String getAddress() {
        return address;
    }

    public static String address = "Gasibu";

    //Permission
    private static final int INITIAL_REQUEST=1337;
    private static final String[] INITIAL_PERMS={
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.INTERNET
    };

    // Sensor
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mProximity;



    //View
    Switch doorSwitch;
    Switch lamp1Switch;
    Switch lamp2Switch;
    Switch lamp3Switch;
    Switch alarmSwitch;
    TextView Distance;

    AddressFragment addressFragment = new AddressFragment();
    LogoFragment logoFragment = new LogoFragment();
    EditText AddressBar;

    // Constant for low filters
    private float timeConstant;
    private float alpha;
    private float dt;
    // Timestamps for low filters
    private long timeStamp;
    private long timeStampOld = System.nanoTime();
    // Gravity and linear acceleration components
    private float[] gravity = new float[] {0, 0, 0};
    private float[] linearAccelValue = new float[] {0, 0, 0};
    private int count = 0;
    private int visibility = 0;

    //@SuppressLint("ValidFragment")
    //public class ViewStatus extends Fragment {
        //Fill user status here
    //}
    //Broadcast receiver
    /*private BroadcastReceiver updateAddress= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            door = intent.getStringExtra("door");
            lamp1 = intent.getStringExtra("lamp1");
            lamp2 = intent.getStringExtra("lamp2");
            lamp3 = intent.getStringExtra("lamp3");
            alarm = intent.getStringExtra("alarm");
            owner = intent.getStringExtra("owner");
            address = intent.getStringExtra("address");
            Log.d("Address", "ALAMAT = " + address);
            System.out.print(address);
            AddressBar.setText(address);
        }
    };*/

    private BroadcastReceiver distanceUi= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String response = intent.getStringExtra(LocationService.DISTANCE_MATRIX);
            Log.e("RESPONSE :", response);
            try {
                JSONObject json = new JSONObject(response);
                JSONArray json2 = json.getJSONArray("rows");
                JSONObject json3 = json2.getJSONObject(0);
                JSONArray json4 = json3.getJSONArray("elements");
                JSONObject json5 = json4.getJSONObject(0);
                JSONObject json6 = json5.getJSONObject("distance");
                Log.e("RESPONSE :", json6.getString("text"));
                Distance.setText(json6.getString("text"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    };
    private BroadcastReceiver addressUI= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            AddressBar= (EditText) findViewById(R.id.AddressBar);
            AddressBar.setText(address);
            AddressBar.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        address = AddressBar.getText().toString();
                        try {
                            sendHTTPPostRequest();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(getApplicationContext(), "Address Updated", Toast.LENGTH_LONG).show();

                        stopService(new Intent(getApplicationContext(), LocationService.class));
                        Intent locservice = new Intent(getApplicationContext(), LocationService.class);
                        locservice.putExtra("address",address);
                        startService(locservice);
                        return true;
                    }
                    return false;
                }
            });

        }
    };
    private BroadcastReceiver toggleSwitch = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (doorSwitchValue == 1) {
                doorSwitch.setChecked(true);
            }
            if (lamp1SwitchValue == 1) {
                lamp1Switch.setChecked(true);
            }
            if (lamp2SwitchValue == 1) {
                lamp2Switch.setChecked(true);
            }
            if (lamp3SwitchValue == 1) {
                lamp3Switch.setChecked(true);
            }
            if (alarmSwitchValue == 1) {
                alarmSwitch.setChecked(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (savedInstanceState != null) {
            doorSwitchValue = savedInstanceState.getInt("doorValue");
            lamp1SwitchValue = savedInstanceState.getInt("lamp1Value");
            lamp2SwitchValue = savedInstanceState.getInt("lamp2Value");
            lamp3SwitchValue = savedInstanceState.getInt("lamp3Value");
            alarmSwitchValue = savedInstanceState.getInt("alarmValue");
            address = savedInstanceState.getString("address");

            // sendBroadcast(new Intent(TOGGLE_SWITCH));
        }

        registerReceiver(distanceUi, new IntentFilter("DISTANCE_UPDATED"));
        registerReceiver(toggleSwitch, new IntentFilter(TOGGLE_SWITCH));
        registerReceiver(addressUI, new IntentFilter("ADDRESS"));
        doorSwitch = (Switch) findViewById(R.id.door_switch);
        lamp1Switch = (Switch) findViewById(R.id.lamp_1_switch);
        lamp2Switch = (Switch) findViewById(R.id.lamp_2_switch);
        lamp3Switch = (Switch) findViewById(R.id.lamp_3_switch);
        alarmSwitch = (Switch) findViewById(R.id.alarm_switch);
        Distance = (TextView)findViewById(R.id.distancevalue);

        Log.d(TAG, "User Name in Home = " + userName);


        loadFragment(logoFragment);
        // Register Event Listener
        doorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    doorSwitchValue = 1;
                } else {
                    doorSwitchValue = 0;
                }
                try {
                    sendHTTPPostRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "doorSwitchValue = " + doorSwitchValue);
            }
        });

        lamp1Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    lamp1SwitchValue = 1;
                } else {
                    lamp1SwitchValue = 0;
                }
                try {
                    sendHTTPPostRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "lamp1SwitchValue = " + lamp1SwitchValue);
            }
        });

        lamp2Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    lamp2SwitchValue = 1;
                } else {
                    lamp2SwitchValue = 0;
                }
                try {
                    sendHTTPPostRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "lamp2SwitchValue = " + lamp2SwitchValue);
            }
        });

        lamp3Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    lamp3SwitchValue = 1;
                } else {
                    lamp3SwitchValue = 0;
                }
                try {
                    sendHTTPPostRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "lamp3SwitchValue = " + lamp3SwitchValue);
            }
        });

        alarmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    alarmSwitchValue = 1;
                } else {
                    alarmSwitchValue = 0;
                }
                try {
                    sendHTTPPostRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "alarmSwitchValue = " + alarmSwitchValue);
            }
        });

        // TODO Add HTTP GET to update current status
        try {
            sendHTTPGETRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent locservice = new Intent(getApplicationContext(), LocationService.class);
        locservice.putExtra("address",address);
        startService(locservice);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if ((mSensorManager != null ? mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null) != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if ((mSensorManager != null ? mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) : null) != null) {
            mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
        Button secondButton = (Button) findViewById(R.id.buttonContact);
// perform setOnClickListener on second Button
        secondButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
// display a message by using a Toast
                Intent intent=new Intent(Intent.ACTION_SEND);
                String[] recipients={"anjasnfriendsupport@gmail.com"};
                intent.putExtra(Intent.EXTRA_EMAIL, recipients);
                intent.putExtra(Intent.EXTRA_SUBJECT,"Subject text here...");
                intent.putExtra(Intent.EXTRA_TEXT,"Body of the content here...");
                intent.setType("text/html");
                intent.setPackage("com.google.android.gm");
                startActivity(Intent.createChooser(intent, "Send mail"));
            }
        });
        Button addressButton = (Button) findViewById(R.id.EditAddress);
// perform setOnClickListener on second Button
        addressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(visibility==0){
                    loadFragment(addressFragment);
                    Intent intent = new Intent("ADDRESS");
                    sendBroadcast(intent);
                    visibility=1;
                }
                else{
                    loadFragment(logoFragment);
                    visibility=0;
                }
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("doorValue", doorSwitchValue);
        outState.putInt("lamp1Value", lamp1SwitchValue);
        outState.putInt("lamp2Value", lamp2SwitchValue);
        outState.putInt("lamp3Value", lamp3SwitchValue);
        outState.putInt("alarmValue", alarmSwitchValue);
        outState.putString("address", address);
    }

    private float[] normalizeAcceleroValue(float[] sensorValues) {
        float[] linearAcceleration = new float[] {0, 0, 0};
        timeConstant = 0.18f;
        timeStamp = System.nanoTime();
        dt = 1 / (count / ((timeStamp - timeStampOld) / 1000000000.0f));
        count++;
        alpha = timeConstant / (timeConstant + dt);

        for (int i = 0; i < 3; i++) {
            gravity[i] = alpha * gravity[i] + (1 - alpha) * sensorValues[i];
            linearAcceleration[i] = sensorValues[i] - gravity[i];
        }

        return linearAcceleration;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            linearAccelValue = normalizeAcceleroValue(sensorEvent.values);
            if (Math.abs(linearAccelValue[1]) > 6f || Math.abs(linearAccelValue[2]) > 6f) {
                doorSwitch.toggle();
                // TODO Add HTTP POST to update status
            }
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (sensorEvent.values[0] == PROXIMITY_NEAR) {
                lamp1Switch.toggle();
                lamp2Switch.toggle();
                lamp3Switch.toggle();
                // TODO Add HTTP POST to update Status
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
//        Log.d();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        registerReceiver(distanceUi, new IntentFilter("DISTANCE_UPDATED"));
        startService(new Intent(this, LocationService.class));
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        stopService(new Intent(this, LocationService.class));
        unregisterReceiver(distanceUi);
    }

    private void loadFragment(Fragment fragment) {
// create a FragmentManager
        FragmentManager fm = getFragmentManager();
// create a FragmentTransaction to begin the transaction and replace the Fragment
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
// replace the FrameLayout with new Fragment
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit(); // save the changes
    }


    private void sendHTTPGETRequest() throws Exception {
        Log.d("SendGETRequest", "Execute");
        class RequestGET extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                // TODO Add variable for email
                String urlString = urlPath + "/getHomeStatus/" + userName;
                String response;
                URL url = null;
                try {
                    url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    Log.d("TAG", "GET Response Code = " + connection.getResponseCode());

                    BufferedReader respWriter = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String input;
                    StringBuffer responseString = new StringBuffer();

                    while((input = respWriter.readLine()) != null) {
                        responseString.append(input);
                    }
                    response = responseString.toString();
                    Log.d(TAG, "Response JSON = " + response);
                    respWriter.close();

                    JSONArray jsonArray = new JSONArray(response);
                    if (jsonArray.length() != 0) {
                        JSONObject responseJSON = jsonArray.getJSONObject(0);
                        doorSwitchValue = Integer.valueOf(responseJSON.getString("door"));
                        lamp1SwitchValue = Integer.valueOf(responseJSON.getString("lamp1"));
                        lamp2SwitchValue = Integer.valueOf(responseJSON.getString("lamp2"));
                        lamp3SwitchValue = Integer.valueOf(responseJSON.getString("lamp3"));
                        alarmSwitchValue = Integer.valueOf(responseJSON.getString("alarm"));
                        address = responseJSON.getString("address");
                        Intent intent = new Intent(TOGGLE_SWITCH);
                        sendBroadcast(intent);
                    } else {
                        urlString = urlPath + "/createNewHome";
                        JSONObject requestJSON = new JSONObject();

                        url = new URL(urlString);
                        HttpURLConnection registConnection = (HttpURLConnection) url.openConnection();
                        registConnection.setRequestMethod("POST");
                        registConnection.setRequestProperty("Content-Type", "application/json");

                        requestJSON.put("door", String.valueOf(doorSwitchValue));
                        requestJSON.put("lamp1", String.valueOf(lamp1SwitchValue));
                        requestJSON.put("lamp2", String.valueOf(lamp2SwitchValue));
                        requestJSON.put("lamp3", String.valueOf(lamp3SwitchValue));
                        requestJSON.put("alarm", String.valueOf(alarmSwitchValue));
                        requestJSON.put("owner", userName);
                        requestJSON.put("address", address);

                        registConnection.setDoOutput(true);
                        DataOutputStream outputStream = new DataOutputStream(registConnection.getOutputStream());
                        outputStream.writeBytes(requestJSON.toString());
                        outputStream.close();

                        Log.d("TAG", "User Name = " + userName);
                        Log.d("TAG", "POST Response Code = " + registConnection.getResponseCode());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "Error = " + e.getMessage());
                }

                return null;
            }
        }
        new RequestGET().execute();
    }

    private void sendHTTPPostRequest() throws Exception {
        Log.d("SendPOSTRequest", "Execute");
        class RequestPOST extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                // TODO Add variable for email
                String urlString = urlPath + "/updateHomeStatus";
                String response;
                URL url = null;
                JSONObject requestJSON = new JSONObject();
                try {
                    url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");

                    requestJSON.put("door", String.valueOf(doorSwitchValue));
                    requestJSON.put("lamp1", String.valueOf(lamp1SwitchValue));
                    requestJSON.put("lamp2", String.valueOf(lamp2SwitchValue));
                    requestJSON.put("lamp3", String.valueOf(lamp3SwitchValue));
                    requestJSON.put("alarm", String.valueOf(alarmSwitchValue));
                    requestJSON.put("owner", userName);
                    requestJSON.put("address", address);

                    connection.setDoOutput(true);
                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                    outputStream.writeBytes(requestJSON.toString());
                    outputStream.close();

                    Log.d("TAG", "POST Response Code = " + connection.getResponseCode());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "Error = " + e.getMessage());
                }
                return null;
            }
        }
        new RequestPOST().execute();
    }
}

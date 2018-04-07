// Copyright 2016 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.anjasnfriends.android.smarthome;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.squareup.picasso.Picasso;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.anjasnfriends.android.smarthome.MainApplication.LOG_TAG;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String SHARED_PREFERENCES_NAME = "AuthStatePreference";
    private static final String AUTH_STATE = "AUTH_STATE";
    private static final String USED_INTENT = "USED_INTENT";

    public static String userName = null;

    MainApplication mMainApplication;

    // state
    AuthState mAuthState;

    //Permission
    private static final int INITIAL_REQUEST=1337;
    private static final String[] INITIAL_PERMS={
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.INTERNET
    };

    // views
    AppCompatButton mAuthorize;
    AppCompatButton mSignOut;

    public static boolean isLogin = false;
    ImageButton myButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Push Notification FCM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }
        //
        FirebaseMessaging.getInstance().subscribeToTopic("SmartHomeAnjas");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
        }
        mMainApplication = (MainApplication) getApplication();
        mAuthorize = (AppCompatButton) findViewById(R.id.authorize);
        mSignOut = (AppCompatButton) findViewById(R.id.signOut);

        enablePostAuthorizationFlows();

        // wire click listeners
        mAuthorize.setOnClickListener(new AuthorizeListener());

        Log.d(TAG, "Token = " + FirebaseInstanceId.getInstance().getToken());
    }

    private void enablePostAuthorizationFlows() {
        mAuthState = restoreAuthState();
        if (mAuthState != null && mAuthState.isAuthorized()) {
            getProfileInfo();
            if (mSignOut.getVisibility() == View.GONE) {
                mSignOut.setVisibility(View.VISIBLE);
                mSignOut.setOnClickListener(new SignOutListener(this));
                mAuthorize.setVisibility(View.GONE);
                myButton = findViewById(R.id.logo);
                myButton.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        Intent intentLoadNewActivity = new Intent(MainActivity.this, HomeActivity.class);
                        intentLoadNewActivity.putExtra("UserName", userName);
                        startActivity(intentLoadNewActivity);
                    }
                });
            }
            if (!isLogin) {
                isLogin = true;
                Intent intent = new Intent(this, HomeActivity.class);
                Log.d(MainApplication.LOG_TAG, "User Name Login = " + userName);
                startActivity(intent);
            }
        } else {
            if (isLogin && myButton != null)
                myButton.setOnClickListener(null);
            isLogin = false;
            mSignOut.setVisibility(View.GONE);
            mAuthorize.setVisibility(View.VISIBLE);
            Log.d(LOG_TAG, "User telah berhasil Sign Out 2");
        }
        Log.d(LOG_TAG, "Status Login = " + isLogin);
    }

    /**
     * Exchanges the code, for the {@link TokenResponse}.
     *
     * @param intent represents the {@link Intent} from the Custom Tabs or the System Browser.
     */
    private void handleAuthorizationResponse(@NonNull Intent intent) {

        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);
        if (response != null) {
            Log.i(LOG_TAG, String.format("Handled Authorization Response %s ", authState.toJsonString()));
            AuthorizationService service = new AuthorizationService(this);
            service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                @Override
                public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                    if (exception != null) {
                        Log.w(LOG_TAG, "Token Exchange failed", exception);
                    } else {
                        if (tokenResponse != null) {
                            authState.update(tokenResponse, exception);
                            persistAuthState(authState);
                            Log.i(LOG_TAG, String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                        }
                    }
                }
            });
        }
    }

    private void persistAuthState(@NonNull AuthState authState) {
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putString(AUTH_STATE, authState.toJsonString())
                .apply();
        Log.d(TAG, "Json String 1 = " + authState.toJsonString());
        enablePostAuthorizationFlows();
    }

    private void clearAuthState() {
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(AUTH_STATE)
                .apply();
    }

    @Nullable
    private AuthState restoreAuthState() {
        String jsonString = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(AUTH_STATE, null);
        Log.d(TAG, "Json String 2 = " + jsonString);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.fromJson(jsonString);
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Kicks off the authorization flow.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        checkIntent(intent);
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case "com.anjasnfriends.android.smarthome.HANDLE_AUTHORIZATION_RESPONSE":
                    if (!intent.hasExtra(USED_INTENT)) {
                        handleAuthorizationResponse(intent);
                        intent.putExtra(USED_INTENT, true);
                    }
                    break;
                default:
                    // do nothing
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkIntent(getIntent());
    }

    public static class AuthorizeListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {

            AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                    Uri.parse("https://accounts.google.com/o/oauth2/v2/auth") /* auth endpoint */,
                    Uri.parse("https://www.googleapis.com/oauth2/v4/token") /* token endpoint */
            );
            String clientId = "511828570984-fuprh0cm7665emlne3rnf9pk34kkn86s.apps.googleusercontent.com";
            Uri redirectUri = Uri.parse("com.google.codelabs.appauth:/oauth2callback");
            AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                    serviceConfiguration,
                    clientId,
                    AuthorizationRequest.RESPONSE_TYPE_CODE,
                    redirectUri
            );
            builder.setScopes("profile");
            AuthorizationRequest request = builder.build();
            AuthorizationService authorizationService = new AuthorizationService(view.getContext());
            String action = "com.anjasnfriends.android.smarthome.HANDLE_AUTHORIZATION_RESPONSE";
            Intent postAuthorizationIntent = new Intent(action);
            PendingIntent pendingIntent = PendingIntent.getActivity(view.getContext(), request.hashCode(), postAuthorizationIntent, 0);
            authorizationService.performAuthorizationRequest(request, pendingIntent);
            isLogin = true;
            Log.d(LOG_TAG, "User telah berhasil login");
        }
    }

    public static class SignOutListener implements Button.OnClickListener {

        private final MainActivity mMainActivity;

        public SignOutListener(@NonNull MainActivity mainActivity) {
            mMainActivity = mainActivity;
        }

        @Override
        public void onClick(View view) {
            mMainActivity.mAuthState = null;
            mMainActivity.clearAuthState();
            mMainActivity.enablePostAuthorizationFlows();
        }
    }

    public void getProfileInfo() {
        mAuthState.performActionWithFreshTokens(new AuthorizationService(this), new AuthState.AuthStateAction() {
            @Override
            public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException exception) {
                new AsyncTask<String, Void, JSONObject>() {
                    @Override
                    protected JSONObject doInBackground(String... tokens) {
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url("https://www.googleapis.com/oauth2/v3/userinfo")
                                .addHeader("Authorization", String.format("Bearer %s", tokens[0]))
                                .build();

                        try {
                            Response response = client.newCall(request).execute();
                            String jsonBody = response.body().string();
                            Log.i(LOG_TAG, String.format("User Info Response %s", jsonBody));
                            return new JSONObject(jsonBody);
                        } catch (Exception exception) {
                            Log.w(LOG_TAG, exception);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(JSONObject userInfo) {
                        if (userInfo != null) {
                            String fullname  = userInfo.optString("name", null);
                            userName = fullname.replaceAll(" ","");
                            Log.d(MainApplication.LOG_TAG, "User name = " + userName);
                        }
                    }
                }.execute(accessToken);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}

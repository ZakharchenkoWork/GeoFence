package com.znshadows.geofence;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private static final int REQUEST_CHECK_SETTINGS = 123;
    public static final int GPS_REQUEST = 111;

    enum LOADING_STATE {
        PERMISSION_CHECK,
        GPS_CHECK,
        INTERNET_CHECK,
        INSIDE_FENCE_CHECK
    }

    LOADING_STATE loadingState = LOADING_STATE.PERMISSION_CHECK;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button start = (Button) findViewById(R.id.start);
        start.setOnClickListener((v)->start());
    }

    public void start() {

        if (loadingState == LOADING_STATE.PERMISSION_CHECK) {
            checkGPSPermissons();
        } else if (loadingState == LOADING_STATE.INTERNET_CHECK) {
            if (isNetworkAvailable()) {
                loadingState = LOADING_STATE.INSIDE_FENCE_CHECK;
                checkLocationInsideFence();
            } else {
                showInternetDialog(() -> finish());
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        try {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            if (lastLocation != null) {
               /*
               lastLocation.setLatitude(34.052235);
                lastLocation.setLongitude(-118.243683);//*/
                Log.d("lastLocation ", "" + lastLocation);
                this.lastLocation = lastLocation;
                loadingState = LOADING_STATE.INTERNET_CHECK;

                Log.d("Latitude ", "" + lastLocation.getLatitude());
                Log.d("Longitude ", "" + lastLocation.getLongitude());
                start();
            } else {
                Log.d("lastLocation ", "" + lastLocation);
                checkGPSSettings();
            }


        } catch (SecurityException se) {
            Log.d("SecurityException ", "" + se.getMessage());
        }

    }

    private void checkGPSSettings() {
        if (loadingState != LOADING_STATE.GPS_CHECK) {
            return;
        }
        buildGoogleApiClient();
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(0);
        locationRequest.setFastestInterval(0);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Location location = null;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        } else if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }

        this.lastLocation = location;
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                if (loadingState != LOADING_STATE.GPS_CHECK) {
                    return;
                }
                final Status status = result.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.v("MainActivity", "All location settings are satisfied.");
                        onConnected(null);
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.v("MainActivity", "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().

                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);

                        } catch (IntentSender.SendIntentException e) {
                            //isResolutionStarted = false;
                            //loadingState = LOADING_STATE.STATE_SELECTION;
                            //start();
                            //Log.v("PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.v("", "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_REQUEST) {
            if (resultCode == -1) {
                onConnected(null);

            } else if (resultCode == 0) {
                new OneButtonDialog(this, OneButtonDialog.DIALOG_TYPE.MESSAGE_ONLY)
                        .setTitle("Oops")
                        .setMessage("Sorry, if you won't give permission then app wil not work")
                        .build();

            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        googleApiClient.connect();
    }

    private void checkLocationInsideFence() {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    loadingState = LOADING_STATE.GPS_CHECK;
                    checkGPSSettings();

                    onConnected(null);
                } else {
                    new OneButtonDialog(this, OneButtonDialog.DIALOG_TYPE.MESSAGE_ONLY)
                            .setTitle("Oops").setMessage("Sorry, if you won't give permission, you can not use this app")
                            .build();
                }
            }
        }
    }

    private void checkGPSPermissons() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            loadingState = LOADING_STATE.GPS_CHECK;
            checkGPSSettings();
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();//TODO: check for wifi network name

    }

    private void showInternetDialog(Runnable onCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.no_internet_message)
                .setCancelable(false)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        startActivity(new Intent(Settings.ACTION_SETTINGS));

                    }
                })
                .setNegativeButton("Cancel", (DialogInterface dialog, int id) -> onCancel.run());
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showGpsDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("GPS is not currently enabled. Click ok to proceed to Location Settings.")
                .setTitle("Toggle GPS");
        builder.setPositiveButton("OK", (DialogInterface dialog, int id) -> {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            MainActivity.this.startActivityForResult(settingsIntent, GPS_REQUEST);

        });
        builder.setNegativeButton("Cancel", (DialogInterface dialog, int id) -> {
            finish();
        });

        builder.show();


    }

    @Override
    public void onStop() {
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }

}

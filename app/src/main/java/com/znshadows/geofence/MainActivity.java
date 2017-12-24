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
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

    private static final int GPS_REQUEST = 123;
    public static final String NO_WIFI = "<unknown ssid>";

    private GoogleApiClient googleApiClient;
    private EditText latitude;
    private EditText longitude;
    private EditText radius;
    private EditText wifiName;
    private Button start;
    private TextView result;



    private LOADING_STATE loadingState = LOADING_STATE.PERMISSION_CHECK;
    private ACTION onLocationReceivedAction = ACTION.FENCE;

    private boolean isResolutionStarted = false;//prevents from duplication of resolution dialog

    private FenceController fenceController = new FenceController();




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = (Button) findViewById(R.id.start);
        start.setOnClickListener((v) -> {
            prepareCoordinates();
            start();

        });

        latitude = (EditText) findViewById(R.id.latitude);
        longitude = (EditText) findViewById(R.id.longitude);
        radius = (EditText) findViewById(R.id.radius);
        wifiName = (EditText) findViewById(R.id.wifiName);
        result = (TextView) findViewById(R.id.result);

        Button currentLatitude = (Button) findViewById(R.id.currentLatitude);
        currentLatitude.setOnClickListener((v) -> {

            if (fenceController.getLastLocation() == null) {
                onLocationReceivedAction = ACTION.LATITUDE;
                start();
            } else {
                latitude.setText("" + fenceController.getLastLocation().getLatitude());
                onLocationReceivedAction = ACTION.FENCE;
            }
        });

        Button currentLongitude = (Button) findViewById(R.id.currentLongitude);
        currentLongitude.setOnClickListener((v) -> {

            if (fenceController.getLastLocation() == null) {
                onLocationReceivedAction = ACTION.LONGITUDE;
                start();
            } else {
                longitude.setText("" + fenceController.getLastLocation().getLongitude());
                onLocationReceivedAction = ACTION.FENCE;
            }
        });

        Button currentWifiName = (Button) findViewById(R.id.currentWifiName);
        currentWifiName.setOnClickListener((v) -> {
            wifiName.setText(getWifiName());
        });
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


    private boolean prepareCoordinates() {
        try {
            //Didn't want to mess with old values
            double latitude = Double.parseDouble(this.latitude.getText().toString());
            double longitude = Double.parseDouble(this.longitude.getText().toString());
            int radiusValue = Integer.parseInt(radius.getText().toString());
            String wifiNameValue = wifiName.getText().toString();
            if (wifiNameValue.equals("") || radiusValue == 0) {
                throw new NumberFormatException();
            }

            fenceController.setCenterLatitude(latitude);
            fenceController.setCenterLongitude(longitude);
            fenceController.setRadiusDistance(radiusValue);
            fenceController.setWifiFenceName(wifiNameValue);
            return true;
        } catch (NumberFormatException nfe) {
            showErrorDialog(getString(R.string.fence_error_message));
            return false;
        }
    }

    private String getWifiName() {

        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr != null) {
            String name = wifiMgr.getConnectionInfo().getSSID().replace("\"", "");
            if (!name.equals(NO_WIFI)) {
                return name;
            }
        }

        return "";
    }


    public void start() {

        if (loadingState == LOADING_STATE.PERMISSION_CHECK) {
            checkGPSPermissions();
        } else if (loadingState == LOADING_STATE.GPS_CHECK) {
            onConnected(null);
        } else if (loadingState == LOADING_STATE.INTERNET_CHECK) {
            if (isNetworkAvailable()) {
                loadingState = LOADING_STATE.INSIDE_FENCE_CHECK;
                tryToCheckData();
            } else {
                showInternetDialog(() -> finish());
            }
        } else if (loadingState == LOADING_STATE.INSIDE_FENCE_CHECK) {
            tryToCheckData();
        }
    }


    private void tryToCheckData() {
        if (fenceController.dataIsReady()) {
            result.setText(isLocationInsideFence() ? getString(R.string.inside) : getString(R.string.outside));
        }
    }


    @Override
    public void onConnected(Bundle connectionHint) {

        try {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            if (lastLocation != null) {

                Log.d("lastLocation ", "" + lastLocation);
                fenceController.setLastLocation(lastLocation);
                if (lastLocation != null) {
                    if (onLocationReceivedAction == ACTION.LATITUDE) {
                        latitude.setText("" + lastLocation.getLatitude());
                        onLocationReceivedAction = ACTION.FENCE;
                    } else if (onLocationReceivedAction == ACTION.LONGITUDE) {
                        longitude.setText("" + lastLocation.getLongitude());
                        onLocationReceivedAction = ACTION.FENCE;
                    }

                }
                loadingState = LOADING_STATE.INTERNET_CHECK;

                Log.d("Latitude ", "" + lastLocation.getLatitude());
                Log.d("Longitude ", "" + lastLocation.getLongitude());
                start();
            } else {
                Log.d("lastLocation ", "" + lastLocation);
                checkGPSEnabled();
            }


        } catch (SecurityException se) {
            Log.d("SecurityException ", "" + se.getMessage());
        }

    }

    private void checkGPSEnabled() {
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

        if (location != null) {
            fenceController.setLastLocation(location);
        }
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
                            if (!isResolutionStarted) {
                                isResolutionStarted = true;
                                status.startResolutionForResult(MainActivity.this, GPS_REQUEST);
                            }

                        } catch (IntentSender.SendIntentException e) {
                            isResolutionStarted = false;
                            //loadingState = LOADING_STATE.STATE_SELECTION;
                            //start();
                            //Log.v("PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.v("", "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        showErrorDialog(getString(R.string.connection_error_message));
                        break;
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_REQUEST) {
            if (resultCode == -1) {
                onConnected(null);

            } else if (resultCode == 0) {
                showErrorDialog(getString(R.string.permission_error_message));


            }
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        fenceController.setLastLocation(location);
        tryToCheckData();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        showErrorDialog(getString(R.string.connection_error_message));
    }

    @Override
    public void onConnectionSuspended(int i) {
        showErrorDialog(getString(R.string.connection_error_message));
    }




    private boolean isLocationInsideFence() {

        if (fenceController.checkWifiName(getWifiName())) {
            return true;
        }
        return fenceController.getDistanceToFence() < fenceController.getRadiusDistance();
    }



    private void checkGPSPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, GPS_REQUEST);
        } else {
            loadingState = LOADING_STATE.GPS_CHECK;
            checkGPSEnabled();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case GPS_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadingState = LOADING_STATE.GPS_CHECK;
                    checkGPSEnabled();
                } else {
                    showErrorDialog(getString(R.string.permission_error_message));

                }
            }
        }
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();

    }

    private void showInternetDialog(Runnable onCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.no_internet_message)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.connect), (DialogInterface dialog, int id) -> {
                    dialog.dismiss();
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                })
                .setNegativeButton(getString(R.string.cancel), (DialogInterface dialog, int id) -> onCancel.run());
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showErrorDialog(String message) {
        new OneButtonDialog(this, OneButtonDialog.DIALOG_TYPE.MESSAGE_ONLY)
                .setTitle(getString(R.string.error_dialog_title))
                .setMessage(message)
                .build();
    }

    @Override
    public void onStop() {
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    enum ACTION {
        LATITUDE,
        LONGITUDE,
        FENCE
    }

    enum LOADING_STATE {
        PERMISSION_CHECK,
        GPS_CHECK,
        INTERNET_CHECK,
        INSIDE_FENCE_CHECK
    }
}

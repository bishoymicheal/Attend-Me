package net.corpy.loginlocation.locationApi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import net.corpy.loginlocation.Utils;
import net.corpy.permissionslib.PermissionsFile;

import java.util.Locale;

/**
 * LocationAPI2 Class
 * <p>
 * Helper Class Contains Locations Detection Methods
 * <p>
 * Version 1.0
 * <p>
 * Updated Version --
 */
public class LocationAPI implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {


    // status callback values
    private static String FAILED_NETWORK_CONNECTIVITY_ERROR = "no network connection";
    private static String FAILED_PERMISSION_ERROR = "no location permission";
    private static String FAILED_LOCATION_PROVIDER_ERROR = "no location provider";
    private static String FAILED_API_CONNECTION_SUSPENDER = "api connection suspended";
    private static String FAILED_API_CONNECTION_FAILED = "api connection failed";
    private static String FAILED_NO_LOCATION_ERROR = "no location history available";

    private static int DISPLACEMENT = 1; // 10 meters
    // boolean flag to toggle periodic location updates
    // Google client to interact with Google API
    private static GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private LocationChangeListener stateChangeListener;
    private boolean mRequestingLocationUpdates = false;
    private LocationRequest mLocationRequest;
    private LocationAsync locationAsync;

    public LocationAPI(Context mContext) {
        this.mContext = mContext;
        buildGoogleApiClient();
        createLocationRequest();
    }

    private boolean isLocationEnabled(Context context) {
        int locationMode;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * creating LocationRequest as an object with updating time interval
     */
    private void createLocationRequest() {
        // Location updates intervals in sec
        // 10 sec
        int UPDATE_INTERVAL = 1000;
        // 5 sec
        int FASTEST_INTERVAL = 500;
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)        // 10 seconds, in milliseconds
                .setFastestInterval(FASTEST_INTERVAL);
    }

    /**
     * start locations request for updates
     */
    private void startLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            connect();
        }
    }

    /**
     * stop reciving location updates
     */
    private void stopLocationUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    /**
     * gets location from gps after checking for permission
     */
    public void getCurrentLocation(LocationChangeListener listener) {
        stateChangeListener = listener;
        if (isConnected()) {
            getLocation();
        } else {
            connect();
        }
    }

    private void getLocation() {
        try {
            if (mContext != null) {
                FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
                mFusedLocationClient.getLastLocation()
                        .addOnCompleteListener(new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                if (stateChangeListener != null)
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        if (!mRequestingLocationUpdates) stopLocationUpdates();
                                        //paths the location class to the listener which is responsible for sending location to the location activity to be displayed for the user
                                        getLocationData(task.getResult());
                                    } else {
                                        LocationServices.FusedLocationApi.requestLocationUpdates
                                                (mGoogleApiClient, mLocationRequest, LocationAPI.this);
                                        showErrorMessage(FAILED_NO_LOCATION_ERROR);
                                    }
                            }
                        });
            }
        } catch (SecurityException e) {
            if (stateChangeListener != null) showErrorMessage(FAILED_PERMISSION_ERROR);
        }
    }

    public void setLocationChangeListener(LocationChangeListener listener) {
        this.stateChangeListener = listener;
    }

    public void disconnect() {
        if (locationAsync != null) locationAsync.cancel(true);
        stopLocationUpdates();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /***
     * Check if google api's is connected
     * @return
     */
    private boolean isConnected() {
        return (mGoogleApiClient.isConnected());
    }

    /**
     * starts connection to google api
     */
    public void connect() {
        if (checkNetworkConnectivity()) {

            if (!PermissionsFile.locationPermissionCheck(mContext)) {
                PermissionsFile.showLocationDialogPermission(mContext,
                        new PermissionsFile.PermissionListener() {
                            @Override
                            public void permissionResult(boolean hasPermission) {
                                if (hasPermission) {
                                    locationSettingCheck();
                                } else showErrorMessage(FAILED_PERMISSION_ERROR);
                            }
                        });
            } else {
                locationSettingCheck();
            }

        } else {

            showNetworkDialog();

        }

    }

    private void showNetworkDialog() {
        showErrorMessage(FAILED_NETWORK_CONNECTIVITY_ERROR);

    }

    /**
     * chek if location service is enabled and ask user to open it if closed
     */
    private void locationSettingCheck() {
        if (isLocationEnabled(mContext)) {

            connectService();

        } else {
            LocationRequestActivity.locationSettingsListener = new LocationSettingsListener() {
                @Override
                public void locationResult(boolean result) {
                    if (result) connectService();
                    else showErrorMessage(FAILED_LOCATION_PROVIDER_ERROR);
                }
            };
            mContext.startActivity(new Intent(mContext, LocationRequestActivity.class));
        }
    }

    private void showErrorMessage(String message) {
        if (stateChangeListener != null) stateChangeListener.onLocationFailed(message);
    }

    /**
     * check if there is network connection
     *
     * @return
     */
    private boolean checkNetworkConnectivity() {
        return Utils.isNetworkConnected(mContext);
    }

    /**
     * initialize google api connection
     */
    private void connectService() {
        if (mGoogleApiClient != null && !isConnected()) {
            mGoogleApiClient.connect();
        } else {
            if (mGoogleApiClient != null) {
                isConnected();
                getCurrentLocation(stateChangeListener);
            }
        }
    }

    public void requestLocationUpdate(boolean mRequestingUpdates) {
        togglePeriodicLocationUpdates(mRequestingUpdates);
    }

    private void togglePeriodicLocationUpdates(boolean mRequestingUpdates) {
        if (mRequestingUpdates) {
            mRequestingLocationUpdates = true;
            // Starting the location updates
            startLocationUpdates();
        } else {
            mRequestingLocationUpdates = false;
            // Stopping the location updates
            stopLocationUpdates();
        }
    }

    /**
     * after connection successfully it will display location and returns the location as a class with all its features
     * in the LocationActivity method named onLocationSuccess(LocationAPI2.LocationData location)
     *
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getCurrentLocation(stateChangeListener);
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        } else stopLocationUpdates();
    }

    /**
     * onConnectionSuspended
     *
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        connect();
        showErrorMessage(FAILED_API_CONNECTION_SUSPENDER);
    }

    /**
     * onConnectionFailed
     *
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        connect();
        showErrorMessage(FAILED_API_CONNECTION_FAILED);

    }

    @Override
    public void onLocationChanged(Location location) {
        getLocationData(location);
    }

    private void getLocationData(Location location) {
        if (location != null) {
            Locale locale = mContext.getResources().getConfiguration().locale;
            Geocoder gcd = new Geocoder(mContext.getApplicationContext(), locale);
            locationAsync = new LocationAsync(gcd, location, new LocationAsync.DataReady() {
                @Override
                public void dataReady(LocationData data) {
                    if (stateChangeListener != null) stateChangeListener.onLocationSuccess(data);
                }
            });
            locationAsync.execute();
        }
    }

    public interface LocationChangeListener {
        void onLocationSuccess(LocationData location);

        void onLocationFailed(String errorMessage);
    }


    public interface LocationSettingsListener {
        void locationResult(boolean result);
    }

}

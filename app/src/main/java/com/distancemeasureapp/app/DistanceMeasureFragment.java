package com.distancemeasureapp.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import java.util.Objects;

import static android.content.Context.SENSOR_SERVICE;


public class DistanceMeasureFragment extends Fragment
      {

    private static final String TAG = "Debugger";
    //two variables for values
    //time in seconds
    //distnace in meters
    private int minTime=60,minDistanceValue=500;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final long INTERVAL = 1000*5;
    private static final long FASTEST_INTERVAL = 1000*5;

    private static final long LOCATIONDISTANCE = 7;


    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    TextView gpsStatusTextView,timeFrame,minDistanceTextView,latLongValue,timeRemaining,distnaceCoverTextView;
    Activity activity;
    Location mCurrentLocation,firstLocation;
    boolean isFirstTime = true;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;


    //Movement dedcutor variable

    // Start with some variables
    private double totalDistanceCover=0.0;
    private boolean isMovementStart = false;
    private boolean isLocationChanged = false;


    LocationManager locationManager;
    LocationListener locationListenerGPS;


    private BroadcastReceiver locationSwitchStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {

                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (isGpsEnabled || isNetworkEnabled) {
                    //location is enabled
                    Log.d("gpsstautus","on");
                    gpsStatusTextView.setText("Connected");

                } else {
                    locationEnabled();
                    //location is disabled
                    gpsStatusTextView.setText("Disconnected ");
                    Log.d("gpsstautus","false");
                }
            }
        }
    };

    public DistanceMeasureFragment() {
    }



    public static DistanceMeasureFragment newInstance(String param1, String param2) {
        DistanceMeasureFragment fragment = new DistanceMeasureFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_distnace_measure, container, false);
        initAll(view);
        registerGpsListner();
        return view;
    }

    private void registerGpsListner() {
        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED);
        activity.registerReceiver(locationSwitchStateReceiver, filter);
        locationEnabled();
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(activity)
                        .setTitle("Location Permission Require")
                        .setMessage("Allow Location Permission")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }


    private void initAll(View view) {
        gpsStatusTextView = view.findViewById(R.id.gps_Status);
        timeFrame = view.findViewById(R.id.time_frame);
        minDistanceTextView = view.findViewById(R.id.minimum_distance);
        latLongValue = view.findViewById(R.id.lat_lang_value);
        timeRemaining = view.findViewById(R.id.time_rem);
        distnaceCoverTextView = view.findViewById(R.id.distance_value);

        minDistanceTextView.setText(minDistanceValue + " meters");
        timeFrame.setText(minTime + " sec");

        if (checkLocationPermission()) {

            getLocation();
        }


    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }


    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    private void startTimer() {

        new CountDownTimer(minTime*1000, 1000) {

            public void onTick(long millisUntilFinished) {
                timeRemaining.setText(millisUntilFinished / 1000+" secs");
                //here you can have your logic to set text to edittext
                if((millisUntilFinished/1000)%5==0){
                    if(isLocationChanged) {
                        double distanceCover = distance(firstLocation.getLatitude(), firstLocation.getLongitude(), mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                        totalDistanceCover = totalDistanceCover + distanceCover;

                        distnaceCoverTextView.setText(String.format("%.2f", totalDistanceCover));
                        isLocationChanged = false;
                    }

                }
            }

            public void onFinish() {
                timeRemaining.setText("Time Over!");
                if(totalDistanceCover<minDistanceValue){
                    Toast.makeText(activity, "You are failed to cover the distance", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(activity, "You are successfully cover the distance", Toast.LENGTH_SHORT).show();

                }
            }

        }.start();
    }


    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        getLocation();
                     //   mGoogleApiClient.connect();
                    }

                } else {
                    checkLocationPermission();

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();



    }


    private boolean locationEnabled () {
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
//                getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        if (!gps_enabled && !network_enabled) {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    activity);
            alertDialogBuilder
                    .setMessage("GPS is disabled in your device. Enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Enable GPS",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    Intent callGPSSettingIntent = new Intent(
                                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivity(callGPSSettingIntent);
                                }
                            });
            alertDialogBuilder.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        }

        return gps_enabled;
    }

    public double GetDistanceFromLatLonInMeters(double lat1, double lon1, double lat2, double lon2)
    {
        final int R = 6371;
        // Radius of the earth in km
        double dLat = deg2rad(lat2 - lat1);
        // deg2rad below
        double dLon = deg2rad(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;
        // Distance in meters
        return d*1000;
    }
    private double deg2rad(double deg)
    {
        return deg * (Math.PI / 180);
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist*1000);
    }



    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    private void getLocation() {

        locationListenerGPS = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                if(isFirstTime){
                    mCurrentLocation = firstLocation = location;
                    isFirstTime = false;
                    gpsStatusTextView.setText("Connected");
                    Log.d(TAG,mCurrentLocation.getLatitude() + ","+mCurrentLocation.getLongitude());
                    double distanceCover = GetDistanceFromLatLonInMeters(firstLocation.getLatitude(),firstLocation.getLongitude(),mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());

                    distnaceCoverTextView.setText(distanceCover+"");
                }else {
                    Log.d(TAG, "Firing onLocationChanged..............................................");
                    //firstLocation = mCurrentLocation;
                    mCurrentLocation = location;


                    //  double speed = location.getSpeed();
                    isLocationChanged = true;
                    if(!isMovementStart) {
                        startTimer();
                        isMovementStart=true;
                    }


                    Log.d(TAG,mCurrentLocation.getLatitude() + ","+mCurrentLocation.getLongitude());


                }

                latLongValue.setText(mCurrentLocation.getLatitude()+","+mCurrentLocation.getLongitude());



            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };


        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Objects.requireNonNull(getContext()).checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && getContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.

                return;
            }
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                5000,
                10, locationListenerGPS);


    }


}

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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.os.Handler;
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
    private int minTime=5,minDistanceValue=500;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final long INTERVAL = 1000*2;

    private static final long LOCATIONDISTANCE = 2;


    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private TextView gpsStatusTextView,timeFrame,minDistanceTextView,latLongValue,timeRemaining,distnaceCoverTextView,motionDeductTextView;
    private Activity activity;
    private Location mCurrentLocation;
    private boolean isFirstTime = true;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    Runnable movementRun,stopRun;


    private LocationManager locationManager;
    private LocationListener locationListenerGPS;

    private Handler handler;

    private boolean checkMovement = false;


          //motion sensor variables
    // Start with some variables
          private SensorManager sensorMan;
          private Sensor accelerometer;

          private float[] mGravity;
          private double mAccel;
          SensorEventListener sensorEventListener;
          private static final int MIN_ACCELERATION =3;

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
        motionDeductTextView = view.findViewById(R.id.motion_deduct);


        minDistanceTextView.setText(minDistanceValue + " meters");
        timeFrame.setText(minTime + " sec");

        if (checkLocationPermission()) {

            getLocation();
        }

        // In onCreate method
        sensorMan = (SensorManager)activity.getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;

        //this is the sensor listener which deduct the user movement
         sensorEventListener=   new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                    mGravity = event.values.clone();
                    // Shake detection
                    float x = mGravity[0];
                    float y = mGravity[1];
                    float z = mGravity[2];

                    mAccel = (x*x+y*y+z*z)/(9.8*9.8);
                   // Log.d("debuggerrun", mAccel + "");

                    //if user acceleration is greater than out set min accleration tha this condition will run
                    if(mAccel>MIN_ACCELERATION){

                        motionDeductTextView.setText("Moving Now");
                        motionDeductTextView.setBackgroundColor(getResources().getColor(R.color.green));
                        //remove all waiting callbacks
                        if(!checkMovement) { handler = new Handler();
                            if (stopRun != null && movementRun != null) {
                                handler.removeCallbacks(stopRun);
                                handler.removeCallbacks(movementRun);
                            }

                            //waiting for the user if they contnously moving over min acceleration in min time
                            movementRun = new Runnable() {
                                public void run() {
                                    checkMovement = true;
                                    Log.d("debuggerrun", "run");
                                    if(mCurrentLocation!=null) {
                                        Log.d("coordinated", mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude());
                                    }
                                    perFormLogic();
                                }
                            };

                            handler.postDelayed(movementRun, minTime* 1000);
                        }


                    }else if (mAccel<MIN_ACCELERATION){
                        //waiting for the user if they continously stop over min acceleration in min time
                        if(checkMovement) {
                            if (movementRun != null && stopRun != null) {
                                handler.removeCallbacks(movementRun);
                                handler.removeCallbacks(stopRun);
                            }

                            motionDeductTextView.setText("NOW STOP");
                            motionDeductTextView.setBackgroundColor(getResources().getColor(R.color.red));


                            stopRun = new Runnable() {
                                public void run() {
                                    checkMovement = false;
                                    Log.d("debuggerrun", "stop");
                                    if(mCurrentLocation!=null) {
                                        Log.d("coordinated", mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude());
                                    }
                                    perFormLogic();

                                }
                            };

                            handler.postDelayed(stopRun, minTime*1000);
                        }

                    }


                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorMan.registerListener(sensorEventListener , accelerometer, SensorManager.SENSOR_DELAY_UI);




    }

    //Your Empty fucntion to perform the logics on user stop and start moving
          private void perFormLogic() {
        //TODO: implement your logic here
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



    public interface OnFragmentInteractionListener {

        void onFragmentInteraction(Uri uri);
    }

    //this overide funcion check if location permsision granted or not
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
//on resume I am setting the listener for GPS
    @Override
    public void onResume() {
        super.onResume();

        sensorMan.registerListener(sensorEventListener, accelerometer,
                SensorManager.SENSOR_DELAY_UI);

    }
    @Override
    public void onPause() {
        super.onPause();
        sensorMan.unregisterListener(sensorEventListener);
    }

//this function check which service is available for getting corrdiantes
    private boolean locationEnabled () {
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
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


   //This function getting the current user location and have a listener on location change so it will
          //calcute the cordinates for the user
    private void getLocation() {

        locationListenerGPS = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                if(isFirstTime){
                    mCurrentLocation  = location;
                    isFirstTime = false;
                    gpsStatusTextView.setText("Connected");
                    Log.d(TAG,mCurrentLocation.getLatitude() + ","+mCurrentLocation.getLongitude());

                }else {
                    Log.d(TAG, "Firing onLocationChanged..............................................");
                    mCurrentLocation = location;


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
                INTERVAL,
                LOCATIONDISTANCE, locationListenerGPS);


    }


}

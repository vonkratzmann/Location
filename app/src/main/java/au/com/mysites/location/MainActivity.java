package au.com.mysites.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static au.com.mysites.location.Constant.PERMISSION_REQUEST_CODE;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

/**
 * Displays latitude and longitude on startup with coordinates displayed
 * in either degrees, degrees and minutes, or degrees, minutes, seconds.
 * The display format is selected via a preference setting list.
 * 2 buttons :
 * 1. Reload Location - updates the latitude and longitude
 * 2. Get Address - displays the address provider by a geocoder
 */
public class MainActivity extends AppCompatActivity {
    //region Fields
    private static final String TAG = MainActivity.class.getSimpleName();

    private FusedLocationProviderClient mFusedLocationClient;
    Location mLastLocation;
    private AddressResultReceiver mResultReceiver;

    Button mButtonLocationUpdate;
    Button mButtonGetAddress;
    //endregion

    //region Lifecycle

    /**
     * loads layout, toolbar
     * sets up FusedLocationProviderClient and addressResultReceiver
     * sets up listeners for the 2 buttons
     * checks location permissions
     *
     * @param savedInstanceState save of state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFusedLocationClient = getFusedLocationProviderClient(this);
        mResultReceiver = new AddressResultReceiver(new Handler());

        mButtonLocationUpdate = findViewById(R.id.ButtonLocationUpdate);
        mButtonLocationUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "OnClick()");
                getAndDisplayMyLocation();
            }
        });

        mButtonGetAddress = findViewById(R.id.ButtonGetAddress);
        mButtonGetAddress.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "OnClick()");
                getAddress();
            }
        });

        //check permissions and if ok get latitude and longitude
        checkPermissions();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onStart()");
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onResume()");

        //display location in case format changed
        if (mLastLocation != null) displayLocation(mLastLocation);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onPause()");
    }


    /**
     * Method for setting up the menu
     *
     * @param menu menu
     * @return boolean
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onCreateOptionsMenu()");

        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our visualizer_menu layout to this menu */
        inflater.inflate(R.menu.menu_main, menu);
        /* Return true so that the visualizer_menu is displayed in the Toolbar */
        return true;
    }
    //endregion

    //region Methods

    /**
     * Checks have permission to access location resources.
     * If we do get location and display it
     * after requestPermission()system calls onRequestPermissionsResult() with the results
     */
    private void checkPermissions() {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "checkPermissions()");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET
                }, PERMISSION_REQUEST_CODE);
            }
            //have the permissions
            getAndDisplayMyLocation();
        } else {
            //not required to ask user for specific permissions as below Android 6 (Marshmallow)
            getAndDisplayMyLocation();
        }
    }


    /**
     * Get format to display coordinates from Preferences
     * @return format of coordinates
     */
    private int getLatLongFormat() {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "getLatLongFormat()");

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String format;

        // get format of latitude and longitude
        String key = getString(R.string.pref_key_format);
        String defaultRate = getString(R.string.pref_default_format);
        format = pref.getString(key, defaultRate);

        if (format.equals(getString(R.string.pref_values_degrees))) return Location.FORMAT_DEGREES;
        if (format.equals(getString(R.string.pref_values_minutes))) return Location.FORMAT_MINUTES;
        if (format.equals(getString(R.string.pref_values_seconds))) return Location.FORMAT_SECONDS;
        //return default
        return Location.FORMAT_DEGREES;
    }


    /**
     * sets up fused location client, which is an API from Google Play Services
     * adds listeners for success and failure
     * if success calls displayLocation() which displays longitude and latitude
     */
    @SuppressLint("MissingPermission")
    void getAndDisplayMyLocation() {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "getAndDisplayMyLocation()");

        //sets up fused location client, which is API from Google Play Services
        mFusedLocationClient = getFusedLocationProviderClient(this);
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {

                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                mLastLocation = location;
                                displayLocation(location);
                            } else {
                                mLastLocation = null;
                                Toast.makeText(getApplicationContext()
                                        ,getString(R.string.location_null), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext()
                                    ,getString(R.string.Gps_error), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(MainActivity.this, getString(R.string.SecurityException),
                    Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Displays latitude and longitude for the supplied location
     *
     * @param location  location containing longitude and latitude
     */
    void displayLocation(Location location) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "displayLocation()");

        int format = getLatLongFormat();
        TextView textView = findViewById(R.id.latitude);
        textView.setText(Location.convert(location.getLatitude(), format));

        textView = findViewById(R.id.longitude);
        textView.setText(Location.convert(location.getLongitude(), format));
    }


    /**
     * Called when the Location Update Button is pressed
     * Gets location to display on UI
     * @param view  view for pressed button
     */
    public void locationUpdate(View view) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "locationUpdate(");
        getAndDisplayMyLocation();
    }


    /*
     * Called when the Display Address Button is pressed
     * Starts service to convert location to an address
     * and display the address on the UI
     */
    public void getAddress() {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "getAddress()");

        if (mLastLocation == null) {
            Toast.makeText(this, getString(R.string.location_null), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ServiceFetchAddress.class);
        intent.putExtra(Constant.RECEIVER, mResultReceiver);
        intent.putExtra(Constant.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }


    /**
     * Displays address on UI
     *
     * @param address   address to be displayed
     */
    void displayAddress(String address) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "displayAddressOutput()");
        TextView textViewAddress = findViewById(R.id.textViewAddress);
        textViewAddress.setText(address);
    }

    /**
     * Used to add code to tidy up before exiting
     */
    private void shutDown() {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "shutDown()");

    }
//endregion

//region listeners

    /**
     * Handle action bar item clicks here. The action bar will
     * automatically handle clicks on the Home/Up button, as long
     * as a parent activity is specified in AndroidManifest.xml.
     *
     * @param item item selected
     * @return item selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onOptionsItemSelected()");

        int id = item.getItemId();

        // check if request to navigate to the settings screen
        if (id == R.id.action_settings) {
            //start the activity
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);
            return true;
        }

        if (id == R.id.quit) {
            //quit application
            shutDown();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Callback from app's permission request
     *
     * @param requestCode  Request code passed in requestPermissions()
     * @param permissions  Requested permissions
     * @param grantResults Grant results for permission: PERMISSION_GRANTED or PERMISSION_DENIED.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onRequestPermissionsResult()");

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    getAndDisplayMyLocation();
                } else {
                    // permission denied exit app, tell user
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                    shutDown();
                    finish();
                }
        }
    }
//endregion

//region InnerClasses

    /**
     *Handles results from the Geocoder
     */
    class AddressResultReceiver extends ResultReceiver {
        @SuppressLint("RestrictedApi")
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        private static final String TAG = "AddressResultReceiver";

        /**
         *
         * @param resultCode    code returned
         * @param resultData    results data
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onReceiveResult()");
            if (resultData == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.address_fail)
                        ,Toast.LENGTH_SHORT).show();
                return;
            }

            // Display the address string
            // or an error message sent from the intent service.
            String mAddressOutput = resultData.getString(Constant.RESULT_DATA_KEY);
            if (mAddressOutput == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.address_fail)
                        ,Toast.LENGTH_SHORT).show();
                mAddressOutput = "";
            }
            if (resultCode == Constant.SUCCESS_RESULT) {
                displayAddress(mAddressOutput);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.address_fail)
                        ,Toast.LENGTH_SHORT).show();
            }
        }
    }
//endregion
}





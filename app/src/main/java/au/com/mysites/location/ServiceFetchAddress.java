package au.com.mysites.location;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Use Geocode to fetch address for the location passed in the intent
 */
public class ServiceFetchAddress extends IntentService {
    public ServiceFetchAddress() {
        super("FetchAddressIntentService");
    }

    private final static String TAG = ServiceFetchAddress.class.getSimpleName();
    protected ResultReceiver mReceiver;

    /**
     * Checks for errors
     * invokes Geocoder
     * gets address, checks for errors
     * logs debug information which is normally suppressed
     * calls method to send results back to main activity
     *
      * @param intent  intent used to start service
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onHandleIntent");

        if (intent == null) {
            if (Debug.DEBUG_INTENT) Log.d(TAG, "Intent is null");
            return;
        }
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String errorMessage = "";

        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra(Constant.LOCATION_DATA_EXTRA);
        List<Address> addresses = null;
        mReceiver = intent.getParcelableExtra(Constant.RECEIVER);

        try {
            //Just a single address.
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = "Service_not_available";
            if (Debug.DEBUG_INTENT) Log.e(TAG, errorMessage, ioException);

        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = "Invalid_lat_long_used";
            if (Debug.DEBUG_INTENT) Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() + ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size() == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = "Address is null";
                if (Debug.DEBUG_INTENT) Log.d(TAG, errorMessage);
            }
            deliverResultToReceiver(Constant.FAILURE_RESULT, errorMessage);
        } else {
            if (Debug.DEBUG_INTENT) Log.d(TAG, "Address_found");
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));

                //for debugging only show each address string
                if (Debug.DEBUG_INTENT) Log.d(TAG, "i=" + Integer.toString(i) + " "
                        + address.getAddressLine(i));
            }

            deliverResultToReceiver(Constant.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"),
                            addressFragments));
        }
    }


    /**
     * send address back to main activity
     *
     * @param resultCode        results code
     * @param message           address
     */
    @SuppressLint("RestrictedApi")
    private void deliverResultToReceiver(int resultCode, String message) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "deliverResultToReceiver");

        Bundle bundle = new Bundle();
        bundle.putString(Constant.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }
}

package au.com.mysites.location;

final class Constant {

    static final int SUCCESS_RESULT = 0;
    static final int FAILURE_RESULT = 1;
    private static final String PACKAGE_NAME = "au.com.mysites.location";
    static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";

    // Used to check user's location settings allow location services to create a LocationRequest
    static final int REQUEST_CHECK_SETTINGS = 10;
    static final int PERMISSION_REQUEST_CODE = 20;

}



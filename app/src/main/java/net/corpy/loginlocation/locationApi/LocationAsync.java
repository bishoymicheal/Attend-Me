package net.corpy.loginlocation.locationApi;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.List;


public class LocationAsync extends AsyncTask<Void, Void, LocationData> {

    private final Geocoder gcd;
    private Location location;
    private String address;
    private final DataReady dataReadyListener;
    private final boolean useLocation;

    public LocationAsync(Geocoder gcd, Location location, DataReady dataReadyListener) {
        this.gcd = gcd;
        this.location = location;
        this.dataReadyListener = dataReadyListener;
        useLocation = true;
    }

    public LocationAsync(Geocoder gcd, String address, DataReady dataReadyListener) {
        this.gcd = gcd;
        this.address = address;
        this.dataReadyListener = dataReadyListener;
        useLocation = false;
    }

//    public JSONObject getLocationInfo(String language, double lat, double lng) {
//
//        HttpURLConnection urlConnection = null;
//        BufferedReader reader = null;
//        String JsonString = null;
//        try {
//
//            URL url = new URL("https://maps.googleapis.com/maps/api/geocode/json?latlng="
//                    + lat + "," + lng + "&language=" + language + "&sensor=true&key=" + BuildConfig.API_KEY);
//            urlConnection = (HttpURLConnection) url.openConnection();
//            urlConnection.setRequestMethod("GET");
//            urlConnection.setUseCaches(true);
//            urlConnection.connect();
//            InputStream inputStream = urlConnection.getInputStream();
//            StringBuffer buffer = new StringBuffer();
//            if (inputStream == null) {
//                return null;
//            }
//            reader = new BufferedReader(new InputStreamReader(inputStream));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                buffer.append(line + "\n");
//            }
//
//            if (buffer.length() == 0) {
//                // Stream was empty.  No point in parsing.
//                return null;
//            }
//            JsonString = buffer.toString();
//        } catch (IOException e) {
//            Log.e("Network Connection ", "Error ", e);
//            return null;
//        } finally {
//            if (urlConnection != null) {
//                urlConnection.disconnect();
//            }
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (final IOException e) {
//                    Log.e("Error", " closing stream", e);
//                }
//            }
//        }
//        JSONObject jsonObject = new JSONObject();
//        try {
//            jsonObject = new JSONObject(JsonString);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        return jsonObject;
//    }

    @Override
    protected void onPostExecute(LocationData locationData) {
        if (dataReadyListener != null) dataReadyListener.dataReady(locationData);
    }

    @Override
    protected LocationData doInBackground(Void... voids) {
        if (useLocation) return getLocationLatLng();
        else return getAddressLatLng();
    }

    private LocationData getLocationLatLng() {
        LocationData locationData = new LocationData();
        List<Address> addresses;
        StringBuilder fullAddress = new StringBuilder();
        locationData.latitude = location.getLatitude();
        locationData.longitude = location.getLongitude();

        try {
            addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                for (int i = 0; i <= addresses.get(0).getMaxAddressLineIndex(); i++) {
                    if (i == 0) fullAddress.append(addresses.get(0).getAddressLine(i));
                    else fullAddress.append("-").append(addresses.get(0).getAddressLine(i));
                }
                locationData.fullAddress = fullAddress.toString();
                locationData.countryCode = addresses.get(0).getCountryCode();
                locationData.countryName = addresses.get(0).getCountryName();
                locationData.featureName = addresses.get(0).getFeatureName();

                locationData.city = addresses.get(0).getLocality();
                locationData.state = addresses.get(0).getAdminArea();
                locationData.postalCode = addresses.get(0).getPostalCode();

            } /*else {
                locationData = getCurrentLocationViaJSON(locationData);
            }*/
        } catch (IOException e) {
//            locationData = getCurrentLocationViaJSON(locationData);
            e.printStackTrace();
        }

        return locationData;
    }

    private LocationData getAddressLatLng() {
        LocationData locationData = new LocationData();
        List<Address> addresses;
        StringBuilder fullAddress = new StringBuilder();
        try {
            addresses = gcd.getFromLocationName(address, 1);
            if (addresses.size() > 0) {
                for (int i = 0; i <= addresses.get(0).getMaxAddressLineIndex(); i++) {
                    if (i == 0) fullAddress.append(addresses.get(0).getAddressLine(i));
                    else fullAddress.append("-").append(addresses.get(0).getAddressLine(i));
                }
                locationData.fullAddress = fullAddress.toString();
                locationData.countryCode = addresses.get(0).getCountryCode();
                locationData.countryName = addresses.get(0).getCountryName();
                locationData.featureName = addresses.get(0).getFeatureName();

                locationData.city = addresses.get(0).getLocality();
                locationData.state = addresses.get(0).getAdminArea();
                locationData.postalCode = addresses.get(0).getPostalCode();
                locationData.latitude = addresses.get(0).getLatitude();
                locationData.longitude = addresses.get(0).getLongitude();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return locationData;
    }

    public interface DataReady {
        void dataReady(LocationData data);
    }


}

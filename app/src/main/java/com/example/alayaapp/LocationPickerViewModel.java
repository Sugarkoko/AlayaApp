package com.example.alayaapp;

import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.maps.model.LatLng;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationPickerViewModel extends ViewModel {

    private static final String TAG = "LocationPickerViewModel";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Address>> geocodeResults = new MutableLiveData<>();
    private final MutableLiveData<String> reverseGeocodeResult = new MutableLiveData<>();

    // Bounding box for Geocoder search bias (Philippines)
    private static final double PH_LOWER_LEFT_LAT = 4.0;
    private static final double PH_LOWER_LEFT_LON = 116.0;
    private static final double PH_UPPER_RIGHT_LAT = 22.0;
    private static final double PH_UPPER_RIGHT_LON = 127.0;

    public LiveData<List<Address>> getGeocodeResults() {
        return geocodeResults;
    }

    public LiveData<String> getReverseGeocodeResult() {
        return reverseGeocodeResult;
    }

    public void searchLocationByName(String query, Geocoder geocoder) {
        executorService.execute(() -> {
            if (geocoder == null) {
                geocodeResults.postValue(null);
                return;
            }
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 10,
                        PH_LOWER_LEFT_LAT, PH_LOWER_LEFT_LON, PH_UPPER_RIGHT_LAT, PH_UPPER_RIGHT_LON);
                geocodeResults.postValue(addresses);
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error for query: " + query, e);
                geocodeResults.postValue(null);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Geocoder illegal argument for bounds: " + query, e);
                try {
                    List<Address> addresses = geocoder.getFromLocationName(query, 10);
                    geocodeResults.postValue(addresses);
                } catch (IOException ioe) {
                    Log.e(TAG, "Geocoder fallback error for query: " + query, ioe);
                    geocodeResults.postValue(null);
                }
            }
        });
    }

    public void reverseGeocode(LatLng point, Geocoder geocoder) {
        executorService.execute(() -> {
            if (geocoder == null) {
                reverseGeocodeResult.postValue("Selected Coordinates");
                return;
            }
            try {
                List<Address> addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    reverseGeocodeResult.postValue(ManualLocationPickerActivity.Helper.getAddressDisplayName(addresses.get(0)));
                } else {
                    reverseGeocodeResult.postValue(String.format(Locale.US, "Lat:%.4f, Lon:%.4f", point.latitude, point.longitude));
                }
            } catch (IOException e) {
                Log.e(TAG, "Reverse geocoding error", e);
                reverseGeocodeResult.postValue(String.format(Locale.US, "Lat:%.4f, Lon:%.4f", point.latitude, point.longitude));
            }
        });
    }
}
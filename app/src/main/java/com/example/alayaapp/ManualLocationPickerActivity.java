package com.example.alayaapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
// import android.preference.PreferenceManager; // No longer needed for OSM config
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alayaapp.databinding.ActivityManualLocationPickerBinding;
import com.example.alayaapp.util.GeoPoint; // Using your local GeoPoint

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
// import com.google.firebase.database.collection.BuildConfig; // Removed if only for OSM User Agent

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManualLocationPickerActivity extends AppCompatActivity implements LocationSuggestionAdapter.OnSuggestionClickListener {
    private static final String TAG = "ManualLocationPicker";
    private ActivityManualLocationPickerBinding binding;
    // private MapView mapView; // OSM MapView removed
    // private Marker selectedLocationMarker; // OSM Marker removed
    private GeoPoint currentSelectedPoint; // Using com.example.alayaapp.util.GeoPoint
    private String currentSelectedName = "Selected Location";

    private FusedLocationProviderClient fusedLocationClientPicker;
    private LocationCallback locationCallbackPicker;
    // private MyLocationNewOverlay myLocationOverlayPicker; // OSM Overlay removed
    private static final int REQUEST_LOCATION_PERMISSION_PICKER = 3;

    private CancellationTokenSource cancellationTokenSource;
    private LocationSuggestionAdapter suggestionAdapter;
    private Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 500;

    // Bounding box for PH geocoding (can be kept if Geocoder supports it well)
    private static final double PH_LOWER_LEFT_LAT = 4.0;
    private static final double PH_LOWER_LEFT_LON = 116.0;
    private static final double PH_UPPER_RIGHT_LAT = 22.0;
    private static final double PH_UPPER_RIGHT_LON = 127.0;

    private boolean isProcessingSuggestionClick = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Context ctx = getApplicationContext(); // Context can be obtained directly if needed
        // Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx)); // OSM Config removed
        // Configuration.getInstance().setUserAgentValue(com.example.alayaapp.BuildConfig.APPLICATION_ID); // OSM User Agent removed

        binding = ActivityManualLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbarManualLocation;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Set Location Manually");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // mapView = binding.mapViewPicker; // mapViewPicker ID will be for the placeholder FrameLayout
        // setupOsmMapPicker(); // Removed

        setupSuggestionRecyclerView();
        fusedLocationClientPicker = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallbackPickerForFallback();
        checkAndRequestPickerPermissions();

        binding.etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                performSearch(binding.etSearchLocation.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });

        binding.etSearchLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isProcessingSuggestionClick) {
                    return;
                }
                searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                String query = s.toString().trim();
                if (query.length() > 2) {
                    binding.btnClearSearch.setVisibility(View.VISIBLE);
                    searchDebounceRunnable = () -> performSearch(query);
                    searchDebounceHandler.postDelayed(searchDebounceRunnable, SEARCH_DEBOUNCE_DELAY_MS);
                } else {
                    binding.btnClearSearch.setVisibility(View.GONE);
                    suggestionAdapter.setSuggestions(null);
                    binding.rvLocationSuggestions.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearchLocation.setText("");
        });

        binding.btnConfirmLocation.setOnClickListener(v -> {
            if (currentSelectedPoint != null && currentSelectedName != null && !currentSelectedName.equals("Selected Location") && !currentSelectedName.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_location_name", currentSelectedName);
                resultIntent.putExtra("selected_latitude", currentSelectedPoint.getLatitude());
                resultIntent.putExtra("selected_longitude", currentSelectedPoint.getLongitude());
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location by search.", Toast.LENGTH_LONG).show();
            }
        });

        // MapEventsReceiver and MapEventsOverlay removed as there's no interactive map
        // The placeholder FrameLayout (map_placeholder_container_picker) can have its own click listener if needed for other purposes
        if (binding.mapPlaceholderContainerPicker != null) {
            binding.mapPlaceholderContainerPicker.setOnClickListener(view -> {
                Toast.makeText(ManualLocationPickerActivity.this, "Tap on map disabled. Please use search.", Toast.LENGTH_SHORT).show();
                hideKeyboard();
                binding.rvLocationSuggestions.setVisibility(View.GONE);
                suggestionAdapter.setSuggestions(null);
            });
        }
    }

    private void setupSuggestionRecyclerView() {
        suggestionAdapter = new LocationSuggestionAdapter(this);
        binding.rvLocationSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLocationSuggestions.setAdapter(suggestionAdapter);
    }

    private void setupLocationCallbackPickerForFallback() {
        locationCallbackPicker = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) return;
                Location location = locationResult.getLastLocation();
                GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "Fallback LocationCallback received: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());

                // No map to animate or check marker status against
                // if (selectedLocationMarker == null && currentSelectedPoint == null) {
                // No map interaction
                // }

                if (fusedLocationClientPicker != null && locationCallbackPicker != null) {
                    Log.d(TAG, "Stopping fallback continuous updates.");
                    fusedLocationClientPicker.removeLocationUpdates(locationCallbackPicker);
                }
            }
        };
    }

    private void startPickerLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "startPickerLocationUpdates: Permissions not granted.");
            return;
        }
        Log.d(TAG, "Attempting to get current location (single request).");
        CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setDurationMillis(10000)
                .build();
        cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClientPicker.getCurrentLocation(currentLocationRequest, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d(TAG, "Successfully got current location (single request): " + location.getLatitude() + ", " + location.getLongitude());
                        // GeoPoint currentLocationGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        // No map to animate or check marker status against
                        // if (selectedLocationMarker == null && currentSelectedPoint == null) {
                        // No map interaction
                        // }
                    } else {
                        Log.d(TAG, "Current location (single request) is null. Attempting fallback.");
                        startBriefContinuousUpdatesAsFallback();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Failed to get current location (single request).", e);
                    startBriefContinuousUpdatesAsFallback();
                });
    }

    private void startBriefContinuousUpdatesAsFallback() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d(TAG, "Starting brief continuous updates as fallback.");
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();
        fusedLocationClientPicker.requestLocationUpdates(locationRequest, locationCallbackPicker, Looper.getMainLooper());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (fusedLocationClientPicker != null && locationCallbackPicker != null) {
                // Check if we still need updates (e.g. user hasn't selected anything)
                // Removed map specific condition "mapView.getZoomLevelDouble() < 10"
                if (currentSelectedPoint == null) {
                    Log.d(TAG, "Timeout for fallback continuous updates, stopping them.");
                    fusedLocationClientPicker.removeLocationUpdates(locationCallbackPicker);
                }
            }
        }, 15000);
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            suggestionAdapter.setSuggestions(null);
            binding.rvLocationSuggestions.setVisibility(View.GONE);
            return;
        }
        if (Geocoder.isPresent()) {
            new GeocodeFromNameTaskAndroid(this).execute(query);
        } else {
            Toast.makeText(this, "Geocoder service not available on this device.", Toast.LENGTH_LONG).show();
            binding.rvLocationSuggestions.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSuggestionClick(Address address) {
        isProcessingSuggestionClick = true;
        hideKeyboard();
        binding.rvLocationSuggestions.setVisibility(View.GONE);
        suggestionAdapter.setSuggestions(null);
        searchDebounceHandler.removeCallbacksAndMessages(null);

        if (address.hasLatitude() && address.hasLongitude()) {
            GeoPoint resultPoint = new GeoPoint(address.getLatitude(), address.getLongitude());
            String displayName = Helper.getAddressDisplayName(address);

            currentSelectedPoint = resultPoint; // Store the selected point
            currentSelectedName = displayName;  // Store the name

            binding.etSearchLocation.setText(displayName);
            binding.etSearchLocation.setSelection(binding.etSearchLocation.getText().length());
            Toast.makeText(this, "Selected: " + displayName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Selected location does not have coordinates.", Toast.LENGTH_SHORT).show();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isProcessingSuggestionClick = false;
        }, 100);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        if (binding.etSearchLocation != null) { // Check if binding is not null
            binding.etSearchLocation.clearFocus();
        }
    }

    private static class GeocodeFromNameTaskAndroid extends AsyncTask<String, Void, List<Address>> {
        private WeakReference<ManualLocationPickerActivity> activityReference;
        private String originalQuery;

        GeocodeFromNameTaskAndroid(ManualLocationPickerActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity != null && !activity.isFinishing()) {
                activity.hideKeyboard();
                originalQuery = activity.binding.etSearchLocation.getText().toString().trim();
            }
        }

        @Override
        protected List<Address> doInBackground(String... params) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing() || !Geocoder.isPresent()) {
                return null;
            }
            String queryForThisTask = params[0];
            Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
            try {
                // Using bounding box for PH if supported by device's Geocoder
                return geocoder.getFromLocationName(queryForThisTask, 10,
                        PH_LOWER_LEFT_LAT, PH_LOWER_LEFT_LON,
                        PH_UPPER_RIGHT_LAT, PH_UPPER_RIGHT_LON);
            } catch (IOException e) {
                Log.e(TAG, "Android Geocoder error for query: " + queryForThisTask, e);
                return null;
            } catch (IllegalArgumentException e) { // Catching potential illegal arg for bounds
                Log.e(TAG, "Android Geocoder illegal argument for bounds with query: " + queryForThisTask, e);
                try { // Fallback without bounds
                    return geocoder.getFromLocationName(queryForThisTask, 10);
                } catch (IOException ioe) {
                    Log.e(TAG, "Android Geocoder fallback error for query: " + queryForThisTask, ioe);
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            String currentTextInBox = activity.binding.etSearchLocation.getText().toString().trim();
            if (originalQuery != null && !originalQuery.equals(currentTextInBox) && !activity.isProcessingSuggestionClick) {
                Log.d(TAG, "Stale results for query '" + originalQuery + "', current box is '" + currentTextInBox + "'. Discarding.");
                return;
            }

            if (addresses == null || addresses.isEmpty()) {
                activity.suggestionAdapter.setSuggestions(null);
                if (!activity.isProcessingSuggestionClick) {
                    activity.binding.rvLocationSuggestions.setVisibility(View.GONE);
                }
            } else {
                List<Address> validAddresses = new ArrayList<>();
                for (Address adr : addresses) {
                    if (adr.hasLatitude() && adr.hasLongitude()) {
                        validAddresses.add(adr);
                    }
                }
                activity.suggestionAdapter.setSuggestions(validAddresses);
                if (!validAddresses.isEmpty() && currentTextInBox.length() > 2 && !activity.isProcessingSuggestionClick) {
                    activity.binding.rvLocationSuggestions.setVisibility(View.VISIBLE);
                } else if (!activity.isProcessingSuggestionClick) {
                    activity.binding.rvLocationSuggestions.setVisibility(View.GONE);
                }
            }
        }
    }

    // setupOsmMapPicker() method removed entirely

    private void checkAndRequestPickerPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_PICKER);
        } else {
            // myLocationOverlayPicker.enableMyLocation(); // OSM Overlay removed
            startPickerLocationUpdates(); // Still attempt to get current location data for other uses
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_PICKER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // myLocationOverlayPicker.enableMyLocation(); // OSM Overlay removed
                startPickerLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied. Cannot auto-get current location.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // updateMarker() method removed entirely

    private static class ReverseGeocodeTask extends AsyncTask<GeoPoint, Void, String> {
        private WeakReference<ManualLocationPickerActivity> activityReference;

        ReverseGeocodeTask(ManualLocationPickerActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(GeoPoint... params) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing() || !Geocoder.isPresent()) {
                return "Pinned Location (Geocoder N/A)"; // Should not be called without map tap
            }
            GeoPoint pointToReverse = params[0];
            Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(pointToReverse.getLatitude(), pointToReverse.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    return Helper.getAddressDisplayName(addresses.get(0));
                }
            } catch (IOException e) {
                Log.e(TAG, "Reverse geocoding error (Android Geocoder)", e);
            }
            return "Pinned Location (Address not found)";
        }

        @Override
        protected void onPostExecute(String addressName) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity != null && !activity.isFinishing() && addressName != null) {
                activity.isProcessingSuggestionClick = true;
                activity.currentSelectedName = addressName;
                // currentSelectedPoint would have been set by the (now removed) map tap

                // No marker or map to update
                // if (activity.selectedLocationMarker != null && activity.currentSelectedPoint != null) {
                //    activity.selectedLocationMarker.setTitle(addressName);
                //    activity.mapView.invalidate();
                // }
                activity.binding.etSearchLocation.setText(addressName);
                activity.binding.etSearchLocation.setSelection(addressName.length());
                Toast.makeText(activity, "Location Updated (from tap): " + addressName, Toast.LENGTH_SHORT).show(); // Feedback
                new Handler(Looper.getMainLooper()).postDelayed(() -> activity.isProcessingSuggestionClick = false, 100);
            }
        }
    }

    public static class Helper {
        public static String getAddressDisplayName(Address address) {
            if (address == null) return "Unknown Location";

            StringBuilder displayNameBuilder = new StringBuilder();

            // Prioritize AddressLine(0) if available and seems reasonable
            String addressLine0 = address.getAddressLine(0);
            if (addressLine0 != null && !addressLine0.isEmpty()) {
                // Basic check to avoid just "Latitude, Longitude"
                if (!addressLine0.matches("^-?[0-9]+\\.[0-9]+, -?[0-9]+\\.[0-9]+$")) {
                    return addressLine0;
                }
            }

            // Feature name (often the POI name)
            String featureName = address.getFeatureName();
            if (featureName != null && !featureName.isEmpty() && !featureName.matches("\\d+.*") /* Avoid numbers like "123" */) {
                displayNameBuilder.append(featureName);
            }

            // Thoroughfare (Street Name)
            String thoroughfare = address.getThoroughfare();
            if (thoroughfare != null && !thoroughfare.isEmpty()) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(thoroughfare)) {
                    displayNameBuilder.append(", ");
                }
                if (!displayNameBuilder.toString().contains(thoroughfare)) {
                    displayNameBuilder.append(thoroughfare);
                }
                // SubThoroughfare (Street Number)
                String subThoroughfare = address.getSubThoroughfare();
                if (subThoroughfare != null && !subThoroughfare.isEmpty()) {
                    displayNameBuilder.append(" ").append(subThoroughfare);
                }
            }

            // SubLocality (Neighborhood/District)
            String subLocality = address.getSubLocality();
            if (subLocality != null && !subLocality.isEmpty()) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(subLocality)) {
                    displayNameBuilder.append(", ");
                }
                if (!displayNameBuilder.toString().contains(subLocality)) {
                    displayNameBuilder.append(subLocality);
                }
            }

            // Locality (City/Town)
            String locality = address.getLocality();
            if (locality != null && !locality.isEmpty()) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(locality)) {
                    displayNameBuilder.append(", ");
                }
                if (!displayNameBuilder.toString().contains(locality)) {
                    displayNameBuilder.append(locality);
                }
            }

            // AdminArea (State/Province)
            String adminArea = address.getAdminArea();
            if (adminArea != null && !adminArea.isEmpty()) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(adminArea)) {
                    displayNameBuilder.append(", ");
                }
                // Avoid appending if locality is same as adminArea (e.g. "Manila, Metro Manila")
                if (!displayNameBuilder.toString().contains(adminArea) && (locality == null || !locality.equalsIgnoreCase(adminArea))) {
                    displayNameBuilder.append(adminArea);
                }
            }

            // CountryName
            String countryName = address.getCountryName();
            if (countryName != null && !countryName.isEmpty()) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(countryName)) {
                    displayNameBuilder.append(", ");
                }
                if (!displayNameBuilder.toString().contains(countryName)) {
                    displayNameBuilder.append(countryName);
                }
            }

            if (displayNameBuilder.length() == 0) {
                if (address.hasLatitude() && address.hasLongitude()) {
                    return "Location (Lat: " + String.format(Locale.US, "%.4f", address.getLatitude()) +
                            ", Lon: " + String.format(Locale.US, "%.4f", address.getLongitude()) + ")";
                } else {
                    return "Unknown Location";
                }
            }
            return displayNameBuilder.toString();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        // if (mapView != null) mapView.onResume(); // OSM MapView removed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // if(myLocationOverlayPicker != null && !myLocationOverlayPicker.isMyLocationEnabled()){ // OSM Overlay removed
            // myLocationOverlayPicker.enableMyLocation();
            // }
            if (currentSelectedPoint == null) { // Only start if no location is yet selected
                startPickerLocationUpdates();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // if (mapView != null) mapView.onPause(); // OSM MapView removed
        if (fusedLocationClientPicker != null ) {
            if (locationCallbackPicker != null) {
                fusedLocationClientPicker.removeLocationUpdates(locationCallbackPicker);
            }
            if (cancellationTokenSource != null) {
                cancellationTokenSource.cancel();
            }
        }
        // if(myLocationOverlayPicker != null) myLocationOverlayPicker.disableMyLocation(); // OSM Overlay removed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // if (mapView != null) mapView.onDetach(); // OSM MapView removed
        searchDebounceHandler.removeCallbacksAndMessages(null);
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
        }
    }
}
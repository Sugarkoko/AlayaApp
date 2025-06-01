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
import android.text.Editable;
import android.text.TextUtils;
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
// No longer need com.example.alayaapp.util.GeoPoint if using LatLng directly
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManualLocationPickerActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        LocationSuggestionAdapter.OnSuggestionClickListener,
        GoogleMap.OnMapClickListener {

    private static final String TAG = "ManualLocationPicker";
    private ActivityManualLocationPickerBinding binding;
    private GoogleMap mMap;
    private Marker selectedLocationMarker;
    private LatLng currentSelectedPoint; // Use Google's LatLng
    private String currentSelectedName = "";

    private FusedLocationProviderClient fusedLocationClientPicker;
    private static final int REQUEST_LOCATION_PERMISSION_PICKER = 3;

    private LocationSuggestionAdapter suggestionAdapter;
    private Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 700;


    private static final double PH_LOWER_LEFT_LAT = 4.0;
    private static final double PH_LOWER_LEFT_LON = 116.0;
    private static final double PH_UPPER_RIGHT_LAT = 22.0;
    private static final double PH_UPPER_RIGHT_LON = 127.0;

    private boolean isUserInteractingWithSearch = false; // To manage text updates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManualLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbarManualLocation;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Set Location Manually");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_picker_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSuggestionRecyclerView();
        fusedLocationClientPicker = LocationServices.getFusedLocationProviderClient(this);

        setupSearchFunctionality();

        binding.btnConfirmLocation.setOnClickListener(v -> {
            if (currentSelectedPoint != null && !TextUtils.isEmpty(currentSelectedName)) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_location_name", currentSelectedName);
                resultIntent.putExtra("selected_latitude", currentSelectedPoint.latitude);
                resultIntent.putExtra("selected_longitude", currentSelectedPoint.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location by tapping the map or searching.", Toast.LENGTH_LONG).show();
            }
        });

        binding.fabPickerMyLocation.setOnClickListener(v -> goToMyCurrentLocation());
    }

    private void setupSearchFunctionality() {
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
                if (!isUserInteractingWithSearch) return; // Prevent recursive calls when setting text programmatically

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

        binding.etSearchLocation.setOnFocusChangeListener((v, hasFocus) -> {
            isUserInteractingWithSearch = hasFocus;
            if (!hasFocus) { // Hide suggestions if search loses focus and no suggestion clicked
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (binding.rvLocationSuggestions.getVisibility() == View.VISIBLE && !isFinishing()) {

                    }
                }, 200); // Small delay
            } else {
                isUserInteractingWithSearch = true; // Ensure this is true when gaining focus
            }
        });


        binding.btnClearSearch.setOnClickListener(v -> {
            isUserInteractingWithSearch = true; // User initiated clear
            binding.etSearchLocation.setText("");
            isUserInteractingWithSearch = false;
            suggestionAdapter.setSuggestions(null);
            binding.rvLocationSuggestions.setVisibility(View.GONE);
            binding.btnClearSearch.setVisibility(View.GONE);
        });
    }


    private void setupSuggestionRecyclerView() {
        suggestionAdapter = new LocationSuggestionAdapter(this);
        binding.rvLocationSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLocationSuggestions.setAdapter(suggestionAdapter);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.getUiSettings().setZoomControlsEnabled(true); // Show zoom controls

        // Default map position (e.g., center of Philippines)
        LatLng philippinesCenter = new LatLng(12.8797, 121.7740);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(philippinesCenter, 5.5f));

        checkAndRequestPickerPermissions(true); // Attempt to get initial location
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        hideKeyboard();
        binding.rvLocationSuggestions.setVisibility(View.GONE);
        updateSelectedLocation(latLng, null, true); // Name will be fetched by reverse geocoding
    }

    private void updateSelectedLocation(LatLng point, String name, boolean performReverseGeocode) {
        currentSelectedPoint = point;

        if (selectedLocationMarker != null) {
            selectedLocationMarker.remove();
        }
        MarkerOptions markerOptions = new MarkerOptions().position(point);
        if (name != null && !name.isEmpty()) {
            markerOptions.title(name);
            currentSelectedName = name;
            setSearchText(name); // Update search text without triggering new search
        }
        selectedLocationMarker = mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15f));

        if (performReverseGeocode && name == null) { // Only reverse geocode if name isn't already provided (e.g. from search)
            setSearchText("Fetching address..."); // Placeholder
            new ReverseGeocodeTaskForGoogleMap(this).execute(point);
        }

        binding.btnConfirmLocation.setEnabled(true);
    }

    private void setSearchText(String text) {
        isUserInteractingWithSearch = false; // Programmatic change
        binding.etSearchLocation.setText(text);
        if (!TextUtils.isEmpty(text)) {
            binding.etSearchLocation.setSelection(text.length());
            binding.btnClearSearch.setVisibility(View.VISIBLE);
        } else {
            binding.btnClearSearch.setVisibility(View.GONE);
        }

        if (binding.etSearchLocation.hasFocus()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> isUserInteractingWithSearch = true, 50);
        }
    }


    private void performSearch(String query) {
        if (query.isEmpty()) {
            suggestionAdapter.setSuggestions(null);
            binding.rvLocationSuggestions.setVisibility(View.GONE);
            return;
        }
        if (Geocoder.isPresent()) {
            new GeocodeFromNameTaskAndroid(this, query).execute(query);
        } else {
            Toast.makeText(this, "Geocoder service not available.", Toast.LENGTH_LONG).show();
            binding.rvLocationSuggestions.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSuggestionClick(Address address) {
        hideKeyboard();
        binding.rvLocationSuggestions.setVisibility(View.GONE);
        isUserInteractingWithSearch = false; // About to set text programmatically

        if (address.hasLatitude() && address.hasLongitude()) {
            LatLng resultPoint = new LatLng(address.getLatitude(), address.getLongitude());
            String displayName = ManualLocationPickerActivity.Helper.getAddressDisplayName(address);
            updateSelectedLocation(resultPoint, displayName, false); // Name is known, no reverse geocode needed
        } else {
            Toast.makeText(this, "Selected location does not have coordinates.", Toast.LENGTH_SHORT).show();
        }
        if (binding.etSearchLocation.hasFocus()) { // If still has focus, allow user to type again
            new Handler(Looper.getMainLooper()).postDelayed(() -> isUserInteractingWithSearch = true, 50);
        }
    }


    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this); // If no view has focus, create a new one
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        if (binding.etSearchLocation != null) {
            binding.etSearchLocation.clearFocus();
        }
    }

    private static class GeocodeFromNameTaskAndroid extends AsyncTask<String, Void, List<Address>> {
        private WeakReference<ManualLocationPickerActivity> activityReference;
        private String originalQueryForThisTask; // Store the query this task was started for

        GeocodeFromNameTaskAndroid(ManualLocationPickerActivity activity, String query) {
            activityReference = new WeakReference<>(activity);
            this.originalQueryForThisTask = query;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity != null && !activity.isFinishing()) {
                // activity.hideKeyboard(); // Keyboard hidden by search action or focus change
            }
        }

        @Override
        protected List<Address> doInBackground(String... params) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing() || !Geocoder.isPresent()) return null;

            Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
            try {
                // Using bounding box for PH if supported by device's Geocoder
                return geocoder.getFromLocationName(originalQueryForThisTask, 10,
                        PH_LOWER_LEFT_LAT, PH_LOWER_LEFT_LON, PH_UPPER_RIGHT_LAT, PH_UPPER_RIGHT_LON);
            } catch (IOException e) {
                Log.e(TAG, "Android Geocoder error for query: " + originalQueryForThisTask, e);
                return null;
            } catch (IllegalArgumentException e) { // Catching potential illegal arg for bounds
                Log.e(TAG, "Android Geocoder illegal argument for bounds: " + originalQueryForThisTask, e);
                try { // Fallback without bounds
                    return geocoder.getFromLocationName(originalQueryForThisTask, 10);
                } catch (IOException ioe) {
                    Log.e(TAG, "Android Geocoder fallback error for query: " + originalQueryForThisTask, ioe);
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            // Check if the search text has changed since this task was started
            String currentSearchText = activity.binding.etSearchLocation.getText().toString().trim();
            if (!activity.isUserInteractingWithSearch && !originalQueryForThisTask.equals(currentSearchText) && !currentSearchText.equals("Fetching address...")) {
                if (activity.binding.rvLocationSuggestions.getVisibility() == View.VISIBLE) {

                } else {
                    Log.d(TAG, "Geocode result for '" + originalQueryForThisTask + "' is stale. Current: '" + currentSearchText + "'. Discarding.");
                    return;
                }
            }


            if (addresses == null || addresses.isEmpty()) {
                activity.suggestionAdapter.setSuggestions(null);
                activity.binding.rvLocationSuggestions.setVisibility(View.GONE);
            } else {
                List<Address> validAddresses = new ArrayList<>();
                for (Address adr : addresses) {
                    if (adr.hasLatitude() && adr.hasLongitude()) {
                        validAddresses.add(adr);
                    }
                }
                activity.suggestionAdapter.setSuggestions(validAddresses);
                if (!validAddresses.isEmpty() && activity.binding.etSearchLocation.hasFocus()) { // Only show if search still has focus
                    activity.binding.rvLocationSuggestions.setVisibility(View.VISIBLE);
                } else {
                    activity.binding.rvLocationSuggestions.setVisibility(View.GONE);
                }
            }
        }
    }


    private void checkAndRequestPickerPermissions(boolean centerOnMapIfGranted) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_PICKER);
        } else {
            if (mMap != null) {
                try {
                    mMap.setMyLocationEnabled(true); // Show blue dot
                    mMap.getUiSettings().setMyLocationButtonEnabled(false); // We use custom FAB
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException on setMyLocationEnabled: " + e.getMessage());
                }
                if (centerOnMapIfGranted) {
                    goToMyCurrentLocation();
                }
            }
        }
    }

    private void goToMyCurrentLocation() {
        if (mMap == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            } catch (SecurityException e) { Log.e(TAG, "SecurityException: " + e.getMessage()); return;}

            fusedLocationClientPicker.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f));
                            // User still needs to tap the map to select this as the chosen location
                            Toast.makeText(this, "Map centered on your location. Tap to select.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Could not get current location.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e ->
                            Toast.makeText(this, "Failed to get current location.", Toast.LENGTH_SHORT).show());
        } else {
            checkAndRequestPickerPermissions(true); // Ask for permission then try again
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_PICKER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    try {
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    } catch (SecurityException e) {Log.e(TAG, "Security Exception after permission grant: " + e.getMessage());}
                }
                goToMyCurrentLocation(); // Center map now that permission is granted
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Reverse Geocoding for Google Map Taps
    private static class ReverseGeocodeTaskForGoogleMap extends AsyncTask<LatLng, Void, String> {
        private WeakReference<ManualLocationPickerActivity> activityReference;

        ReverseGeocodeTaskForGoogleMap(ManualLocationPickerActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(LatLng... params) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing() || !Geocoder.isPresent()) {
                return "Selected Coordinates";
            }
            LatLng pointToReverse = params[0];
            Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(pointToReverse.latitude, pointToReverse.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    return Helper.getAddressDisplayName(addresses.get(0));
                }
            } catch (IOException e) {
                Log.e(TAG, "Reverse geocoding error (Google Map Tap)", e);
            }
            return String.format(Locale.US, "Lat:%.4f, Lon:%.4f", pointToReverse.latitude, pointToReverse.longitude);
        }

        @Override
        protected void onPostExecute(String addressName) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity != null && !activity.isFinishing() && addressName != null) {
                activity.currentSelectedName = addressName;
                activity.setSearchText(addressName); // Update search text
                if (activity.selectedLocationMarker != null) {
                    activity.selectedLocationMarker.setTitle(addressName);

                }
            }
        }
    }


    // Helper class remains the same from your previous version
    public static class Helper {
        public static String getAddressDisplayName(Address address) {
            if (address == null) return "Unknown Location";
            StringBuilder displayNameBuilder = new StringBuilder();
            String addressLine0 = address.getAddressLine(0);
            if (addressLine0 != null && !addressLine0.isEmpty() && !addressLine0.matches("^-?[0-9]+\\.[0-9]+, -?[0-9]+\\.[0-9]+$")) {
                return addressLine0;
            }
            String featureName = address.getFeatureName();
            if (featureName != null && !featureName.isEmpty() && !featureName.matches("\\d+.*")) {
                displayNameBuilder.append(featureName);
            }
            String thoroughfare = address.getThoroughfare();
            if (thoroughfare != null && !thoroughfare.isEmpty()) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(thoroughfare)) displayNameBuilder.append(", ");
                if (!displayNameBuilder.toString().contains(thoroughfare)) displayNameBuilder.append(thoroughfare);
                String subThoroughfare = address.getSubThoroughfare();
                if (subThoroughfare != null && !subThoroughfare.isEmpty()) displayNameBuilder.append(" ").append(subThoroughfare);
            }
            String subLocality = address.getSubLocality();
            if (subLocality != null && !subLocality.isEmpty()) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(subLocality)) displayNameBuilder.append(", ");
                if (!displayNameBuilder.toString().contains(subLocality)) displayNameBuilder.append(subLocality);
            }
            String locality = address.getLocality();
            if (locality != null && !locality.isEmpty()) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(locality)) displayNameBuilder.append(", ");
                if (!displayNameBuilder.toString().contains(locality)) displayNameBuilder.append(locality);
            }
            String adminArea = address.getAdminArea();
            if (adminArea != null && !adminArea.isEmpty()) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(adminArea)) displayNameBuilder.append(", ");
                if (!displayNameBuilder.toString().contains(adminArea) && (locality == null || !locality.equalsIgnoreCase(adminArea))) {
                    displayNameBuilder.append(adminArea);
                }
            }
            String countryName = address.getCountryName();
            if (countryName != null && !countryName.isEmpty()) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(countryName)) displayNameBuilder.append(", ");
                if (!displayNameBuilder.toString().contains(countryName)) displayNameBuilder.append(countryName);
            }
            if (displayNameBuilder.length() == 0) {
                if (address.hasLatitude() && address.hasLongitude()) {
                    return "Location (Lat: " + String.format(Locale.US, "%.4f", address.getLatitude()) + ", Lon: " + String.format(Locale.US, "%.4f", address.getLongitude()) + ")";
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

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchDebounceHandler.removeCallbacksAndMessages(null);

    }
}
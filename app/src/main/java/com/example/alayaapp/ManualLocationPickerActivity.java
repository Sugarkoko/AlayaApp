package com.example.alayaapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import androidx.lifecycle.ViewModelProvider; // New import
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alayaapp.databinding.ActivityManualLocationPickerBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManualLocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback, LocationSuggestionAdapter.OnSuggestionClickListener, GoogleMap.OnMapClickListener {

    private static final String TAG = "ManualLocationPicker";
    // NEW: Constants for intent extras
    public static final String EXTRA_INITIAL_LAT = "initial_latitude";
    public static final String EXTRA_INITIAL_LON = "initial_longitude";
    public static final String EXTRA_INITIAL_NAME = "initial_location_name";

    private ActivityManualLocationPickerBinding binding;
    private GoogleMap mMap;
    private Marker selectedLocationMarker;
    private LatLng currentSelectedPoint;
    private String currentSelectedName = "";
    private FusedLocationProviderClient fusedLocationClientPicker;
    private static final int REQUEST_LOCATION_PERMISSION_PICKER = 3;

    private LocationSuggestionAdapter suggestionAdapter;
    private Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 700;
    private boolean isUserInteractingWithSearch = false;

    private Geocoder geocoder; // Geocoder instance


    private LocationPickerViewModel viewModel;


    private LatLng initialPoint = null;
    private String initialName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  Initialize ViewModel ---
        viewModel = new ViewModelProvider(this).get(LocationPickerViewModel.class);

        binding = ActivityManualLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbarManualLocation;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Set Location Manually");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        //  Read initial location data from intent
        if (getIntent().hasExtra(EXTRA_INITIAL_LAT)) {
            double lat = getIntent().getDoubleExtra(EXTRA_INITIAL_LAT, 0.0);
            double lon = getIntent().getDoubleExtra(EXTRA_INITIAL_LON, 0.0);
            initialName = getIntent().getStringExtra(EXTRA_INITIAL_NAME);
            if (lat != 0.0 && lon != 0.0) {
                initialPoint = new LatLng(lat, lon);
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_picker_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSuggestionRecyclerView();
        fusedLocationClientPicker = LocationServices.getFusedLocationProviderClient(this);

        if (Geocoder.isPresent()) {
            geocoder = new Geocoder(this, Locale.getDefault());
        } else {
            Toast.makeText(this, "Geocoder service not available.", Toast.LENGTH_LONG).show();
        }

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


        setupObservers();
    }

    private void setupObservers() {
        // Observer for geocoding (search) results
        viewModel.getGeocodeResults().observe(this, addresses -> {
            String currentSearchText = binding.etSearchLocation.getText().toString().trim();
            if (addresses == null || addresses.isEmpty()) {
                suggestionAdapter.setSuggestions(null);
                binding.rvLocationSuggestions.setVisibility(View.GONE);
            } else {
                List<Address> validAddresses = new ArrayList<>();
                for (Address adr : addresses) {
                    if (adr.hasLatitude() && adr.hasLongitude()) {
                        validAddresses.add(adr);
                    }
                }
                suggestionAdapter.setSuggestions(validAddresses);
                if (!validAddresses.isEmpty() && binding.etSearchLocation.hasFocus()) {
                    binding.rvLocationSuggestions.setVisibility(View.VISIBLE);
                } else {
                    binding.rvLocationSuggestions.setVisibility(View.GONE);
                }
            }
        });

        // Observer for reverse geocoding results
        viewModel.getReverseGeocodeResult().observe(this, addressName -> {
            if (addressName != null) {
                currentSelectedName = addressName;
                setSearchText(addressName);
                if (selectedLocationMarker != null) {
                    selectedLocationMarker.setTitle(addressName);
                }
            }
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            suggestionAdapter.setSuggestions(null);
            binding.rvLocationSuggestions.setVisibility(View.GONE);
            return;
        }
        if (geocoder != null) {
            //  Call ViewModel
            viewModel.searchLocationByName(query, geocoder);
        }
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
            setSearchText(name);
        }

        selectedLocationMarker = mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15f));

        if (performReverseGeocode && name == null) {
            setSearchText("Fetching address...");
            // --- REFACTORED: Call ViewModel ---
            if (geocoder != null) {
                viewModel.reverseGeocode(point, geocoder);
            }
        }
        binding.btnConfirmLocation.setEnabled(true);
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
                if (!isUserInteractingWithSearch) return;

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
            if (!hasFocus) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (binding.rvLocationSuggestions.getVisibility() == View.VISIBLE && !isFinishing()) {
                        // binding.rvLocationSuggestions.setVisibility(View.GONE);
                    }
                }, 200);
            } else {
                isUserInteractingWithSearch = true;
            }
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            isUserInteractingWithSearch = true;
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
        mMap.getUiSettings().setZoomControlsEnabled(true);


        if (initialPoint != null) {
            // A previous manual location was passed in, so we use it.
            updateSelectedLocation(initialPoint, initialName, false); // false = don't reverse geocode
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPoint, 15f));
            checkAndRequestPickerPermissions(false); // Enable location layer but don't center on it
        } else {
            // No previous location, so use the default behavior
            LatLng philippinesCenter = new LatLng(12.8797, 121.7740);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(philippinesCenter, 5.5f));
            checkAndRequestPickerPermissions(true); // Center on GPS if permission is granted
        }
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        hideKeyboard();
        binding.rvLocationSuggestions.setVisibility(View.GONE);
        updateSelectedLocation(latLng, null, true);
    }

    private void setSearchText(String text) {
        isUserInteractingWithSearch = false;
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

    @Override
    public void onSuggestionClick(Address address) {
        hideKeyboard();
        binding.rvLocationSuggestions.setVisibility(View.GONE);
        isUserInteractingWithSearch = false;

        if (address.hasLatitude() && address.hasLongitude()) {
            LatLng resultPoint = new LatLng(address.getLatitude(), address.getLongitude());
            String displayName = ManualLocationPickerActivity.Helper.getAddressDisplayName(address);
            updateSelectedLocation(resultPoint, displayName, false);
        } else {
            Toast.makeText(this, "Selected location does not have coordinates.", Toast.LENGTH_SHORT).show();
        }

        if (binding.etSearchLocation.hasFocus()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> isUserInteractingWithSearch = true, 50);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        if (binding.etSearchLocation != null) {
            binding.etSearchLocation.clearFocus();
        }
    }

    private void checkAndRequestPickerPermissions(boolean centerOnMapIfGranted) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_PICKER);
        } else {
            if (mMap != null) {
                try {
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + e.getMessage());
                return;
            }

            fusedLocationClientPicker.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f));
                            Toast.makeText(this, "Map centered on your location. Tap to select.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Could not get current location.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e -> Toast.makeText(this, "Failed to get current location.", Toast.LENGTH_SHORT).show());
        } else {
            checkAndRequestPickerPermissions(true);
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
                    } catch (SecurityException e) {
                        Log.e(TAG, "Security Exception after permission grant: " + e.getMessage());
                    }
                }
                // Only go to current location if an initial point wasn't already set
                if (initialPoint == null) {
                    goToMyCurrentLocation();
                }
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

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
                if (subThoroughfare != null && !subThoroughfare.isEmpty())
                    displayNameBuilder.append(" ").append(subThoroughfare);
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
    protected void onDestroy() {
        super.onDestroy();
        searchDebounceHandler.removeCallbacksAndMessages(null);
    }
}
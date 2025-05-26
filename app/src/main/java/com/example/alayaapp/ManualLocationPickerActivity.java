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
import android.preference.PreferenceManager;
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
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.database.collection.BuildConfig;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManualLocationPickerActivity extends AppCompatActivity implements LocationSuggestionAdapter.OnSuggestionClickListener {

    private static final String TAG = "ManualLocationPicker";
    private ActivityManualLocationPickerBinding binding;
    private MapView mapView;
    private Marker selectedLocationMarker;
    private GeoPoint currentSelectedPoint;
    private String currentSelectedName = "Selected Location";

    private FusedLocationProviderClient fusedLocationClientPicker;
    private LocationCallback locationCallbackPicker;
    private MyLocationNewOverlay myLocationOverlayPicker;
    private static final int REQUEST_LOCATION_PERMISSION_PICKER = 3;
    private CancellationTokenSource cancellationTokenSource;


    private LocationSuggestionAdapter suggestionAdapter;
    private Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 500;

    private static final double PH_LOWER_LEFT_LAT = 4.0;
    private static final double PH_LOWER_LEFT_LON = 116.0;
    private static final double PH_UPPER_RIGHT_LAT = 22.0;
    private static final double PH_UPPER_RIGHT_LON = 127.0;

    private boolean isProcessingSuggestionClick = false; // Flag to manage TextWatcher behavior

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        binding = ActivityManualLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbarManualLocation;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Set Location Manually");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        mapView = binding.mapViewPicker;
        setupOsmMapPicker();
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
                if (isProcessingSuggestionClick) { // If text is being set by a suggestion click, skip auto-search
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
            if (currentSelectedPoint != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_location_name", currentSelectedName);
                resultIntent.putExtra("selected_latitude", currentSelectedPoint.getLatitude());
                resultIntent.putExtra("selected_longitude", currentSelectedPoint.getLongitude());
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location on the map or by search.", Toast.LENGTH_LONG).show();
            }
        });

        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                hideKeyboard();
                binding.rvLocationSuggestions.setVisibility(View.GONE);
                suggestionAdapter.setSuggestions(null);
                currentSelectedPoint = p;
                updateMarker(p, "Fetching address...");
                new ReverseGeocodeTask(ManualLocationPickerActivity.this).execute(p);
                return true;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        };
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(0, mapEventsOverlay);
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
                if (selectedLocationMarker == null && currentSelectedPoint == null) {
                    mapView.getController().animateTo(currentLocation);
                    mapView.getController().setZoom(15.0);
                }
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
                        GeoPoint currentLocationGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        if (selectedLocationMarker == null && currentSelectedPoint == null) {
                            mapView.getController().animateTo(currentLocationGeoPoint);
                            mapView.getController().setZoom(15.0);
                        }
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
                if (selectedLocationMarker == null && currentSelectedPoint == null && mapView.getZoomLevelDouble() < 10) {
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
        isProcessingSuggestionClick = true; // Set flag before any action that might trigger TextWatcher

        hideKeyboard();
        binding.rvLocationSuggestions.setVisibility(View.GONE);
        suggestionAdapter.setSuggestions(null);
        searchDebounceHandler.removeCallbacksAndMessages(null); // Cancel any pending searches immediately

        if (address.hasLatitude() && address.hasLongitude()) {
            GeoPoint resultPoint = new GeoPoint(address.getLatitude(), address.getLongitude());
            String displayName = Helper.getAddressDisplayName(address);
            updateMarker(resultPoint, displayName); // Sets currentSelectedName

            binding.etSearchLocation.setText(displayName); // This will trigger TextWatcher, but flag is set
            binding.etSearchLocation.setSelection(binding.etSearchLocation.getText().length());
        } else {
            Toast.makeText(this, "Selected location does not have coordinates.", Toast.LENGTH_SHORT).show();
        }
        // Reset flag after a short delay to allow TextWatcher to process the setText
        // without triggering a new search for the just-selected item.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isProcessingSuggestionClick = false;
        }, 100); // A small delay, adjust if needed
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        binding.etSearchLocation.clearFocus();
    }

    private static class GeocodeFromNameTaskAndroid extends AsyncTask<String, Void, List<Address>> {
        private WeakReference<ManualLocationPickerActivity> activityReference;
        private String originalQuery; // Store the query that initiated this task

        GeocodeFromNameTaskAndroid(ManualLocationPickerActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity != null && !activity.isFinishing()) {
                activity.hideKeyboard();
                originalQuery = activity.binding.etSearchLocation.getText().toString().trim(); // Capture query at task start
            }
        }

        @Override
        protected List<Address> doInBackground(String... params) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing() || !Geocoder.isPresent()) {
                return null;
            }
            // Use the passed param as the query, not necessarily what's currently in EditText
            String queryForThisTask = params[0];
            Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
            try {
                return geocoder.getFromLocationName(queryForThisTask, 10,
                        PH_LOWER_LEFT_LAT, PH_LOWER_LEFT_LON,
                        PH_UPPER_RIGHT_LAT, PH_UPPER_RIGHT_LON);
            } catch (IOException e) {
                Log.e(TAG, "Android Geocoder error for query: " + queryForThisTask, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            // Only update suggestions if the EditText content hasn't significantly changed
            // from the query that initiated THIS specific AsyncTask. This helps prevent
            // stale results from overwriting newer ones if the user types fast.
            String currentTextInBox = activity.binding.etSearchLocation.getText().toString().trim();
            if (originalQuery != null && !originalQuery.equals(currentTextInBox) && !activity.isProcessingSuggestionClick) {
                // User has typed something new while this task was running for an older query.
                // Or a suggestion was just clicked. Don't show these stale results.
                Log.d(TAG, "Stale results for query '" + originalQuery + "', current box is '" + currentTextInBox + "'. Discarding.");
                return;
            }


            if (addresses == null || addresses.isEmpty()) {
                activity.suggestionAdapter.setSuggestions(null);
                // Only hide if not processing a click (click already hides it)
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

                // Show suggestions if there are valid results, query is long enough,
                // and we are not in the middle of processing a suggestion click (which handles its own hiding)
                if (!validAddresses.isEmpty() && currentTextInBox.length() > 2 && !activity.isProcessingSuggestionClick) {
                    activity.binding.rvLocationSuggestions.setVisibility(View.VISIBLE);
                } else if (!activity.isProcessingSuggestionClick) { // Ensure it's hidden if no valid results or query too short
                    activity.binding.rvLocationSuggestions.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setupOsmMapPicker() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        IMapController mapController = mapView.getController();
        mapController.setZoom(6.0);
        mapController.setCenter(new GeoPoint(12.8797, 121.7740));


        myLocationOverlayPicker = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlayPicker.disableFollowLocation();
        mapView.getOverlays().add(myLocationOverlayPicker);
    }

    private void checkAndRequestPickerPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_PICKER);
        } else {
            myLocationOverlayPicker.enableMyLocation();
            startPickerLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_PICKER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                myLocationOverlayPicker.enableMyLocation();
                startPickerLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied. Cannot auto-center map.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateMarker(GeoPoint point, String title) {
        currentSelectedName = title; // This is important for the logic in onPostExecute
        currentSelectedPoint = point;

        if (selectedLocationMarker == null) {
            selectedLocationMarker = new Marker(mapView);
            selectedLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(selectedLocationMarker);
        }
        selectedLocationMarker.setPosition(point);
        selectedLocationMarker.setTitle(title);
        mapView.getController().animateTo(point);
        if (mapView.getZoomLevelDouble() < 15.0) {
            mapView.getController().setZoom(16.0);
        }
        mapView.invalidate();
    }

    private static class ReverseGeocodeTask extends AsyncTask<GeoPoint, Void, String> {
        private WeakReference<ManualLocationPickerActivity> activityReference;

        ReverseGeocodeTask(ManualLocationPickerActivity activity) {
            activityReference = new WeakReference<>(activity);
        }
        @Override
        protected String doInBackground(GeoPoint... params) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing() || !Geocoder.isPresent()) {
                return "Pinned Location (Geocoder N/A)";
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
                activity.isProcessingSuggestionClick = true; // To prevent TextWatcher interference
                activity.currentSelectedName = addressName;
                if (activity.selectedLocationMarker != null && activity.currentSelectedPoint != null) {
                    activity.selectedLocationMarker.setTitle(addressName);
                    activity.mapView.invalidate();
                }
                activity.binding.etSearchLocation.setText(addressName);
                activity.binding.etSearchLocation.setSelection(addressName.length());
                // Reset flag after TextWatcher has processed
                new Handler(Looper.getMainLooper()).postDelayed(() -> activity.isProcessingSuggestionClick = false, 100);
            }
        }
    }

    public static class Helper {
        public static String getAddressDisplayName(Address address) {
            if (address == null) return "Unknown Location";
            StringBuilder displayNameBuilder = new StringBuilder();
            String addressLine0 = address.getAddressLine(0);
            if (addressLine0 != null && !addressLine0.isEmpty()) {
                return addressLine0;
            }
            String featureName = address.getFeatureName();
            if (featureName != null && !featureName.isEmpty() && !featureName.matches("\\d+.*")) {
                displayNameBuilder.append(featureName);
            }
            String thoroughfare = address.getThoroughfare();
            if (thoroughfare != null) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(thoroughfare)) displayNameBuilder.append(", ");
                if (!displayNameBuilder.toString().contains(thoroughfare)) displayNameBuilder.append(thoroughfare);
                String subThoroughfare = address.getSubThoroughfare();
                if (subThoroughfare != null) {
                    displayNameBuilder.append(" ").append(subThoroughfare);
                }
            }
            String subLocality = address.getSubLocality();
            if (subLocality != null) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(subLocality)) displayNameBuilder.append(", ");
                if(!displayNameBuilder.toString().contains(subLocality)) displayNameBuilder.append(subLocality);
            }
            String locality = address.getLocality();
            if (locality != null) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(locality)) displayNameBuilder.append(", ");
                if(!displayNameBuilder.toString().contains(locality)) displayNameBuilder.append(locality);
            }
            String adminArea = address.getAdminArea();
            if (adminArea != null) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(adminArea)) displayNameBuilder.append(", ");
                if(!displayNameBuilder.toString().contains(adminArea) && (locality == null || !locality.equals(adminArea))) displayNameBuilder.append(adminArea);
            }
            String countryName = address.getCountryName();
            if (countryName != null) {
                if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(countryName)) displayNameBuilder.append(", ");
                if(!displayNameBuilder.toString().contains(countryName)) displayNameBuilder.append(countryName);
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
        if (mapView != null) mapView.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(myLocationOverlayPicker != null && !myLocationOverlayPicker.isMyLocationEnabled()){
                myLocationOverlayPicker.enableMyLocation();
            }
            if(selectedLocationMarker == null && currentSelectedPoint == null) {
                startPickerLocationUpdates();
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (fusedLocationClientPicker != null ) {
            if (locationCallbackPicker != null) {
                fusedLocationClientPicker.removeLocationUpdates(locationCallbackPicker);
            }
            if (cancellationTokenSource != null) {
                cancellationTokenSource.cancel();
            }
        }
        if(myLocationOverlayPicker != null) myLocationOverlayPicker.disableMyLocation();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDetach();
        searchDebounceHandler.removeCallbacksAndMessages(null);
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
        }
    }
}
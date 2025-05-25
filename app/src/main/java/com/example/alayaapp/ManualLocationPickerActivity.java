package com.example.alayaapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
// Android's Geocoder and Address
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.alayaapp.databinding.ActivityManualLocationPickerBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.api.IMapController;
// REMOVE: import org.osmdroid.bonuspack.location.GeocoderNominatim;
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
import java.util.List;
import java.util.Locale;

public class ManualLocationPickerActivity extends AppCompatActivity {

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

        fusedLocationClientPicker = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallbackPicker();
        checkAndRequestPickerPermissions();


        binding.btnSearchLocation.setOnClickListener(v -> performSearch());
        binding.etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
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
                Toast.makeText(this, "Please tap on the map to select a location.", Toast.LENGTH_LONG).show();
            }
        });

        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                currentSelectedPoint = p;
                updateMarker(p, "Fetching address..."); // Initial name for pinned
                new ReverseGeocodeTaskInternal(ManualLocationPickerActivity.this).execute(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(0, mapEventsOverlay);
    }

    private void setupOsmMapPicker() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        IMapController mapController = mapView.getController();
        mapController.setZoom(6.0);
        mapController.setCenter(new GeoPoint(12.8797, 121.7740)); // Philippines

        myLocationOverlayPicker = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlayPicker.disableFollowLocation();
        mapView.getOverlays().add(myLocationOverlayPicker);
    }

    private void setupLocationCallbackPicker() {
        locationCallbackPicker = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) return;
                Location location = locationResult.getLastLocation();
                GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

                if (selectedLocationMarker == null && currentSelectedPoint == null) {
                    mapView.getController().animateTo(currentLocation);
                    mapView.getController().setZoom(15.0);
                }

                if (fusedLocationClientPicker != null && locationCallbackPicker != null) {
                    fusedLocationClientPicker.removeLocationUpdates(locationCallbackPicker);
                }
            }
        };
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

    // Corrected code
    private void startPickerLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1) // <-- Corrected method
                .build();
        fusedLocationClientPicker.requestLocationUpdates(locationRequest, locationCallbackPicker, Looper.getMainLooper());
    }

    private void performSearch() {
        // String query = binding.etSearchLocation.getText().toString().trim(); // Keep for future
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        // Temporarily disable text search
        Toast.makeText(this, "Text search feature is temporarily unavailable. Tap map to select.", Toast.LENGTH_LONG).show();
        // new GeocodeTaskInternal().execute(query); // This was for osmdroid-bonuspack
    }

    private void updateMarker(GeoPoint point, String title) {
        currentSelectedName = title;
        currentSelectedPoint = point;

        if (selectedLocationMarker == null) {
            selectedLocationMarker = new Marker(mapView);
            selectedLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(selectedLocationMarker);
        }
        selectedLocationMarker.setPosition(point);
        selectedLocationMarker.setTitle(title);
        mapView.getController().animateTo(point);
        mapView.invalidate();
        binding.etSearchLocation.setText(title); // Update search bar with (reverse geocoded) name
        binding.etSearchLocation.clearFocus();
    }

    // COMMENT OUT GeocodeTaskInternal that used GeocoderNominatim
    /*
    private class GeocodeTaskInternal extends AsyncTask<String, Void, List<org.osmdroid.bonuspack.location.Address>> {
        // ...
    }
    */

    // MODIFIED ReverseGeocodeTaskInternal to use android.location.Geocoder
    private static class ReverseGeocodeTaskInternal extends AsyncTask<GeoPoint, Void, String> {
        private ManualLocationPickerActivity activity;

        ReverseGeocodeTaskInternal(ManualLocationPickerActivity activity) {
            this.activity = activity;
        }
        @Override
        protected String doInBackground(GeoPoint... params) {
            if (activity == null || activity.isFinishing() || !Geocoder.isPresent()) {
                return "Pinned Location (Geocoder N/A)";
            }
            GeoPoint pointToReverse = params[0];
            Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(pointToReverse.getLatitude(), pointToReverse.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    return getAddressDisplayName(addresses.get(0));
                }
            } catch (IOException e) {
                Log.e(TAG, "Reverse geocoding error (Android Geocoder)", e);
            }
            return "Pinned Location (Address not found)";
        }

        @Override
        protected void onPostExecute(String addressName) {
            if (activity != null && !activity.isFinishing() && addressName != null) {
                activity.currentSelectedName = addressName;
                if (activity.selectedLocationMarker != null && activity.currentSelectedPoint != null) {
                    activity.selectedLocationMarker.setTitle(addressName);
                }
                activity.binding.etSearchLocation.setText(addressName);
            }
        }
    }

    // Helper to get a displayable name from an android.location.Address object
    private static String getAddressDisplayName(Address address) {
        StringBuilder displayNameBuilder = new StringBuilder();
        // Try to build a comprehensive address line
        if (address.getFeatureName() != null) { // POI, building name
            displayNameBuilder.append(address.getFeatureName());
        }
        if (address.getThoroughfare() != null) { // Street name
            if (displayNameBuilder.length() > 0) displayNameBuilder.append(", ");
            displayNameBuilder.append(address.getThoroughfare());
            if (address.getSubThoroughfare() != null) { // Street number
                displayNameBuilder.append(" ").append(address.getSubThoroughfare());
            }
        }
        if (address.getSubLocality() != null) { // Neighborhood, district
            if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(address.getSubLocality())) displayNameBuilder.append(", ");
            if(!displayNameBuilder.toString().contains(address.getSubLocality())) displayNameBuilder.append(address.getSubLocality());
        }
        if (address.getLocality() != null) { // City
            if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(address.getLocality())) displayNameBuilder.append(", ");
            if(!displayNameBuilder.toString().contains(address.getLocality())) displayNameBuilder.append(address.getLocality());
        }
        if (address.getAdminArea() != null) { // Province/State
            if (displayNameBuilder.length() > 0 && !displayNameBuilder.toString().contains(address.getAdminArea())) displayNameBuilder.append(", ");
            if(!displayNameBuilder.toString().contains(address.getAdminArea())) displayNameBuilder.append(address.getAdminArea());
        }
        if (address.getCountryName() != null) {
            if (displayNameBuilder.length() > 0) displayNameBuilder.append(", ");
            displayNameBuilder.append(address.getCountryName());
        }

        if (displayNameBuilder.length() == 0) { // Fallback if no parts were found
            if (address.getAddressLine(0) != null) { // Use the first address line
                return address.getAddressLine(0);
            } else {
                return "Unknown Location";
            }
        }
        return displayNameBuilder.toString();
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
        if (fusedLocationClientPicker != null && locationCallbackPicker != null) {
            fusedLocationClientPicker.removeLocationUpdates(locationCallbackPicker);
        }
        if(myLocationOverlayPicker != null) myLocationOverlayPicker.disableMyLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDetach();
    }
}
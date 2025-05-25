package com.example.alayaapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address; // Using android.location.Address
import android.location.Geocoder;  // Using android.location.Geocoder
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
                Toast.makeText(this, "Please select a location on the map or by search.", Toast.LENGTH_LONG).show();
            }
        });

        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                currentSelectedPoint = p;
                updateMarker(p, "Fetching address...");
                new ReverseGeocodeTask(ManualLocationPickerActivity.this).execute(p);
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

    private void startPickerLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1) // Changed from setNumUpdates(1)
                .build();
        fusedLocationClientPicker.requestLocationUpdates(locationRequest, locationCallbackPicker, Looper.getMainLooper());
    }

    private void performSearch() {
        String query = binding.etSearchLocation.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a location to search.", Toast.LENGTH_SHORT).show();
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        // Use Android's Geocoder for text search
        if (Geocoder.isPresent()) {
            new GeocodeFromNameTaskAndroid(this).execute(query);
        } else {
            Toast.makeText(this, "Geocoder service not available on this device.", Toast.LENGTH_LONG).show();
        }
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
        if (mapView.getZoomLevelDouble() < 15.0) {
            mapView.getController().setZoom(16.0);
        }
        mapView.invalidate();
        binding.etSearchLocation.setText(title);
        binding.etSearchLocation.clearFocus();
    }

    // AsyncTask for Forward Geocoding (text search) using ANDROID'S Geocoder
    private static class GeocodeFromNameTaskAndroid extends AsyncTask<String, Void, List<Address>> {
        private WeakReference<ManualLocationPickerActivity> activityReference;

        GeocodeFromNameTaskAndroid(ManualLocationPickerActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected List<Address> doInBackground(String... params) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing() || !Geocoder.isPresent()) {
                return null;
            }
            String locationName = params[0];
            Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
            try {
                // Android Geocoder's getFromLocationName
                // You can optionally provide a bounding box for more specific results
                // lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude
                return geocoder.getFromLocationName(locationName, 5); // Get max 5 results
            } catch (IOException e) {
                Log.e(TAG, "Android Geocoder error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            ManualLocationPickerActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (addresses == null || addresses.isEmpty()) {
                Toast.makeText(activity, "Location not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (addresses.size() == 1) {
                Address firstResult = addresses.get(0);
                // Android Geocoder.getFromLocationName already returns lat/lon if available
                if (firstResult.hasLatitude() && firstResult.hasLongitude()){
                    GeoPoint resultPoint = new GeoPoint(firstResult.getLatitude(), firstResult.getLongitude());
                    String displayName = Helper.getAddressDisplayName(firstResult);
                    activity.updateMarker(resultPoint, displayName);
                } else {
                    Toast.makeText(activity, "Location found, but coordinates missing.", Toast.LENGTH_SHORT).show();
                }
            } else {
                activity.showSearchResultsDialog(addresses);
            }
        }
    }

    private void showSearchResultsDialog(List<Address> addresses) {
        ArrayList<String> addressDisplayNames = new ArrayList<>();
        // Filter out results without coordinates, as we need them for the map
        List<Address> validAddresses = new ArrayList<>();
        for (Address address : addresses) {
            if (address.hasLatitude() && address.hasLongitude()) {
                addressDisplayNames.add(Helper.getAddressDisplayName(address));
                validAddresses.add(address);
            }
        }

        if (validAddresses.isEmpty()){
            Toast.makeText(this, "No locations with coordinates found for your search.", Toast.LENGTH_LONG).show();
            return;
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, addressDisplayNames);

        new AlertDialog.Builder(this)
                .setTitle("Select a Location")
                .setAdapter(adapter, (dialog, which) -> {
                    Address selectedAddress = validAddresses.get(which);
                    GeoPoint resultPoint = new GeoPoint(selectedAddress.getLatitude(), selectedAddress.getLongitude());
                    String displayName = Helper.getAddressDisplayName(selectedAddress);
                    updateMarker(resultPoint, displayName);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    // AsyncTask for Reverse Geocoding (tap on map) using Android's Geocoder
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
                activity.currentSelectedName = addressName;
                if (activity.selectedLocationMarker != null && activity.currentSelectedPoint != null) {
                    activity.selectedLocationMarker.setTitle(addressName);
                }
                activity.binding.etSearchLocation.setText(addressName);
            }
        }
    }

    // Static Helper class for address formatting
    public static class Helper {
        public static String getAddressDisplayName(Address address) {
            if (address == null) return "Unknown Location";

            StringBuilder displayNameBuilder = new StringBuilder();
            // Android's Geocoder often has good info in AddressLine(0) or FeatureName
            String featureName = address.getFeatureName();
            String addressLine0 = address.getAddressLine(0);

            if (addressLine0 != null && !addressLine0.isEmpty()) {
                return addressLine0; // Prefer the full first address line if available from Android Geocoder
            }

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
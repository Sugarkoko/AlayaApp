package com.example.alayaapp;

import androidx.activity.result.ActivityResultLauncher; // Added
import androidx.activity.result.contract.ActivityResultContracts; // Added
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Added
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat; // Added
import androidx.core.content.ContextCompat; // Added
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest; // Added
import android.content.Intent;
import android.content.SharedPreferences; // Added
import android.content.pm.PackageManager; // Added
import android.location.Address; // Added
import android.location.Geocoder; // Added
import android.location.Location; // Added
import android.os.Bundle;
import android.os.Looper; // Added
import android.util.Log; // Added
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton; // Added
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient; // Added
import com.google.android.gms.location.LocationCallback; // Added
import com.google.android.gms.location.LocationRequest; // Added
import com.google.android.gms.location.LocationResult; // Added
import com.google.android.gms.location.LocationServices; // Added
import com.google.android.gms.location.Priority; // Added
import com.google.android.material.bottomnavigation.BottomNavigationView;
// import com.google.android.material.navigation.NavigationBarView; // Not strictly needed if using setOnItemSelectedListener

import java.io.IOException; // Added
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale; // Added

// Assuming you have a GeoPoint utility or use LatLng directly
import com.example.alayaapp.util.GeoPoint; // If you use your custom GeoPoint

public class ItinerariesActivity extends AppCompatActivity implements ItineraryAdapter.OnStartDragListener {

    BottomNavigationView bottomNavigationView;
    View ivEditItineraryIcon; // Renamed from ivEditItinerary for clarity (it's an ImageView)
    TextView tvSaveChanges;
    RecyclerView rvSuggestedItinerary;
    ItineraryAdapter itineraryAdapter;
    ItemTouchHelper itemTouchHelper;
    ItineraryItemTouchHelperCallback touchHelperCallback;
    List<ItineraryItem> suggestedList = new ArrayList<>();
    private boolean isEditMode = false;
    final int CURRENT_ITEM_ID = R.id.navigation_itineraries;

    // --- Location Related Variables (copied from HomeActivity) ---
    private static final String TAG_LOCATION = "ItinerariesLocation"; // Specific tag
    private static final int REQUEST_LOCATION_PERMISSION_ITINERARIES = 2; // Different request code
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates = false;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AlayaAppPrefs";
    private static final String KEY_LOCATION_MODE = "location_mode";
    private static final String KEY_MANUAL_LOCATION_NAME = "manual_location_name";
    private static final String KEY_MANUAL_LATITUDE = "manual_latitude";
    private static final String KEY_MANUAL_LONGITUDE = "manual_longitude";
    private String currentLocationNameToDisplay = "Tap to get current location";
    private GeoPoint manualGeoPoint = null; // Or use com.google.android.gms.maps.model.LatLng

    private TextView tvLocationCityItineraries; // For location name display
    private TextView tvLocationStatusItineraries; // For status text
    private ImageButton ibEditLocationItineraries; // The edit button

    private final ActivityResultLauncher<Intent> manualLocationPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String locationName = data.getStringExtra("selected_location_name");
                    double latitude = data.getDoubleExtra("selected_latitude", 0.0);
                    double longitude = data.getDoubleExtra("selected_longitude", 0.0);
                    if (locationName != null && !locationName.isEmpty()) {
                        currentLocationNameToDisplay = locationName;
                        manualGeoPoint = new GeoPoint(latitude, longitude); // Or LatLng
                        tvLocationCityItineraries.setText(currentLocationNameToDisplay);
                        tvLocationStatusItineraries.setText("Manually set: " + currentLocationNameToDisplay);
                        saveLocationPreference("manual", locationName, latitude, longitude);
                        stopLocationUpdates();
                        // TODO: Optionally, re-filter or update itineraries based on new location
                    }
                }
            });
    // --- End of Location Variables ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itineraries);

        // Find Views for Itinerary specific elements
        ivEditItineraryIcon = findViewById(R.id.iv_edit_itinerary);
        tvSaveChanges = findViewById(R.id.tv_save_changes);
        rvSuggestedItinerary = findViewById(R.id.rv_suggested_itinerary);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Find Views for new Location section elements
        tvLocationCityItineraries = findViewById(R.id.tv_location_city_itineraries);
        tvLocationStatusItineraries = findViewById(R.id.tv_location_status_itineraries);
        ibEditLocationItineraries = findViewById(R.id.ib_edit_location_itineraries);

        // --- Initialize Location Services ---
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback(); // Call method to setup the callback
        // --- End of Location Services Init ---

        setupRecyclerView();
        loadPlaceholderData(); // For suggested itinerary items

        bottomNavigationView.setSelectedItemId(CURRENT_ITEM_ID);
        setupBottomNavListener();

        ivEditItineraryIcon.setOnClickListener(v -> enterEditMode());
        tvSaveChanges.setOnClickListener(v -> exitEditModeAndSave());

        // --- Location UI Setup ---
        ibEditLocationItineraries.setOnClickListener(v -> showLocationChoiceDialog());
        loadLocationPreferenceAndInitialize(); // Load and display current/manual location
        // --- End of Location UI Setup ---
    }

    private void setupRecyclerView() {
        // ... (your existing RecyclerView setup)
        itineraryAdapter = new ItineraryAdapter(suggestedList, this);
        rvSuggestedItinerary.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestedItinerary.setAdapter(itineraryAdapter);
        touchHelperCallback = new ItineraryItemTouchHelperCallback(itineraryAdapter);
        itemTouchHelper = new ItemTouchHelper(touchHelperCallback);
        itemTouchHelper.attachToRecyclerView(rvSuggestedItinerary);
    }

    private void loadPlaceholderData() {
        // ... (your existing placeholder data loading)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0);
        suggestedList.add(new ItineraryItem(1, (Calendar)cal.clone(), "Breakfast at Café by the Ruins", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1);
        suggestedList.add(new ItineraryItem(2, (Calendar)cal.clone(), "Burnham Park", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 2);
        suggestedList.add(new ItineraryItem(3, (Calendar)cal.clone(), "Lunch at Choco-late de Batirol", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 2);
        suggestedList.add(new ItineraryItem(4, (Calendar)cal.clone(), "Mines View Park", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1);
        suggestedList.add(new ItineraryItem(5, (Calendar)cal.clone(), "Baguio Cathedral", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1);
        suggestedList.add(new ItineraryItem(6, (Calendar)cal.clone(), "Lemon and Olives", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1);
        suggestedList.add(new ItineraryItem(7, (Calendar)cal.clone(), "Dinner at Café Yagam", "4.5"));
        itineraryAdapter.notifyDataSetChanged();
    }

    private void enterEditMode() {
        // ... (your existing edit mode logic)
        isEditMode = true;
        ivEditItineraryIcon.setVisibility(View.GONE);
        tvSaveChanges.setVisibility(View.VISIBLE);
        itineraryAdapter.setEditMode(true);
        if(touchHelperCallback != null) touchHelperCallback.setEditMode(true);
        Toast.makeText(this, "Edit mode enabled. Drag handles to reorder.", Toast.LENGTH_SHORT).show();
    }

    private void exitEditModeAndSave() {
        // ... (your existing save logic)
        isEditMode = false;
        ivEditItineraryIcon.setVisibility(View.VISIBLE);
        tvSaveChanges.setVisibility(View.GONE);
        itineraryAdapter.setEditMode(false);
        if(touchHelperCallback != null) touchHelperCallback.setEditMode(false);
        Toast.makeText(this, "Changes Saved (Placeholder)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (itemTouchHelper != null && isEditMode) {
            itemTouchHelper.startDrag(viewHolder);
        }
    }

    // --- Bottom Nav Logic (mostly unchanged) ---
    private void setupBottomNavListener() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();
            if (destinationItemId == CURRENT_ITEM_ID) return true;
            Class<?> destinationActivityClass = null;
            if (destinationItemId == R.id.navigation_home) destinationActivityClass = HomeActivity.class;
            else if (destinationItemId == R.id.navigation_map) destinationActivityClass = MapsActivity.class;
            else if (destinationItemId == R.id.navigation_profile) destinationActivityClass = ProfileActivity.class;

            if (destinationActivityClass != null) {
                Intent intent = new Intent(getApplicationContext(), destinationActivityClass);
                startActivity(intent);
                boolean slideRightToLeft = getItemIndex(destinationItemId) > getItemIndex(CURRENT_ITEM_ID);
                if (slideRightToLeft) overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                else overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            }
            return false;
        });
    }

    private int getItemIndex(int itemId) {
        if (itemId == R.id.navigation_home) return 0;
        if (itemId == R.id.navigation_itineraries) return 1;
        if (itemId == R.id.navigation_map) return 2;
        if (itemId == R.id.navigation_profile) return 3;
        return -1;
    }

    // --- LOCATION METHODS (Copied and adapted from HomeActivity) ---
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        // TODO: If itineraries depend on location, re-fetch or filter here.
                        // stopLocationUpdates(); // Optional: Stop if only one good fix is needed
                        break;
                    }
                }
            }
        };
    }

    private void showLocationChoiceDialog() {
        final CharSequence[] options = {"Use My Current GPS Location", "Set Location Manually", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Location Method");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Use My Current GPS Location")) {
                saveLocationPreference("auto", null, 0, 0);
                manualGeoPoint = null;
                tvLocationCityItineraries.setText("Fetching GPS location...");
                tvLocationStatusItineraries.setText("Mode: GPS");
                checkAndRequestLocationPermissions();
            } else if (options[item].equals("Set Location Manually")) {
                Intent intent = new Intent(ItinerariesActivity.this, ManualLocationPickerActivity.class);
                manualLocationPickerLauncher.launch(intent);
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void loadLocationPreferenceAndInitialize() {
        String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
        if (mode.equals("manual")) {
            String name = sharedPreferences.getString(KEY_MANUAL_LOCATION_NAME, "");
            double lat = Double.longBitsToDouble(sharedPreferences.getLong(KEY_MANUAL_LATITUDE, Double.doubleToRawLongBits(0.0)));
            double lon = Double.longBitsToDouble(sharedPreferences.getLong(KEY_MANUAL_LONGITUDE, Double.doubleToRawLongBits(0.0)));
            if (!name.isEmpty() && lat != 0.0 && lon != 0.0) {
                currentLocationNameToDisplay = name;
                manualGeoPoint = new GeoPoint(lat, lon); // Or LatLng
                tvLocationCityItineraries.setText(currentLocationNameToDisplay);
                tvLocationStatusItineraries.setText("Manually set: " + currentLocationNameToDisplay);
                stopLocationUpdates();
            } else {
                // Fallback to auto if manual data is incomplete
                saveLocationPreference("auto", null, 0,0);
                checkAndRequestLocationPermissions();
            }
        } else { // "auto" mode
            tvLocationCityItineraries.setText("Tap to get current location");
            tvLocationStatusItineraries.setText("Mode: GPS. Waiting for location...");
            checkAndRequestLocationPermissions();
        }
    }

    private void saveLocationPreference(String mode, String name, double lat, double lon) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LOCATION_MODE, mode);
        if (mode.equals("manual") && name != null) {
            editor.putString(KEY_MANUAL_LOCATION_NAME, name);
            editor.putLong(KEY_MANUAL_LATITUDE, Double.doubleToRawLongBits(lat));
            editor.putLong(KEY_MANUAL_LONGITUDE, Double.doubleToRawLongBits(lon));
        } else {
            editor.remove(KEY_MANUAL_LOCATION_NAME);
            editor.remove(KEY_MANUAL_LATITUDE);
            editor.remove(KEY_MANUAL_LONGITUDE);
        }
        editor.apply();
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        // Ensure we only update if in "auto" mode and location is for this activity
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String subLocality = address.getSubLocality();
                String thoroughfare = address.getThoroughfare();
                String country = address.getCountryName();
                StringBuilder addressTextBuilder = new StringBuilder();

                if (city != null && !city.isEmpty()) addressTextBuilder.append(city);
                else if (subLocality != null && !subLocality.isEmpty()) addressTextBuilder.append(subLocality);
                else if (thoroughfare != null && !thoroughfare.isEmpty()) addressTextBuilder.append(thoroughfare);
                else addressTextBuilder.append("Unknown Area");

                if (country != null && !country.isEmpty() && addressTextBuilder.length() > 0 && !addressTextBuilder.toString().equals("Unknown Area")) {
                    if (!addressTextBuilder.toString().equalsIgnoreCase(country) && (city == null || !city.equalsIgnoreCase(country))) {
                        addressTextBuilder.append(", ").append(country);
                    }
                } else if (country != null && !country.isEmpty() && addressTextBuilder.toString().equals("Unknown Area")) {
                    addressTextBuilder.replace(0, addressTextBuilder.length(), country);
                }
                currentLocationNameToDisplay = addressTextBuilder.length() == 0 ? "Location Name Not Found" : addressTextBuilder.toString();
                tvLocationCityItineraries.setText(currentLocationNameToDisplay);
                tvLocationStatusItineraries.setText("GPS: " + currentLocationNameToDisplay);
                Log.d(TAG_LOCATION, "Fetched Address (Auto): " + currentLocationNameToDisplay);
            } else {
                tvLocationCityItineraries.setText("Location Name Not Found (Auto)");
                tvLocationStatusItineraries.setText("GPS: Location Name Not Found");
                Log.w(TAG_LOCATION, "No address found for the location (Auto).");
            }
        } catch (IOException e) {
            Log.e(TAG_LOCATION, "Geocoder service not available or IO error (Auto)", e);
            tvLocationCityItineraries.setText("Service to get address unavailable (Auto)");
        } catch (IllegalArgumentException e) {
            Log.e(TAG_LOCATION, "Invalid latitude or longitude values (Auto).", e);
            tvLocationCityItineraries.setText("Invalid location data (Auto)");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-set selected item in bottom nav in case of back navigation
        bottomNavigationView.setSelectedItemId(CURRENT_ITEM_ID);

        // Location updates
        String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
        if (mode.equals("auto")) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                // If permission wasn't granted or revoked, re-check.
                // User might grant it from settings then come back.
                checkAndRequestLocationPermissions();
            }
        } else { // Manual mode
            stopLocationUpdates();
            // Ensure manual location is displayed correctly
            String name = sharedPreferences.getString(KEY_MANUAL_LOCATION_NAME, "Not Set");
            tvLocationCityItineraries.setText(name);
            tvLocationStatusItineraries.setText("Manually set: " + name);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission to show your current location for itineraries. Please allow.")
                        .setPositiveButton("OK", (dialogInterface, i) ->
                                ActivityCompat.requestPermissions(ItinerariesActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                        REQUEST_LOCATION_PERMISSION_ITINERARIES))
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            tvLocationCityItineraries.setText("Permission needed");
                            tvLocationStatusItineraries.setText("Location permission denied.");
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_LOCATION_PERMISSION_ITINERARIES);
            }
        } else {
            // Permission already granted
            String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
            if (mode.equals("auto")) {
                fetchLastLocation(); // Attempt to get a quick last location
                startLocationUpdates(); // Start continuous updates
            } else {
                stopLocationUpdates(); // Ensure updates are stopped in manual mode
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_ITINERARIES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
                if (mode.equals("auto")) {
                    fetchLastLocation();
                    startLocationUpdates();
                }
            } else {
                // Permission denied
                tvLocationCityItineraries.setText("Location permission denied");
                tvLocationStatusItineraries.setText("Location permission denied.");
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;

        tvLocationStatusItineraries.setText("Fetching last known location...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.d(TAG_LOCATION, "Last known location is null. Waiting for updates.");
                        if (sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) { // Only if still in auto
                            tvLocationStatusItineraries.setText("Last location null, waiting for live updates...");
                        }
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG_LOCATION, "Error trying to get last GPS location", e);
                    tvLocationCityItineraries.setText("Failed to get location");
                    tvLocationStatusItineraries.setText("Failed to get last location.");
                });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Permissions not granted
        }
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) {
            Log.d(TAG_LOCATION, "In manual mode, not starting GPS updates.");
            stopLocationUpdates(); // Ensure updates are stopped
            return;
        }
        if (requestingLocationUpdates) {
            Log.d(TAG_LOCATION, "startLocationUpdates: Already requesting.");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 seconds
                .setMinUpdateIntervalMillis(5000) // 5 seconds
                .build();
        requestingLocationUpdates = true;
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        tvLocationStatusItineraries.setText("Updating location (GPS)...");
        Log.d(TAG_LOCATION, "Requested location updates.");
    }

    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
            Log.d(TAG_LOCATION, "Location updates stopped.");
        }
    }
    // --- END OF LOCATION METHODS ---
}
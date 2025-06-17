package com.example.alayaapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ItinerariesActivity extends AppCompatActivity implements ItineraryAdapter.ItineraryHeaderListener {
    BottomNavigationView bottomNavigationView;
    RecyclerView rvMain;
    ItineraryAdapter itineraryAdapter;
    FloatingActionButton fabSaveTrip;
    ProgressBar pbItineraries;
    final int CURRENT_ITEM_ID = R.id.navigation_itineraries;
    private static final String TAG_LOCATION = "ItinerariesActivity";
    private static final int REQUEST_LOCATION_PERMISSION_ITINERARIES = 2;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates = false;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AlayaAppPrefs";
    private static final String KEY_LOCATION_MODE = "location_mode";
    private static final String KEY_MANUAL_LOCATION_NAME = "manual_location_name";
    private static final String KEY_MANUAL_LATITUDE = "manual_latitude";
    private static final String KEY_MANUAL_LONGITUDE = "manual_longitude";
    private GeoPoint currentGeoPoint = null;
    private FirebaseFirestore db;
    private List<Place> allPlacesList = new ArrayList<>();
    private static final String KEY_TRIP_DATE_YEAR = "trip_date_year";
    private static final String KEY_TRIP_DATE_MONTH = "trip_date_month";
    private static final String KEY_TRIP_DATE_DAY = "trip_date_day";
    private static final String KEY_TRIP_TIME_HOUR = "trip_time_hour";
    private static final String KEY_TRIP_TIME_MINUTE = "trip_time_minute";
    private static final String KEY_TRIP_END_TIME_HOUR = "trip_end_time_hour";
    private static final String KEY_TRIP_END_TIME_MINUTE = "trip_end_time_minute";
    private Calendar tripStartCalendar;
    private Calendar tripEndCalendar;
    private static final double BAGUIO_REGION_MIN_LAT = 16.35;
    private static final double BAGUIO_REGION_MAX_LAT = 16.50;
    private static final double BAGUIO_REGION_MIN_LON = 120.55;
    private static final double BAGUIO_REGION_MAX_LON = 120.65;
    private List<Object> displayItems = new ArrayList<>();

    // --- ViewModel ---
    public ItineraryViewModel itineraryViewModel;

    private final ActivityResultLauncher<Intent> manualLocationPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String locationName = data.getStringExtra("selected_location_name");
                    double latitude = data.getDoubleExtra("selected_latitude", 0.0);
                    double longitude = data.getDoubleExtra("selected_longitude", 0.0);
                    if (locationName != null && !locationName.isEmpty()) {
                        currentGeoPoint = new GeoPoint(latitude, longitude);
                        itineraryViewModel.updateLocationStatus(locationName, "Manually set: " + locationName);
                        saveLocationPreference("manual", locationName, latitude, longitude);
                        stopLocationUpdates();
                        fetchPlacesAndTriggerGeneration(false);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itineraries);
        rvMain = findViewById(R.id.rv_itineraries_main);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabSaveTrip = findViewById(R.id.fab_save_trip);
        pbItineraries = findViewById(R.id.pb_itineraries);

        itineraryViewModel = new ViewModelProvider(this).get(ItineraryViewModel.class);

        db = FirebaseFirestore.getInstance();
        tripStartCalendar = Calendar.getInstance();
        tripEndCalendar = Calendar.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupRecyclerView();
        setupViewModelObservers();
        setupBottomNavListener();
        setupActionListeners();
        setupLocationCallback();

        loadLocationPreferenceAndInitialize();
    }

    private void setupRecyclerView() {
        itineraryAdapter = new ItineraryAdapter(this, displayItems, this);
        rvMain.setLayoutManager(new LinearLayoutManager(this));
        rvMain.setAdapter(itineraryAdapter);
    }

    private void setupViewModelObservers() {
        itineraryViewModel.displayList.observe(this, items -> {
            displayItems.clear();
            if (items != null) {
                displayItems.addAll(items);
            }
            itineraryAdapter.notifyDataSetChanged();
            fabSaveTrip.setVisibility(items != null && !items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        itineraryViewModel.isLoading.observe(this, isLoading -> {
            pbItineraries.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            rvMain.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        itineraryViewModel.isItinerarySaved.observe(this, isSaved -> {
            fabSaveTrip.setImageResource(isSaved ? R.drawable.ic_profile_placeholder : android.R.drawable.ic_menu_save);
            fabSaveTrip.setEnabled(!isSaved);
        });
    }

    private void setupActionListeners() {
        bottomNavigationView.setSelectedItemId(CURRENT_ITEM_ID);
        fabSaveTrip.setOnClickListener(v -> {
            loadTripDateTime(); // Ensure calendar objects are up-to-date
            itineraryViewModel.saveCurrentTripToHistory(tripStartCalendar);
        });
    }

    private void fetchPlacesAndTriggerGeneration(boolean forceRegenerate) {
        if (currentGeoPoint == null) {
            Toast.makeText(this, "Cannot generate itinerary without a start location.", Toast.LENGTH_LONG).show();
            itineraryViewModel.clearItinerary();
            return;
        }
        if (!isLocationInAllowedRegion(currentGeoPoint.getLatitude(), currentGeoPoint.getLongitude())) {
            redirectToHomeWithDialog();
            return;
        }
        loadTripDateTime();

        db.collection("places").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                allPlacesList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Place place = document.toObject(Place.class);
                        if (place != null && place.getCoordinates() != null && place.getOpeningHours() != null) {
                            place.setDocumentId(document.getId());
                            allPlacesList.add(place);
                        }
                    } catch (Exception e) {
                        Log.e(TAG_LOCATION, "Error converting document to Place object: " + document.getId(), e);
                    }
                }
                String locationName = itineraryViewModel.currentLocationName.getValue() != null ? itineraryViewModel.currentLocationName.getValue() : "Selected Location";
                itineraryViewModel.loadOrGenerateItinerary(currentGeoPoint, locationName, tripStartCalendar, tripEndCalendar, allPlacesList, forceRegenerate);
            } else {
                Log.w(TAG_LOCATION, "Error getting documents from Firestore.", task.getException());
                Toast.makeText(ItinerariesActivity.this, "Failed to load places for itinerary.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadTripDateTime() {
        if (sharedPreferences.contains(KEY_TRIP_DATE_YEAR)) {
            int year = sharedPreferences.getInt(KEY_TRIP_DATE_YEAR, tripStartCalendar.get(Calendar.YEAR));
            int month = sharedPreferences.getInt(KEY_TRIP_DATE_MONTH, tripStartCalendar.get(Calendar.MONTH));
            int day = sharedPreferences.getInt(KEY_TRIP_DATE_DAY, tripStartCalendar.get(Calendar.DAY_OF_MONTH));
            tripStartCalendar.set(year, month, day);
            tripEndCalendar.set(year, month, day);
        }
        int startHour = sharedPreferences.getInt(KEY_TRIP_TIME_HOUR, 9);
        int startMinute = sharedPreferences.getInt(KEY_TRIP_TIME_MINUTE, 0);
        tripStartCalendar.set(Calendar.HOUR_OF_DAY, startHour);
        tripStartCalendar.set(Calendar.MINUTE, startMinute);
        int endHour = sharedPreferences.getInt(KEY_TRIP_END_TIME_HOUR, 18);
        int endMinute = sharedPreferences.getInt(KEY_TRIP_END_TIME_MINUTE, 0);
        tripEndCalendar.set(Calendar.HOUR_OF_DAY, endHour);
        tripEndCalendar.set(Calendar.MINUTE, endMinute);
    }

    public void showLocationChoiceDialog() {
        final CharSequence[] options = {"Use My Current GPS Location", "Set Location Manually", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Start Location");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Use My Current GPS Location")) {
                saveLocationPreference("auto", null, 0, 0);
                currentGeoPoint = null;
                itineraryViewModel.updateLocationStatus("Fetching GPS location...", "Mode: GPS");
                checkAndRequestLocationPermissions();
            } else if (options[item].equals("Set Location Manually")) {
                Intent intent = new Intent(ItinerariesActivity.this, ManualLocationPickerActivity.class);
                if (currentGeoPoint != null) {
                    intent.putExtra(ManualLocationPickerActivity.EXTRA_INITIAL_LAT, currentGeoPoint.getLatitude());
                    intent.putExtra(ManualLocationPickerActivity.EXTRA_INITIAL_LON, currentGeoPoint.getLongitude());
                    intent.putExtra(ManualLocationPickerActivity.EXTRA_INITIAL_NAME, itineraryViewModel.currentLocationName.getValue());
                }
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
            if (!name.isEmpty() && lat != 0.0) {
                currentGeoPoint = new GeoPoint(lat, lon);
                itineraryViewModel.updateLocationStatus(name, "Manually set: " + name);
                stopLocationUpdates();
                fetchPlacesAndTriggerGeneration(false);
            } else {
                saveLocationPreference("auto", null, 0,0);
                checkAndRequestLocationPermissions();
            }
        } else {
            itineraryViewModel.updateLocationStatus("Tap to get current location", "Mode: GPS. Waiting for location...");
            checkAndRequestLocationPermissions();
        }
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;
        if (!isLocationInAllowedRegion(latitude, longitude)) {
            stopLocationUpdates();
            redirectToHomeWithDialog();
            return;
        }
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String subLocality = address.getSubLocality();
                String thoroughfare = address.getThoroughfare();
                StringBuilder addressTextBuilder = new StringBuilder();
                if (city != null && !city.isEmpty()) addressTextBuilder.append(city);
                else if (subLocality != null && !subLocality.isEmpty()) addressTextBuilder.append(subLocality);
                else if (thoroughfare != null && !thoroughfare.isEmpty()) addressTextBuilder.append(thoroughfare);
                else addressTextBuilder.append("Unknown Area");

                currentGeoPoint = new GeoPoint(latitude, longitude);
                itineraryViewModel.updateLocationStatus(addressTextBuilder.toString(), "GPS: " + addressTextBuilder.toString());
                fetchPlacesAndTriggerGeneration(false);
            } else {
                itineraryViewModel.updateLocationStatus("Location Name Not Found (Auto)", "GPS: Location Name Not Found");
            }
        } catch (IOException e) {
            itineraryViewModel.updateLocationStatus("Service to get address unavailable (Auto)", "Error getting address");
        }
    }

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

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || !sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        stopLocationUpdates(); // Got a location, stop for now.
                        break;
                    }
                }
            }
        };
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

    private boolean isLocationInAllowedRegion(double latitude, double longitude) {
        return latitude >= BAGUIO_REGION_MIN_LAT && latitude <= BAGUIO_REGION_MAX_LAT &&
                longitude >= BAGUIO_REGION_MIN_LON && longitude <= BAGUIO_REGION_MAX_LON;
    }

    private void redirectToHomeWithDialog() {
        Intent intent = new Intent(ItinerariesActivity.this, HomeActivity.class);
        intent.putExtra(HomeActivity.EXTRA_SHOW_OUTSIDE_REGION_DIALOG, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(CURRENT_ITEM_ID);
        String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
        if (mode.equals("auto")) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                checkAndRequestLocationPermissions();
            }
        } else {
            stopLocationUpdates();
            String name = sharedPreferences.getString(KEY_MANUAL_LOCATION_NAME, "Not Set");
            itineraryViewModel.updateLocationStatus(name, "Manually set: " + name);
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
                        .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(ItinerariesActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_ITINERARIES))
                        .setNegativeButton("Cancel", (dialog, which) ->
                                itineraryViewModel.updateLocationStatus("Permission needed", "Location permission denied.")
                        )
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_ITINERARIES);
            }
        } else {
            String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
            if (mode.equals("auto")) {
                fetchLastLocation();
                startLocationUpdates();
            } else {
                stopLocationUpdates();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_ITINERARIES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
                if (mode.equals("auto")) {
                    fetchLastLocation();
                    startLocationUpdates();
                }
            } else {
                itineraryViewModel.updateLocationStatus("Permission denied", "Location permission denied.");
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!"auto".equals(sharedPreferences.getString(KEY_LOCATION_MODE, "auto"))) return;

        itineraryViewModel.updateLocationStatus(itineraryViewModel.currentLocationName.getValue(), "Fetching last known location...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        itineraryViewModel.updateLocationStatus(itineraryViewModel.currentLocationName.getValue(), "Last location null, waiting for live updates...");
                    }
                })
                .addOnFailureListener(this, e -> {
                    itineraryViewModel.updateLocationStatus("Failed to get location", "Failed to get last location.");
                });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) {
            stopLocationUpdates();
            return;
        }
        if (requestingLocationUpdates) {
            return;
        }
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();
        requestingLocationUpdates = true;
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        itineraryViewModel.updateLocationStatus(itineraryViewModel.currentLocationName.getValue(), "Updating location (GPS)...");
    }

    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
        }
    }

    @Override
    public void onRegenerateClicked() {
        fetchPlacesAndTriggerGeneration(true);
    }

    @Override
    public void onClearClicked() {
        itineraryViewModel.clearItinerary();
    }

    @Override
    public void onEditLocationClicked() {
        showLocationChoiceDialog();
    }
}
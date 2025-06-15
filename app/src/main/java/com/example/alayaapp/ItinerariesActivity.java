package com.example.alayaapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import android.widget.Toast;

import com.example.alayaapp.ItineraryGenerator;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ItinerariesActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    RecyclerView rvMain;
    ItineraryAdapter itineraryAdapter;
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
    private GeoPoint manualGeoPoint = null;
    private FirebaseFirestore db;
    private List<Place> allPlacesList = new ArrayList<>();
    private ItineraryGenerator itineraryGenerator;
    private static final String KEY_TRIP_DATE_YEAR = "trip_date_year";
    private static final String KEY_TRIP_DATE_MONTH = "trip_date_month";
    private static final String KEY_TRIP_DATE_DAY = "trip_date_day";
    private Calendar tripDateCalendar;
    private static final double BAGUIO_REGION_MIN_LAT = 16.35;
    private static final double BAGUIO_REGION_MAX_LAT = 16.50;
    private static final double BAGUIO_REGION_MIN_LON = 120.55;
    private static final double BAGUIO_REGION_MAX_LON = 120.65;

    private List<Object> displayItems = new ArrayList<>();
    private String currentLocationName = "Tap to get current location";
    private String currentLocationStatus = "Set your location to begin";

    private final ActivityResultLauncher<Intent> manualLocationPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String locationName = data.getStringExtra("selected_location_name");
                    double latitude = data.getDoubleExtra("selected_latitude", 0.0);
                    double longitude = data.getDoubleExtra("selected_longitude", 0.0);
                    if (locationName != null && !locationName.isEmpty()) {
                        manualGeoPoint = new GeoPoint(latitude, longitude);
                        currentLocationName = locationName;
                        currentLocationStatus = "Manually set: " + locationName;
                        saveLocationPreference("manual", locationName, latitude, longitude);
                        stopLocationUpdates();
                        fetchPlacesAndGenerateItinerary(manualGeoPoint);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itineraries);

        rvMain = findViewById(R.id.rv_itineraries_main);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        db = FirebaseFirestore.getInstance();
        itineraryGenerator = new ItineraryGenerator();
        tripDateCalendar = Calendar.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationCallback();
        setupRecyclerView();
        bottomNavigationView.setSelectedItemId(CURRENT_ITEM_ID);
        setupBottomNavListener();

        loadLocationPreferenceAndInitialize();
    }

    private void setupRecyclerView() {
        itineraryAdapter = new ItineraryAdapter(this, displayItems);
        rvMain.setLayoutManager(new LinearLayoutManager(this));
        rvMain.setAdapter(itineraryAdapter);
    }

    private void buildDisplayList(List<ItineraryItem> generatedItems) {
        displayItems.clear();
        displayItems.add(new ItineraryAdapter.LocationHeaderData());

        if (generatedItems != null && !generatedItems.isEmpty()) {
            displayItems.add("Suggested Itinerary");
            displayItems.addAll(generatedItems);
        }

        List<RecommendedItineraryAdapter.RecommendedPlace> recommendedPlaces = new ArrayList<>();
        recommendedPlaces.add(new RecommendedItineraryAdapter.RecommendedPlace("Arca's Yard Cafe", "3.0", "(1K)"));
        recommendedPlaces.add(new RecommendedItineraryAdapter.RecommendedPlace("Wright Park Riding Center", "3.3", "(4K)"));
        recommendedPlaces.add(new RecommendedItineraryAdapter.RecommendedPlace("Baguio Orchidarium", "3.5", "(2K)"));
        recommendedPlaces.add(new RecommendedItineraryAdapter.RecommendedPlace("Camp John Hay Picnic Area", "4.0", "(5K)"));

        if (!recommendedPlaces.isEmpty()) {
            displayItems.add("Recommended Other Itineraries");
            displayItems.add(new ItineraryAdapter.HorizontalListContainer(recommendedPlaces));
        }

        itineraryAdapter.notifyDataSetChanged();
    }

    private void loadTripDate() {
        if (sharedPreferences.contains(KEY_TRIP_DATE_YEAR)) {
            int year = sharedPreferences.getInt(KEY_TRIP_DATE_YEAR, tripDateCalendar.get(Calendar.YEAR));
            int month = sharedPreferences.getInt(KEY_TRIP_DATE_MONTH, tripDateCalendar.get(Calendar.MONTH));
            int day = sharedPreferences.getInt(KEY_TRIP_DATE_DAY, tripDateCalendar.get(Calendar.DAY_OF_MONTH));
            tripDateCalendar.set(year, month, day);
        }
    }

    private void fetchPlacesAndGenerateItinerary(GeoPoint startLocation) {
        if (startLocation == null) {
            Toast.makeText(this, "Cannot generate itinerary without a start location.", Toast.LENGTH_LONG).show();
            buildDisplayList(new ArrayList<>());
            return;
        }
        loadTripDate();
        Toast.makeText(this, "Generating itinerary...", Toast.LENGTH_SHORT).show();

        db.collection("places")
                .get()
                .addOnCompleteListener(task -> {
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

                        List<ItineraryItem> generatedItems = itineraryGenerator.generate(startLocation, allPlacesList, tripDateCalendar);
                        if (!generatedItems.isEmpty()) {
                            Toast.makeText(this, "Itinerary generated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Could not generate an itinerary for this date/location.", Toast.LENGTH_LONG).show();
                        }
                        buildDisplayList(generatedItems);

                    } else {
                        Log.w(TAG_LOCATION, "Error getting documents from Firestore.", task.getException());
                        Toast.makeText(ItinerariesActivity.this, "Failed to load places for itinerary.", Toast.LENGTH_LONG).show();
                        buildDisplayList(new ArrayList<>());
                    }
                });
    }

    public void showLocationChoiceDialog() {
        final CharSequence[] options = {"Use My Current GPS Location", "Set Location Manually", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Location Method");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Use My Current GPS Location")) {
                saveLocationPreference("auto", null, 0, 0);
                manualGeoPoint = null;
                currentLocationName = "Fetching GPS location...";
                currentLocationStatus = "Mode: GPS";
                itineraryAdapter.notifyItemChanged(0); // Update header
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
                manualGeoPoint = new GeoPoint(lat, lon);
                currentLocationName = name;
                currentLocationStatus = "Manually set: " + name;
                stopLocationUpdates();
                if (manualGeoPoint != null) {
                    fetchPlacesAndGenerateItinerary(manualGeoPoint);
                }
            } else {
                saveLocationPreference("auto", null, 0,0);
                checkAndRequestLocationPermissions();
            }
        } else {
            currentLocationName = "Tap to get current location";
            currentLocationStatus = "Mode: GPS. Waiting for location...";
            checkAndRequestLocationPermissions();
        }
        buildDisplayList(new ArrayList<>()); // Initial build with header
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;

        if (!isLocationInAllowedRegion(latitude, longitude)) {
            stopLocationUpdates();
            currentLocationName = "Outside supported region";
            currentLocationStatus = "Please set a location in Baguio.";
            itineraryAdapter.notifyItemChanged(0);
            showOutsideRegionDialog();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                // ... (address building logic is the same)
                Address address = addresses.get(0);
                String city = address.getLocality();
                String subLocality = address.getSubLocality();
                String thoroughfare = address.getThoroughfare();
                StringBuilder addressTextBuilder = new StringBuilder();
                if (city != null && !city.isEmpty()) addressTextBuilder.append(city);
                else if (subLocality != null && !subLocality.isEmpty()) addressTextBuilder.append(subLocality);
                else if (thoroughfare != null && !thoroughfare.isEmpty()) addressTextBuilder.append(thoroughfare);
                else addressTextBuilder.append("Unknown Area");
                currentLocationName = addressTextBuilder.toString();
                currentLocationStatus = "GPS: " + currentLocationName;
                GeoPoint startPoint = new GeoPoint(latitude, longitude);
                fetchPlacesAndGenerateItinerary(startPoint);
            } else {
                currentLocationName = "Location Name Not Found (Auto)";
                currentLocationStatus = "GPS: Location Name Not Found";
                itineraryAdapter.notifyItemChanged(0);
            }
        } catch (IOException e) {
            currentLocationName = "Service to get address unavailable (Auto)";
            itineraryAdapter.notifyItemChanged(0);
        }
    }

    // --- Helper getters for the adapter ---
    public String getCurrentLocationNameToDisplay() {
        return currentLocationName;
    }

    public String getCurrentLocationStatusToDisplay() {
        return currentLocationStatus;
    }

    // --- Unchanged methods below ---
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
                        stopLocationUpdates();
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

    private void showOutsideRegionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Outside Baguio Region")
                .setMessage("Your current location is outside the supported region. Please set a location within Baguio to generate an itinerary.")
                .setPositiveButton("Set Manually", (dialog, which) -> {
                    Intent intent = new Intent(ItinerariesActivity.this, ManualLocationPickerActivity.class);
                    manualLocationPickerLauncher.launch(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
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
            currentLocationName = sharedPreferences.getString(KEY_MANUAL_LOCATION_NAME, "Not Set");
            currentLocationStatus = "Manually set: " + currentLocationName;
            itineraryAdapter.notifyItemChanged(0);
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
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            currentLocationName = "Permission needed";
                            currentLocationStatus = "Location permission denied.";
                            itineraryAdapter.notifyItemChanged(0);
                        })
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
                currentLocationName = "Location permission denied";
                currentLocationStatus = "Location permission denied.";
                itineraryAdapter.notifyItemChanged(0);
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;
        currentLocationStatus = "Fetching last known location...";
        itineraryAdapter.notifyItemChanged(0);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        if (sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) {
                            currentLocationStatus = "Last location null, waiting for live updates...";
                            itineraryAdapter.notifyItemChanged(0);
                        }
                    }
                })
                .addOnFailureListener(this, e -> {
                    currentLocationName = "Failed to get location";
                    currentLocationStatus = "Failed to get last location.";
                    itineraryAdapter.notifyItemChanged(0);
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
        currentLocationStatus = "Updating location (GPS)...";
        itineraryAdapter.notifyItemChanged(0);
    }

    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
        }
    }
}
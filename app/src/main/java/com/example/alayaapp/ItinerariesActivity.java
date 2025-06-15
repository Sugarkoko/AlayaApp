package com.example.alayaapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import android.widget.ImageButton;
import android.widget.TextView;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ItinerariesActivity extends AppCompatActivity implements ItineraryAdapter.OnStartDragListener {
    BottomNavigationView bottomNavigationView;
    View ivEditItineraryIcon;
    TextView tvSaveChanges;
    RecyclerView rvSuggestedItinerary;
    ItineraryAdapter itineraryAdapter;
    ItemTouchHelper itemTouchHelper;
    ItineraryItemTouchHelperCallback touchHelperCallback;
    private boolean isEditMode = false;
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
    private String currentLocationNameToDisplay = "Tap to get current location";
    private GeoPoint manualGeoPoint = null;
    private TextView tvLocationCityItineraries;
    private TextView tvLocationStatusItineraries;
    private ImageButton ibEditLocationItineraries;
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

    // A single list to hold all items for the main RecyclerView
    private List<Object> displayItems = new ArrayList<>();

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
                        manualGeoPoint = new GeoPoint(latitude, longitude);
                        tvLocationCityItineraries.setText(currentLocationNameToDisplay);
                        tvLocationStatusItineraries.setText("Manually set: " + currentLocationNameToDisplay);
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

        ivEditItineraryIcon = findViewById(R.id.iv_edit_itinerary);
        tvSaveChanges = findViewById(R.id.tv_save_changes);
        rvSuggestedItinerary = findViewById(R.id.rv_suggested_itinerary);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        tvLocationCityItineraries = findViewById(R.id.tv_location_city_itineraries);
        tvLocationStatusItineraries = findViewById(R.id.tv_location_status_itineraries);
        ibEditLocationItineraries = findViewById(R.id.ib_edit_location_itineraries);

        db = FirebaseFirestore.getInstance();
        itineraryGenerator = new ItineraryGenerator();
        tripDateCalendar = Calendar.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationCallback();
        setupRecyclerView();
        bottomNavigationView.setSelectedItemId(CURRENT_ITEM_ID);
        setupBottomNavListener();

        ivEditItineraryIcon.setOnClickListener(v -> enterEditMode());
        tvSaveChanges.setOnClickListener(v -> exitEditModeAndSave());
        ibEditLocationItineraries.setOnClickListener(v -> showLocationChoiceDialog());

        loadLocationPreferenceAndInitialize();
    }

    private void setupRecyclerView() {
        // The adapter now takes a List<Object>
        itineraryAdapter = new ItineraryAdapter(displayItems, this);
        rvSuggestedItinerary.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestedItinerary.setAdapter(itineraryAdapter);
        touchHelperCallback = new ItineraryItemTouchHelperCallback(itineraryAdapter);
        itemTouchHelper = new ItemTouchHelper(touchHelperCallback);
        itemTouchHelper.attachToRecyclerView(rvSuggestedItinerary);
    }

    private void buildDisplayList(List<ItineraryItem> generatedItems) {
        displayItems.clear();

        // Add the main itinerary items
        if (generatedItems != null && !generatedItems.isEmpty()) {
            displayItems.add("Suggested Itinerary"); // Header
            displayItems.addAll(generatedItems);
            ivEditItineraryIcon.setVisibility(View.VISIBLE);
        } else {
            ivEditItineraryIcon.setVisibility(View.GONE);
        }

        // Add the recommended items section
        List<RecommendedItineraryAdapter.RecommendedPlace> recommendedPlaces = new ArrayList<>();
        recommendedPlaces.add(new RecommendedItineraryAdapter.RecommendedPlace("Arca's Yard Cafe", "3.0", "(1K)"));
        recommendedPlaces.add(new RecommendedItineraryAdapter.RecommendedPlace("Wright Park Riding Center", "3.3", "(4K)"));
        recommendedPlaces.add(new RecommendedItineraryAdapter.RecommendedPlace("Baguio Orchidarium", "3.5", "(2K)"));
        recommendedPlaces.add(new RecommendedItineraryAdapter.RecommendedPlace("Camp John Hay Picnic Area", "4.0", "(5K)"));

        if (!recommendedPlaces.isEmpty()) {
            displayItems.add("Recommended Other Itineraries"); // Header
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
            buildDisplayList(new ArrayList<>()); // Build list with no items
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
                        buildDisplayList(generatedItems); // Build the final list for the adapter

                    } else {
                        Log.w(TAG_LOCATION, "Error getting documents from Firestore.", task.getException());
                        Toast.makeText(ItinerariesActivity.this, "Failed to load places for itinerary.", Toast.LENGTH_LONG).show();
                        buildDisplayList(new ArrayList<>()); // Build empty list on failure
                    }
                });
    }

    private void enterEditMode() {
        isEditMode = true;
        ivEditItineraryIcon.setVisibility(View.GONE);
        tvSaveChanges.setVisibility(View.VISIBLE);
        itineraryAdapter.setEditMode(true);
        if(touchHelperCallback != null) touchHelperCallback.setEditMode(true);
        Toast.makeText(this, "Edit mode enabled. Drag handles to reorder.", Toast.LENGTH_SHORT).show();
    }

    private void exitEditModeAndSave() {
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

    // --- Other methods (BottomNav, Permissions, Location logic) remain unchanged ---
    // ... (paste the rest of your ItinerariesActivity methods here, they don't need to change)
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
                if (locationResult == null) return;
                if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;
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
                manualGeoPoint = new GeoPoint(lat, lon);
                tvLocationCityItineraries.setText(currentLocationNameToDisplay);
                tvLocationStatusItineraries.setText("Manually set: " + currentLocationNameToDisplay);
                stopLocationUpdates();
                if (manualGeoPoint != null) {
                    fetchPlacesAndGenerateItinerary(manualGeoPoint);
                }
            } else {
                saveLocationPreference("auto", null, 0,0);
                checkAndRequestLocationPermissions();
            }
        } else {
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

    private void getAddressFromLocation(double latitude, double longitude) {
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;

        if (!isLocationInAllowedRegion(latitude, longitude)) {
            stopLocationUpdates();
            tvLocationCityItineraries.setText("Outside supported region");
            tvLocationStatusItineraries.setText("Please set a location in Baguio.");
            showOutsideRegionDialog();
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
                GeoPoint startPoint = new GeoPoint(latitude, longitude);
                fetchPlacesAndGenerateItinerary(startPoint);
            } else {
                tvLocationCityItineraries.setText("Location Name Not Found (Auto)");
                tvLocationStatusItineraries.setText("GPS: Location Name Not Found");
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
                        .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(ItinerariesActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_ITINERARIES))
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            tvLocationCityItineraries.setText("Permission needed");
                            tvLocationStatusItineraries.setText("Location permission denied.");
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
                tvLocationCityItineraries.setText("Location permission denied");
                tvLocationStatusItineraries.setText("Location permission denied.");
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                        if (sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) {
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
        tvLocationStatusItineraries.setText("Updating location (GPS)...");
    }

    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
        }
    }
}
package com.example.alayaapp;

import android.Manifest;
import android.app.DatePickerDialog;
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
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alayaapp.databinding.ActivityHomeBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.alayaapp.util.GeoPoint;

import java.io.IOException;
import java.text.SimpleDateFormat; // Added
import java.util.ArrayList;
import java.util.Calendar; // Added
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;

    final int CURRENT_ITEM_ID = R.id.navigation_home;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TAG = "HomeActivity";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates = false;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AlayaAppPrefs";
    // Location Prefs (already exist)
    private static final String KEY_LOCATION_MODE = "location_mode";
    private static final String KEY_MANUAL_LOCATION_NAME = "manual_location_name";
    private static final String KEY_MANUAL_LATITUDE = "manual_latitude";
    private static final String KEY_MANUAL_LONGITUDE = "manual_longitude";
    // Trip Date Prefs (NEW)
    private static final String KEY_TRIP_DATE_YEAR = "trip_date_year";
    private static final String KEY_TRIP_DATE_MONTH = "trip_date_month";
    private static final String KEY_TRIP_DATE_DAY = "trip_date_day";


    private String currentLocationNameToDisplay = "Tap to get current location";
    private GeoPoint manualGeoPoint = null;

    private PlaceAdapter placeAdapter;
    private List<Place> placesList;
    private FirebaseFirestore db;

    private Calendar tripDateCalendar;
    private DatePickerDialog.OnDateSetListener dateSetListener;

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
                        binding.tvLocationCity2.setText(currentLocationNameToDisplay);
                        if (binding.tvDirectionText != null) {
                            binding.tvDirectionText.setText("Manually set: " + currentLocationNameToDisplay);
                        }
                        saveLocationPreference("manual", locationName, latitude, longitude);
                        stopLocationUpdates();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();

        db = FirebaseFirestore.getInstance();
        placesList = new ArrayList<>();
        binding.rvPlacesList.setLayoutManager(new LinearLayoutManager(this));
        placeAdapter = new PlaceAdapter(this, placesList);
        binding.rvPlacesList.setAdapter(placeAdapter);

        tripDateCalendar = Calendar.getInstance(); // Initialize calendar

        setupTripDatePicker();
        loadSavedTripDate();

        binding.bottomNavigation.setSelectedItemId(CURRENT_ITEM_ID);
        binding.ibEditLocation.setOnClickListener(v -> showLocationChoiceDialog());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == CURRENT_ITEM_ID) {
                return true;
            }
            Intent intent = null;
            if (itemId == R.id.navigation_itineraries) {
                intent = new Intent(getApplicationContext(), ItinerariesActivity.class);
            } else if (itemId == R.id.navigation_map) {
                intent = new Intent(getApplicationContext(), MapsActivity.class);
            } else if (itemId == R.id.navigation_profile) {
                intent = new Intent(getApplicationContext(), ProfileActivity.class);
            }
            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });

        loadLocationPreferenceAndInitialize();
        fetchPlacesFromFirestore();
    }


    private void setupTripDatePicker() {
        dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            tripDateCalendar.set(Calendar.YEAR, year);
            tripDateCalendar.set(Calendar.MONTH, monthOfYear);
            tripDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateTripDateButtonText();
            saveTripDate(year, monthOfYear, dayOfMonth);
            Toast.makeText(HomeActivity.this, "Trip date set to: " + getFormattedDate(tripDateCalendar), Toast.LENGTH_SHORT).show();
        };

        binding.btnTripDate.setOnClickListener(v -> {
            new DatePickerDialog(HomeActivity.this, dateSetListener,
                    tripDateCalendar.get(Calendar.YEAR),
                    tripDateCalendar.get(Calendar.MONTH),
                    tripDateCalendar.get(Calendar.DAY_OF_MONTH))
                    .show();
        });
    }

    private void loadSavedTripDate() {
        if (sharedPreferences.contains(KEY_TRIP_DATE_YEAR)) {
            int year = sharedPreferences.getInt(KEY_TRIP_DATE_YEAR, tripDateCalendar.get(Calendar.YEAR));
            int month = sharedPreferences.getInt(KEY_TRIP_DATE_MONTH, tripDateCalendar.get(Calendar.MONTH));
            int day = sharedPreferences.getInt(KEY_TRIP_DATE_DAY, tripDateCalendar.get(Calendar.DAY_OF_MONTH));
            tripDateCalendar.set(year, month, day);
        }
        updateTripDateButtonText();
    }

    private void updateTripDateButtonText() {
        binding.btnTripDate.setText(getFormattedDate(tripDateCalendar));
    }

    private String getFormattedDate(Calendar calendar) {
        // Example format: "Mon, Apr 22, 2024"

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        if (sharedPreferences.contains(KEY_TRIP_DATE_YEAR)) { // Check if a date was ever saved
            return sdf.format(calendar.getTime());
        } else {
            return "When is your Trip?";
        }
    }

    private void saveTripDate(int year, int month, int day) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_TRIP_DATE_YEAR, year);
        editor.putInt(KEY_TRIP_DATE_MONTH, month);
        editor.putInt(KEY_TRIP_DATE_DAY, day);
        editor.apply();
    }


    private void fetchPlacesFromFirestore() {
        Log.d(TAG, "fetchPlacesFromFirestore: Method entered. Showing ProgressBar.");
        if (binding.progressBarHome != null) binding.progressBarHome.setVisibility(View.VISIBLE);
        if (binding.rvPlacesList != null) binding.rvPlacesList.setVisibility(View.GONE);
        if (binding.tvEmptyPlaces != null) binding.tvEmptyPlaces.setVisibility(View.GONE);

        db.collection("places")
                .get()
                .addOnCompleteListener(task -> {
                    if (binding.progressBarHome != null) binding.progressBarHome.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            placesList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Place place = document.toObject(Place.class);
                                    if (place != null) {
                                        place.setDocumentId(document.getId());
                                        placesList.add(place);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting document: " + document.getId(), e);
                                }
                            }
                            if (!placesList.isEmpty()) {
                                placeAdapter.notifyDataSetChanged();
                                if (binding.rvPlacesList != null) binding.rvPlacesList.setVisibility(View.VISIBLE);
                                if (binding.tvEmptyPlaces != null) binding.tvEmptyPlaces.setVisibility(View.GONE);
                            } else {
                                if (binding.rvPlacesList != null) binding.rvPlacesList.setVisibility(View.GONE);
                                if (binding.tvEmptyPlaces != null) {
                                    binding.tvEmptyPlaces.setText("No places to display or error in data.");
                                    binding.tvEmptyPlaces.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            if (binding.rvPlacesList != null) binding.rvPlacesList.setVisibility(View.GONE);
                            if (binding.tvEmptyPlaces != null) {
                                binding.tvEmptyPlaces.setText("No places found nearby.");
                                binding.tvEmptyPlaces.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        Log.w(TAG, "Error getting documents from Firestore.", task.getException());
                        if (binding.rvPlacesList != null) binding.rvPlacesList.setVisibility(View.GONE);
                        if (binding.tvEmptyPlaces != null) {
                            binding.tvEmptyPlaces.setText("Failed to load places. Please check connection.");
                            binding.tvEmptyPlaces.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(HomeActivity.this, "Failed to load places.", Toast.LENGTH_LONG).show();
                    }
                });
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
                binding.tvLocationCity2.setText("Fetching GPS location...");
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Mode: GPS");
                checkAndRequestLocationPermissions();
            } else if (options[item].equals("Set Location Manually")) {
                Intent intent = new Intent(HomeActivity.this, ManualLocationPickerActivity.class);
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
                binding.tvLocationCity2.setText(currentLocationNameToDisplay);
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Manually set: " + currentLocationNameToDisplay);
                stopLocationUpdates();
            } else {
                saveLocationPreference("auto", null, 0,0); // Fallback
                checkAndRequestLocationPermissions();
            }
        } else {
            binding.tvLocationCity2.setText("Tap to get current location");
            if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Mode: GPS. Waiting for location...");
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
                binding.tvLocationCity2.setText(currentLocationNameToDisplay);
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("GPS: " + currentLocationNameToDisplay);
            } else {
                binding.tvLocationCity2.setText("Location Name Not Found (Auto)");
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("GPS: Location Name Not Found");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder service not available or IO error (Auto)", e);
            binding.tvLocationCity2.setText("Service to get address unavailable (Auto)");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid latitude or longitude values (Auto).", e);
            binding.tvLocationCity2.setText("Invalid location data (Auto)");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(CURRENT_ITEM_ID);
        }
        String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
        if (mode.equals("auto")) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        } else {
            stopLocationUpdates();
            String name = sharedPreferences.getString(KEY_MANUAL_LOCATION_NAME, "Not Set");
            binding.tvLocationCity2.setText(name); // Ensure manual location is displayed on resume
            if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Manually set: " + name);
        }
        loadSavedTripDate();
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
                        .setMessage("This app needs the Location permission to show your current location. Please allow.")
                        .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION))
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            binding.tvLocationCity2.setText("Permission needed");
                            if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Location permission denied.");
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
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
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
                if (mode.equals("auto")) {
                    fetchLastLocation();
                    startLocationUpdates();
                }
            } else {
                binding.tvLocationCity2.setText("Location permission denied");
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Location permission denied.");
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;
        if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Fetching last known location...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.d(TAG, "Last known location is null. Waiting for updates.");
                        if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Last location null, waiting for live updates...");
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error trying to get last GPS location", e);
                    binding.tvLocationCity2.setText("Failed to get location");
                    if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Failed to get last location.");
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
        if (requestingLocationUpdates) return;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();
        requestingLocationUpdates = true;
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Updating location (GPS)...");
    }

    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
        }
    }
}
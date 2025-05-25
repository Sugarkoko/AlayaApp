package com.example.alayaapp;

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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.alayaapp.databinding.ActivityHomeBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.util.GeoPoint; // For storing manual GeoPoint

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    final int CURRENT_ITEM_ID = R.id.navigation_home;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TAG = "HomeActivityLocation";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates = false;

    // SharedPreferences for location mode
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AlayaAppPrefs";
    private static final String KEY_LOCATION_MODE = "location_mode"; // "auto" or "manual"
    private static final String KEY_MANUAL_LOCATION_NAME = "manual_location_name";
    private static final String KEY_MANUAL_LATITUDE = "manual_latitude";
    private static final String KEY_MANUAL_LONGITUDE = "manual_longitude";

    private String currentLocationNameToDisplay = "Tap to get current location"; // Default or loaded
    private GeoPoint manualGeoPoint = null; // Store manually set location

    // ActivityResultLauncher for ManualLocationPickerActivity
    private final ActivityResultLauncher<Intent> manualLocationPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String locationName = data.getStringExtra("selected_location_name");
                    double latitude = data.getDoubleExtra("selected_latitude", 0.0);
                    double longitude = data.getDoubleExtra("selected_longitude", 0.0);

                    if (locationName != null && !locationName.isEmpty()) {
                        currentLocationNameToDisplay = locationName;
                        manualGeoPoint = new GeoPoint(latitude, longitude);
                        binding.tvLocationCity2.setText(currentLocationNameToDisplay);
                        if (binding.tvDirectionText != null) { // Check if tvDirectionText exists and is not null
                            binding.tvDirectionText.setText("Manually set: " + currentLocationNameToDisplay);
                        }
                        saveLocationPreference("manual", locationName, latitude, longitude);
                        stopLocationUpdates(); // Stop GPS if manual is set
                        // TODO: Trigger update of "Spots near you" if it becomes dynamic based on manualGeoPoint
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

        binding.bottomNavigation.setSelectedItemId(CURRENT_ITEM_ID);

        if (binding.cardBurnhamPark != null) {
            binding.cardBurnhamPark.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, BurnhamDetailsActivity.class);
                startActivity(intent);
            });
        }

        binding.ibEditLocation.setOnClickListener(v -> showLocationChoiceDialog());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == CURRENT_ITEM_ID) {
                return true;
            } else if (itemId == R.id.navigation_itineraries) {
                Intent intent = new Intent(getApplicationContext(), ItinerariesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.navigation_map) {
                Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });

        loadLocationPreferenceAndInitialize();
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;

                // Only process if in "auto" mode
                if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        // Consider stopping updates if you only need one good fix in auto mode after app start
                        // stopLocationUpdates();
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
                checkAndRequestLocationPermissions(); // This will trigger fetchLastLocation or startLocationUpdates
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
                manualGeoPoint = new GeoPoint(lat, lon);
                binding.tvLocationCity2.setText(currentLocationNameToDisplay);
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Manually set: " + currentLocationNameToDisplay);
                stopLocationUpdates(); // Ensure GPS is off if manual is loaded
            } else {
                // Fallback to auto if manual data is corrupt or incomplete
                saveLocationPreference("auto", null, 0,0);
                checkAndRequestLocationPermissions();
            }
        } else { // "auto"
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
        } else { // "auto" or clearing manual
            editor.remove(KEY_MANUAL_LOCATION_NAME);
            editor.remove(KEY_MANUAL_LATITUDE);
            editor.remove(KEY_MANUAL_LONGITUDE);
        }
        editor.apply();
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        // Only update UI if in "auto" mode and GPS is the source
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) {
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String subLocality = address.getSubLocality();
                String thoroughfare = address.getThoroughfare(); // Street name
                String country = address.getCountryName();

                StringBuilder addressTextBuilder = new StringBuilder();
                if (city != null && !city.isEmpty()) {
                    addressTextBuilder.append(city);
                } else if (subLocality != null && !subLocality.isEmpty()) {
                    addressTextBuilder.append(subLocality);
                } else if (thoroughfare != null && !thoroughfare.isEmpty()) {
                    addressTextBuilder.append(thoroughfare);
                } else {
                    addressTextBuilder.append("Unknown Area");
                }

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

                Log.d(TAG, "Fetched Address (Auto): " + currentLocationNameToDisplay);
            } else {
                binding.tvLocationCity2.setText("Location Name Not Found (Auto)");
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("GPS: Location Name Not Found");
                Log.w(TAG, "No address found for the location (Auto).");
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
            // Manual mode: display stored location (already handled by loadLocationPreferenceAndInitialize)
            // but ensure GPS updates are stopped.
            stopLocationUpdates();
            String name = sharedPreferences.getString(KEY_MANUAL_LOCATION_NAME, "Not Set");
            binding.tvLocationCity2.setText(name);
            if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Manually set: " + name);
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
            // Permission already granted
            String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
            if (mode.equals("auto")) {
                fetchLastLocation();
                startLocationUpdates(); // Start continuous updates if in auto mode and permission granted
            } else {
                // In manual mode, no need to fetch GPS unless user explicitly switches
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

        // Only fetch if in "auto" mode
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) {
            return;
        }
        if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Fetching last known location...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.d(TAG, "Last known location is null. Waiting for updates.");
                        if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Last location null, waiting for live updates...");
                        // startLocationUpdates() will be called if permissions are good
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
        // Only start if in "auto" mode
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) {
            Log.d(TAG, "In manual mode, not starting GPS updates.");
            stopLocationUpdates(); // Ensure they are stopped if user switched to manual
            return;
        }

        if (requestingLocationUpdates) { // Don't request if already doing so
            Log.d(TAG, "startLocationUpdates: Already requesting.");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // e.g., 10 seconds interval
                .setMinUpdateIntervalMillis(5000) // e.g., 5 seconds minimum interval
                .build();
        requestingLocationUpdates = true;
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Updating location (GPS)...");
        Log.d(TAG, "Requested location updates.");
    }

    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
            Log.d(TAG, "Location updates stopped.");
            if (binding.tvDirectionText != null && sharedPreferences.getString(KEY_LOCATION_MODE,"auto").equals("auto")) {
                // binding.tvDirectionText.setText("GPS updates stopped."); // Optional feedback
            }
        }
    }
}
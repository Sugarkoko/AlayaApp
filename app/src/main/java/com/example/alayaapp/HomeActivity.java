package com.example.alayaapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
import com.google.android.material.bottomnavigation.BottomNavigationView; // Keep if still using directly

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    // Declare the BottomNavigationView if needed, though binding.bottomNavigation should work
    // BottomNavigationView bottomNavigationView;
    // CardView burnhamCard; // Access via binding.cardBurnhamPark

    final int CURRENT_ITEM_ID = R.id.navigation_home;

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TAG = "HomeActivityLocation";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        stopLocationUpdates(); // Stop after getting one location
                        break;
                    }
                }
            }
        };

        // --- Set Initial State for Bottom Nav ---
        binding.bottomNavigation.setSelectedItemId(CURRENT_ITEM_ID);

        // --- Set Listeners ---
        if (binding.cardBurnhamPark != null) {
            binding.cardBurnhamPark.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, BurnhamDetailsActivity.class);
                startActivity(intent);
                // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        binding.tvLocationCity2.setOnClickListener(v -> {
            checkAndRequestLocationPermissions();
        });

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
    }

    private void checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission to show your current location. Please allow.")
                        .setPositiveButton("OK", (dialogInterface, i) -> {
                            ActivityCompat.requestPermissions(HomeActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    REQUEST_LOCATION_PERMISSION);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            binding.tvLocationCity2.setText("Permission needed to show location");
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_LOCATION_PERMISSION);
            }
        } else {
            fetchLastLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLastLocation();
            } else {
                binding.tvLocationCity2.setText("Location permission denied");
                Toast.makeText(this, "Location permission denied. Cannot fetch current location.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            binding.tvLocationCity2.setText("Permission not granted");
            // If this is reached, it means permission was revoked after initial check or a logic error.
            // Re-triggering permission check might be an option, but could lead to loops.
            // Best to inform the user.
            Toast.makeText(this, "Location permission is required. Please grant it in app settings.", Toast.LENGTH_LONG).show();
            return;
        }

        binding.tvLocationCity2.setText("Fetching location...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.d(TAG, "Last known location is null. Requesting new location update.");
                        startLocationUpdates();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error trying to get last GPS location", e);
                    binding.tvLocationCity2.setText("Failed to get location.");
                    Toast.makeText(HomeActivity.this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should have permission by this point if flow is correct
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // e.g., 10 seconds interval
                .setMinUpdateIntervalMillis(5000) // e.g., 5 seconds minimum interval
                .build();

        requestingLocationUpdates = true;
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
        binding.tvLocationCity2.setText("Updating location...");
    }

    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
            Log.d(TAG, "Location updates stopped.");
        }
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String subLocality = address.getSubLocality(); // More specific like neighborhood
                String thoroughfare = address.getThoroughfare(); // Street name
                String country = address.getCountryName();

                StringBuilder addressText = new StringBuilder();
                if (city != null && !city.isEmpty()) {
                    addressText.append(city);
                } else if (subLocality != null && !subLocality.isEmpty()) {
                    addressText.append(subLocality);
                } else if (thoroughfare != null && !thoroughfare.isEmpty()) {
                    addressText.append(thoroughfare);
                } else {
                    addressText.append("Unknown Area");
                }
                if (country != null && !country.isEmpty() && addressText.length() > 0 && !addressText.toString().equals("Unknown Area")) {
                    if (!city.equalsIgnoreCase(country)) { // Avoid "Philippines, Philippines"
                        addressText.append(", ").append(country);
                    }
                } else if (country != null && !country.isEmpty() && addressText.toString().equals("Unknown Area")) {
                    addressText.replace(0, addressText.length(), country);
                }


                if (addressText.length() == 0) {
                    binding.tvLocationCity2.setText("Location Name Not Found");
                } else {
                    binding.tvLocationCity2.setText(addressText.toString());
                }
                Log.d(TAG, "Fetched Address: " + addressText.toString());

            } else {
                binding.tvLocationCity2.setText("Location Name Not Found");
                Log.w(TAG, "No address found for the location.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder service not available or IO error", e);
            binding.tvLocationCity2.setText("Service to get address unavailable");
            // Don't show a toast for every geocoder failure, could be annoying if offline
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid latitude or longitude values.", e);
            binding.tvLocationCity2.setText("Invalid location data");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // This ensures the "Home" icon is selected if the user navigates back
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(CURRENT_ITEM_ID);
        }
        // If you want location updates to resume when activity is resumed (and was previously requesting):
        // if (requestingLocationUpdates && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        //     startLocationUpdates();
        // }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates(); // Important to conserve battery
    }
}
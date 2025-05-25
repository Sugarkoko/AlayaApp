package com.example.alayaapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager; // For OSMDroid configuration
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alayaapp.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

// OSMDroid Imports
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.api.IMapController;


public class MapsActivity extends AppCompatActivity {

    private static final String TAG = "MapsActivity";
    private ActivityMapsBinding binding;
    private MapView mapView; // OSMDroid MapView
    private MyLocationNewOverlay myLocationOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates = false;

    private static final int REQUEST_LOCATION_PERMISSION_MAPS = 2; // Different from HomeActivity's
    final int CURRENT_ITEM_ID = R.id.navigation_map;
    private GeoPoint currentDeviceLocation = null; // To store the current location

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        EdgeToEdge.enable(this);

        try {
            binding = ActivityMapsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading page.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mapView = binding.mapViewOsm;

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Keep bottom padding 0 for CoordinatorLayout

            return insets;
        });

        setupOsmMap();
        setupBottomNavigation();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();
        checkAndRequestLocationPermissions();

        binding.fabMyLocation.setOnClickListener(v -> {
            if (currentDeviceLocation != null) {
                mapView.getController().animateTo(currentDeviceLocation);
            } else if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                mapView.getController().animateTo(myLocationOverlay.getMyLocation());
            } else {
                Toast.makeText(this, "Current location not available yet.", Toast.LENGTH_SHORT).show();
                checkAndRequestLocationPermissions(); // Try to get location again
            }
        });
    }

    private void setupOsmMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Default tile source
        mapView.setMultiTouchControls(true); // Enable pinch zoom, etc.
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT); // Optional: Show zoom buttons

        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0); // Default zoom level
        // Set a default center point (e.g., Baguio City or 0,0) until location is found
        mapController.setCenter(new GeoPoint(16.4023, 120.5960)); // Example: Baguio City

        // Initialize MyLocationNewOverlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation(); // This will attempt to get location and draw it
        // myLocationOverlay.enableFollowLocation(); // Uncomment if you want the map to follow the location
        mapView.getOverlays().add(myLocationOverlay);
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                currentDeviceLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "New location: " + currentDeviceLocation.getLatitude() + ", " + currentDeviceLocation.getLongitude());

                // myLocationOverlay will handle drawing the blue dot.
                // If not using enableFollowLocation, you might want to center manually once:
                if (!myLocationOverlay.isFollowLocationEnabled()) {
                    mapView.getController().animateTo(currentDeviceLocation);
                }
                binding.tvDirectionText.setText("Location Acquired. Current: " + String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude()));
                // stopLocationUpdates(); // Stop after getting a good location if not continuous tracking
            }
        };
    }


    private void checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission to show your current location on the map. Please allow.")
                        .setPositiveButton("OK", (dialogInterface, i) ->
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                        REQUEST_LOCATION_PERMISSION_MAPS))
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
                            binding.tvDirectionText.setText("Location permission denied.");
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_LOCATION_PERMISSION_MAPS);
            }
        } else {
            // Permission already granted
            startLocationUpdates();
            fetchLastKnownLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_MAPS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
                fetchLastKnownLocation();
                if (myLocationOverlay != null) {
                    myLocationOverlay.enableMyLocation(); // Re-enable if it was off
                }
            } else {
                Toast.makeText(this, "Location permission denied. Map functionality limited.", Toast.LENGTH_LONG).show();
                binding.tvDirectionText.setText("Location permission denied.");
            }
        }
    }

    private void fetchLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Permissions not granted
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentDeviceLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "Last known location: " + currentDeviceLocation.getLatitude() + ", " + currentDeviceLocation.getLongitude());
                        mapView.getController().setCenter(currentDeviceLocation);
                        mapView.getController().setZoom(17.0); // Zoom in closer for known location
                        binding.tvDirectionText.setText("Current: " + String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude()));

                    } else {
                        Log.d(TAG, "Last known location is null. Requesting updates.");
                        // startLocationUpdates(); // Already called if permission granted path
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error trying to get last GPS location", e);
                    Toast.makeText(MapsActivity.this, "Failed to get last known location.", Toast.LENGTH_SHORT).show();
                });
    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "startLocationUpdates: Permissions not granted.");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 seconds
                .setMinUpdateIntervalMillis(5000) // 5 seconds
                .build();

        requestingLocationUpdates = true;
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        binding.tvDirectionText.setText("Acquiring location...");
        Log.d(TAG, "Requested location updates.");
    }

    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
            Log.d(TAG, "Location updates stopped.");
        }
    }


    private void setupBottomNavigation() {
        if (binding.bottomNavigationMapsPage == null) {
            Log.e(TAG, "BottomNavigationView (bottom_navigation_maps_page) not found in layout!");
            return;
        }
        binding.bottomNavigationMapsPage.setSelectedItemId(CURRENT_ITEM_ID);
        binding.bottomNavigationMapsPage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();
            if (destinationItemId == CURRENT_ITEM_ID) {
                return true;
            }
            Class<?> destinationActivityClass = null;
            boolean finishCurrent = true;

            if (destinationItemId == R.id.navigation_home) {
                destinationActivityClass = HomeActivity.class;
            } else if (destinationItemId == R.id.navigation_itineraries) {
                destinationActivityClass = ItinerariesActivity.class;
            } else if (destinationItemId == R.id.navigation_profile) {
                destinationActivityClass = ProfileActivity.class;
            }

            if (destinationActivityClass != null) {
                Intent intent = new Intent(MapsActivity.this, destinationActivityClass);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                boolean slideRightToLeft = getItemIndex(destinationItemId) > getItemIndex(CURRENT_ITEM_ID);
                if (slideRightToLeft) {
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
                if (finishCurrent) {
                    finish();
                }
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

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume(); // Needed for osmdroid
        }
        // If you want location updates to resume when activity is resumed:
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates(); // Or based on a flag if you want to control it more finely
        }
        // Ensure correct bottom nav item is selected
        binding.bottomNavigationMapsPage.setSelectedItemId(CURRENT_ITEM_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause(); // Needed for osmdroid
        }
        stopLocationUpdates(); // Conserve battery
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach(); // Clean up osmdroid resources
        }
        // Clean up location client if needed, though it's usually managed well by system
    }
}
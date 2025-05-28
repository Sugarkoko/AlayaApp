package com.example.alayaapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences; // Added
import android.content.pm.PackageManager;
import android.location.Location; // Added
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alayaapp.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory; // Added for custom marker color
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private static final String TAG = "MapsActivity";
    private ActivityMapsBinding binding;
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    final int CURRENT_ITEM_ID = R.id.navigation_map;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    public static final String EXTRA_TARGET_LATITUDE = "com.example.alayaapp.TARGET_LATITUDE";
    public static final String EXTRA_TARGET_LONGITUDE = "com.example.alayaapp.TARGET_LONGITUDE";
    public static final String EXTRA_TARGET_NAME = "com.example.alayaapp.TARGET_NAME";

    // SharedPreferences Keys (ensure these match HomeActivity)
    private static final String PREFS_NAME = "AlayaAppPrefs";
    private static final String KEY_LOCATION_MODE = "location_mode";
    private static final String KEY_MANUAL_LATITUDE = "manual_latitude";
    private static final String KEY_MANUAL_LONGITUDE = "manual_longitude";
    private static final String KEY_MANUAL_LOCATION_NAME = "manual_location_name";

    private String currentLocationMode = "auto"; // Default to auto
    private LatLng manualHomeLocation = null;
    private String manualHomeLocationName = "Manually Set Location";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, v.getPaddingBottom());
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        loadHomeLocationPreference(); // Load preferences before map is ready
        setupBottomNavigation();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "SupportMapFragment not found!");
            Toast.makeText(this, "Error: Map Fragment not found.", Toast.LENGTH_LONG).show();
        }

        binding.fabMyLocation.setOnClickListener(v -> {
            if (mMap != null) {
                if ("auto".equals(currentLocationMode)) {
                    centerOnActualGPSLocation(true); // true to animate
                } else if (manualHomeLocation != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(manualHomeLocation, 15f));
                    Toast.makeText(this, "Centering on your manually set home location.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadHomeLocationPreference() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentLocationMode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
        if ("manual".equals(currentLocationMode)) {
            double lat = Double.longBitsToDouble(sharedPreferences.getLong(KEY_MANUAL_LATITUDE, Double.doubleToRawLongBits(0.0)));
            double lon = Double.longBitsToDouble(sharedPreferences.getLong(KEY_MANUAL_LONGITUDE, Double.doubleToRawLongBits(0.0)));
            manualHomeLocationName = sharedPreferences.getString(KEY_MANUAL_LOCATION_NAME, "Manually Set Location");
            if (lat != 0.0 && lon != 0.0) {
                manualHomeLocation = new LatLng(lat, lon);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this);

        if (binding.tvDirectionText != null) {
            binding.tvDirectionText.setText("Map is ready. Determining view...");
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_TARGET_LATITUDE) && intent.hasExtra(EXTRA_TARGET_LONGITUDE)) {
            // Priority 1: Focus on specific place passed from PlaceDetailsActivity
            double targetLat = intent.getDoubleExtra(EXTRA_TARGET_LATITUDE, 0);
            double targetLng = intent.getDoubleExtra(EXTRA_TARGET_LONGITUDE, 0);
            String targetName = intent.getStringExtra(EXTRA_TARGET_NAME);

            if (targetLat != 0 && targetLng != 0) {
                LatLng targetLocation = new LatLng(targetLat, targetLng);
                mMap.addMarker(new MarkerOptions().position(targetLocation).title(targetName != null ? targetName : "Selected Location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 15f));
                if (binding.tvDirectionText != null) {
                    binding.tvDirectionText.setText("Showing: " + (targetName != null ? targetName : "Selected Location"));
                }
            }
        } else {
            // Priority 2: Focus based on Home tab's location choice
            if ("auto".equals(currentLocationMode)) {
                if (binding.tvDirectionText != null) {
                    binding.tvDirectionText.setText("Using current GPS location.");
                }
                centerOnActualGPSLocation(false); // Don't animate first time, just move
            } else if (manualHomeLocation != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(manualHomeLocation)
                        .title(manualHomeLocationName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))); // Different color for manual home
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(manualHomeLocation, 15f));
                if (binding.tvDirectionText != null) {
                    binding.tvDirectionText.setText(manualHomeLocationName); // MODIFIED LINE
                }
            } else {
                // Fallback: General view if no specific home setting or target
                LatLng philippines = new LatLng(12.8797, 121.7740);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(philippines, 6f));
                if (binding.tvDirectionText != null) {
                    binding.tvDirectionText.setText("Explore the map.");
                }
            }
        }
        fetchAndDisplayAllPois(); // Load all Points of Interest
    }

    private void fetchAndDisplayAllPois() {
        db.collection("places")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (mMap == null) return;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Place place = document.toObject(Place.class);
                            if (place != null && place.getLatitude() != null && place.getLongitude() != null) {
                                // Avoid re-adding marker if it's the manually set home location and already added
                                LatLng placeLocation = new LatLng(place.getLatitude(), place.getLongitude());
                                boolean isManualHomeAndAlreadyAdded = "manual".equals(currentLocationMode) &&
                                        manualHomeLocation != null &&
                                        manualHomeLocation.equals(placeLocation) &&
                                        getIntent() == null; // Only skip if not specifically targeted by intent

                                if (!isManualHomeAndAlreadyAdded) {
                                    MarkerOptions markerOptions = new MarkerOptions()
                                            .position(placeLocation)
                                            .title(place.getName());
                                    Marker marker = mMap.addMarker(markerOptions);
                                    if (marker != null) {
                                        marker.setTag(document.getId());
                                    }
                                }
                            }
                        }
                        // Update text after POIs are loaded, if not showing a specific target
                        if (getIntent() == null || !getIntent().hasExtra(EXTRA_TARGET_LATITUDE)) {
                            if (binding.tvDirectionText != null && binding.tvDirectionText.getText().toString().contains("Determining view...")) {
                                binding.tvDirectionText.setText("Places loaded. Tap markers for details.");
                            }
                        }
                    } else {
                        Log.w(TAG, "Error getting POI documents for map.", task.getException());
                        if (binding.tvDirectionText != null) {
                            binding.tvDirectionText.setText("Could not load points of interest.");
                        }
                        Toast.makeText(MapsActivity.this, "Failed to load points of interest.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {
        String placeDocumentId = (String) marker.getTag();
        // Prevent opening details for the "Your Set Location" marker if it has no proper document ID
        if (placeDocumentId != null && !placeDocumentId.isEmpty()) {
            // Check if the clicked marker is the manual home location marker which might not have a "placeDocumentId" tag
            // or has a generic tag. We primarily want to open details for actual POIs.
            if (marker.getTitle() != null && marker.getTitle().equals(manualHomeLocationName) && (placeDocumentId.equals(manualHomeLocationName) || placeDocumentId.equals("manual_home_marker_tag"))){
                Toast.makeText(this, "This is your manually set home location.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(MapsActivity.this, PlaceDetailsActivity.class);
            intent.putExtra(PlaceDetailsActivity.EXTRA_PLACE_DOCUMENT_ID, placeDocumentId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Details not available for this marker.", Toast.LENGTH_SHORT).show();
        }
    }

    private void centerOnActualGPSLocation(boolean animate) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true); // Enable blue dot for actual GPS
                mMap.getUiSettings().setMyLocationButtonEnabled(false); // We use our custom FAB

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                if (animate) {
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f));
                                } else {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f));
                                }
                                Toast.makeText(this, "Centered on current GPS location.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Current GPS location not available.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(this, e -> {
                            Toast.makeText(this, "Failed to get current GPS location.", Toast.LENGTH_SHORT).show();
                        });
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if ("auto".equals(currentLocationMode)) {
                    centerOnActualGPSLocation(true); // Animate to new location
                }
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
                if (mMap != null) mMap.setMyLocationEnabled(false); // Ensure blue dot is off if permission denied
            }
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
}
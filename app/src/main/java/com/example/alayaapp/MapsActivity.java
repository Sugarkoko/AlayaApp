package com.example.alayaapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;

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

    private static final String PREFS_NAME = "AlayaAppPrefs";
    private static final String KEY_LOCATION_MODE = "location_mode";
    private static final String KEY_MANUAL_LATITUDE = "manual_latitude";
    private static final String KEY_MANUAL_LONGITUDE = "manual_longitude";
    private static final String KEY_MANUAL_LOCATION_NAME = "manual_location_name";

    private String currentLocationMode = "auto";
    private LatLng manualHomeLocation = null;
    private String manualHomeLocationName = "Manually Set Location";

    private HashMap<String, Place> markerPlaceMap;
    private CustomInfoWindowAdapter customInfoWindowAdapter;
    private Marker manualHomeMarker = null;


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

        markerPlaceMap = new HashMap<>();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        loadHomeLocationPreference();
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
                    centerOnActualGPSLocation(true);
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
        customInfoWindowAdapter = new CustomInfoWindowAdapter(MapsActivity.this, markerPlaceMap);
        mMap.setInfoWindowAdapter(customInfoWindowAdapter);
        mMap.setOnInfoWindowClickListener(this);

        if (binding.tvDirectionText != null) {
            binding.tvDirectionText.setText("Map is ready. Determining view...");
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_TARGET_LATITUDE) && intent.hasExtra(EXTRA_TARGET_LONGITUDE)) {
            double targetLat = intent.getDoubleExtra(EXTRA_TARGET_LATITUDE, 0);
            double targetLng = intent.getDoubleExtra(EXTRA_TARGET_LONGITUDE, 0);
            String targetName = intent.getStringExtra(EXTRA_TARGET_NAME);

            if (targetLat != 0 && targetLng != 0) {
                LatLng targetLocation = new LatLng(targetLat, targetLng);
                Marker marker = mMap.addMarker(new MarkerOptions().position(targetLocation).title(targetName != null ? targetName : "Selected Location"));
                // We will attempt to populate markerPlaceMap for this marker if it's a known POI in fetchAndDisplayAllPois
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 15f));
                if (binding.tvDirectionText != null) {
                    binding.tvDirectionText.setText("Showing: " + (targetName != null ? targetName : "Selected Location"));
                }
            }
        } else {
            if ("auto".equals(currentLocationMode)) {
                if (binding.tvDirectionText != null) {
                    binding.tvDirectionText.setText("Using current GPS location.");
                }
                centerOnActualGPSLocation(false);
            } else if (manualHomeLocation != null) {
                manualHomeMarker = mMap.addMarker(new MarkerOptions()
                        .position(manualHomeLocation)
                        .title(manualHomeLocationName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                Place manualPlaceStub = new Place(); // Create a stub for the info window
                manualPlaceStub.setName(manualHomeLocationName);
                // manualPlaceStub.setCategory("Your set location"); // You could add a generic category
                if(manualHomeMarker != null) { // ensure marker was added
                    markerPlaceMap.put(manualHomeMarker.getId(), manualPlaceStub);
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(manualHomeLocation, 15f));
                if (binding.tvDirectionText != null) {
                    binding.tvDirectionText.setText(manualHomeLocationName);
                }
            } else {
                LatLng philippines = new LatLng(12.8797, 121.7740);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(philippines, 6f));
                if (binding.tvDirectionText != null) {
                    binding.tvDirectionText.setText("Explore the map.");
                }
            }
        }
        fetchAndDisplayAllPois();
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
                                place.setDocumentId(document.getId());

                                LatLng placeLocation = new LatLng(place.getLatitude(), place.getLongitude());
                                boolean isThisTheManualHomePoi = manualHomeLocation != null && manualHomeLocation.equals(placeLocation);

                                if (isThisTheManualHomePoi && manualHomeMarker != null) {
                                    // This POI is the manually set home location.
                                    // Update the existing manualHomeMarker's data in markerPlaceMap
                                    // and its title to ensure it has the full details.
                                    markerPlaceMap.put(manualHomeMarker.getId(), place);
                                    manualHomeMarker.setTitle(place.getName());
                                    // If you want to ensure its icon reverts to default if it's a POI:
                                    // manualHomeMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                    Log.d(TAG, "Updated manualHomeMarker with full POI data: " + place.getName());
                                } else {
                                    // This is a regular POI, or the manual home location was not a POI / manualHomeMarker is null.
                                    // Add a new marker for it.
                                    MarkerOptions markerOptions = new MarkerOptions()
                                            .position(placeLocation)
                                            .title(place.getName());
                                    Marker marker = mMap.addMarker(markerOptions);
                                    if (marker != null) {
                                        markerPlaceMap.put(marker.getId(), place);
                                    }
                                }
                            }
                        }
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
        Place place = markerPlaceMap.get(marker.getId());

        if (place != null && place.getDocumentId() != null && !place.getDocumentId().isEmpty()) {
            // This will now correctly handle the manualHomeMarker if it was updated with a full Place object
            // (meaning it corresponds to an actual POI).
            Intent intent = new Intent(MapsActivity.this, PlaceDetailsActivity.class);
            intent.putExtra(PlaceDetailsActivity.EXTRA_PLACE_DOCUMENT_ID, place.getDocumentId());
            startActivity(intent);
        } else if (place != null && place.getName().equals(manualHomeLocationName) && (place.getDocumentId() == null || place.getDocumentId().isEmpty())) {
            // This is the manually set home location that IS NOT a specific POI from Firestore
            // (it still has the stub Place object without a documentId).
            Toast.makeText(this, "This is your manually set home: " + place.getName(), Toast.LENGTH_SHORT).show();
        } else {
            // Fallback for markers that might not have a Place object or document ID in the map for some reason
            Toast.makeText(this, "No further details available for this selection.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Info window clicked, but no document ID or insufficient place data for marker: " + marker.getTitle());
        }
    }

    private void centerOnActualGPSLocation(boolean animate) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                if (animate) {
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f));
                                } else {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f));
                                }
                                if (binding.tvDirectionText != null && "auto".equals(currentLocationMode)) {
                                    if (getIntent() == null || !getIntent().hasExtra(EXTRA_TARGET_LATITUDE)) {
                                        binding.tvDirectionText.setText("Centered on your current GPS location.");
                                    }
                                }
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
                    centerOnActualGPSLocation(true);
                }
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
                if (mMap != null) {
                    try { mMap.setMyLocationEnabled(false); } catch (SecurityException se) {Log.e(TAG, "SecurityException on setMyLocationEnabled(false)");}
                }
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
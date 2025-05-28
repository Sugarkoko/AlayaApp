package com.example.alayaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // For logging potential issues
import android.widget.Toast; // For user feedback or placeholders

import androidx.activity.EdgeToEdge; // Keeping this as it was in your original
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alayaapp.databinding.ActivityMapsBinding; // Import ViewBinding

// Google Maps imports
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private ActivityMapsBinding binding; // Use ViewBinding
    private GoogleMap mMap; // Google Map object

    // Define the current item ID for MapsActivity
    final int CURRENT_ITEM_ID = R.id.navigation_map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // From your original code

        // Inflate layout using ViewBinding
        try {
            binding = ActivityMapsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading page.", Toast.LENGTH_LONG).show();
            finish(); // Can't proceed if layout is broken
            return;
        }

        // Apply Window Insets for EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, v.getPaddingBottom());
            return insets;
        });

        // --- Setup Bottom Navigation ---
        setupBottomNavigation();

        // --- Setup Google Map ---
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_container); // Use the ID from your layout
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "SupportMapFragment not found! Check the ID in activity_maps.xml.");
            Toast.makeText(this, "Error: Map Fragment not found.", Toast.LENGTH_LONG).show();
        }

        // FAB click listener
        if (binding.fabMyLocation != null) {
            binding.fabMyLocation.setOnClickListener(v -> {
                // TODO: Implement "My Location" functionality
                // This would typically involve:
                // 1. Checking for location permissions.
                // 2. Requesting permissions if not granted.
                // 3. Getting the current location using FusedLocationProviderClient.
                // 4. Moving the map camera to the current location.
                Toast.makeText(MapsActivity.this, "My Location TBD (Needs Permissions & Location API)", Toast.LENGTH_SHORT).show();
                if (mMap != null) {
                    // Example: Move to a default location if "My Location" is not yet implemented
                    LatLng defaultLocation = new LatLng(14.5995, 120.9842); // Manila
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
                }
            });
        } else {
            Log.w(TAG, "fabMyLocation is null in binding.");
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Update UI text
        if (binding.tvDirectionText != null) {
            binding.tvDirectionText.setText("Map is ready. Explore!");
        }

        // Example: Add a marker in Manila, Philippines and move the camera
        LatLng manila = new LatLng(14.5995, 120.9842);
        mMap.addMarker(new MarkerOptions().position(manila).title("Marker in Manila"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(manila, 10f)); // Zoom level 10

        // You can customize the map's UI settings here
        // mMap.getUiSettings().setZoomControlsEnabled(true);
        // mMap.getUiSettings().setCompassEnabled(true);

        // TODO: Add more map interactions, load markers for places, etc.
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
                return true; // Already on the Maps screen
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
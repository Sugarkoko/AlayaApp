package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
// import androidx.fragment.app.FragmentTransaction; // For map fragment - Commented out
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
// import android.widget.ImageView; // Not directly used if using binding
// import android.widget.TextView; // Not directly used if using binding
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityTransportationModeBinding;
import com.example.alayaapp.databinding.ItemTransportOptionBinding; // Binding for included layout

// --- Comment out Google Maps imports ---
// import com.google.android.gms.maps.CameraUpdateFactory;
// import com.google.android.gms.maps.GoogleMap;
// import com.google.android.gms.maps.OnMapReadyCallback;
// import com.google.android.gms.maps.SupportMapFragment;
// import com.google.android.gms.maps.model.LatLng;
// import com.google.android.gms.maps.model.MarkerOptions;
// import com.google.android.gms.maps.model.PolylineOptions;

// --- Remove OnMapReadyCallback interface ---
public class TransportationModeActivity extends AppCompatActivity /* implements OnMapReadyCallback */ {

    private ActivityTransportationModeBinding binding;
    // private GoogleMap mMap; // Commented out

    // --- Comment out LatLng constants or replace with OSMDroid's GeoPoint if migrating now ---
    // For now, just commenting them out as they are Google Maps specific
    // private final LatLng HOTEL_VENIZ_COORDS = new LatLng(16.4122, 120.5966); // Example
    // private final LatLng BURNHAM_PARK_COORDS = new LatLng(16.4097, 120.5935); // Example

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransportationModeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle Back Arrow Click (Assuming you have a back arrow, if not, add one or remove this)
        // Example: if (binding.ivBackArrowTransport != null) { // Replace with your actual ID
        //    binding.ivBackArrowTransport.setOnClickListener(v -> finish());
        // }


        setupTransportationOptions();
        // setupMapFragment(); // Commented out call
        setupBottomNavListener();

        // Set initial selected item for Bottom Nav
        binding.bottomNavigationTransportPage.setSelectedItemId(R.id.navigation_home); // Or relevant default
    }

    private void setupTransportationOptions() {
        // This logic populates each unique binding instance
        populateTransportOption(binding.transportTaxi, "Taxi", "Best!", R.drawable.ic_taxi, "5 mins", "₱30 - 50", R.drawable.bg_best_badge, R.color.badge_best_text);
        populateTransportOption(binding.transportBus, "Bus", "Fastest!", R.drawable.ic_bus, "4 mins", "₱15 - 20", R.drawable.bg_fastest_badge, R.color.badge_fastest_text);
        populateTransportOption(binding.transportBike, "Bike", null, R.drawable.ic_bike, "4 mins", "FREE", 0, 0);
        populateTransportOption(binding.transportMotorcycle, "Motorcycle", null, R.drawable.ic_motorcycle, "4 mins", "FREE", 0, 0);
        populateTransportOption(binding.transportWalk, "Walk", null, R.drawable.ic_walk, "10 mins", "FREE", 0, 0);

        // Update click listeners to show placeholder or navigate without map for now
        binding.transportTaxi.getRoot().setOnClickListener(v -> showToastForMode("taxi"));
        binding.transportBus.getRoot().setOnClickListener(v -> showToastForMode("bus"));
        binding.transportBike.getRoot().setOnClickListener(v -> showToastForMode("bike"));
        binding.transportMotorcycle.getRoot().setOnClickListener(v -> showToastForMode("motorcycle"));
        binding.transportWalk.getRoot().setOnClickListener(v -> showToastForMode("walk"));
    }

    private void showToastForMode(String mode) {
        Toast.makeText(this, "Selected mode: " + mode + " (Map route display TBD)", Toast.LENGTH_SHORT).show();
        // showRouteOnMap(mode); // This would need to be refactored for OSMDroid
    }

    private void populateTransportOption(ItemTransportOptionBinding itemBinding, String name, String badgeText, int iconRes, String duration, String price, int badgeBgRes, int badgeTextColorRes) {
        itemBinding.tvTransportName.setText(name);
        itemBinding.ivTransportIcon.setImageResource(iconRes);
        itemBinding.tvTransportDuration.setText(duration);
        itemBinding.tvTransportPrice.setText(price);

        if (badgeText != null) {
            itemBinding.tvTransportBadge.setText(badgeText);
            itemBinding.tvTransportBadge.setBackgroundResource(badgeBgRes);
            // Use ContextCompat for deprecated getColor method if minSdk < 23
            itemBinding.tvTransportBadge.setTextColor(getResources().getColor(badgeTextColorRes, getTheme()));
            itemBinding.tvTransportBadge.setVisibility(View.VISIBLE);
        } else {
            itemBinding.tvTransportBadge.setVisibility(View.GONE);
        }
    }

    /*
    // --- Comment out entire Google Maps specific methods ---
    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_container_transport); // Ensure this ID is in your XML

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.map_fragment_container_transport, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap == null) {
            Toast.makeText(this, "Error - Map not available", Toast.LENGTH_SHORT).show();
            return;
        }
        // Add markers for start and end points (example)
        // mMap.addMarker(new MarkerOptions().position(HOTEL_VENIZ_COORDS).title("Hotel Veniz"));
        // mMap.addMarker(new MarkerOptions().position(BURNHAM_PARK_COORDS).title("Burnham Park"));
        // Move camera to show both markers or a default route overview
        // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BURNHAM_PARK_COORDS, 15f)); // Zoom to destination
        // TODO: Initially, you might show a default route (e.g., walking)
        // showRouteOnMap("walk"); // Or your default
        Toast.makeText(this, "Map ready (Google Maps - Needs migration to OSM)", Toast.LENGTH_SHORT).show();
    }

    private void showRouteOnMap(String mode) {
        if (mMap == null) return;
        mMap.clear(); // Clear previous routes/markers if any

        // Re-add markers
        // mMap.addMarker(new MarkerOptions().position(HOTEL_VENIZ_COORDS).title("Hotel Veniz"));
        // mMap.addMarker(new MarkerOptions().position(BURNHAM_PARK_COORDS).title("Burnham Park"));

        Toast.makeText(this, "Showing route for: " + mode + " (Google Maps - Needs migration to OSM)", Toast.LENGTH_SHORT).show();
        // TODO: Implement actual route drawing using Google Directions API
        // For now, just center the map or draw a straight line as placeholder
        // Example: mMap.addPolyline(new PolylineOptions().add(HOTEL_VENIZ_COORDS, BURNHAM_PARK_COORDS));
        // mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(BURNHAM_PARK_COORDS, 15f));
    }
    */

    private void setupBottomNavListener() {
        binding.bottomNavigationTransportPage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();
            // If this page had its own "current" item ID, you'd check against it here.
            // For now, assume any selection navigates away or reloads.

            Class<?> destinationActivityClass = null;
            if (destinationItemId == R.id.navigation_home) {
                destinationActivityClass = HomeActivity.class;
            } else if (destinationItemId == R.id.navigation_itineraries) {
                destinationActivityClass = ItinerariesActivity.class;
            } else if (destinationItemId == R.id.navigation_map) {
                // If MapsActivity is the main map screen, navigate there.
                // If this screen IS the map screen, or a sub-map screen, handle appropriately.
                destinationActivityClass = MapsActivity.class; // Assuming you want to go to the main OSM Map
                // Toast.makeText(this, "Map (TBD - Already on a map related screen or go to main map)", Toast.LENGTH_SHORT).show();
                // return true; // Prevent re-navigation for now if it's complex
            } else if (destinationItemId == R.id.navigation_profile) {
                destinationActivityClass = ProfileActivity.class;
            }

            if (destinationActivityClass != null) {
                navigateTo(destinationActivityClass, destinationItemId);
                return true;
            }
            return false;
        });
    }

    private void navigateTo(Class<?> destinationActivityClass, int destinationItemId) {
        Intent intent = new Intent(getApplicationContext(), destinationActivityClass);
        // Clear previous activities if navigating to a main tab like Home
        if (destinationActivityClass == HomeActivity.class ||
                destinationActivityClass == ItinerariesActivity.class ||
                destinationActivityClass == MapsActivity.class ||
                destinationActivityClass == ProfileActivity.class) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        startActivity(intent);

        // Implement indexed sliding or basic fade
        // You'll need to define a CURRENT_NAV_ITEM_ID for this activity if using sliding
        // For now, using a basic fade to avoid complexity if index is not defined for this specific screen.
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish(); // Close this activity
    }
}
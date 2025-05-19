package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction; // For map fragment

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityTransportationModeBinding;
import com.example.alayaapp.databinding.ItemTransportOptionBinding; // Binding for included layout
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
// Add PolylineOptions if you plan to draw routes
// import com.google.android.gms.maps.model.PolylineOptions;


public class TransportationModeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityTransportationModeBinding binding;
    private GoogleMap mMap;

    // Define constants for clarity (example coordinates)
    private final LatLng HOTEL_VENIZ_COORDS = new LatLng(16.4122, 120.5966); // Example
    private final LatLng BURNHAM_PARK_COORDS = new LatLng(16.4097, 120.5935); // Example


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransportationModeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle Back Arrow Click


        // Setup data for transportation options (Placeholder)
        setupTransportationOptions();

        // Setup Map Fragment
        setupMapFragment();

        // Setup Bottom Navigation
        // Decide which item should be selected. If this page is reached from "Map", select Map.
        // If it's a detail page, maybe none, or keep the previous activity's selection.
        // For now, let's assume we keep Home selected if no other logic dictates.
        binding.bottomNavigationTransportPage.setSelectedItemId(R.id.navigation_home);
        setupBottomNavListener();
    }

    private void setupTransportationOptions() {
        // This logic populates each unique binding instance
        populateTransportOption(binding.transportTaxi, "Taxi", "Best!", R.drawable.ic_taxi, "5 mins", "₱30 - 50", R.drawable.bg_best_badge, R.color.badge_best_text);
        populateTransportOption(binding.transportBus, "Bus", "Fastest!", R.drawable.ic_bus, "4 mins", "₱15 - 20", R.drawable.bg_fastest_badge, R.color.badge_fastest_text);
        populateTransportOption(binding.transportBike, "Bike", null, R.drawable.ic_bike, "4 mins", "FREE", 0, 0);
        populateTransportOption(binding.transportMotorcycle, "Motorcycle", null, R.drawable.ic_motorcycle, "4 mins", "FREE", 0, 0);
        populateTransportOption(binding.transportWalk, "Walk", null, R.drawable.ic_walk, "10 mins", "FREE", 0, 0);

        binding.transportTaxi.getRoot().setOnClickListener(v -> showRouteOnMap("taxi"));
        binding.transportBus.getRoot().setOnClickListener(v -> showRouteOnMap("bus"));
        // ... etc.
    }


    private void populateTransportOption(ItemTransportOptionBinding itemBinding, String name, String badgeText, int iconRes, String duration, String price, int badgeBgRes, int badgeTextColorRes) {
        itemBinding.tvTransportName.setText(name);
        itemBinding.ivTransportIcon.setImageResource(iconRes);
        itemBinding.tvTransportDuration.setText(duration);
        itemBinding.tvTransportPrice.setText(price);

        if (badgeText != null) {
            itemBinding.tvTransportBadge.setText(badgeText);
            itemBinding.tvTransportBadge.setBackgroundResource(badgeBgRes);
            itemBinding.tvTransportBadge.setTextColor(getResources().getColor(badgeTextColorRes, getTheme()));
            itemBinding.tvTransportBadge.setVisibility(View.VISIBLE);
        } else {
            itemBinding.tvTransportBadge.setVisibility(View.GONE);
        }
    }


    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_container_transport); // Check if already added
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
        mMap.addMarker(new MarkerOptions().position(HOTEL_VENIZ_COORDS).title("Hotel Veniz"));
        mMap.addMarker(new MarkerOptions().position(BURNHAM_PARK_COORDS).title("Burnham Park"));

        // Move camera to show both markers or a default route overview
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BURNHAM_PARK_COORDS, 15f)); // Zoom to destination

        // TODO: Initially, you might show a default route (e.g., walking)
        // showRouteOnMap("walk"); // Or your default
    }

    private void showRouteOnMap(String mode) {
        if (mMap == null) return;
        mMap.clear(); // Clear previous routes/markers if any

        // Re-add markers
        mMap.addMarker(new MarkerOptions().position(HOTEL_VENIZ_COORDS).title("Hotel Veniz"));
        mMap.addMarker(new MarkerOptions().position(BURNHAM_PARK_COORDS).title("Burnham Park"));


        Toast.makeText(this, "Showing route for: " + mode, Toast.LENGTH_SHORT).show();
        // TODO: Implement actual route drawing using Google Directions API
        // For now, just center the map or draw a straight line as placeholder
        // Example: mMap.addPolyline(new PolylineOptions().add(HOTEL_VENIZ_COORDS, BURNHAM_PARK_COORDS));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(BURNHAM_PARK_COORDS, 15f));
    }


    private void setupBottomNavListener() {
        binding.bottomNavigationTransportPage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();

            // if (destinationItemId == CURRENT_NAV_ITEM_ID) { // If this page had a "current" item
            //     return true;
            // }

            Class<?> destinationActivityClass = null;
            if (destinationItemId == R.id.navigation_home) {
                destinationActivityClass = HomeActivity.class;
            } else if (destinationItemId == R.id.navigation_itineraries) {
                destinationActivityClass = ItinerariesActivity.class;
            } else if (destinationItemId == R.id.navigation_map) {
                Toast.makeText(this, "Map (TBD - Already on a map related screen)", Toast.LENGTH_SHORT).show();
                // Potentially navigate to a main MapActivity if this is a sub-page
                // destinationActivityClass = MapActivity.class;
                return true; // Prevent re-navigation for now
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
        if (destinationActivityClass == HomeActivity.class ) { // || destinationActivityClass == ItinerariesActivity.class etc.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        startActivity(intent);

        // Basic fade animation for now, or implement indexed sliding
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        finish(); // Close this activity
    }
}
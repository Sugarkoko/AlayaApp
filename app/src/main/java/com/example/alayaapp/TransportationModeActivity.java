package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityTransportationModeBinding;
import com.example.alayaapp.databinding.ItemTransportOptionBinding;


public class TransportationModeActivity extends AppCompatActivity  {

    private ActivityTransportationModeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransportationModeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());




        setupTransportationOptions();

        setupBottomNavListener();


        binding.bottomNavigationTransportPage.setSelectedItemId(R.id.navigation_home); // Or relevant default
    }

    private void setupTransportationOptions() {

        populateTransportOption(binding.transportTaxi, "Taxi", "Best!", R.drawable.ic_taxi, "5 mins", "₱30 - 50", R.drawable.bg_best_badge, R.color.badge_best_text);
        populateTransportOption(binding.transportBus, "Bus", "Fastest!", R.drawable.ic_bus, "4 mins", "₱15 - 20", R.drawable.bg_fastest_badge, R.color.badge_fastest_text);
        populateTransportOption(binding.transportBike, "Bike", null, R.drawable.ic_bike, "4 mins", "FREE", 0, 0);
        populateTransportOption(binding.transportMotorcycle, "Motorcycle", null, R.drawable.ic_motorcycle, "4 mins", "FREE", 0, 0);
        populateTransportOption(binding.transportWalk, "Walk", null, R.drawable.ic_walk, "10 mins", "FREE", 0, 0);


        binding.transportTaxi.getRoot().setOnClickListener(v -> showToastForMode("taxi"));
        binding.transportBus.getRoot().setOnClickListener(v -> showToastForMode("bus"));
        binding.transportBike.getRoot().setOnClickListener(v -> showToastForMode("bike"));
        binding.transportMotorcycle.getRoot().setOnClickListener(v -> showToastForMode("motorcycle"));
        binding.transportWalk.getRoot().setOnClickListener(v -> showToastForMode("walk"));
    }

    private void showToastForMode(String mode) {
        Toast.makeText(this, "Selected mode: " + mode + " (Map route display TBD)", Toast.LENGTH_SHORT).show();

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



    private void setupBottomNavListener() {
        binding.bottomNavigationTransportPage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();


            Class<?> destinationActivityClass = null;
            if (destinationItemId == R.id.navigation_home) {
                destinationActivityClass = HomeActivity.class;
            } else if (destinationItemId == R.id.navigation_itineraries) {
                destinationActivityClass = ItinerariesActivity.class;
            } else if (destinationItemId == R.id.navigation_map) {

                destinationActivityClass = MapsActivity.class;
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


        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish(); // Close this activity
    }
}
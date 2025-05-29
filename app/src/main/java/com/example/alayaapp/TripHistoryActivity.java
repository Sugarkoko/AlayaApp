package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
// Removed Toast import as it's not used in this minimal version

import com.example.alayaapp.databinding.ActivityTripHistoryBinding;

public class TripHistoryActivity extends AppCompatActivity {

    private ActivityTripHistoryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTripHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- Back Arrow Click Listener ---
        if (binding.ivBackArrowTripHistory != null) {
            binding.ivBackArrowTripHistory.setOnClickListener(v -> {
                finish(); // Go back to the previous activity
            });
        }


        if (binding.itemBaguioCity != null) {
            binding.itemBaguioCity.setOnClickListener(v -> {
                Intent intent = new Intent(TripHistoryActivity.this, ItineraryLogDetailActivity.class);
                intent.putExtra("LOCATION_NAME", "Baguio City");

                startActivity(intent);

            });
        }

        binding.bottomNavigationTripHistory.setSelectedItemId(R.id.navigation_profile);

        binding.bottomNavigationTripHistory.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Example: Navigate to Home
            if (itemId == R.id.navigation_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish(); // Close this activity when going to a main tab
                return true;
            }


            if (itemId == binding.bottomNavigationTripHistory.getSelectedItemId()) {
                return true;
            }



            return false; // Item not handled by this minimal setup
        });
    }
}
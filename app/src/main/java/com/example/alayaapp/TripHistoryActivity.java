package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import com.example.alayaapp.databinding.ActivityTripHistoryBinding;

public class TripHistoryActivity extends AppCompatActivity {

    private ActivityTripHistoryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTripHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivBackArrowTripHistory.setOnClickListener(v -> finish());

        // Click listener for the hardcoded "Baguio City" view button


        // TODO: Set up other view button listeners similarly if needed for UI testing

        // Setup Bottom Nav (basic example, implement your full logic)
        binding.bottomNavigationTripHistory.setSelectedItemId(R.id.navigation_profile); // Example
        binding.bottomNavigationTripHistory.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                startActivity(new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
                return true;
            }
            // Add other navigation cases
            return false;
        });
    }
}
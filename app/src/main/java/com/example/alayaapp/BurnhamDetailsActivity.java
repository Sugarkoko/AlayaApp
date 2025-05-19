package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent; // Added for starting new activity
import android.os.Bundle;
// import android.view.View; // No longer needed if using lambdas with ViewBinding
// import android.widget.ImageView; // No longer needed with ViewBinding

import com.example.alayaapp.databinding.ActivityBurnhamDetailsBinding; // Import ViewBinding class

public class BurnhamDetailsActivity extends AppCompatActivity {

    private ActivityBurnhamDetailsBinding binding; // Declare binding variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using ViewBinding
        binding = ActivityBurnhamDetailsBinding.inflate(getLayoutInflater());
        // Set the content view from the binding's root
        setContentView(binding.getRoot());

        // --- Set Click Listeners ---

        // Back Arrow Click Listener
        binding.ivBackArrow.setOnClickListener(v -> {
            // Finish this activity to go back to the previous one in the stack
            finish();
        });

        // "View in Mode of Transportation" Click Listener
        binding.tvViewTransportation.setOnClickListener(v -> {
            Intent intent = new Intent(BurnhamDetailsActivity.this, TransportationModeActivity.class);

            // Optionally, pass data to TransportationModeActivity if needed.
            // For example, if TransportationModeActivity needs to know which park it's for:
            // intent.putExtra("PARK_NAME", "Burnham Park");
            // intent.putExtra("PARK_LATITUDE", 16.4097); // Example Lat
            // intent.putExtra("PARK_LONGITUDE", 120.5935); // Example Lng
            // You would then retrieve these in TransportationModeActivity's onCreate using getIntent().getExtras()

            startActivity(intent);

            // Optional: Add a transition animation if you like
            // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // TODO: Add listeners for other interactive elements if any (e.g., "View in Maps")
        // binding.tvViewInMaps.setOnClickListener(v -> { ... });

        // TODO: Load dynamic data into your TextViews if not already hardcoded in XML
        // e.g., binding.tvParkTitle.setText("Burnham Park");
        // binding.tvRatingValue.setText("4.4");
        // etc.
    }
}
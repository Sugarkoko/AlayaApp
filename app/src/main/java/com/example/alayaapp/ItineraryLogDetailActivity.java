package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.example.alayaapp.databinding.ActivityItineraryLogDetailBinding;
// You'll need ItemItineraryLogEntryDetailBinding if you want to manipulate included items
// import com.example.alayaapp.databinding.ItemItineraryLogEntryDetailBinding;

public class ItineraryLogDetailActivity extends AppCompatActivity {

    private ActivityItineraryLogDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItineraryLogDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve data passed from TripHistoryActivity (if any)
        // String locationName = getIntent().getStringExtra("LOCATION_NAME");
        // String locationId = getIntent().getStringExtra("LOCATION_ID");

        // For now, the layout is static.
        // If you passed locationName, you could set it:
        // if (locationName != null) {
        //    binding.tvLocationNameLogDetail.setText(locationName);
        // }

        binding.ivBackArrowLogDetail.setOnClickListener(v -> finish());

        // TODO: Manually set the text for each included item if needed for exact UI preview,
        //  otherwise the tools:text in item_itinerary_log_entry_detail.xml will show.
        // Example for the first item in Day 1:
        // ItemItineraryLogEntryDetailBinding itemD1_1_binding = binding.itemD11; // if you added tools:id
        // if(itemD1_1_binding != null) { // Check because tools:id might not always generate direct binding field
        //    itemD1_1_binding.tvItineraryTime.setText("9:00 AM");
        //    itemD1_1_binding.tvItineraryActivity.setText("Breakfast at Café by the Ruins");
        //    itemD1_1_binding.tvItineraryRating.setText("4.5");
        // }
        // Repeat for all other static items if you need to override their tools:text for this specific page's preview.
        // It's often easier to rely on tools:text in the item layout itself for this static phase.

        // Setup Bottom Nav (basic example)
        binding.bottomNavigationLogDetail.setSelectedItemId(R.id.navigation_profile); // Example
        // Add setOnItemSelectedListener if needed
    }
}
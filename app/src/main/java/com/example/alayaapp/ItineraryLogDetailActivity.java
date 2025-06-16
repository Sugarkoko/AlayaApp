package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityItineraryLogDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

public class ItineraryLogDetailActivity extends AppCompatActivity {

    private ActivityItineraryLogDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ItineraryLogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItineraryLogDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        String tripId = getIntent().getStringExtra("TRIP_ID");

        setupRecyclerView();
        binding.ivBackArrowLogDetail.setOnClickListener(v -> finish());

        // The line referencing the bottom navigation has been removed.
        // binding.bottomNavigationLogDetail.setSelectedItemId(R.id.navigation_profile);

        if (tripId != null && !tripId.isEmpty()) {
            loadTripDetails(tripId);
        } else {
            Toast.makeText(this, "Error: Could not find trip details.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        binding.rvItineraryLog.setLayoutManager(new LinearLayoutManager(this));
        // Adapter will be set after data is loaded
    }

    private void loadTripDetails(String tripId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in to view details.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .collection("tripHistory").document(tripId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Trip trip = document.toObject(Trip.class);
                            if (trip != null) {
                                populateUI(trip);
                            }
                        } else {
                            Log.d("LogDetail", "No such document");
                            Toast.makeText(this, "Trip not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("LogDetail", "get failed with ", task.getException());
                        Toast.makeText(this, "Failed to load trip details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateUI(Trip trip) {
        binding.tvLocationHeaderLogDetail.setText(trip.getTripTitle());
        binding.tvDateRangeLogDetail.setText(trip.getTripDate());

        if (trip.getItinerary() != null && !trip.getItinerary().isEmpty()) {
            adapter = new ItineraryLogAdapter(trip.getItinerary());
            binding.rvItineraryLog.setAdapter(adapter);
        } else {
            // Handle case where itinerary list is empty, though unlikely if saved correctly
            adapter = new ItineraryLogAdapter(new ArrayList<>());
            binding.rvItineraryLog.setAdapter(adapter);
        }
    }
}
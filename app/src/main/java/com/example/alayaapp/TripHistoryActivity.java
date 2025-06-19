package com.example.alayaapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityTripHistoryBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TripHistoryActivity extends AppCompatActivity implements TripHistoryAdapter.OnTripInteractionListener {
    private ActivityTripHistoryBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TripHistoryAdapter adapter;
    private List<Trip> tripList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTripHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();

        binding.ivBackArrowTripHistory.setOnClickListener(v -> finish());


        loadTripHistory();
    }

    private void setupRecyclerView() {
        tripList = new ArrayList<>();
        adapter = new TripHistoryAdapter(this, tripList, this);
        binding.rvTripHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTripHistory.setAdapter(adapter);
    }

    private void loadTripHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            binding.tvNoHistoryMessage.setText("Please sign in to view your trip history.");
            binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
            binding.rvTripHistory.setVisibility(View.GONE);
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .collection("tripHistory")
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        tripList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Trip trip = document.toObject(Trip.class);
                            trip.setDocumentId(document.getId());
                            tripList.add(trip);
                        }

                        if (tripList.isEmpty()) {
                            binding.tvNoHistoryMessage.setText("You haven't saved any trips yet.");
                            binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
                            binding.rvTripHistory.setVisibility(View.GONE);
                        } else {
                            binding.tvNoHistoryMessage.setVisibility(View.GONE);
                            binding.rvTripHistory.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.w("TripHistoryActivity", "Error getting documents.", task.getException());
                        Toast.makeText(this, "Failed to load trip history.", Toast.LENGTH_SHORT).show();
                        binding.tvNoHistoryMessage.setText("Could not load trip history.");
                        binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onTripLongPressed(Trip trip, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to permanently delete your '" + trip.getTripTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTripFromFirestore(trip, position);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTripFromFirestore(Trip trip, int position) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || trip.getDocumentId() == null || trip.getDocumentId().isEmpty()) {
            Toast.makeText(this, "Error: Could not delete trip.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .collection("tripHistory").document(trip.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    tripList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, tripList.size());

                    Toast.makeText(this, "Trip deleted successfully.", Toast.LENGTH_SHORT).show();

                    if (tripList.isEmpty()) {
                        binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
                        binding.rvTripHistory.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete trip. Please try again.", Toast.LENGTH_SHORT).show();
                    Log.e("TripHistoryActivity", "Error deleting trip", e);
                });
    }
}
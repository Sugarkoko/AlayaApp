package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
// import android.net.Uri; // REMOVED - No longer needed for external maps for this button
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.alayaapp.databinding.ActivityPlaceDetailsBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.Locale;

public class PlaceDetailsActivity extends AppCompatActivity {
    private static final String TAG = "PlaceDetailsActivity";
    public static final String EXTRA_PLACE_DOCUMENT_ID = "PLACE_DOCUMENT_ID";
    private ActivityPlaceDetailsBinding binding;
    private FirebaseFirestore db;
    private Place currentPlace; // To store the fetched place data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaceDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        String placeDocumentId = getIntent().getStringExtra(EXTRA_PLACE_DOCUMENT_ID);

        if (placeDocumentId != null && !placeDocumentId.isEmpty()) {
            loadPlaceDetails(placeDocumentId);
        } else {
            Log.e(TAG, "No Place Document ID provided!");
            Toast.makeText(this, "Error: Place details not found.", Toast.LENGTH_LONG).show();
            finish(); // Close activity if no ID is provided
            return; // Stop further execution
        }

        binding.ivBackArrow.setOnClickListener(v -> finish());

        binding.tvViewTransportation.setOnClickListener(v -> {
            if (currentPlace != null && currentPlace.getName() != null) {
                Intent intent = new Intent(PlaceDetailsActivity.this, TransportationModeActivity.class);
                intent.putExtra("PARK_NAME", currentPlace.getName());
                // Ensure coordinates are available before trying to pass them
                if (currentPlace.getLatitude() != null && currentPlace.getLongitude() != null) {
                    intent.putExtra("PARK_LATITUDE", currentPlace.getLatitude());
                    intent.putExtra("PARK_LONGITUDE", currentPlace.getLongitude());
                } else {
                    Log.w(TAG, "Latitude or Longitude is null for transportation details of " + currentPlace.getName());
                    Toast.makeText(this, "Location coordinates not available for transportation.", Toast.LENGTH_SHORT).show();
                    // Optionally, don't start the activity if lat/lng are crucial for TransportationModeActivity
                    // return; // Uncomment if TransportationModeActivity requires coordinates
                }
                startActivity(intent);
            } else {
                Toast.makeText(this, "Place details not loaded yet.", Toast.LENGTH_SHORT).show();
            }
        });

        // *** CORRECTED "View in Maps" BUTTON LISTENER ***
        binding.tvView.setOnClickListener(v -> {
            if (currentPlace != null && currentPlace.getLatitude() != null && currentPlace.getLongitude() != null) {
                Intent mapIntent = new Intent(PlaceDetailsActivity.this, MapsActivity.class);
                // Pass the latitude, longitude, and name of the place to MapsActivity
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LATITUDE, currentPlace.getLatitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LONGITUDE, currentPlace.getLongitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_NAME, currentPlace.getName());

                // Optional: Add flags to manage activity stack, e.g., bring MapsActivity to front
                // mapIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Location data not available to view on map.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Attempted to view in map, but currentPlace or its coordinates are null. CurrentPlace: " + currentPlace);
                if(currentPlace != null) {
                    Log.w(TAG, "CurrentPlace Lat: " + currentPlace.getLatitude() + ", Lng: " + currentPlace.getLongitude());
                }
            }
        });
    }

    private void loadPlaceDetails(String documentId) {
        DocumentReference placeRef = db.collection("places").document(documentId);
        placeRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    currentPlace = document.toObject(Place.class); // Deserialize to Place object
                    if (currentPlace != null) {
                        currentPlace.setDocumentId(document.getId()); // Store document ID in POJO
                        populateUI(currentPlace);
                    } else {
                        Log.e(TAG, "Failed to convert document to Place object. Check Place.java and Firestore field names/types.");
                        Toast.makeText(this, "Error: Could not parse place data.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d(TAG, "No such document with ID: " + documentId);
                    Toast.makeText(this, "Error: Place details not found in database.", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "Firestore get failed with ", task.getException());
                String errorMessage = "Error: Could not load place details.";
                if (task.getException() instanceof FirebaseFirestoreException) {
                    FirebaseFirestoreException e = (FirebaseFirestoreException) task.getException();
                    errorMessage += " Code: " + e.getCode().name();
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateUI(Place place) {
        // Ensure place is not null before accessing its members
        if (place == null) {
            Log.e(TAG, "populateUI called with null place object.");
            Toast.makeText(this, "Error displaying place details.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.tvParkTitle.setText(place.getName() != null ? place.getName() : "N/A");
        binding.tvRatingValue.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));

        if (binding.tvReviewCount != null) { // Check if tvReviewCount is part of the layout
            binding.tvReviewCount.setText(place.getReview_count_text() != null ? place.getReview_count_text() : "(N/A)");
        }

        binding.tvAboutDescription.setText(place.getAbout() != null ? place.getAbout() : "No description available.");
        binding.tvOpenDescription.setText(place.getOpen() != null ? place.getOpen() : "Hours not specified.");

        if (binding.tvBestTimeDescription != null) { // Check if tvBestTimeDescription is part of the layout
            binding.tvBestTimeDescription.setText(place.getBest_time() != null ? place.getBest_time() : "Not specified.");
        }

        if (binding.tvEntranceFeeDescription != null) { // Check if tvEntranceFeeDescription is part of the layout
            if (place.getPrice_range() == 0) {
                binding.tvEntranceFeeDescription.setText("Free");
            } else {
                binding.tvEntranceFeeDescription.setText(String.format(Locale.getDefault(), "PHP %d", place.getPrice_range()));
            }
        }

        if (place.getImage_url() != null && !place.getImage_url().isEmpty()) {
            Glide.with(this)
                    .load(place.getImage_url())
                    .placeholder(R.drawable.img_placeholder) // Your placeholder
                    .error(R.drawable.img_error)         // Your error image
                    .into(binding.ivParkImage);
        } else {
            binding.ivParkImage.setImageResource(R.drawable.img_placeholder); // Fallback placeholder
        }
    }
}
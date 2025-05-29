package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
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
    public static final String EXTRA_PLACE_DOCUMENT_ID = "PLACE_DOCUMENT_ID"; // Make sure this matches MapsActivity
    private ActivityPlaceDetailsBinding binding;
    private FirebaseFirestore db;
    private Place currentPlace;

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
            finish();
            return;
        }

        binding.ivBackArrow.setOnClickListener(v -> finish());

        binding.tvViewTransportation.setOnClickListener(v -> {
            if (currentPlace != null && currentPlace.getLatitude() != null && currentPlace.getLongitude() != null) {
                Intent mapIntent = new Intent(PlaceDetailsActivity.this, MapsActivity.class);
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LATITUDE, currentPlace.getLatitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LONGITUDE, currentPlace.getLongitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_NAME, currentPlace.getName());
                mapIntent.putExtra(MapsActivity.EXTRA_DRAW_ROUTE, true); // Signal to draw a route

                // Pass the document ID if it's a known POI for better info window in MapsActivity
                if (currentPlace.getDocumentId() != null && !currentPlace.getDocumentId().isEmpty()) {
                    mapIntent.putExtra(PlaceDetailsActivity.EXTRA_PLACE_DOCUMENT_ID, currentPlace.getDocumentId());
                }
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Location data not available to plan a route.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Attempted to view transportation/route, but currentPlace or its coordinates are null. CurrentPlace: " + currentPlace);
                if(currentPlace != null) {
                    Log.w(TAG, "CurrentPlace Lat: " + currentPlace.getLatitude() + ", Lng: " + currentPlace.getLongitude());
                }
            }
        });

        binding.tvView.setOnClickListener(v -> {
            if (currentPlace != null && currentPlace.getLatitude() != null && currentPlace.getLongitude() != null) {
                Intent mapIntent = new Intent(PlaceDetailsActivity.this, MapsActivity.class);
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LATITUDE, currentPlace.getLatitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LONGITUDE, currentPlace.getLongitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_NAME, currentPlace.getName());
                // NO EXTRA_DRAW_ROUTE = true, so it will just center
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
                    currentPlace = document.toObject(Place.class);
                    if (currentPlace != null) {
                        currentPlace.setDocumentId(document.getId());
                        populateUI(currentPlace);
                    } else {
                        Log.e(TAG, "Failed to convert document to Place object.");
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
        if (place == null) {
            Log.e(TAG, "populateUI called with null place object.");
            Toast.makeText(this, "Error displaying place details.", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.tvParkTitle.setText(place.getName() != null ? place.getName() : "N/A");
        binding.tvRatingValue.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));
        if (binding.tvReviewCount != null) {
            binding.tvReviewCount.setText(place.getReview_count_text() != null ? place.getReview_count_text() : "(N/A)");
        }
        binding.tvAboutDescription.setText(place.getAbout() != null ? place.getAbout() : "No description available.");
        binding.tvOpenDescription.setText(place.getOpen() != null ? place.getOpen() : "Hours not specified.");
        if (binding.tvBestTimeDescription != null) {
            binding.tvBestTimeDescription.setText(place.getBest_time() != null ? place.getBest_time() : "Not specified.");
        }
        if (binding.tvEntranceFeeDescription != null) {
            if (place.getPrice_range() == 0 && (place.getAbout() == null || !place.getAbout().toLowerCase().contains("fee"))) { // Basic check if "Free" or not
                binding.tvEntranceFeeDescription.setText("Free / Varies"); // Default to Varies if unsure
            } else if (place.getPrice_range() == 0) {
                binding.tvEntranceFeeDescription.setText("Free");
            }
            else {
                binding.tvEntranceFeeDescription.setText(String.format(Locale.getDefault(), "PHP %d", place.getPrice_range()));
            }
        }

        if (place.getImage_url() != null && !place.getImage_url().isEmpty()) {
            Glide.with(this)
                    .load(place.getImage_url())
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_error)
                    .into(binding.ivParkImage);
        } else {
            binding.ivParkImage.setImageResource(R.drawable.img_placeholder);
        }
    }
}
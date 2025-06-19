package com.example.alayaapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;
import java.util.Locale;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View mWindow;
    private Context mContext;
    private HashMap<String, Place> markerPlaceMap;

    public CustomInfoWindowAdapter(Context context, HashMap<String, Place> markerPlaceMap) {
        mContext = context;
        this.markerPlaceMap = markerPlaceMap;
        mWindow = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null);
    }

    private void renderWindowText(Marker marker, View view) {
        Place place = markerPlaceMap.get(marker.getId());

        TextView tvTitle = view.findViewById(R.id.info_window_title);
        TextView tvCategory = view.findViewById(R.id.info_window_category);
        TextView tvRating = view.findViewById(R.id.info_window_rating);
        TextView tvOpen = view.findViewById(R.id.info_window_open);
        ImageView ivImage = view.findViewById(R.id.info_window_image);

        if (place != null) {
            tvTitle.setText(place.getName() != null ? place.getName() : "N/A");
            tvCategory.setText(place.getCategory() != null ? "Category: " + place.getCategory() : "Category: N/A");

            if (place.getRating() > 0) {
                tvRating.setText(String.format(Locale.getDefault(), "Rating: %.1f ★", place.getRating()));
                tvRating.setVisibility(View.VISIBLE);
            } else if (place.getName() != null && place.getName().equals(marker.getTitle()) && place.getCategory() == null){
                tvRating.setVisibility(View.GONE);
            }
            else {
                tvRating.setText("Rating: N/A");
                tvRating.setVisibility(View.VISIBLE);
            }

            tvOpen.setText(place.getOpen() != null ? "Open: " + place.getOpen() : "Open: N/A");

            // Load image with Glide if URL exists
            if (place.getImage_url() != null && !place.getImage_url().isEmpty()) {
                Glide.with(mContext.getApplicationContext()) // Use application context for Glide in adapter
                        .load(place.getImage_url())
                        .placeholder(R.drawable.img_placeholder)
                        .error(R.drawable.img_error)
                        .into(ivImage);
                ivImage.setVisibility(View.VISIBLE);
            } else {
                ivImage.setVisibility(View.GONE);
            }
        } else {
            // Fallback if place data is somehow missing for this marker ID
            tvTitle.setText(marker.getTitle() != null ? marker.getTitle() : "Unknown Location");
            tvCategory.setText("Details not available");
            tvRating.setVisibility(View.GONE);
            tvOpen.setVisibility(View.GONE);
            ivImage.setVisibility(View.GONE);
        }
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        renderWindowText(marker, mWindow);
        return mWindow;
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        renderWindowText(marker, mWindow);
        return mWindow;
    }
}
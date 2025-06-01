package com.example.alayaapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Locale;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {

    private static final String TAG = "PlaceAdapter";
    private Context context;
    private List<Place> placeList; // This list is the SAME instance as in HomeActivity

    public PlaceAdapter(Context context, List<Place> placeList) {
        this.context = context;
        this.placeList = placeList; // Stores the reference to the list from HomeActivity
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place_card, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        if (placeList == null || placeList.isEmpty() || position < 0 || position >= placeList.size()) {
            Log.w(TAG, "Invalid position or empty list in onBindViewHolder: " + position);
            return;
        }
        Place place = placeList.get(position);
        if (place == null) {
            Log.e(TAG, "Place object at position " + position + " is null.");
            return;
        }

        holder.tvPlaceName.setText(place.getName() != null ? place.getName() : "N/A");
        holder.tvPlaceRating.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));
        holder.tvPlaceReviews.setText(place.getReview_count_text() != null ? place.getReview_count_text() : "");
        holder.tvPlaceDistance.setText(place.getDistance_text() != null ? place.getDistance_text() : "");

        if (place.getImage_url() != null && !place.getImage_url().isEmpty()) {
            Glide.with(context)
                    .load(place.getImage_url())
                    .placeholder(R.drawable.img_placeholder) // Use a generic placeholder
                    .error(R.drawable.img_error)          // Use a generic error image
                    .into(holder.ivPlaceImage);
        } else {
            Log.w(TAG, "Image URL is null or empty for place: " + (place.getName() != null ? place.getName() : "Unknown"));
            holder.ivPlaceImage.setImageResource(R.drawable.img_placeholder);
        }

        // Inside PlaceAdapter.java onBindViewHolder()
        holder.itemView.setOnClickListener(v -> {
            if (place.getDocumentId() != null && !place.getDocumentId().isEmpty()) {

                Intent intent = new Intent(context, PlaceDetailsActivity.class);
                // Assuming 'PlaceDetailsActivity.EXTRA_PLACE_DOCUMENT_ID' is the correct constant
                intent.putExtra(PlaceDetailsActivity.EXTRA_PLACE_DOCUMENT_ID, place.getDocumentId());
                context.startActivity(intent);
            } else {
                Log.e(TAG, "Document ID is null or empty for clicked place: " + (place.getName() != null ? place.getName() : "Unknown"));
                Toast.makeText(context, "Cannot open details for this item.", Toast.LENGTH_SHORT).show(); // Added Toast for this case
            }
        });
    }

    @Override
    public int getItemCount() {
        return placeList != null ? placeList.size() : 0;
    }



    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlaceImage, ivStar;
        TextView tvPlaceName, tvPlaceRating, tvPlaceReviews, tvPlaceDistance;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlaceImage = itemView.findViewById(R.id.iv_item_place_image);
            ivStar = itemView.findViewById(R.id.iv_item_star);
            tvPlaceName = itemView.findViewById(R.id.tv_item_place_name);
            tvPlaceRating = itemView.findViewById(R.id.tv_item_place_rating);
            tvPlaceReviews = itemView.findViewById(R.id.tv_item_place_reviews);
            tvPlaceDistance = itemView.findViewById(R.id.tv_item_place_distance);
        }
    }
}
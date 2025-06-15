package com.example.alayaapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecommendedItineraryAdapter extends RecyclerView.Adapter<RecommendedItineraryAdapter.RecommendedViewHolder> {

    private final List<RecommendedPlace> recommendedPlaces;
    private final Context context;

    public RecommendedItineraryAdapter(Context context, List<RecommendedPlace> recommendedPlaces) {
        this.context = context;
        this.recommendedPlaces = recommendedPlaces;
    }

    @NonNull
    @Override
    public RecommendedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_itinerary_recommended, parent, false);
        return new RecommendedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendedViewHolder holder, int position) {
        RecommendedPlace place = recommendedPlaces.get(position);
        holder.tvPlaceName.setText(place.getName());
        holder.tvRating.setText(place.getRating());
        holder.tvReviews.setText(place.getReviews());

        holder.itemView.setOnClickListener(v -> {
            // Placeholder action
            Toast.makeText(context, "Clicked on " + place.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return recommendedPlaces.size();
    }

    static class RecommendedViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaceName, tvRating, tvReviews;

        public RecommendedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tv_recommended_place_name);
            tvRating = itemView.findViewById(R.id.tv_recommended_place_rating);
            tvReviews = itemView.findViewById(R.id.tv_recommended_place_reviews);
        }
    }

    // Simple data class for the recommended items
    public static class RecommendedPlace {
        private final String name;
        private final String rating;
        private final String reviews;

        public RecommendedPlace(String name, String rating, String reviews) {
            this.name = name;
            this.rating = rating;
            this.reviews = reviews;
        }

        public String getName() {
            return name;
        }

        public String getRating() {
            return rating;
        }

        public String getReviews() {
            return reviews;
        }
    }
}
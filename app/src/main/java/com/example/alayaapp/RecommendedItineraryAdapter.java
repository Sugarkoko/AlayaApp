package com.example.alayaapp;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class RecommendedItineraryAdapter extends RecyclerView.Adapter<RecommendedItineraryAdapter.RecommendedViewHolder> {

    private final List<Place> recommendedPlaces;
    private final Context context;

    public RecommendedItineraryAdapter(Context context, List<Place> recommendedPlaces) {
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
        Place place = recommendedPlaces.get(position);

        holder.tvPlaceName.setText(place.getName());

        // This logic handles cases where data might be missing in Firestore.
        // It works together with the layout fix to create a robust UI.
        if (place.getRating() > 0) {
            holder.llRatingContainer.setVisibility(View.VISIBLE);
            holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));

            if (place.getReview_count_text() != null && !TextUtils.isEmpty(place.getReview_count_text())) {
                holder.tvReviews.setText(place.getReview_count_text());
                holder.tvReviews.setVisibility(View.VISIBLE);
            } else {
                holder.tvReviews.setVisibility(View.GONE);
            }
        } else {
            holder.llRatingContainer.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (place.getDocumentId() != null && !place.getDocumentId().isEmpty()) {
                Intent intent = new Intent(context, PlaceDetailsActivity.class);
                intent.putExtra(PlaceDetailsActivity.EXTRA_PLACE_DOCUMENT_ID, place.getDocumentId());
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Details not available for " + place.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return recommendedPlaces.size();
    }

    static class RecommendedViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaceName, tvRating, tvReviews;
        LinearLayout llRatingContainer;

        public RecommendedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tv_recommended_place_name);
            tvRating = itemView.findViewById(R.id.tv_recommended_place_rating);
            tvReviews = itemView.findViewById(R.id.tv_recommended_place_reviews);
            llRatingContainer = itemView.findViewById(R.id.ll_rating_container);
        }
    }
}
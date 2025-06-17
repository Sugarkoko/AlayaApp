package com.example.alayaapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class ItineraryAlternativesAdapter extends RecyclerView.Adapter<ItineraryAlternativesAdapter.AlternativeViewHolder> {

    private final Context context;
    private final List<Place> alternatives;
    private final OnAlternativeClickListener listener;

    public interface OnAlternativeClickListener {
        void onAlternativeClick(Place selectedPlace);
    }

    public ItineraryAlternativesAdapter(Context context, List<Place> alternatives, OnAlternativeClickListener listener) {
        this.context = context;
        this.alternatives = alternatives;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlternativeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alternative_itinerary_card, parent, false);
        return new AlternativeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlternativeViewHolder holder, int position) {
        Place place = alternatives.get(position);
        holder.bind(place, listener);
    }

    @Override
    public int getItemCount() {
        return alternatives.size();
    }

    static class AlternativeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvDistance;

        public AlternativeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_alternative_name);
            tvCategory = itemView.findViewById(R.id.tv_alternative_category);
            tvDistance = itemView.findViewById(R.id.tv_alternative_distance);
        }

        void bind(final Place place, final OnAlternativeClickListener listener) {
            tvName.setText(place.getName());
            tvCategory.setText(place.getCategory() != null ? place.getCategory() : "N/A");

            if (place.getDistance() >= 0) {
                tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", place.getDistance()));
                tvDistance.setVisibility(View.VISIBLE);
            } else {
                tvDistance.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onAlternativeClick(place));
        }
    }
}
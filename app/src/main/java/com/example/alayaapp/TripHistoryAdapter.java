package com.example.alayaapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.TripViewHolder> {
    private final Context context;
    private final List<Trip> tripList;
    private final OnTripInteractionListener listener;

    // Interface for communication with the Activity
    public interface OnTripInteractionListener {
        void onTripLongPressed(Trip trip, int position);
    }

    public TripHistoryAdapter(Context context, List<Trip> tripList, OnTripInteractionListener listener) {
        this.context = context;
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trip_history, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.tvTripTitle.setText(trip.getTripTitle());
        holder.tvTripDate.setText(trip.getTripDate());


        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String currentMonthYear = (trip.getSavedAt() != null) ? sdf.format(trip.getSavedAt()) : "Unknown Date";

        if (position == 0) {
            holder.tvTripMonthYear.setVisibility(View.VISIBLE);
            holder.tvTripMonthYear.setText(currentMonthYear);
        } else {
            Trip previousTrip = tripList.get(position - 1);
            String previousMonthYear = (previousTrip.getSavedAt() != null) ? sdf.format(previousTrip.getSavedAt()) : "Unknown Date";
            if (currentMonthYear.equals(previousMonthYear)) {
                holder.tvTripMonthYear.setVisibility(View.GONE);
            } else {
                holder.tvTripMonthYear.setVisibility(View.VISIBLE);
                holder.tvTripMonthYear.setText(currentMonthYear);
            }
        }


        if (trip.getItinerary() != null && !trip.getItinerary().isEmpty()) {
            StringBuilder preview = new StringBuilder("Starts with: ");
            preview.append(trip.getItinerary().get(0).get("activity"));
            if (trip.getItinerary().size() > 1) {
                preview.append(", ").append(trip.getItinerary().get(1).get("activity"));
            }
            if (trip.getItinerary().size() > 2) {
                preview.append("...");
            }
            holder.tvItineraryPreview.setText(preview.toString());
            holder.tvItineraryPreview.setVisibility(View.VISIBLE);
        } else {
            holder.tvItineraryPreview.setVisibility(View.GONE);
        }


        holder.tripCard.setOnClickListener(v -> {
            if (trip.getDocumentId() != null && !trip.getDocumentId().isEmpty()) {
                Intent intent = new Intent(context, ItineraryLogDetailActivity.class);
                intent.putExtra("TRIP_ID", trip.getDocumentId());
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Error: Cannot open trip details.", Toast.LENGTH_SHORT).show();
            }
        });


        holder.tripCard.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onTripLongPressed(trip, position);
                return true; // Consume the event
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTripTitle, tvTripDate, tvTripMonthYear, tvItineraryPreview;
        CardView tripCard;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripTitle = itemView.findViewById(R.id.tv_name_trip);
            tvTripDate = itemView.findViewById(R.id.tv_date_trip);
            tvTripMonthYear = itemView.findViewById(R.id.tv_month_year_header);
            tvItineraryPreview = itemView.findViewById(R.id.tv_itinerary_preview);
            tripCard = itemView.findViewById(R.id.card_trip_item);
        }
    }
}
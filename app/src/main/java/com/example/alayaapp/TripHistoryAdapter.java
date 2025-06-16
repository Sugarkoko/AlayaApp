package com.example.alayaapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.TripViewHolder> {

    private final Context context;
    private final List<Trip> tripList;

    public TripHistoryAdapter(Context context, List<Trip> tripList) {
        this.context = context;
        this.tripList = tripList;
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

        if (trip.getSavedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            holder.tvTripMonthYear.setText(sdf.format(trip.getSavedAt()));
        } else {
            holder.tvTripMonthYear.setText("Date not available");
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ItineraryLogDetailActivity.class);
            intent.putExtra("TRIP_ID", trip.getDocumentId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTripTitle, tvTripDate, tvTripMonthYear;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripTitle = itemView.findViewById(R.id.tv_name_trip);
            tvTripDate = itemView.findViewById(R.id.tv_date_trip);
            tvTripMonthYear = itemView.findViewById(R.id.tv_month_year_header);
        }
    }
}
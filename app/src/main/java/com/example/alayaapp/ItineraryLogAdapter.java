package com.example.alayaapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class ItineraryLogAdapter extends RecyclerView.Adapter<ItineraryLogAdapter.LogViewHolder> {

    private final List<Map<String, String>> itineraryItems;

    public ItineraryLogAdapter(List<Map<String, String>> itineraryItems) {
        this.itineraryItems = itineraryItems;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_log_entry_detail, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        Map<String, String> item = itineraryItems.get(position);
        holder.tvTime.setText(item.get("time"));
        holder.tvActivity.setText(item.get("activity"));
        holder.tvRating.setText(item.get("rating"));
    }

    @Override
    public int getItemCount() {
        return itineraryItems.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvActivity, tvRating;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_itinerary_time);
            tvActivity = itemView.findViewById(R.id.tv_itinerary_activity);
            tvRating = itemView.findViewById(R.id.tv_itinerary_rating);
        }
    }
}
package com.example.alayaapp;

import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationSuggestionAdapter extends RecyclerView.Adapter<LocationSuggestionAdapter.ViewHolder> {

    private List<Address> suggestions = new ArrayList<>();
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(Address address);
    }

    public LocationSuggestionAdapter(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    public void setSuggestions(List<Address> newSuggestions) {
        this.suggestions.clear();
        if (newSuggestions != null) {
            this.suggestions.addAll(newSuggestions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_location_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Address address = suggestions.get(position);
        holder.bind(address, listener);
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMainText;
        TextView tvSecondaryText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMainText = itemView.findViewById(R.id.tv_suggestion_main_text);
            tvSecondaryText = itemView.findViewById(R.id.tv_suggestion_secondary_text);
        }

        void bind(final Address address, final OnSuggestionClickListener listener) {
            String mainText = ManualLocationPickerActivity.Helper.getAddressDisplayName(address);
            tvMainText.setText(mainText);

            // Construct secondary text (e.g., AdminArea, Country)
            StringBuilder secondaryBuilder = new StringBuilder();
            if (address.getAdminArea() != null && !mainText.toLowerCase(Locale.ROOT).contains(address.getAdminArea().toLowerCase(Locale.ROOT))) {
                secondaryBuilder.append(address.getAdminArea());
            }
            if (address.getCountryName() != null && !mainText.toLowerCase(Locale.ROOT).contains(address.getCountryName().toLowerCase(Locale.ROOT))) {
                if (secondaryBuilder.length() > 0) secondaryBuilder.append(", ");
                secondaryBuilder.append(address.getCountryName());
            }

            if (secondaryBuilder.length() > 0) {
                tvSecondaryText.setText(secondaryBuilder.toString());
                tvSecondaryText.setVisibility(View.VISIBLE);
            } else {
                tvSecondaryText.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onSuggestionClick(address));
        }
    }
}
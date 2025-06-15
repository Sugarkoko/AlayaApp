package com.example.alayaapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ItineraryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_LOCATION_HEADER = 0;
    private static final int VIEW_TYPE_HEADER = 1;
    private static final int VIEW_TYPE_ITINERARY_CARD = 2;
    private static final int VIEW_TYPE_HORIZONTAL_LIST = 3;

    private final List<Object> displayItems;
    private final ItinerariesActivity activity; // Reference to the activity

    public ItineraryAdapter(ItinerariesActivity activity, List<Object> displayItems) {
        this.activity = activity;
        this.displayItems = displayItems;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {
        Object item = displayItems.get(position);
        if (item instanceof LocationHeaderData) {
            return VIEW_TYPE_LOCATION_HEADER;
        } else if (item instanceof String) {
            return VIEW_TYPE_HEADER;
        } else if (item instanceof ItineraryItem) {
            return VIEW_TYPE_ITINERARY_CARD;
        } else if (item instanceof HorizontalListContainer) {
            return VIEW_TYPE_HORIZONTAL_LIST;
        }
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        switch (viewType) {
            case VIEW_TYPE_LOCATION_HEADER:
                View locationHeaderView = inflater.inflate(R.layout.item_itinerary_location_header, parent, false);
                return new LocationHeaderViewHolder(locationHeaderView);
            case VIEW_TYPE_HEADER:
                View headerView = inflater.inflate(R.layout.item_itinerary_header, parent, false);
                return new HeaderViewHolder(headerView);
            case VIEW_TYPE_ITINERARY_CARD:
                View cardView = inflater.inflate(R.layout.list_item_itinerary, parent, false);
                return new CardViewHolder(cardView);
            case VIEW_TYPE_HORIZONTAL_LIST:
                View horizontalView = inflater.inflate(R.layout.item_itinerary_horizontal_list, parent, false);
                return new HorizontalListViewHolder(horizontalView);
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_LOCATION_HEADER:
                ((LocationHeaderViewHolder) holder).bind();
                break;
            case VIEW_TYPE_HEADER:
                ((HeaderViewHolder) holder).bind((String) displayItems.get(position));
                break;
            case VIEW_TYPE_ITINERARY_CARD:
                ((CardViewHolder) holder).bind((ItineraryItem) displayItems.get(position));
                break;
            case VIEW_TYPE_HORIZONTAL_LIST:
                ((HorizontalListViewHolder) holder).bind((HorizontalListContainer) displayItems.get(position));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    @Override
    public long getItemId(int position) {
        Object item = displayItems.get(position);
        if (item instanceof ItineraryItem) {
            return ((ItineraryItem) item).getId();
        }
        return position + 10000;
    }

    // --- ViewHolder for the main Location Header ---
    class LocationHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationCity, tvLocationStatus;
        ImageButton ibEditLocation;

        LocationHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationCity = itemView.findViewById(R.id.tv_location_city_itineraries);
            tvLocationStatus = itemView.findViewById(R.id.tv_location_status_itineraries);
            ibEditLocation = itemView.findViewById(R.id.ib_edit_location_itineraries);
        }

        void bind() {
            tvLocationCity.setText(activity.getCurrentLocationNameToDisplay());
            tvLocationStatus.setText(activity.getCurrentLocationStatusToDisplay());
            ibEditLocation.setOnClickListener(v -> activity.showLocationChoiceDialog());
        }
    }

    // --- ViewHolder for Section Headers ---
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tv_section_header);
        }
        void bind(String title) {
            tvHeader.setText(title);
        }
    }

    // --- ViewHolder for Itinerary Cards ---
    class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTime, tvActivity, tvRating;
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_item_time);
            tvActivity = itemView.findViewById(R.id.tv_item_activity);
            tvRating = itemView.findViewById(R.id.tv_item_rating);
            ivImage = itemView.findViewById(R.id.iv_item_image);
        }

        void bind(ItineraryItem item) {
            tvTime.setText(timeFormat.format(item.getTime().getTime()));
            tvActivity.setText(item.getActivity());
            tvRating.setText(item.getRating());

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(activity)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .error(R.drawable.img_error)
                        .into(ivImage);
            } else {
                ivImage.setImageResource(R.drawable.img_placeholder);
            }
        }
    }

    // --- ViewHolder for the Horizontal RecyclerView ---
    class HorizontalListViewHolder extends RecyclerView.ViewHolder {
        RecyclerView rvHorizontal;
        HorizontalListViewHolder(@NonNull View itemView) {
            super(itemView);
            rvHorizontal = itemView.findViewById(R.id.rv_horizontal);
        }
        void bind(HorizontalListContainer container) {
            RecommendedItineraryAdapter adapter = new RecommendedItineraryAdapter(activity, container.getRecommendedPlaces());
            rvHorizontal.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
            rvHorizontal.setAdapter(adapter);
        }
    }

    // --- Data container classes ---
    public static class LocationHeaderData {}
    public static class HorizontalListContainer {
        private final List<RecommendedItineraryAdapter.RecommendedPlace> recommendedPlaces;
        public HorizontalListContainer(List<RecommendedItineraryAdapter.RecommendedPlace> places) {
            this.recommendedPlaces = places;
        }
        public List<RecommendedItineraryAdapter.RecommendedPlace> getRecommendedPlaces() {
            return recommendedPlaces;
        }
    }
}
package com.example.alayaapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private static final int VIEW_TYPE_FOOTER_MESSAGE = 4;

    /**
     * Listener interface for actions originating from the header.
     */
    public interface ItineraryHeaderListener {
        void onRegenerateClicked();
        void onClearClicked();
        void onEditLocationClicked();
        void onCustomizeClicked();
    }

    private final List<Object> displayItems;
    private final ItinerariesActivity activity; // Context
    private final ItineraryHeaderListener headerListener; // Listener for header actions

    public ItineraryAdapter(ItinerariesActivity activity, List<Object> displayItems, ItineraryHeaderListener listener) {
        this.activity = activity;
        this.displayItems = displayItems;
        this.headerListener = listener;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {
        Object item = displayItems.get(position);
        if (item instanceof LocationHeaderData) {
            return VIEW_TYPE_LOCATION_HEADER;
        } else if (item instanceof ItineraryItem) {
            return VIEW_TYPE_ITINERARY_CARD;
        } else if (item instanceof HorizontalListContainer) {
            return VIEW_TYPE_HORIZONTAL_LIST;
        } else if (item instanceof String) {
            // Differentiate between a main header and a footer message
            String text = (String) item;
            if (text.equals("Suggested Itinerary")) {
                return VIEW_TYPE_HEADER;
            } else {
                return VIEW_TYPE_FOOTER_MESSAGE; // Assume other strings are footers/disclaimers
            }
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
            case VIEW_TYPE_FOOTER_MESSAGE:
                View footerView = inflater.inflate(R.layout.item_itinerary_footer_message, parent, false);
                return new FooterMessageViewHolder(footerView);
            default:
                throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_LOCATION_HEADER:
                ((LocationHeaderViewHolder) holder).bind((LocationHeaderData) displayItems.get(position), headerListener);
                break;
            case VIEW_TYPE_HEADER:
                ((HeaderViewHolder) holder).bind((String) displayItems.get(position));
                break;
            case VIEW_TYPE_ITINERARY_CARD:
                int sequenceNumber = 1;
                for (int i = 0; i < position; i++) {
                    if (displayItems.get(i) instanceof ItineraryItem) {
                        sequenceNumber++;
                    }
                }
                ((CardViewHolder) holder).bind((ItineraryItem) displayItems.get(position), sequenceNumber);
                break;
            case VIEW_TYPE_HORIZONTAL_LIST:
                ((HorizontalListViewHolder) holder).bind((HorizontalListContainer) displayItems.get(position));
                break;
            case VIEW_TYPE_FOOTER_MESSAGE:
                ((FooterMessageViewHolder) holder).bind((String) displayItems.get(position));
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
        // Use hashcode for stable IDs for other objects, which is better than just position
        return item.hashCode();
    }

    // --- ViewHolder for the main Location Header ---
    class LocationHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationCity, tvLocationStatus, tvHeaderMessage, tvItinerariesTitle;
        ImageButton ibEditLocation;
        Button btnRegenerate, btnClear, btnCustomize;

        LocationHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItinerariesTitle = itemView.findViewById(R.id.tv_itineraries_title);
            tvLocationCity = itemView.findViewById(R.id.tv_location_city_itineraries);
            tvLocationStatus = itemView.findViewById(R.id.tv_location_status_itineraries);
            ibEditLocation = itemView.findViewById(R.id.ib_edit_location_itineraries);
            tvHeaderMessage = itemView.findViewById(R.id.tv_header_message);
            btnRegenerate = itemView.findViewById(R.id.btn_regenerate_itinerary);
            btnClear = itemView.findViewById(R.id.btn_clear_itinerary);
            btnCustomize = itemView.findViewById(R.id.btn_customize_itinerary);
        }

        void bind(LocationHeaderData data, ItineraryHeaderListener listener) {
            // The ViewModel is now the source of truth for this text
            activity.itineraryViewModel.currentLocationName.observe(activity, tvLocationCity::setText);
            activity.itineraryViewModel.currentLocationStatus.observe(activity, tvLocationStatus::setText);

            ibEditLocation.setOnClickListener(v -> listener.onEditLocationClicked());
            btnRegenerate.setOnClickListener(v -> listener.onRegenerateClicked());
            btnClear.setOnClickListener(v -> listener.onClearClicked());
            btnCustomize.setOnClickListener(v -> listener.onCustomizeClicked());

            // Hide title if there are no items
            boolean hasItems = displayItems.stream().anyMatch(item -> item instanceof ItineraryItem);
            tvItinerariesTitle.setVisibility(hasItems ? View.VISIBLE : View.GONE);

            String message = data.getHeaderMessage();
            if (message != null && !message.isEmpty()) {
                tvHeaderMessage.setText(message);
                tvHeaderMessage.setVisibility(View.VISIBLE);
            } else {
                tvHeaderMessage.setVisibility(View.GONE);
            }
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
        TextView tvTime, tvActivity, tvRating, tvItemNumber;
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_item_time);
            tvActivity = itemView.findViewById(R.id.tv_item_activity);
            tvRating = itemView.findViewById(R.id.tv_item_rating);
            ivImage = itemView.findViewById(R.id.iv_item_image);
            tvItemNumber = itemView.findViewById(R.id.tv_item_number);
        }

        void bind(ItineraryItem item, int sequenceNumber) {
            tvTime.setText(timeFormat.format(item.getTime().getTime()));
            tvActivity.setText(item.getActivity());
            tvRating.setText(item.getRating());
            tvItemNumber.setText(String.valueOf(sequenceNumber));

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
        TextView tvTitle;

        HorizontalListViewHolder(@NonNull View itemView) {
            super(itemView);
            rvHorizontal = itemView.findViewById(R.id.rv_horizontal);
            tvTitle = itemView.findViewById(R.id.tv_horizontal_section_header);
        }

        void bind(HorizontalListContainer container) {
            tvTitle.setText(container.getTitle());
            // The adapter now takes a List<Place>
            RecommendedItineraryAdapter adapter = new RecommendedItineraryAdapter(activity, container.getRecommendedPlaces());
            rvHorizontal.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
            rvHorizontal.setAdapter(adapter);
        }
    }

    // --- ViewHolder for Footer Message ---
    static class FooterMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvFooterMessage;
        FooterMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFooterMessage = itemView.findViewById(R.id.tv_footer_message);
        }
        void bind(String message) {
            tvFooterMessage.setText(message);
        }
    }

    // --- Data container classes ---
    public static class LocationHeaderData {
        private final String headerMessage;
        public LocationHeaderData(String headerMessage) {
            this.headerMessage = headerMessage;
        }
        public String getHeaderMessage() {
            return headerMessage;
        }
    }

    public static class HorizontalListContainer {
        private final String title;
        private final List<Place> recommendedPlaces; // Now holds a list of real Place objects

        public HorizontalListContainer(String title, List<Place> places) {
            this.title = title;
            this.recommendedPlaces = places;
        }

        public String getTitle() {
            return title;
        }

        public List<Place> getRecommendedPlaces() {
            return recommendedPlaces;
        }
    }
}
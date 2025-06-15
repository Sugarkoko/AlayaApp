package com.example.alayaapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// This adapter now handles multiple view types
public class ItineraryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // View Type constants
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITINERARY_CARD = 1;
    private static final int VIEW_TYPE_HORIZONTAL_LIST = 2;

    private List<Object> displayItems; // Can hold Headers, ItineraryItems, etc.
    private final OnStartDragListener dragStartListener;
    private Context context;
    private boolean isEditMode = false;

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public ItineraryAdapter(List<Object> displayItems, OnStartDragListener dragStartListener) {
        this.displayItems = displayItems;
        this.dragStartListener = dragStartListener;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {
        Object item = displayItems.get(position);
        if (item instanceof String) {
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
        this.context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
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
                // Should not happen
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_HEADER:
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                headerViewHolder.bind((String) displayItems.get(position));
                break;
            case VIEW_TYPE_ITINERARY_CARD:
                CardViewHolder cardViewHolder = (CardViewHolder) holder;
                cardViewHolder.bind((ItineraryItem) displayItems.get(position));
                break;
            case VIEW_TYPE_HORIZONTAL_LIST:
                HorizontalListViewHolder horizontalViewHolder = (HorizontalListViewHolder) holder;
                horizontalViewHolder.bind((HorizontalListContainer) displayItems.get(position));
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
        // For other types, use their position as a stable ID.
        // Add a large offset to avoid collision with ItineraryItem IDs.
        return position + 10000;
    }

    public void setEditMode(boolean editMode) {
        boolean needsUpdate = isEditMode != editMode;
        isEditMode = editMode;
        if (needsUpdate) {
            notifyDataSetChanged(); // A full redraw is needed to show/hide all handles
        }
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        // Ensure we are only moving ItineraryItem objects
        if (displayItems.get(fromPosition) instanceof ItineraryItem && displayItems.get(toPosition) instanceof ItineraryItem) {
            Collections.swap(displayItems, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
            recalculateTimes();
            return true;
        }
        return false;
    }

    public void recalculateTimes() {
        // Find the first ItineraryItem to start recalculating from
        int firstItemIndex = -1;
        for (int i = 0; i < displayItems.size(); i++) {
            if (displayItems.get(i) instanceof ItineraryItem) {
                firstItemIndex = i;
                break;
            }
        }

        if (firstItemIndex == -1) return; // No items to recalculate

        Calendar currentTime = (Calendar) ((ItineraryItem) displayItems.get(firstItemIndex)).getTime().clone();
        for (int i = firstItemIndex; i < displayItems.size(); i++) {
            if (displayItems.get(i) instanceof ItineraryItem) {
                ItineraryItem currentItem = (ItineraryItem) displayItems.get(i);
                Calendar itemTime = (Calendar) currentTime.clone();
                currentItem.setTime(itemTime);
                notifyItemChanged(i);
                currentTime.add(Calendar.HOUR_OF_DAY, 1); // Or use your fixed duration logic
            }
        }
    }

    // --- ViewHolder for Section Headers ---
    class HeaderViewHolder extends RecyclerView.ViewHolder {
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
        ImageView ivDragHandle, ivImage;
        TextView tvTime, tvActivity, tvRating;
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        @SuppressLint("ClickableViewAccessibility")
        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            tvTime = itemView.findViewById(R.id.tv_item_time);
            tvActivity = itemView.findViewById(R.id.tv_item_activity);
            tvRating = itemView.findViewById(R.id.tv_item_rating);
            ivImage = itemView.findViewById(R.id.iv_item_image);

            ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    dragStartListener.onStartDrag(this);
                }
                return false;
            });
        }

        void bind(ItineraryItem item) {
            tvTime.setText(timeFormat.format(item.getTime().getTime()));
            tvActivity.setText(item.getActivity());
            tvRating.setText(item.getRating());

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .error(R.drawable.img_error)
                        .into(ivImage);
            } else {
                ivImage.setImageResource(R.drawable.img_placeholder);
            }
            ivDragHandle.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
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
            RecommendedItineraryAdapter adapter = new RecommendedItineraryAdapter(context, container.getRecommendedPlaces());
            rvHorizontal.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            rvHorizontal.setAdapter(adapter);
        }
    }

    // --- Data container for the horizontal list ---
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
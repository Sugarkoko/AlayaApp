package com.example.alayaapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alayaapp.databinding.ListItemItineraryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList; // Added for temporary list
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ItineraryViewHolder> {

    private List<ItineraryItem> itineraryList;
    private boolean isEditMode = false;
    private final OnStartDragListener dragStartListener;
    private final OnItemClickListener itemClickListener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private Drawable selectableItemBackground;

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ItineraryAdapter(List<ItineraryItem> itineraryList,
                            OnStartDragListener dragStartListener,
                            OnItemClickListener itemClickListener) {
        this.itineraryList = itineraryList;
        this.dragStartListener = dragStartListener;
        this.itemClickListener = itemClickListener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ItineraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (selectableItemBackground == null) {
            TypedValue outValue = new TypedValue();
            parent.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            selectableItemBackground = ContextCompat.getDrawable(parent.getContext(), outValue.resourceId);
        }

        ListItemItineraryBinding binding = ListItemItineraryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ItineraryViewHolder(binding);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ItineraryViewHolder holder, int position) {
        ItineraryItem item = itineraryList.get(position);
        if (item == null) return;

        if (item.getTime() != null) {
            holder.binding.tvItemTime.setText(timeFormat.format(item.getTime().getTime()));
        } else {
            holder.binding.tvItemTime.setText("N/A");
        }
        holder.binding.tvItemActivity.setText(item.getActivity() != null ? item.getActivity() : "Unknown Activity");
        holder.binding.tvItemRating.setText(item.getRating() != null ? item.getRating() : "-");

        holder.binding.ivDragHandle.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        holder.binding.ivDragHandle.setOnTouchListener(null);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setBackground(null);

        final int currentPosition = holder.getAdapterPosition();

        if (isEditMode) {
            holder.binding.ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (dragStartListener != null) {
                        dragStartListener.onStartDrag(holder);
                        return true;
                    }
                }
                return false;
            });

            holder.itemView.setOnClickListener(v -> {
                if (itemClickListener != null && currentPosition != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(currentPosition);
                }
            });
            holder.itemView.setBackground(null);

        } else {
            holder.itemView.setOnClickListener(v -> {
                if (itemClickListener != null && currentPosition != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(currentPosition);
                }
            });
            holder.itemView.setBackground(selectableItemBackground);
        }
    }


    @Override
    public int getItemCount() {
        return itineraryList != null ? itineraryList.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        if (itineraryList != null && position >= 0 && position < itineraryList.size() && itineraryList.get(position) != null) {
            return itineraryList.get(position).getId();
        }
        return RecyclerView.NO_ID;
    }

    public void setEditMode(boolean editMode) {
        boolean needsUpdate = isEditMode != editMode;
        isEditMode = editMode;
        if (needsUpdate) {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        if (itineraryList == null || fromPosition < 0 || fromPosition >= itineraryList.size() || toPosition < 0 || toPosition >= itineraryList.size()) {
            return false;
        }
        if (fromPosition == toPosition) {
            return false;
        }

        // 1. Capture original time slots before swapping
        List<Calendar> originalTimeSlots = new ArrayList<>(itineraryList.size());
        for (ItineraryItem item : itineraryList) {
            originalTimeSlots.add(item.getTime() != null ? (Calendar) item.getTime().clone() : null);
        }


        // 2. Perform the swap in the list
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(itineraryList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(itineraryList, i, i - 1);
            }
        }

        // 3. Notify the adapter about the visual move
        notifyItemMoved(fromPosition, toPosition);

        // 4. Assign preserved times to the newly ordered list
        assignPreservedTimes(originalTimeSlots);

        return true;
    }

    // New method to assign original times to the reordered list
    private void assignPreservedTimes(List<Calendar> originalTimeSlots) {
        if (itineraryList == null || originalTimeSlots == null || itineraryList.size() != originalTimeSlots.size()) {
            // Data inconsistency, maybe log an error or fall back to full recalc?
            recalculateTimes(); // Fallback for safety
            return;
        }

        for (int i = 0; i < itineraryList.size(); i++) {
            ItineraryItem currentItem = itineraryList.get(i);
            Calendar preservedTime = originalTimeSlots.get(i);
            if (currentItem != null) {
                // Assign the preserved time (might be null if original was null)
                currentItem.setTime(preservedTime != null ? (Calendar) preservedTime.clone() : null);
                // Notify item changed to update the view with the new time
                notifyItemChanged(i);
            }
        }
    }


    public void recalculateTimes() {
        if (itineraryList == null || itineraryList.isEmpty()) return;

        Calendar firstTime = null;
        int firstTimeIndex = -1;
        for(int i = 0; i < itineraryList.size(); i++) {
            ItineraryItem current = itineraryList.get(i);
            if(current != null && current.getTime() != null) {
                firstTime = (Calendar) current.getTime().clone();
                firstTimeIndex = i;
                break;
            }
        }

        if (firstTime == null) {
            if (!itineraryList.isEmpty() && itineraryList.get(0) != null) {
                firstTime = Calendar.getInstance();
                firstTime.set(Calendar.HOUR_OF_DAY, 9); firstTime.set(Calendar.MINUTE, 0); firstTime.set(Calendar.SECOND, 0);
                itineraryList.get(0).setTime((Calendar)firstTime.clone());
                firstTimeIndex = 0;
                notifyItemChanged(0);
            } else {
                return;
            }
        }


        Calendar currentTime = (Calendar) firstTime.clone();
        for (int i = firstTimeIndex + 1; i < itineraryList.size(); i++) {
            ItineraryItem currentItem = itineraryList.get(i);
            if (currentItem != null) {
                currentTime.add(Calendar.HOUR_OF_DAY, 1);
                currentItem.setTime((Calendar) currentTime.clone());
                notifyItemChanged(i);
            }
        }

        currentTime = (Calendar) firstTime.clone();
        for (int i = firstTimeIndex - 1; i >= 0; i--) {
            ItineraryItem currentItem = itineraryList.get(i);
            if (currentItem != null) {
                currentTime.add(Calendar.HOUR_OF_DAY, -1);
                currentItem.setTime((Calendar) currentTime.clone());
                notifyItemChanged(i);
            }
        }
    }

    public List<ItineraryItem> getCurrentList() {
        return itineraryList;
    }

    static class ItineraryViewHolder extends RecyclerView.ViewHolder {
        private final ListItemItineraryBinding binding;

        ItineraryViewHolder(@NonNull ListItemItineraryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
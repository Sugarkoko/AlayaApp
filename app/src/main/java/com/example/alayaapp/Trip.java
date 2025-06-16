package com.example.alayaapp;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Trip {

    @Exclude private String documentId;

    private String tripTitle;
    private String tripDate;
    private String tripSignature; // NEW: For checking duplicates
    @ServerTimestamp private Date savedAt;
    private List<Map<String, String>> itinerary;

    public Trip() {
        // Public no-arg constructor needed for Firestore
    }

    public Trip(String tripTitle, String tripDate, String tripSignature, List<Map<String, String>> itinerary) {
        this.tripTitle = tripTitle;
        this.tripDate = tripDate;
        this.tripSignature = tripSignature;
        this.itinerary = itinerary;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getTripTitle() {
        return tripTitle;
    }

    public void setTripTitle(String tripTitle) {
        this.tripTitle = tripTitle;
    }

    public String getTripDate() {
        return tripDate;
    }

    public void setTripDate(String tripDate) {
        this.tripDate = tripDate;
    }

    public String getTripSignature() {
        return tripSignature;
    }

    public void setTripSignature(String tripSignature) {
        this.tripSignature = tripSignature;
    }

    public Date getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(Date savedAt) {
        this.savedAt = savedAt;
    }

    public List<Map<String, String>> getItinerary() {
        return itinerary;
    }

    public void setItinerary(List<Map<String, String>> itinerary) {
        this.itinerary = itinerary;
    }
}
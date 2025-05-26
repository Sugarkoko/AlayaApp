package com.example.alayaapp;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint; // <<< IMPORT THIS
import com.google.firebase.firestore.IgnoreExtraProperties;
// import java.util.List; // No longer needed for 'coordinates' if using GeoPoint

@IgnoreExtraProperties
public class Place {

    // Fields directly matching your Firestore document screenshot
    private String about;
    private String best_time;
    private String category;
    private String city;
    // private List<String> coordinates; // OLD: Was expecting a List of Strings
    private GeoPoint coordinates;      // MODIFIED: Now expecting a GeoPoint object
    private long id;
    private String image_url;
    private String name;
    private String open;
    private long price_range;
    private double rating;

    // Fields used in UI but might be missing from some/all Firestore documents
    // IMPORTANT: Add these to your Firestore documents if you want them displayed
    private String review_count_text;
    private String distance_text;

    @Exclude
    private String documentId; // Firestore's auto-generated document ID

    public Place() {}

    // --- Getters ---
    public String getAbout() { return about; }
    public String getBest_time() { return best_time; }
    public String getCategory() { return category; }
    public String getCity() { return city; }
    // public List<String> getCoordinates() { return coordinates; } // OLD Getter
    public GeoPoint getCoordinates() { return coordinates; }      // MODIFIED: Getter for GeoPoint
    public long getId() { return id; }
    public String getImage_url() { return image_url; }
    public String getName() { return name; }
    public String getOpen() { return open; }
    public long getPrice_range() { return price_range; }
    public double getRating() { return rating; }

    public String getReview_count_text() { return review_count_text; }
    public String getDistance_text() { return distance_text; }

    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    // MODIFIED: Convenience methods for lat/lng to use GeoPoint directly
    @Exclude
    public Double getLatitude() {
        if (coordinates != null) {
            return coordinates.getLatitude(); // Directly get latitude from GeoPoint
        }
        return null;
    }

    @Exclude
    public Double getLongitude() {
        if (coordinates != null) {
            return coordinates.getLongitude(); // Directly get longitude from GeoPoint
        }
        return null;
    }
}
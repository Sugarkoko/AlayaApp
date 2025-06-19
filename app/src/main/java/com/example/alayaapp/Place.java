package com.example.alayaapp;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Map;

@IgnoreExtraProperties
public class Place {

    private String about;
    private String best_time;
    private String category;
    private String city;
    private transient GeoPoint coordinates;
    private Double latitude;
    private Double longitude;
    private long id;
    private String image_url;
    private String name;
    private String open;
    private long price_range;
    private double rating;
    private String review_count_text;
    private String distance_text;


    private int averageVisitDuration; // Use int for minutes
    private Map<String, Map<String, String>> openingHours; // Map of day to {open, close}

    @Exclude
    private String documentId; // To store the Firestore document ID

    //  Field for sorting by distance
    @Exclude
    private double distance;

    // Public no-arg constructor required for Firestore
    public Place() {}


    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }
    public String getBest_time() { return best_time; }
    public void setBest_time(String best_time) { this.best_time = best_time; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public GeoPoint getCoordinates() { return coordinates; }
    public void setCoordinates(GeoPoint coordinates) {
        this.coordinates = coordinates;
        if (coordinates != null) {
            this.latitude = coordinates.getLatitude();
            this.longitude = coordinates.getLongitude();
        }
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getImage_url() { return image_url; }
    public void setImage_url(String image_url) { this.image_url = image_url; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOpen() { return open; }
    public void setOpen(String open) { this.open = open; }
    public long getPrice_range() { return price_range; }
    public void setPrice_range(long price_range) { this.price_range = price_range; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public String getReview_count_text() { return review_count_text; }
    public void setReview_count_text(String review_count_text) { this.review_count_text = review_count_text; }
    public String getDistance_text() { return distance_text; }
    public void setDistance_text(String distance_text) { this.distance_text = distance_text; }

    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    // --- Getters and Setters for New Fields ---
    public int getAverageVisitDuration() { return averageVisitDuration; }
    public void setAverageVisitDuration(int averageVisitDuration) { this.averageVisitDuration = averageVisitDuration; }
    public Map<String, Map<String, String>> getOpeningHours() { return openingHours; }
    public void setOpeningHours(Map<String, Map<String, String>> openingHours) { this.openingHours = openingHours; }


    @Exclude
    public double getDistance() { return distance; }
    @Exclude
    public void setDistance(double distance) { this.distance = distance; }


    @Exclude
    public Double getLatitude() {
        // Now returns the primitive field
        if (latitude != null) {
            return latitude;
        } else if (coordinates != null) {
            return coordinates.getLatitude();
        }
        return null;
    }

    @Exclude
    public Double getLongitude() {
        // Now returns the primitive field
        if (longitude != null) {
            return longitude;
        } else if (coordinates != null) {
            return coordinates.getLongitude();
        }
        return null;
    }
}
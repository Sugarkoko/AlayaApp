package com.example.alayaapp;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Place {
    private String about;
    private String best_time;
    private String category;
    private String city;
    private GeoPoint coordinates;
    private long id;
    private String image_url;
    private String name;
    private String open;
    private long price_range;
    private double rating;
    private String review_count_text;
    private String distance_text;
    @Exclude private String documentId;

    public Place() {}

    // --- Getters ---
    public String getAbout() { return about; }
    public String getBest_time() { return best_time; }
    public String getCategory() { return category; }
    public String getCity() { return city; }
    public GeoPoint getCoordinates() { return coordinates; }
    public long getId() { return id; }
    public String getImage_url() { return image_url; }
    public String getName() { return name; }
    public String getOpen() { return open; }
    public long getPrice_range() { return price_range; }
    public double getRating() { return rating; }
    public String getReview_count_text() { return review_count_text; }
    public String getDistance_text() { return distance_text; }
    @Exclude public String getDocumentId() { return documentId; }

    // --- Setters ---
    public void setAbout(String about) { this.about = about; }
    public void setBest_time(String best_time) { this.best_time = best_time; }
    public void setCategory(String category) { this.category = category; }
    public void setCity(String city) { this.city = city; }
    public void setCoordinates(GeoPoint coordinates) { this.coordinates = coordinates; }
    public void setId(long id) { this.id = id; }
    public void setImage_url(String image_url) { this.image_url = image_url; }
    public void setName(String name) { this.name = name; } // <<< THIS WAS MISSING
    public void setOpen(String open) { this.open = open; }
    public void setPrice_range(long price_range) { this.price_range = price_range; }
    public void setRating(double rating) { this.rating = rating; }
    public void setReview_count_text(String review_count_text) { this.review_count_text = review_count_text; }
    public void setDistance_text(String distance_text) { this.distance_text = distance_text; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }


    @Exclude public Double getLatitude() {
        if (coordinates != null) {
            return coordinates.getLatitude();
        }
        return null;
    }
    @Exclude public Double getLongitude() {
        if (coordinates != null) {
            return coordinates.getLongitude();
        }
        return null;
    }
}
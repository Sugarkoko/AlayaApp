package com.example.alayaapp.util; // Make sure this package declaration is correct

public class GeoPoint {
    private double latitude;
    private double longitude;
    private double altitude; // Optional, if you ever used it from OSM GeoPoint

    public GeoPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = 0.0; // Default altitude
    }

    public GeoPoint(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    @Override
    public String toString() {
        return "GeoPoint [latitude=" + latitude + ", longitude=" + longitude + ", altitude=" + altitude + "]";
    }
}
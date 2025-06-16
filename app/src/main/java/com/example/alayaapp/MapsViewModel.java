package com.example.alayaapp;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.PolyUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapsViewModel extends ViewModel {

    private static final String TAG = "MapsViewModel";
    private final MutableLiveData<DirectionsResult> directionsResult = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public LiveData<DirectionsResult> getDirectionsResult() {
        return directionsResult;
    }

    public void fetchDirections(LatLng origin, LatLng dest, String apiKey, String travelMode) {
        executorService.execute(() -> {
            String urlString = getDirectionsUrl(origin, dest, null, apiKey, travelMode);
            Log.d(TAG, "Request URL: " + urlString);
            String jsonData = "";
            try {
                jsonData = downloadUrl(urlString);
            } catch (IOException e) {
                Log.e(TAG, "Error downloading URL: " + e.getMessage());
                directionsResult.postValue(null); // Post null on error
                return;
            }

            if (jsonData.isEmpty()) {
                Log.e(TAG, "Downloaded JSON data is empty.");
                directionsResult.postValue(null);
                return;
            }

            DirectionsResult result = parseDirections(jsonData);
            directionsResult.postValue(result);
        });
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest, List<LatLng> waypoints, String key, String modeParam) {
        String strOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        String strDest = "destination=" + dest.latitude + "," + dest.longitude;
        String modeQueryParam = "mode=" + modeParam;
        String strWaypoints = "";
        if (waypoints != null && !waypoints.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("waypoints=optimize:true|");
            for (int i = 0; i < waypoints.size(); i++) {
                LatLng point = waypoints.get(i);
                sb.append(point.latitude).append(",").append(point.longitude);
                if (i < waypoints.size() - 1) {
                    sb.append("|");
                }
            }
            strWaypoints = "&" + sb.toString();
        }
        String parameters = strOrigin + "&" + strDest + strWaypoints + "&" + modeQueryParam;
        parameters += "&key=" + key;
        String output = "json";
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.e(TAG, "Exception downloading URL: " + e.toString());
            throw new IOException("Error downloading URL", e);
        } finally {
            if (iStream != null) {
                try {
                    iStream.close();
                } catch (IOException e) { /* ignore */ }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return data;
    }

    private DirectionsResult parseDirections(String jsonData) {
        List<LatLng> polylinePoints = new ArrayList<>();
        LatLngBounds routeBounds = null;
        String duration = "";
        String distance = "";
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String status = jsonObject.optString("status");
            if (!"OK".equals(status)) {
                Log.e(TAG, "Directions API non-OK status: " + status + " - " + jsonObject.optString("error_message"));
                return null;
            }
            JSONArray routesArray = jsonObject.getJSONArray("routes");
            if (routesArray.length() > 0) {
                JSONObject route = routesArray.getJSONObject(0);
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");
                polylinePoints = PolyUtil.decode(encodedPolyline);
                JSONObject boundsJson = route.getJSONObject("bounds");
                JSONObject northeastJson = boundsJson.getJSONObject("northeast");
                JSONObject southwestJson = boundsJson.getJSONObject("southwest");
                LatLng northeast = new LatLng(northeastJson.getDouble("lat"), northeastJson.getDouble("lng"));
                LatLng southwest = new LatLng(southwestJson.getDouble("lat"), southwestJson.getDouble("lng"));
                routeBounds = new LatLngBounds(southwest, northeast);
                if (route.has("legs")) {
                    JSONArray legs = route.getJSONArray("legs");
                    if (legs.length() > 0) {
                        duration = legs.getJSONObject(0).getJSONObject("duration").getString("text");
                        distance = legs.getJSONObject(0).getJSONObject("distance").getString("text");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing directions JSON: " + e.getMessage());
            return null;
        }
        if (polylinePoints.isEmpty() && jsonData.contains("\"status\" : \"OK\"") && jsonData.contains("\"routes\" : [ ]")) {
            Log.w(TAG, "Polyline points are empty even though status was OK. This implies no route for the mode (ZERO_RESULTS).");
            return null;
        }
        return new DirectionsResult(polylinePoints, routeBounds, duration, distance);
    }

    // Public inner class to hold the result
    public static class DirectionsResult {
        public final List<LatLng> polylinePoints;
        public final LatLngBounds routeBounds;
        public final String durationText;
        public final String distanceText;

        DirectionsResult(List<LatLng> polylinePoints, LatLngBounds routeBounds, String duration, String distance) {
            this.polylinePoints = polylinePoints;
            this.routeBounds = routeBounds;
            this.durationText = duration;
            this.distanceText = distance;
        }
    }
}
package com.example.alayaapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.view.ViewGroup;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alayaapp.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.maps.android.PolyUtil; // Import PolyUtil

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {
    private static final String TAG = "MapsActivity";
    private ActivityMapsBinding binding;
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    final int CURRENT_ITEM_ID = R.id.navigation_map;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    public static final String EXTRA_TARGET_LATITUDE = "com.example.alayaapp.TARGET_LATITUDE";
    public static final String EXTRA_TARGET_LONGITUDE = "com.example.alayaapp.TARGET_LONGITUDE";
    public static final String EXTRA_TARGET_NAME = "com.example.alayaapp.TARGET_NAME";
    public static final String EXTRA_DRAW_ROUTE = "com.example.alayaapp.DRAW_ROUTE";

    private static final String PREFS_NAME = "AlayaAppPrefs";
    private static final String KEY_LOCATION_MODE = "location_mode";
    private static final String KEY_MANUAL_LATITUDE = "manual_latitude";
    private static final String KEY_MANUAL_LONGITUDE = "manual_longitude";
    private static final String KEY_MANUAL_LOCATION_NAME = "manual_location_name";

    private String currentLocationMode = "auto";
    private LatLng manualHomeLocation = null;
    private String manualHomeLocationName = "Manually Set Location";

    private HashMap<String, Place> markerPlaceMap;
    private CustomInfoWindowAdapter customInfoWindowAdapter;

    private Marker manualHomeMarker = null;
    private Marker routeOriginMarker;
    private Marker routeDestinationMarker;
    private Polyline currentRoutePolyline;

    private LatLng pendingRouteDestLatLng;
    private String pendingRouteDestName;
    private String pendingRouteDestDocId;
    private boolean awaitingPermissionsForRoute = false;

    private String directionsApiKey;

    private static class DirectionsResult {
        List<LatLng> polylinePoints;
        LatLngBounds routeBounds;

        DirectionsResult(List<LatLng> polylinePoints, LatLngBounds routeBounds) {
            this.polylinePoints = polylinePoints;
            this.routeBounds = routeBounds;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        try {
            binding = ActivityMapsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading page.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainMapsCoordinatorLayout, (v, insets) -> { // Use the CoordinatorLayout ID
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding to the CoordinatorLayout itself or a direct child that holds the map and top panel
            // For this layout, cl_map_content_area is a direct child.
            // Or, adjust margins of the top panel and bottom navigation.
            // For simplicity, let's assume binding.getRoot() which is main_maps_coordinator_layout needs padding.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Don't pad bottom for bottom nav
            // Adjust BottomNavigationView's bottom margin if needed
            if (binding.bottomNavigationMapsPage != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.bottomNavigationMapsPage.getLayoutParams();
                params.bottomMargin = systemBars.bottom;
                binding.bottomNavigationMapsPage.setLayoutParams(params);
            }

            return insets;
        });


        markerPlaceMap = new HashMap<>();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            directionsApiKey = bundle.getString("com.google.android.geo.API_KEY");
            if (directionsApiKey == null || directionsApiKey.isEmpty()) {
                Log.e(TAG, "Directions API Key not found in AndroidManifest.xml");
                Toast.makeText(this, "API Key for directions is missing.", Toast.LENGTH_LONG).show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            Log.e(TAG, "Failed to load meta-data, NullPointer: " + e.getMessage());
        }

        setupBottomNavigation();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "SupportMapFragment not found!");
            Toast.makeText(this, "Error: Map Fragment not found.", Toast.LENGTH_LONG).show();
        }

        binding.fabMyLocation.setOnClickListener(v -> {
            if (mMap != null) {
                if ("auto".equals(currentLocationMode)) {
                    centerOnActualGPSLocation(true);
                } else if (manualHomeLocation != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(manualHomeLocation, 15f));
                    Toast.makeText(this, "Centering on your manually set home location.", Toast.LENGTH_SHORT).show();
                } else {
                    centerOnActualGPSLocation(true);
                    Toast.makeText(this, "Home location not set. Attempting to use current GPS.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadHomeLocationPreference() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentLocationMode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
        if ("manual".equals(currentLocationMode)) {
            double lat = Double.longBitsToDouble(sharedPreferences.getLong(KEY_MANUAL_LATITUDE, Double.doubleToRawLongBits(0.0)));
            double lon = Double.longBitsToDouble(sharedPreferences.getLong(KEY_MANUAL_LONGITUDE, Double.doubleToRawLongBits(0.0)));
            manualHomeLocationName = sharedPreferences.getString(KEY_MANUAL_LOCATION_NAME, "Manually Set Location");
            if (lat != 0.0 && lon != 0.0) {
                manualHomeLocation = new LatLng(lat, lon);
            } else {
                manualHomeLocation = null;
            }
        } else {
            manualHomeLocation = null;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        loadHomeLocationPreference();

        customInfoWindowAdapter = new CustomInfoWindowAdapter(MapsActivity.this, markerPlaceMap);
        mMap.setInfoWindowAdapter(customInfoWindowAdapter);
        mMap.setOnInfoWindowClickListener(this);

        clearRouteElements();

        Intent intent = getIntent();
        boolean drawRouteFlag = intent.getBooleanExtra(EXTRA_DRAW_ROUTE, false);
        LatLng intentDestLatLng = null;
        String intentDestName = null;
        String intentDestDocId = null;

        if (intent.hasExtra(EXTRA_TARGET_LATITUDE) && intent.hasExtra(EXTRA_TARGET_LONGITUDE)) {
            intentDestLatLng = new LatLng(
                    intent.getDoubleExtra(EXTRA_TARGET_LATITUDE, 0),
                    intent.getDoubleExtra(EXTRA_TARGET_LONGITUDE, 0)
            );
            intentDestName = intent.getStringExtra(EXTRA_TARGET_NAME);
            intentDestDocId = intent.getStringExtra(PlaceDetailsActivity.EXTRA_PLACE_DOCUMENT_ID);
        }

        if (drawRouteFlag && intentDestLatLng != null) {
            binding.tvDirectionText.setText("Preparing route to " + (intentDestName != null ? intentDestName : "destination") + "...");
            if ("auto".equals(currentLocationMode)) {
                fetchCurrentLocationForRoute(intentDestLatLng, intentDestName, intentDestDocId);
            } else if (manualHomeLocation != null) {
                initiateRouteDrawing(manualHomeLocation, manualHomeLocationName, intentDestLatLng, intentDestName, intentDestDocId);
            } else {
                Toast.makeText(this, "Your home location isn't set. Cannot draw route.", Toast.LENGTH_LONG).show();
                centerMapOnLocation(intentDestLatLng, intentDestName, 15f);
            }
        } else if (intentDestLatLng != null) {
            centerMapOnLocation(intentDestLatLng, intentDestName, 15f);
        } else {
            if ("auto".equals(currentLocationMode)) {
                centerOnActualGPSLocation(false);
            } else if (manualHomeLocation != null) {
                addManualHomeMarkerToMap();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(manualHomeLocation, 15f));
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText(manualHomeLocationName);
            } else {
                LatLng philippines = new LatLng(12.8797, 121.7740);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(philippines, 6f));
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Explore the map.");
            }
        }
        fetchAndDisplayAllPois();
    }

    private void addManualHomeMarkerToMap() {
        if (mMap == null || manualHomeLocation == null) return;

        if (manualHomeMarker != null && manualHomeMarker.getId() != null) {
            markerPlaceMap.remove(manualHomeMarker.getId());
            manualHomeMarker.remove();
        }

        manualHomeMarker = mMap.addMarker(new MarkerOptions()
                .position(manualHomeLocation)
                .title(manualHomeLocationName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        Place manualPlaceStub = new Place();
        manualPlaceStub.setName(manualHomeLocationName);
        manualPlaceStub.setCategory("Your Set Location");
        if (manualHomeMarker != null) {
            markerPlaceMap.put(manualHomeMarker.getId(), manualPlaceStub);
        }
    }

    private void centerMapOnLocation(LatLng location, String name, float zoom) {
        if (mMap == null || location == null) return;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoom));
        if (binding.tvDirectionText != null) {
            binding.tvDirectionText.setText("Showing: " + (name != null ? name : "Selected Location"));
        }
    }

    private void fetchCurrentLocationForRoute(final LatLng destLatLng, final String destName, final String destDocId) {
        if (mMap == null) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            } catch (SecurityException e) {
                Log.e(TAG, "Security Exception enabling my location layer: " + e.getMessage());
            }

            binding.tvDirectionText.setText("Fetching current location for route...");
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng originLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            initiateRouteDrawing(originLatLng, "Your Current Location", destLatLng, destName, destDocId);
                        } else {
                            Toast.makeText(MapsActivity.this, "Could not get current location. Please ensure GPS is enabled.", Toast.LENGTH_LONG).show();
                            centerMapOnLocation(destLatLng, destName, 15f);
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Log.e(TAG, "Failed to get current location for route.", e);
                        Toast.makeText(MapsActivity.this, "Failed to get current location. Showing destination only.", Toast.LENGTH_LONG).show();
                        centerMapOnLocation(destLatLng, destName, 15f);
                    });
        } else {
            pendingRouteDestLatLng = destLatLng;
            pendingRouteDestName = destName;
            pendingRouteDestDocId = destDocId;
            awaitingPermissionsForRoute = true;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void clearRouteElements() {
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
            currentRoutePolyline = null;
        }
        if (routeOriginMarker != null) {
            if (manualHomeMarker == null || !routeOriginMarker.getId().equals(manualHomeMarker.getId())) {
                if (routeOriginMarker.getId() != null) markerPlaceMap.remove(routeOriginMarker.getId());
                routeOriginMarker.remove();
            }
            routeOriginMarker = null;
        }
        if (routeDestinationMarker != null) {
            if (routeDestinationMarker.getId() != null) markerPlaceMap.remove(routeDestinationMarker.getId());
            routeDestinationMarker.remove();
            routeDestinationMarker = null;
        }
        if (manualHomeMarker != null && manualHomeMarker.getId() != null && manualHomeLocation != null) {
            Place manualPlaceStub = new Place();
            manualPlaceStub.setName(manualHomeLocationName);
            manualPlaceStub.setCategory("Your Set Location");
            markerPlaceMap.put(manualHomeMarker.getId(), manualPlaceStub);
            manualHomeMarker.setTitle(manualHomeLocationName);
        }
    }

    private void initiateRouteDrawing(LatLng originLatLng, String originName, LatLng destLatLng, String destName, String destDocId) {
        if (mMap == null || originLatLng == null || destLatLng == null) {
            Log.e(TAG, "Cannot draw route: Map or LatLngs are null.");
            Toast.makeText(this, "Error preparing route.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (directionsApiKey == null || directionsApiKey.isEmpty()) {
            Toast.makeText(this, "API Key for directions is missing. Cannot draw road route.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Directions API key is null or empty. Drawing straight line fallback.");
            drawStraightLineFallback(originLatLng, originName, destLatLng, destName, destDocId);
            return;
        }

        clearRouteElements();

        // Add origin marker
        if (manualHomeLocation != null && manualHomeLocation.equals(originLatLng)) {
            routeOriginMarker = manualHomeMarker;
            if (routeOriginMarker != null) {
                routeOriginMarker.setTitle(originName);
                Place originPlaceData = markerPlaceMap.get(routeOriginMarker.getId());
                if (originPlaceData != null) {
                    originPlaceData.setName(originName);
                    if (originName.equals(manualHomeLocationName)) originPlaceData.setCategory("Manually Set Home");
                } else if (routeOriginMarker.getId() != null) {
                    Place stub = new Place(); stub.setName(originName);
                    stub.setCategory(originName.equals(manualHomeLocationName) ? "Manually Set Home" : "Route Origin");
                    markerPlaceMap.put(routeOriginMarker.getId(), stub);
                }
            } else {
                addManualHomeMarkerToMap();
                routeOriginMarker = manualHomeMarker;
                if (routeOriginMarker != null) routeOriginMarker.setTitle(originName);
                else {
                    routeOriginMarker = mMap.addMarker(new MarkerOptions()
                            .position(originLatLng).title(originName)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    if (routeOriginMarker != null && routeOriginMarker.getId() != null) {
                        Place stub = new Place(); stub.setName(originName);
                        markerPlaceMap.put(routeOriginMarker.getId(), stub);
                    }
                }
            }
        } else {
            routeOriginMarker = mMap.addMarker(new MarkerOptions()
                    .position(originLatLng).title(originName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            if (routeOriginMarker != null && routeOriginMarker.getId() != null) {
                Place originStub = new Place(); originStub.setName(originName);
                if (originName.equals("Your Current Location")) originStub.setCategory("Current GPS Location");
                markerPlaceMap.put(routeOriginMarker.getId(), originStub);
            }
        }

        // Add destination marker
        Place fullDestinationPlace = null;
        if (destDocId != null && !destDocId.isEmpty()) {
            for (Place p : markerPlaceMap.values()) {
                if (p.getDocumentId() != null && destDocId.equals(p.getDocumentId())) {
                    fullDestinationPlace = p;
                    break;
                }
            }
        }
        routeDestinationMarker = mMap.addMarker(new MarkerOptions()
                .position(destLatLng).title(destName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        if (routeDestinationMarker != null && routeDestinationMarker.getId() != null) {
            if (fullDestinationPlace != null) {
                markerPlaceMap.put(routeDestinationMarker.getId(), fullDestinationPlace);
            } else {
                Place destStub = new Place(); destStub.setName(destName);
                destStub.setCategory("Route Destination");
                markerPlaceMap.put(routeDestinationMarker.getId(), destStub);
            }
        }

        if (binding.tvDirectionText != null) {
            binding.tvDirectionText.setText("Calculating route from " + originName + " to " + destName + "...");
        }
        new FetchDirectionsTask(this, directionsApiKey).execute(originLatLng, destLatLng);
    }

    private void drawStraightLineFallback(LatLng originLatLng, String originName, LatLng destLatLng, String destName, String destDocId) {
        Log.w(TAG, "Drawing straight line as fallback for route from " + originName + " to " + destName);
        if (mMap == null || originLatLng == null || destLatLng == null) return;

        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .add(originLatLng, destLatLng)
                .color(Color.parseColor("#FFA500")) // Orange for fallback
                .width(10)
                .pattern(Arrays.asList(new Dash(20), new Gap(10)));
        currentRoutePolyline = mMap.addPolyline(polylineOptions);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(originLatLng);
        builder.include(destLatLng);
        LatLngBounds bounds = builder.build();
        int padding = (int) (getResources().getDisplayMetrics().widthPixels * 0.20);

        try {
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cu);
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException for newLatLngBounds (Fallback). Map not ready.", e);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 12f));
        }
        if (binding.tvDirectionText != null) {
            binding.tvDirectionText.setText("Showing straight line to " + destName + " (Directions API unavailable)");
        }
    }

    public void drawActualRoute(DirectionsResult directionsResult) {
        if (mMap == null || directionsResult == null || directionsResult.polylinePoints == null || directionsResult.polylinePoints.isEmpty()) {
            Toast.makeText(this, "Could not draw route.", Toast.LENGTH_SHORT).show();
            if (binding.tvDirectionText != null) {
                binding.tvDirectionText.setText("Failed to calculate route path.");
            }
            if (routeOriginMarker != null && routeDestinationMarker != null) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(routeOriginMarker.getPosition());
                builder.include(routeDestinationMarker.getPosition());
                int padding = (int) (getResources().getDisplayMetrics().widthPixels * 0.20);
                try {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
                } catch (Exception e) {
                    Log.e(TAG, "Error animating camera to fallback bounds: " + e.getMessage());
                }
            }
            return;
        }

        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(directionsResult.polylinePoints)
                .color(Color.parseColor("#3F51B5")) // AlayaApp Blue/Primary
                .width(15);
        currentRoutePolyline = mMap.addPolyline(polylineOptions);

        if (directionsResult.routeBounds != null) {
            int padding = (int) (getResources().getDisplayMetrics().widthPixels * 0.15);
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(directionsResult.routeBounds, padding));
            } catch (IllegalStateException e) {
                Log.e(TAG, "Cannot animate camera to route bounds yet: " + e.getMessage());
                if (routeDestinationMarker != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(routeDestinationMarker.getPosition(), 12f));
                }
            }
        } else if (routeOriginMarker != null && routeDestinationMarker != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(routeOriginMarker.getPosition());
            builder.include(routeDestinationMarker.getPosition());
            int padding = (int) (getResources().getDisplayMetrics().widthPixels * 0.20);
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
            } catch (Exception e) {
                Log.e(TAG, "Error animating camera to origin/dest bounds: " + e.getMessage());
            }
        }
        if (binding.tvDirectionText != null && routeOriginMarker != null && routeDestinationMarker != null) {
            binding.tvDirectionText.setText("Route: " + routeOriginMarker.getTitle() + " to " + routeDestinationMarker.getTitle());
        } else if (binding.tvDirectionText != null) {
            binding.tvDirectionText.setText("Route calculated.");
        }
    }

    private void fetchAndDisplayAllPois() {
        db.collection("places")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (mMap == null) return;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Place place = document.toObject(Place.class);
                            if (place != null && place.getLatitude() != null && place.getLongitude() != null) {
                                place.setDocumentId(document.getId());
                                LatLng placeLocation = new LatLng(place.getLatitude(), place.getLongitude());

                                boolean isManualHomePoi = manualHomeMarker != null && manualHomeMarker.getPosition().equals(placeLocation);
                                boolean isRouteOriginPoi = routeOriginMarker != null && routeOriginMarker.getPosition().equals(placeLocation);
                                boolean isRouteDestPoi = routeDestinationMarker != null && routeDestinationMarker.getPosition().equals(placeLocation);

                                if (isManualHomePoi) {
                                    markerPlaceMap.put(manualHomeMarker.getId(), place);
                                    manualHomeMarker.setTitle(place.getName());
                                } else if (isRouteOriginPoi && (manualHomeMarker == null || !routeOriginMarker.getId().equals(manualHomeMarker.getId()))) {
                                    markerPlaceMap.put(routeOriginMarker.getId(), place);
                                    routeOriginMarker.setTitle(place.getName());
                                } else if (isRouteDestPoi) {
                                    markerPlaceMap.put(routeDestinationMarker.getId(), place);
                                    routeDestinationMarker.setTitle(place.getName());
                                } else {
                                    boolean markerExistsForThisPoi = false;
                                    for (Map.Entry<String, Place> entry : markerPlaceMap.entrySet()) {
                                        if (entry.getValue() != null && place.getDocumentId().equals(entry.getValue().getDocumentId())) {
                                            markerExistsForThisPoi = true;
                                            break;
                                        }
                                    }
                                    if (!markerExistsForThisPoi) {
                                        MarkerOptions markerOptions = new MarkerOptions()
                                                .position(placeLocation)
                                                .title(place.getName());
                                        Marker marker = mMap.addMarker(markerOptions);
                                        if (marker != null && marker.getId() != null) {
                                            markerPlaceMap.put(marker.getId(), place);
                                        }
                                    }
                                }
                            }
                        }
                        if (binding.tvDirectionText != null) {
                            String currentText = binding.tvDirectionText.getText().toString();
                            if (currentText.contains("Determining view...") || currentText.equals("Explore the map.") ||
                                    (!currentText.toLowerCase().contains("route") && !currentText.toLowerCase().contains("showing:"))) {
                                binding.tvDirectionText.setText("Places loaded. Tap markers for details.");
                            }
                        }
                    } else {
                        Log.w(TAG, "Error getting POI documents.", task.getException());
                        if (binding.tvDirectionText != null && !binding.tvDirectionText.getText().toString().toLowerCase().contains("route")) {
                            binding.tvDirectionText.setText("Could not load points of interest.");
                        }
                        Toast.makeText(MapsActivity.this, "Failed to load points of interest.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {
        Place place = markerPlaceMap.get(marker.getId());
        if (place != null && place.getDocumentId() != null && !place.getDocumentId().isEmpty()) {
            Intent intent = new Intent(MapsActivity.this, PlaceDetailsActivity.class);
            intent.putExtra(PlaceDetailsActivity.EXTRA_PLACE_DOCUMENT_ID, place.getDocumentId());
            startActivity(intent);
        } else if (place != null) {
            Toast.makeText(this, "Details for: " + place.getName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No further details available.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Info window clicked, but no Place data for marker: " + marker.getTitle() + " ID: " + marker.getId());
        }
    }

    private void centerOnActualGPSLocation(boolean animate) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                try {
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException on setMyLocationEnabled: " + e.getMessage());
                }

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                if (animate) {
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f));
                                } else {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f));
                                }
                                if (binding.tvDirectionText != null && "auto".equals(currentLocationMode)) {
                                    boolean drawingRoute = getIntent().getBooleanExtra(EXTRA_DRAW_ROUTE, false);
                                    boolean hasTarget = getIntent().hasExtra(EXTRA_TARGET_LATITUDE);
                                    if (!drawingRoute && !hasTarget) {
                                        binding.tvDirectionText.setText("Centered on your current GPS location.");
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Current GPS location not available.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(this, e -> {
                            Toast.makeText(this, "Failed to get current GPS location.", Toast.LENGTH_SHORT).show();
                        });
            }
        } else {
            awaitingPermissionsForRoute = false;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    if (mMap != null) mMap.setMyLocationEnabled(true);
                } catch (SecurityException se) {Log.e(TAG, "Security Exception after permission grant");}

                if (awaitingPermissionsForRoute && pendingRouteDestLatLng != null) {
                    awaitingPermissionsForRoute = false;
                    fetchCurrentLocationForRoute(pendingRouteDestLatLng, pendingRouteDestName, pendingRouteDestDocId);
                    pendingRouteDestLatLng = null;
                    pendingRouteDestName = null;
                    pendingRouteDestDocId = null;
                } else if ("auto".equals(currentLocationMode)){
                    centerOnActualGPSLocation(true);
                }
            } else {
                awaitingPermissionsForRoute = false;
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
                try {
                    if (mMap != null) mMap.setMyLocationEnabled(false);
                } catch (SecurityException se) {Log.e(TAG, "SecurityException on setMyLocationEnabled(false)");}

                if (pendingRouteDestLatLng != null) {
                    centerMapOnLocation(pendingRouteDestLatLng, pendingRouteDestName, 15f);
                    pendingRouteDestLatLng = null;
                    pendingRouteDestName = null;
                    pendingRouteDestDocId = null;
                }
            }
        }
    }

    private void setupBottomNavigation() {
        if (binding.bottomNavigationMapsPage == null) {
            Log.e(TAG, "BottomNavigationView not found!");
            return;
        }
        binding.bottomNavigationMapsPage.setSelectedItemId(CURRENT_ITEM_ID);
        binding.bottomNavigationMapsPage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();
            if (destinationItemId == CURRENT_ITEM_ID) return true;

            Class<?> destinationActivityClass = null;
            if (destinationItemId == R.id.navigation_home) destinationActivityClass = HomeActivity.class;
            else if (destinationItemId == R.id.navigation_itineraries) destinationActivityClass = ItinerariesActivity.class;
            else if (destinationItemId == R.id.navigation_profile) destinationActivityClass = ProfileActivity.class;

            if (destinationActivityClass != null) {
                Intent intent = new Intent(MapsActivity.this, destinationActivityClass);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);

                boolean slideRightToLeft = getItemIndex(destinationItemId) > getItemIndex(CURRENT_ITEM_ID);
                if (slideRightToLeft) {
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
                finish();
                return true;
            }
            return false;
        });
    }

    private int getItemIndex(int itemId) {
        if (itemId == R.id.navigation_home) return 0;
        if (itemId == R.id.navigation_itineraries) return 1;
        if (itemId == R.id.navigation_map) return 2;
        if (itemId == R.id.navigation_profile) return 3;
        return -1;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (mMap != null && mMap.getUiSettings() != null) {
            onMapReady(mMap);
        }
    }

    private static class FetchDirectionsTask extends AsyncTask<LatLng, Void, DirectionsResult> {
        private WeakReference<MapsActivity> activityReference;
        private String apiKey;

        FetchDirectionsTask(MapsActivity context, String apiKey) {
            this.activityReference = new WeakReference<>(context);
            this.apiKey = apiKey;
        }

        @Override
        protected DirectionsResult doInBackground(LatLng... params) {
            MapsActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return null;

            LatLng origin = params[0];
            LatLng dest = params[1];

            String urlString = getDirectionsUrl(origin, dest, apiKey);
            String jsonData = "";
            try {
                jsonData = downloadUrl(urlString);
            } catch (IOException e) {
                Log.e("FetchDirectionsTask", "Error downloading URL: " + e.getMessage());
                return null;
            }

            if (jsonData.isEmpty()) {
                Log.e("FetchDirectionsTask", "Downloaded JSON data is empty.");
                return null;
            }
            return parseDirections(jsonData);
        }

        @Override
        protected void onPostExecute(DirectionsResult result) {
            MapsActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            if (result != null && result.polylinePoints != null && !result.polylinePoints.isEmpty()) {
                activity.drawActualRoute(result);
                if (activity.binding.tvDirectionText != null && activity.routeOriginMarker != null && activity.routeDestinationMarker != null) {
                    activity.binding.tvDirectionText.setText("Route: " + activity.routeOriginMarker.getTitle() + " to " + activity.routeDestinationMarker.getTitle());
                }
            } else {
                Toast.makeText(activity, "Could not calculate directions.", Toast.LENGTH_LONG).show();
                if (activity.binding.tvDirectionText != null) {
                    activity.binding.tvDirectionText.setText("Failed to get directions. Showing straight line.");
                }
                if (activity.routeOriginMarker != null && activity.routeDestinationMarker != null) {
                    activity.drawStraightLineFallback(
                            activity.routeOriginMarker.getPosition(), activity.routeOriginMarker.getTitle(),
                            activity.routeDestinationMarker.getPosition(), activity.routeDestinationMarker.getTitle(),
                            null
                    );
                }
            }
        }

        private String getDirectionsUrl(LatLng origin, LatLng dest, String key) {
            String strOrigin = "origin=" + origin.latitude + "," + origin.longitude;
            String strDest = "destination=" + dest.latitude + "," + dest.longitude;
            String mode = "mode=driving";
            String apiKeyParam = "key=" + key;
            String parameters = strOrigin + "&" + strDest + "&" + mode + "&" + apiKeyParam;
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
                Log.e("FetchDirectionsTask", "Exception downloading URL: " + e.toString());
                throw new IOException("Error downloading URL", e);
            } finally {
                if (iStream != null) {
                    try { iStream.close(); } catch (IOException e) { /* ignore */ }
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
            try {
                JSONObject jsonObject = new JSONObject(jsonData);
                String status = jsonObject.optString("status");
                if (!"OK".equals(status)) {
                    Log.e("FetchDirectionsTask", "Directions API non-OK status: " + status + " - " + jsonObject.optString("error_message"));
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
                }
            } catch (Exception e) {
                Log.e("FetchDirectionsTask", "Error parsing directions JSON: " + e.getMessage());
                return null;
            }
            if (polylinePoints.isEmpty()) return null;
            return new DirectionsResult(polylinePoints, routeBounds);
        }
    }
}
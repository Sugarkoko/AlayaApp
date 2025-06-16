package com.example.alayaapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider; // New import

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
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private String apiTravelMode = "walking";
    private String selectedUIMode = "walk";
    private LatLng currentRouteOriginLatLng;
    private String currentRouteOriginName;
    private LinearLayout transportModeContainer;
    private LinearLayout btnModeWalkCard, btnModeTaxiCard, btnModeTwoWheelsCard;
    private ImageView ivModeWalk, ivModeTaxi, ivModeTwoWheels;
    private TextView tvModeWalk, tvModeTaxi, tvModeTwoWheels;
    private ImageButton btnShowItineraryRoute;
    private ItineraryGenerator itineraryGenerator;
    private boolean isShowingSegmentedRoute = false;
    private List<ItineraryItem> fullItinerary = new ArrayList<>();
    private List<Place> allPlacesList = new ArrayList<>();
    private int currentItinerarySegmentIndex = -1;
    private LatLng userStartLocationForItinerary;
    private String userStartLocationNameForItinerary;
    private Calendar tripStartCalendar;
    private Calendar tripEndCalendar;
    private CardView routeInfoPane;
    private TextView tvRouteEta, tvRouteDistance, tvRouteItineraryNumber, tvRouteDestinationName;
    private Button btnRouteNext, btnRoutePrevious;

    // --- NEW: ViewModel instance ---
    private MapsViewModel mapsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // --- NEW: Initialize the ViewModel ---
        mapsViewModel = new ViewModelProvider(this).get(MapsViewModel.class);

        try {
            binding = ActivityMapsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading page.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainMapsCoordinatorLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
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
        itineraryGenerator = new ItineraryGenerator();
        tripStartCalendar = Calendar.getInstance();
        tripEndCalendar = Calendar.getInstance();
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
        transportModeContainer = binding.transportModeSelectorContainer;
        btnModeWalkCard = binding.btnModeWalkCard;
        ivModeWalk = binding.ivModeWalk;
        tvModeWalk = binding.tvModeWalk;
        btnModeTaxiCard = binding.btnModeTaxiCard;
        ivModeTaxi = binding.ivModeTaxi;
        tvModeTaxi = binding.tvModeTaxi;
        btnModeTwoWheelsCard = binding.btnModeTwoWheelsCard;
        ivModeTwoWheels = binding.ivModeTwoWheels;
        tvModeTwoWheels = binding.tvModeTwoWheels;
        routeInfoPane = binding.routeInfoPane;
        tvRouteEta = binding.tvRouteEta;
        tvRouteDistance = binding.tvRouteDistance;
        tvRouteItineraryNumber = binding.tvRouteItineraryNumber;
        tvRouteDestinationName = binding.tvRouteDestinationName;
        btnRouteNext = binding.btnRouteNext;
        btnRoutePrevious = binding.btnRoutePrevious;
        btnRouteNext.setOnClickListener(v -> displayNextItinerarySegment());
        btnRoutePrevious.setOnClickListener(v -> displayPreviousItinerarySegment());
        btnShowItineraryRoute = binding.btnShowItineraryRoute;
        btnShowItineraryRoute.setOnClickListener(v -> {
            if (isShowingSegmentedRoute) {
                hideSegmentedRouteView();
            } else {
                startSegmentedRouteView();
            }
        });
        setupTransportModeButtons();
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

        // --- NEW: Observe the LiveData for directions results ---
        mapsViewModel.getDirectionsResult().observe(this, result -> {
            if (result != null && result.polylinePoints != null && !result.polylinePoints.isEmpty()) {
                drawActualRoute(result);
            } else {
                String failedMode = selectedUIMode.equals("two_wheels") ? "2-Wheels" : selectedUIMode.substring(0, 1).toUpperCase() + selectedUIMode.substring(1);
                Toast.makeText(this, "Could not calculate directions for " + failedMode + ".", Toast.LENGTH_LONG).show();
                if (binding.tvDirectionText != null) {
                    binding.tvDirectionText.setText("Failed to get directions for " + failedMode + ". Showing straight line.");
                }
                if (routeOriginMarker != null && routeDestinationMarker != null) {
                    drawStraightLineFallback(
                            routeOriginMarker.getPosition(), routeOriginMarker.getTitle(),
                            routeDestinationMarker.getPosition(), routeDestinationMarker.getTitle(),
                            pendingRouteDestDocId
                    );
                }
            }
        });
    }

    private void initiateRouteDrawing(LatLng originLatLng, String originName, LatLng destLatLng, String destName, String destDocId) {
        if (mMap == null || originLatLng == null || destLatLng == null) {
            Log.e(TAG, "Cannot draw route: Map or LatLngs are null.");
            Toast.makeText(this, "Error preparing route.", Toast.LENGTH_SHORT).show();
            transportModeContainer.setVisibility(View.GONE);
            return;
        }
        if (directionsApiKey == null || directionsApiKey.isEmpty()) {
            Toast.makeText(this, "API Key for directions is missing. Cannot draw road route.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Directions API key is null or empty. Drawing straight line fallback.");
            drawStraightLineFallback(originLatLng, originName, destLatLng, destName, destDocId);
            transportModeContainer.setVisibility(View.VISIBLE);
            return;
        }
        clearRouteElements(false);
        this.currentRouteOriginLatLng = originLatLng;
        this.currentRouteOriginName = originName;
        transportModeContainer.setVisibility(View.VISIBLE);
        updateTransportModeUI();
        routeOriginMarker = mMap.addMarker(new MarkerOptions()
                .position(originLatLng).title(originName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        routeDestinationMarker = mMap.addMarker(new MarkerOptions()
                .position(destLatLng).title(destName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        String modeDisplayName = selectedUIMode.equals("two_wheels") ? "2-Wheels" : selectedUIMode.substring(0, 1).toUpperCase() + selectedUIMode.substring(1);
        if (binding.tvDirectionText != null) {
            binding.tvDirectionText.setText("Calculating " + modeDisplayName + " route from " + originName + " to " + destName + "...");
        }
        // --- REFACTORED: Call ViewModel instead of AsyncTask ---
        mapsViewModel.fetchDirections(originLatLng, destLatLng, directionsApiKey, this.apiTravelMode);
    }

    public void drawActualRoute(MapsViewModel.DirectionsResult directionsResult) {
        if (mMap == null || directionsResult == null || directionsResult.polylinePoints == null || directionsResult.polylinePoints.isEmpty()) {
            String failedModeDisplay = selectedUIMode.equals("two_wheels") ? "2-Wheels" : selectedUIMode.substring(0, 1).toUpperCase() + selectedUIMode.substring(1);
            Toast.makeText(this, "Could not draw " + failedModeDisplay + " route.", Toast.LENGTH_SHORT).show();
            if (binding.tvDirectionText != null) {
                binding.tvDirectionText.setText("Failed to calculate " + failedModeDisplay + " route path.");
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
                .color(Color.parseColor("#3F51B5"))
                .width(15);
        currentRoutePolyline = mMap.addPolyline(polylineOptions);
        if (isShowingSegmentedRoute) {
            tvRouteEta.setText(directionsResult.durationText);
            tvRouteDistance.setText("(" + directionsResult.distanceText + ")");
        }
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
        String modeDisplayName = selectedUIMode.equals("two_wheels") ? "2-Wheels" : selectedUIMode.substring(0, 1).toUpperCase() + selectedUIMode.substring(1);
        if (binding.tvDirectionText != null && routeOriginMarker != null && routeDestinationMarker != null) {
            binding.tvDirectionText.setText(modeDisplayName + " Route: " + routeOriginMarker.getTitle() + " to " + routeDestinationMarker.getTitle());
        } else if (binding.tvDirectionText != null) {
            binding.tvDirectionText.setText(modeDisplayName + " route calculated.");
        }
    }

    private void displayItinerarySegment(int index) {
        if (!isShowingSegmentedRoute || fullItinerary.isEmpty() || index < 0 || index >= fullItinerary.size()) {
            Log.e(TAG, "Cannot display segment, invalid state or index: " + index);
            hideSegmentedRouteView();
            return;
        }
        clearRouteElements(false);
        LatLng origin, destination;
        String originName, destinationName;
        ItineraryItem currentStop = fullItinerary.get(index);
        destination = new LatLng(currentStop.getCoordinates().getLatitude(), currentStop.getCoordinates().getLongitude());
        destinationName = currentStop.getActivity();
        if (index == 0) {
            origin = userStartLocationForItinerary;
            originName = userStartLocationNameForItinerary;
        } else {
            ItineraryItem previousStop = fullItinerary.get(index - 1);
            origin = new LatLng(previousStop.getCoordinates().getLatitude(), previousStop.getCoordinates().getLongitude());
            originName = previousStop.getActivity();
        }
        tvRouteDestinationName.setText(destinationName);
        tvRouteItineraryNumber.setText(getOrdinal(index + 1) + " location");
        tvRouteEta.setText("...");
        tvRouteDistance.setText("...");
        routeOriginMarker = mMap.addMarker(new MarkerOptions().position(origin).title(originName).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        routeDestinationMarker = mMap.addMarker(new MarkerOptions().position(destination).title(destinationName).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // --- REFACTORED: Call ViewModel instead of AsyncTask ---
        mapsViewModel.fetchDirections(origin, destination, directionsApiKey, apiTravelMode);

        btnRouteNext.setEnabled(index < fullItinerary.size() - 1);
        btnRoutePrevious.setEnabled(index > 0);
    }

    // --- The FetchDirectionsTask inner class has been completely removed. ---
    // --- All other methods remain the same. ---

    // ... (Keep all other methods like onMapReady, onInfoWindowClick, setupBottomNavigation, etc. exactly as they were)
    // The rest of your MapsActivity.java file remains unchanged.
    // Just ensure the FetchDirectionsTask class is deleted.
    // ...
    private void setupTransportModeButtons() {
        btnModeWalkCard.setOnClickListener(v -> selectTravelMode("walk"));
        btnModeTaxiCard.setOnClickListener(v -> selectTravelMode("taxi"));
        btnModeTwoWheelsCard.setOnClickListener(v -> selectTravelMode("two_wheels"));
        updateTransportModeUI();
    }

    private void selectTravelMode(String uiMode) {
        String newApiMode;
        switch (uiMode) {
            case "taxi":
            case "two_wheels":
                newApiMode = "driving";
                break;
            case "walk":
            default:
                newApiMode = "walking";
                break;
        }
        if (this.selectedUIMode.equals(uiMode) && (currentRoutePolyline != null || isShowingSegmentedRoute)) {
            Log.d(TAG, "UI Mode " + uiMode + " already selected and route shown.");
            return;
        }
        this.selectedUIMode = uiMode;
        this.apiTravelMode = newApiMode;
        updateTransportModeUI();
        if (isShowingSegmentedRoute) {
            displayItinerarySegment(currentItinerarySegmentIndex);
        } else if (currentRouteOriginLatLng != null && pendingRouteDestLatLng != null) {
            Log.d(TAG, "Redrawing route for UI mode: " + this.selectedUIMode + ", API mode: " + this.apiTravelMode);
            if (currentRoutePolyline != null) {
                currentRoutePolyline.remove();
                currentRoutePolyline = null;
            }
            initiateRouteDrawing(currentRouteOriginLatLng, currentRouteOriginName, pendingRouteDestLatLng, pendingRouteDestName, pendingRouteDestDocId);
        } else {
            Log.d(TAG, "No active route to redraw for mode: " + this.selectedUIMode);
        }
    }
    private void startSegmentedRouteView() {
        binding.tvDirectionText.setText("Generating itinerary...");
        fullItinerary.clear();
        if ("auto".equals(currentLocationMode)) {
            fetchCurrentLocationForItinerary();
        } else if (manualHomeLocation != null) {
            userStartLocationForItinerary = manualHomeLocation;
            userStartLocationNameForItinerary = manualHomeLocationName;
            generateItineraryAndStart();
        } else {
            Toast.makeText(this, "Your start location is not set. Please set it on the Home screen or enable GPS.", Toast.LENGTH_LONG).show();
            binding.tvDirectionText.setText("Could not determine start location.");
        }
    }

    private void fetchCurrentLocationForItinerary() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission needed to plan route.", Toast.LENGTH_LONG).show();
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        userStartLocationForItinerary = new LatLng(location.getLatitude(), location.getLongitude());
                        userStartLocationNameForItinerary = "Your Current Location";
                        generateItineraryAndStart();
                    } else {
                        Toast.makeText(MapsActivity.this, "Could not get current GPS location.", Toast.LENGTH_LONG).show();
                        binding.tvDirectionText.setText("Failed to get GPS location.");
                    }
                });
    }

    private void loadTripDateTime() {
        final String KEY_TRIP_DATE_YEAR = "trip_date_year";
        final String KEY_TRIP_DATE_MONTH = "trip_date_month";
        final String KEY_TRIP_DATE_DAY = "trip_date_day";
        final String KEY_TRIP_TIME_HOUR = "trip_time_hour";
        final String KEY_TRIP_TIME_MINUTE = "trip_time_minute";
        final String KEY_TRIP_END_TIME_HOUR = "trip_end_time_hour";
        final String KEY_TRIP_END_TIME_MINUTE = "trip_end_time_minute";
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (sharedPreferences.contains(KEY_TRIP_DATE_YEAR)) {
            int year = sharedPreferences.getInt(KEY_TRIP_DATE_YEAR, tripStartCalendar.get(Calendar.YEAR));
            int month = sharedPreferences.getInt(KEY_TRIP_DATE_MONTH, tripStartCalendar.get(Calendar.MONTH));
            int day = sharedPreferences.getInt(KEY_TRIP_DATE_DAY, tripStartCalendar.get(Calendar.DAY_OF_MONTH));
            tripStartCalendar.set(year, month, day);
            tripEndCalendar.set(year, month, day);
        }
        int startHour = sharedPreferences.getInt(KEY_TRIP_TIME_HOUR, 9);
        int startMinute = sharedPreferences.getInt(KEY_TRIP_TIME_MINUTE, 0);
        tripStartCalendar.set(Calendar.HOUR_OF_DAY, startHour);
        tripStartCalendar.set(Calendar.MINUTE, startMinute);
        int endHour = sharedPreferences.getInt(KEY_TRIP_END_TIME_HOUR, 18);
        int endMinute = sharedPreferences.getInt(KEY_TRIP_END_TIME_MINUTE, 0);
        tripEndCalendar.set(Calendar.HOUR_OF_DAY, endHour);
        tripEndCalendar.set(Calendar.MINUTE, endMinute);
    }

    private void generateItineraryAndStart() {
        db.collection("places").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                allPlacesList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Place place = document.toObject(Place.class);
                    if (place != null && place.getCoordinates() != null && place.getOpeningHours() != null) {
                        place.setDocumentId(document.getId());
                        allPlacesList.add(place);
                    }
                }
                loadTripDateTime(); // Load user's selected times
                GeoPoint startGeoPoint = new GeoPoint(userStartLocationForItinerary.latitude, userStartLocationForItinerary.longitude);
                List<ItineraryItem> generatedItems = itineraryGenerator.generate(startGeoPoint, allPlacesList, tripStartCalendar, tripEndCalendar);
                if (generatedItems.size() < 2) {
                    showInteractiveFallbackDialog(startGeoPoint);
                } else {
                    fullItinerary = generatedItems;
                    isShowingSegmentedRoute = true;
                    currentItinerarySegmentIndex = 0;
                    displayItinerarySegment(currentItinerarySegmentIndex);
                    transportModeContainer.setVisibility(View.VISIBLE);
                    routeInfoPane.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Itinerary route ready!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MapsActivity.this, "Failed to load places data.", Toast.LENGTH_SHORT).show();
                binding.tvDirectionText.setText("Error loading places.");
            }
        });
    }

    private void showInteractiveFallbackDialog(GeoPoint startLocation) {
        new AlertDialog.Builder(this)
                .setTitle("Route Plan Limited")
                .setMessage("We couldn't find many open attractions for your selected time to build a route. Would you like us to generate a suggested route based on the optimal hours for that day?")
                .setPositiveButton("Yes, Suggest Route", (dialog, which) -> {
                    generateFallbackItinerary(startLocation);
                })
                .setNegativeButton("No, Thanks", (dialog, which) -> {
                    binding.tvDirectionText.setText("No route found for your selected time.");
                    dialog.dismiss();
                })
                .show();
    }

    private void generateFallbackItinerary(GeoPoint startLocation) {
        Toast.makeText(this, "Finding best times and creating a new route...", Toast.LENGTH_SHORT).show();
        String dayOfWeek = tripStartCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US).toLowerCase();
        int earliestOpen = 24 * 60;
        int latestClose = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        for (Place place : allPlacesList) {
            if (place.getOpeningHours() != null && place.getOpeningHours().containsKey(dayOfWeek)) {
                Map<String, String> hours = place.getOpeningHours().get(dayOfWeek);
                try {
                    if (hours.get("open") != null) {
                        Calendar openCal = Calendar.getInstance();
                        openCal.setTime(sdf.parse(hours.get("open")));
                        int openMinutes = openCal.get(Calendar.HOUR_OF_DAY) * 60 + openCal.get(Calendar.MINUTE);
                        if (openMinutes < earliestOpen) earliestOpen = openMinutes;
                    }
                    if (hours.get("close") != null) {
                        Calendar closeCal = Calendar.getInstance();
                        closeCal.setTime(sdf.parse(hours.get("close")));
                        int closeMinutes = closeCal.get(Calendar.HOUR_OF_DAY) * 60 + closeCal.get(Calendar.MINUTE);
                        if (closeMinutes < earliestOpen) closeMinutes += 24 * 60;
                        if (closeMinutes > latestClose) latestClose = closeMinutes;
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Could not parse hours for fallback route: " + place.getName(), e);
                }
            }
        }
        Calendar fallbackStart = (Calendar) tripStartCalendar.clone();
        fallbackStart.set(Calendar.HOUR_OF_DAY, earliestOpen / 60);
        fallbackStart.set(Calendar.MINUTE, earliestOpen % 60);
        Calendar fallbackEnd = (Calendar) tripStartCalendar.clone();
        if (latestClose >= 24 * 60) {
            fallbackEnd.add(Calendar.DAY_OF_YEAR, 1);
            latestClose -= 24*60;
        }
        fallbackEnd.set(Calendar.HOUR_OF_DAY, latestClose / 60);
        fallbackEnd.set(Calendar.MINUTE, latestClose % 60);
        List<ItineraryItem> fallbackItems = itineraryGenerator.generate(startLocation, allPlacesList, fallbackStart, fallbackEnd);
        if (fallbackItems.isEmpty()) {
            Toast.makeText(this, "No alternative route could be generated.", Toast.LENGTH_LONG).show();
            binding.tvDirectionText.setText("No attractions found for this day.");
            return;
        }
        fullItinerary = fallbackItems;
        isShowingSegmentedRoute = true;
        currentItinerarySegmentIndex = 0;
        displayItinerarySegment(currentItinerarySegmentIndex);
        transportModeContainer.setVisibility(View.VISIBLE);
        routeInfoPane.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Suggested route is ready!", Toast.LENGTH_SHORT).show();
    }

    private void displayNextItinerarySegment() {
        if (currentItinerarySegmentIndex < fullItinerary.size() - 1) {
            currentItinerarySegmentIndex++;
            displayItinerarySegment(currentItinerarySegmentIndex);
        }
    }

    private void displayPreviousItinerarySegment() {
        if (currentItinerarySegmentIndex > 0) {
            currentItinerarySegmentIndex--;
            displayItinerarySegment(currentItinerarySegmentIndex);
        }
    }

    private void hideSegmentedRouteView() {
        isShowingSegmentedRoute = false;
        currentItinerarySegmentIndex = -1;
        fullItinerary.clear();
        routeInfoPane.setVisibility(View.GONE);
        transportModeContainer.setVisibility(View.GONE);
        clearRouteElements(true); // Clear everything and restore POIs
    }

    private String getOrdinal(int number) {
        if (number >= 11 && number <= 13) {
            return number + "th";
        }
        switch (number % 10) {
            case 1: return number + "st";
            case 2: return number + "nd";
            case 3: return number + "rd";
            default: return number + "th";
        }
    }
    private void updateTransportModeUI() {
        ImageView[] icons = {ivModeWalk, ivModeTaxi, ivModeTwoWheels};
        TextView[] texts = {tvModeWalk, tvModeTaxi, tvModeTwoWheels};
        String[] modes = {"walk", "taxi", "two_wheels"};
        for (int i = 0; i < modes.length; i++) {
            boolean isSelected = modes[i].equals(selectedUIMode);
            icons[i].setColorFilter(ContextCompat.getColor(this, isSelected ? R.color.colorPrimary : R.color.textSecondary), android.graphics.PorterDuff.Mode.SRC_IN);
            texts[i].setTextColor(ContextCompat.getColor(this, isSelected ? R.color.colorPrimary : R.color.textSecondary));
        }
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
        clearRouteElements(true);
        Intent intent = getIntent();
        boolean drawRouteFlag = intent.getBooleanExtra(EXTRA_DRAW_ROUTE, false);
        if (intent.hasExtra(EXTRA_TARGET_LATITUDE) && intent.hasExtra(EXTRA_TARGET_LONGITUDE)) {
            pendingRouteDestLatLng = new LatLng(
                    intent.getDoubleExtra(EXTRA_TARGET_LATITUDE, 0),
                    intent.getDoubleExtra(EXTRA_TARGET_LONGITUDE, 0)
            );
            pendingRouteDestName = intent.getStringExtra(EXTRA_TARGET_NAME);
            pendingRouteDestDocId = intent.getStringExtra(PlaceDetailsActivity.EXTRA_PLACE_DOCUMENT_ID);
        }
        if (drawRouteFlag && pendingRouteDestLatLng != null) {
            transportModeContainer.setVisibility(View.VISIBLE);
            selectTravelMode("walk");
            binding.tvDirectionText.setText("Preparing route to " + (pendingRouteDestName != null ? pendingRouteDestName : "destination") + "...");
            if ("auto".equals(currentLocationMode)) {
                fetchCurrentLocationForRoute(pendingRouteDestLatLng, pendingRouteDestName, pendingRouteDestDocId);
            } else if (manualHomeLocation != null) {
                currentRouteOriginLatLng = manualHomeLocation;
                currentRouteOriginName = manualHomeLocationName;
                initiateRouteDrawing(manualHomeLocation, manualHomeLocationName, pendingRouteDestLatLng, pendingRouteDestName, pendingRouteDestDocId);
            } else {
                Toast.makeText(this, "Your home location isn't set. Cannot draw route.", Toast.LENGTH_LONG).show();
                centerMapOnLocation(pendingRouteDestLatLng, pendingRouteDestName, 15f);
                transportModeContainer.setVisibility(View.GONE);
            }
        } else if (pendingRouteDestLatLng != null) {
            transportModeContainer.setVisibility(View.GONE);
            centerMapOnLocation(pendingRouteDestLatLng, pendingRouteDestName, 15f);
        } else {
            transportModeContainer.setVisibility(View.GONE);
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
                            currentRouteOriginLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            currentRouteOriginName = "Your Current Location";
                            initiateRouteDrawing(currentRouteOriginLatLng, currentRouteOriginName, destLatLng, destName, destDocId);
                        } else {
                            Toast.makeText(MapsActivity.this, "Could not get current location. Please ensure GPS is enabled.", Toast.LENGTH_LONG).show();
                            centerMapOnLocation(destLatLng, destName, 15f);
                            transportModeContainer.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Log.e(TAG, "Failed to get current location for route.", e);
                        Toast.makeText(MapsActivity.this, "Failed to get current location. Showing destination only.", Toast.LENGTH_LONG).show();
                        centerMapOnLocation(destLatLng, destName, 15f);
                        transportModeContainer.setVisibility(View.GONE);
                    });
        } else {
            pendingRouteDestLatLng = destLatLng;
            pendingRouteDestName = destName;
            pendingRouteDestDocId = destDocId;
            awaitingPermissionsForRoute = true;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    protected void clearRouteElements(boolean restorePois) {
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
            currentRoutePolyline = null;
        }
        if (routeOriginMarker != null) {
            routeOriginMarker.remove();
            routeOriginMarker = null;
        }
        if (routeDestinationMarker != null) {
            routeDestinationMarker.remove();
            routeDestinationMarker = null;
        }
        if (restorePois) {
            mMap.clear();
            markerPlaceMap.clear();
            addManualHomeMarkerToMap();
            fetchAndDisplayAllPois();
            binding.tvDirectionText.setText("Explore the map.");
        }
    }

    private void drawStraightLineFallback(LatLng originLatLng, String originName, LatLng destLatLng, String destName, String destDocId) {
        Log.w(TAG, "Drawing straight line as fallback for route from " + originName + " to " + destName);
        if (mMap == null || originLatLng == null || destLatLng == null) return;
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
        }
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(originLatLng, destLatLng)
                .color(Color.parseColor("#FFA500"))
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
                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(placeLocation)
                                        .title(place.getName());
                                Marker marker = mMap.addMarker(markerOptions);
                                if (marker != null && marker.getId() != null) {
                                    markerPlaceMap.put(marker.getId(), place);
                                }
                            }
                        }
                        if (binding.tvDirectionText != null) {
                            String currentText = binding.tvDirectionText.getText().toString();
                            if (currentText.contains("Loading map...") || currentText.equals("Explore the map.") || (!currentText.toLowerCase().contains("route") && !currentText.toLowerCase().contains("showing:"))) {
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
                } catch (SecurityException se) {
                    Log.e(TAG, "Security Exception after permission grant");
                }
                if (awaitingPermissionsForRoute && pendingRouteDestLatLng != null) {
                    awaitingPermissionsForRoute = false;
                    fetchCurrentLocationForRoute(pendingRouteDestLatLng, pendingRouteDestName, pendingRouteDestDocId);
                } else if ("auto".equals(currentLocationMode)) {
                    centerOnActualGPSLocation(true);
                }
            } else {
                awaitingPermissionsForRoute = false;
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
                try {
                    if (mMap != null) mMap.setMyLocationEnabled(false);
                } catch (SecurityException se) {
                    Log.e(TAG, "SecurityException on setMyLocationEnabled(false)");
                }
                if (pendingRouteDestLatLng != null) {
                    centerMapOnLocation(pendingRouteDestLatLng, pendingRouteDestName, 15f);
                    transportModeContainer.setVisibility(View.GONE);
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
            if (destinationItemId == R.id.navigation_home)
                destinationActivityClass = HomeActivity.class;
            else if (destinationItemId == R.id.navigation_itineraries)
                destinationActivityClass = ItinerariesActivity.class;
            else if (destinationItemId == R.id.navigation_profile)
                destinationActivityClass = ProfileActivity.class;
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
            clearRouteElements(true);
            onMapReady(mMap);
        }
    }
}
package com.example.alayaapp;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.alayaapp.databinding.ActivityHomeBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    final int CURRENT_ITEM_ID = R.id.navigation_home;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TAG = "HomeActivity";
    public static final String EXTRA_SHOW_OUTSIDE_REGION_DIALOG = "SHOW_OUTSIDE_REGION_DIALOG";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates = false;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SharedPreferences sharedPreferences;
    private static final String KEY_LOCATION_MODE = "location_mode";
    private static final String KEY_MANUAL_LOCATION_NAME = "manual_location_name";
    private static final String KEY_MANUAL_LATITUDE = "manual_latitude";
    private static final String KEY_MANUAL_LONGITUDE = "manual_longitude";
    private static final String KEY_TRIP_DATE_YEAR = "trip_date_year";
    private static final String KEY_TRIP_DATE_MONTH = "trip_date_month";
    private static final String KEY_TRIP_DATE_DAY = "trip_date_day";
    private static final String KEY_TRIP_TIME_HOUR = "trip_time_hour";
    private static final String KEY_TRIP_TIME_MINUTE = "trip_time_minute";
    private static final String KEY_TRIP_END_TIME_HOUR = "trip_end_time_hour";
    private static final String KEY_TRIP_END_TIME_MINUTE = "trip_end_time_minute";
    private String currentLocationNameToDisplay = "Tap to get current location";
    private GeoPoint currentUserGeoPoint = null;
    private PlaceAdapter placeAdapter;
    private List<Place> placesList;
    private FirebaseFirestore db;
    private Calendar tripDateCalendar;
    private Calendar tripEndCalendar;
    private static final double BAGUIO_REGION_MIN_LAT = 16.35;
    private static final double BAGUIO_REGION_MAX_LAT = 16.50;
    private static final double BAGUIO_REGION_MIN_LON = 120.55;
    private static final double BAGUIO_REGION_MAX_LON = 120.65;
    private static final double NEARBY_RADIUS_KM = 10.0;
    private boolean wasRedirectedForDialog = false;
    private AlertDialog outsideRegionDialog;
    // NEW: Constant for the one-time region disclaimer
    private static final String PREF_KEY_SEEN_REGION_DISCLAIMER = "has_seen_region_disclaimer";

    private final ActivityResultLauncher<Intent> manualLocationPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String locationName = data.getStringExtra("selected_location_name");
                    double latitude = data.getDoubleExtra("selected_latitude", 0.0);
                    double longitude = data.getDoubleExtra("selected_longitude", 0.0);

                    if (isLocationInAllowedRegion(latitude, longitude)) {
                        dismissOutsideRegionDialog();
                    }

                    if (locationName != null && !locationName.isEmpty()) {
                        currentLocationNameToDisplay = locationName;
                        currentUserGeoPoint = new GeoPoint(latitude, longitude);
                        binding.tvLocationCity2.setText(currentLocationNameToDisplay);
                        if (binding.tvDirectionText != null) {
                            binding.tvDirectionText.setText("Manually set: " + currentLocationNameToDisplay);
                        }
                        saveLocationPreference("manual", locationName, latitude, longitude);
                        stopLocationUpdates();
                        fetchAndFilterPlaces(currentUserGeoPoint, false);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_SHOW_OUTSIDE_REGION_DIALOG, false)) {
            wasRedirectedForDialog = true;
        }

        sharedPreferences = UserPreferences.get(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationCallback();

        swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Pull-to-refresh triggered.");
            if (currentUserGeoPoint != null) {
                fetchAndFilterPlaces(currentUserGeoPoint, true);
            } else {
                Toast.makeText(this, "Location not set. Cannot refresh.", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });


        // This listener prevents the refresh gesture unless the user is at the top of the screen.
        binding.scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // The refresh gesture is enabled only when the scroll view is at the very top.
                swipeRefreshLayout.setEnabled(scrollY == 0);
            }
        });


        db = FirebaseFirestore.getInstance();
        placesList = new ArrayList<>();
        binding.rvPlacesList.setLayoutManager(new LinearLayoutManager(this));
        placeAdapter = new PlaceAdapter(this, placesList);
        binding.rvPlacesList.setAdapter(placeAdapter);

        tripDateCalendar = Calendar.getInstance();
        tripEndCalendar = Calendar.getInstance();
        setupDateTimePickers();
        loadSavedTripDateTime();

        binding.bottomNavigation.setSelectedItemId(CURRENT_ITEM_ID);
        binding.ibEditLocation.setOnClickListener(v -> showLocationChoiceDialog());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == CURRENT_ITEM_ID) {
                return true;
            }

            if (itemId == R.id.navigation_itineraries || itemId == R.id.navigation_map) {
                if (currentUserGeoPoint != null && !isLocationInAllowedRegion(currentUserGeoPoint.getLatitude(), currentUserGeoPoint.getLongitude())) {
                    showOutsideRegionDialog();
                    return false;
                }
            }

            Intent intent = null;
            if (itemId == R.id.navigation_itineraries) {
                intent = new Intent(getApplicationContext(), ItinerariesActivity.class);
            } else if (itemId == R.id.navigation_map) {
                intent = new Intent(getApplicationContext(), MapsActivity.class);
            } else if (itemId == R.id.navigation_profile) {
                intent = new Intent(getApplicationContext(), ProfileActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });

        loadLocationPreferenceAndInitialize();
        // Show the one-time disclaimer dialog if needed.
        showRegionDisclaimerIfFirstTime();
    }

    // Method to show a one-time disclaimer about region support.
    private void showRegionDisclaimerIfFirstTime() {
        if (!sharedPreferences.getBoolean(PREF_KEY_SEEN_REGION_DISCLAIMER, false)) {
            new AlertDialog.Builder(this)
                    .setTitle("Region Support Notice")
                    .setMessage("Welcome to Alaya! Please note that our app is currently optimized for discovering places and planning itineraries within the Baguio City region. Support for other areas is coming soon!")
                    .setPositiveButton("Got It", (dialog, which) -> {
                        // User has acknowledged the message, save the preference
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(PREF_KEY_SEEN_REGION_DISCLAIMER, true);
                        editor.apply();
                        dialog.dismiss();
                    })
                    .setCancelable(false) // Prevent dismissing by tapping outside
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(CURRENT_ITEM_ID);
        }
        if (wasRedirectedForDialog) {
            showOutsideRegionDialog();
            wasRedirectedForDialog = false;
        }
        String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
        if (mode.equals("auto")) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        } else {
            stopLocationUpdates();
            String name = sharedPreferences.getString(KEY_MANUAL_LOCATION_NAME, "Not Set");
            binding.tvLocationCity2.setText(name);
            if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Manually set: " + name);
        }
        loadSavedTripDateTime();
    }

    private void dismissOutsideRegionDialog() {
        if (outsideRegionDialog != null && outsideRegionDialog.isShowing()) {
            outsideRegionDialog.dismiss();
            outsideRegionDialog = null;
        }
    }

    private void showOutsideRegionDialog() {
        if (outsideRegionDialog != null && outsideRegionDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Outside Baguio Region")
                .setMessage("Your current location is outside the supported region. Please set a location within Baguio to get recommendations.")
                .setPositiveButton("Set Manually", (dialog, which) -> {
                    Intent intent = new Intent(HomeActivity.this, ManualLocationPickerActivity.class);
                    manualLocationPickerLauncher.launch(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setOnDismissListener(dialog -> outsideRegionDialog = null);
        outsideRegionDialog = builder.create();
        outsideRegionDialog.show();
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        currentUserGeoPoint = new GeoPoint(latitude, longitude);
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;

        if (!isLocationInAllowedRegion(latitude, longitude)) {
            stopLocationUpdates();
            binding.tvLocationCity2.setText("Outside supported region");
            binding.tvDirectionText.setText("Please set a location in Baguio.");
            showOutsideRegionDialog();
            fetchAndFilterPlaces(null, false);
            return;
        }

        dismissOutsideRegionDialog();
        fetchAndFilterPlaces(new GeoPoint(latitude, longitude), false);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String subLocality = address.getSubLocality();
                String thoroughfare = address.getThoroughfare();
                String country = address.getCountryName();

                StringBuilder addressTextBuilder = new StringBuilder();
                if (city != null && !city.isEmpty()) addressTextBuilder.append(city);
                else if (subLocality != null && !subLocality.isEmpty()) addressTextBuilder.append(subLocality);
                else if (thoroughfare != null && !thoroughfare.isEmpty()) addressTextBuilder.append(thoroughfare);
                else addressTextBuilder.append("Unknown Area");

                currentLocationNameToDisplay = addressTextBuilder.toString();
                binding.tvLocationCity2.setText(currentLocationNameToDisplay);
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("GPS: " + currentLocationNameToDisplay);
            } else {
                binding.tvLocationCity2.setText("Location Name Not Found (Auto)");
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("GPS: Location Name Not Found");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder service not available or IO error (Auto)", e);
            binding.tvLocationCity2.setText("Service to get address unavailable (Auto)");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid latitude or longitude values (Auto).", e);
            binding.tvLocationCity2.setText("Invalid location data (Auto)");
        }
    }

    private void setupDateTimePickers() {
        binding.rlTripDate.setOnClickListener(v -> {
            DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
                tripDateCalendar.set(Calendar.YEAR, year);
                tripDateCalendar.set(Calendar.MONTH, monthOfYear);
                tripDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                tripEndCalendar.set(Calendar.YEAR, year);
                tripEndCalendar.set(Calendar.MONTH, monthOfYear);
                tripEndCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                saveTripDate(year, monthOfYear, dayOfMonth);
                updateDateTimeUI();
            };
            DatePickerDialog datePickerDialog = new DatePickerDialog(HomeActivity.this, R.style.GreenDatePickerDialog, dateSetListener,
                    tripDateCalendar.get(Calendar.YEAR), tripDateCalendar.get(Calendar.MONTH), tripDateCalendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        binding.rlTripTime.setOnClickListener(v -> showStartTimePickerDialog());
    }

    private void showStartTimePickerDialog() {
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            tripDateCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            tripDateCalendar.set(Calendar.MINUTE, minute);
            saveTripTime(hourOfDay, minute);
            updateDateTimeUI();
            showEndTimePickerDialog();
        };
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, R.style.GreenTimePickerDialog, timeSetListener,
                tripDateCalendar.get(Calendar.HOUR_OF_DAY), tripDateCalendar.get(Calendar.MINUTE), false);
        timePickerDialog.setTitle("Select Start Time");
        timePickerDialog.show();
    }

    private void showEndTimePickerDialog() {
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            Calendar tempEndCal = (Calendar) tripEndCalendar.clone();
            tempEndCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            tempEndCal.set(Calendar.MINUTE, minute);

            if (tempEndCal.before(tripDateCalendar)) {
                Toast.makeText(this, "End time must be after start time.", Toast.LENGTH_LONG).show();
                return;
            }

            tripEndCalendar.setTime(tempEndCal.getTime());
            saveTripEndTime(hourOfDay, minute);
            updateDateTimeUI();
        };
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, R.style.GreenTimePickerDialog, timeSetListener,
                tripEndCalendar.get(Calendar.HOUR_OF_DAY), tripEndCalendar.get(Calendar.MINUTE), false);
        timePickerDialog.setTitle("Select End Time");
        timePickerDialog.show();
    }

    private void loadSavedTripDateTime() {
        if (sharedPreferences.contains(KEY_TRIP_DATE_YEAR)) {
            int year = sharedPreferences.getInt(KEY_TRIP_DATE_YEAR, tripDateCalendar.get(Calendar.YEAR));
            int month = sharedPreferences.getInt(KEY_TRIP_DATE_MONTH, tripDateCalendar.get(Calendar.MONTH));
            int day = sharedPreferences.getInt(KEY_TRIP_DATE_DAY, tripDateCalendar.get(Calendar.DAY_OF_MONTH));

            Calendar savedDate = Calendar.getInstance();
            savedDate.set(year, month, day);

            if (savedDate.before(Calendar.getInstance())) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(KEY_TRIP_DATE_YEAR);
                editor.remove(KEY_TRIP_DATE_MONTH);
                editor.remove(KEY_TRIP_DATE_DAY);
                editor.apply();
                tripDateCalendar = Calendar.getInstance();
            } else {
                tripDateCalendar.set(year, month, day);
                tripEndCalendar.set(year, month, day);
            }
        }

        if (sharedPreferences.contains(KEY_TRIP_TIME_HOUR)) {
            int hour = sharedPreferences.getInt(KEY_TRIP_TIME_HOUR, 9);
            int minute = sharedPreferences.getInt(KEY_TRIP_TIME_MINUTE, 0);
            tripDateCalendar.set(Calendar.HOUR_OF_DAY, hour);
            tripDateCalendar.set(Calendar.MINUTE, minute);
        }

        if (sharedPreferences.contains(KEY_TRIP_END_TIME_HOUR)) {
            int hour = sharedPreferences.getInt(KEY_TRIP_END_TIME_HOUR, 18);
            int minute = sharedPreferences.getInt(KEY_TRIP_END_TIME_MINUTE, 0);
            tripEndCalendar.set(Calendar.HOUR_OF_DAY, hour);
            tripEndCalendar.set(Calendar.MINUTE, minute);
        }
        updateDateTimeUI();
    }

    private void updateDateTimeUI() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        if (sharedPreferences.contains(KEY_TRIP_DATE_YEAR)) {
            binding.tvTripDate.setText(dateFormat.format(tripDateCalendar.getTime()));
        } else {
            binding.tvTripDate.setText("Set Trip Date");
        }

        boolean hasStartTime = sharedPreferences.contains(KEY_TRIP_TIME_HOUR);
        boolean hasEndTime = sharedPreferences.contains(KEY_TRIP_END_TIME_HOUR);

        if (hasStartTime && hasEndTime) {
            String timeRange = timeFormat.format(tripDateCalendar.getTime()) + " - " + timeFormat.format(tripEndCalendar.getTime());
            binding.tvTripTime.setText(timeRange);
        } else {
            binding.tvTripTime.setText("Set Start & End Time");
        }
    }

    private void saveTripDate(int year, int month, int day) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_TRIP_DATE_YEAR, year);
        editor.putInt(KEY_TRIP_DATE_MONTH, month);
        editor.putInt(KEY_TRIP_DATE_DAY, day);
        editor.apply();
    }

    private void saveTripTime(int hour, int minute) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_TRIP_TIME_HOUR, hour);
        editor.putInt(KEY_TRIP_TIME_MINUTE, minute);
        editor.apply();
    }

    private void saveTripEndTime(int hour, int minute) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_TRIP_END_TIME_HOUR, hour);
        editor.putInt(KEY_TRIP_END_TIME_MINUTE, minute);
        editor.apply();
    }

    private boolean isLocationInAllowedRegion(double latitude, double longitude) {
        return latitude >= BAGUIO_REGION_MIN_LAT && latitude <= BAGUIO_REGION_MAX_LAT &&
                longitude >= BAGUIO_REGION_MIN_LON && longitude <= BAGUIO_REGION_MAX_LON;
    }

    private double calculateDistance(GeoPoint start, GeoPoint end) {
        if (start == null || end == null) return Double.MAX_VALUE;
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(end.getLatitude() - start.getLatitude());
        double lonDistance = Math.toRadians(end.getLongitude() - start.getLongitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void fetchAndFilterPlaces(GeoPoint userLocation, boolean isManualRefresh) {
        if (userLocation == null) {
            placesList.clear();
            placeAdapter.notifyDataSetChanged();
            binding.rvPlacesList.setVisibility(View.GONE);
            binding.tvEmptyPlaces.setText("Set your location to find nearby spots.");
            binding.tvEmptyPlaces.setVisibility(View.VISIBLE);
            if (isManualRefresh) {
                swipeRefreshLayout.setRefreshing(false);
            } else {
                binding.progressBarHome.setVisibility(View.GONE);
            }
            return;
        }

        Log.d(TAG, "Fetching and filtering places based on location: " + userLocation.toString());
        if (!isManualRefresh) {
            binding.progressBarHome.setVisibility(View.VISIBLE);
        }
        binding.rvPlacesList.setVisibility(View.GONE);
        binding.tvEmptyPlaces.setVisibility(View.GONE);

        db.collection("places")
                .get()
                .addOnCompleteListener(task -> {
                    if (isManualRefresh) {
                        swipeRefreshLayout.setRefreshing(false);
                    } else {
                        binding.progressBarHome.setVisibility(View.GONE);
                    }

                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Place> nearbyPlaces = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Place place = document.toObject(Place.class);
                                if (place != null && place.getCoordinates() != null) {
                                    place.setDocumentId(document.getId());
                                    double distance = calculateDistance(userLocation, place.getCoordinates());
                                    if (distance <= NEARBY_RADIUS_KM) {
                                        place.setDistance(distance);
                                        place.setDistance_text(String.format(Locale.getDefault(), "%.1f km", distance));
                                        nearbyPlaces.add(place);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document: " + document.getId(), e);
                            }
                        }
                        Collections.sort(nearbyPlaces, Comparator.comparingDouble(Place::getDistance));
                        placesList.clear();
                        placesList.addAll(nearbyPlaces);

                        if (!placesList.isEmpty()) {
                            placeAdapter.notifyDataSetChanged();
                            binding.rvPlacesList.setVisibility(View.VISIBLE);
                            binding.tvEmptyPlaces.setVisibility(View.GONE);
                        } else {
                            binding.rvPlacesList.setVisibility(View.GONE);
                            binding.tvEmptyPlaces.setText(String.format(Locale.getDefault(), "No spots found within %.0fkm.", NEARBY_RADIUS_KM));
                            binding.tvEmptyPlaces.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.w(TAG, "Error getting documents from Firestore.", task.getException());
                        binding.rvPlacesList.setVisibility(View.GONE);
                        binding.tvEmptyPlaces.setText("Failed to load places. Please check connection.");
                        binding.tvEmptyPlaces.setVisibility(View.VISIBLE);
                        Toast.makeText(HomeActivity.this, "Failed to load places.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        stopLocationUpdates();
                        break;
                    }
                }
            }
        };
    }

    private void showLocationChoiceDialog() {
        final CharSequence[] options = {"Use My Current GPS Location", "Set Location Manually", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Location Method");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Use My Current GPS Location")) {
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Location Choice")
                        .setMessage("This will use your device's GPS to find your current location. Continue?")
                        .setPositiveButton("Yes", (confirmDialog, which) -> {
                            saveLocationPreference("auto", null, 0, 0);
                            currentUserGeoPoint = null;
                            binding.tvLocationCity2.setText("Fetching GPS location...");
                            if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Mode: GPS");
                            checkAndRequestLocationPermissions();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else if (options[item].equals("Set Location Manually")) {
                Intent intent = new Intent(HomeActivity.this, ManualLocationPickerActivity.class);
                if (currentUserGeoPoint != null) {
                    intent.putExtra(ManualLocationPickerActivity.EXTRA_INITIAL_LAT, currentUserGeoPoint.getLatitude());
                    intent.putExtra(ManualLocationPickerActivity.EXTRA_INITIAL_LON, currentUserGeoPoint.getLongitude());
                    intent.putExtra(ManualLocationPickerActivity.EXTRA_INITIAL_NAME, currentLocationNameToDisplay);
                }
                manualLocationPickerLauncher.launch(intent);
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void loadLocationPreferenceAndInitialize() {
        String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
        if (mode.equals("manual")) {
            String name = sharedPreferences.getString(KEY_MANUAL_LOCATION_NAME, "");
            double lat = Double.longBitsToDouble(sharedPreferences.getLong(KEY_MANUAL_LATITUDE, Double.doubleToRawLongBits(0.0)));
            double lon = Double.longBitsToDouble(sharedPreferences.getLong(KEY_MANUAL_LONGITUDE, Double.doubleToRawLongBits(0.0)));
            if (!name.isEmpty() && lat != 0.0 && lon != 0.0) {
                currentLocationNameToDisplay = name;
                currentUserGeoPoint = new GeoPoint(lat, lon);
                binding.tvLocationCity2.setText(currentLocationNameToDisplay);
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Manually set: " + currentLocationNameToDisplay);
                stopLocationUpdates();
                fetchAndFilterPlaces(currentUserGeoPoint, false);
            } else {
                saveLocationPreference("auto", null, 0, 0);
                checkAndRequestLocationPermissions();
            }
        } else {
            binding.tvLocationCity2.setText("Tap to get current location");
            if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Mode: GPS. Waiting for location...");
            checkAndRequestLocationPermissions();
        }
    }

    private void saveLocationPreference(String mode, String name, double lat, double lon) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LOCATION_MODE, mode);
        if (mode.equals("manual") && name != null) {
            editor.putString(KEY_MANUAL_LOCATION_NAME, name);
            editor.putLong(KEY_MANUAL_LATITUDE, Double.doubleToRawLongBits(lat));
            editor.putLong(KEY_MANUAL_LONGITUDE, Double.doubleToRawLongBits(lon));
        } else {
            editor.remove(KEY_MANUAL_LOCATION_NAME);
            editor.remove(KEY_MANUAL_LATITUDE);
            editor.remove(KEY_MANUAL_LONGITUDE);
        }
        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        dismissOutsideRegionDialog();
    }

    private void checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission to show your current location. Please allow.")
                        .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION))
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            binding.tvLocationCity2.setText("Permission needed");
                            if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Location permission denied.");
                            fetchAndFilterPlaces(null, false);
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            }
        } else {
            String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
            if (mode.equals("auto")) {
                fetchLastLocation();
                startLocationUpdates();
            } else {
                stopLocationUpdates();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String mode = sharedPreferences.getString(KEY_LOCATION_MODE, "auto");
                if (mode.equals("auto")) {
                    fetchLastLocation();
                    startLocationUpdates();
                }
            } else {
                binding.tvLocationCity2.setText("Location permission denied");
                if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Location permission denied.");
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
                fetchAndFilterPlaces(null, false);
            }
        }
    }

    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) return;

        if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Fetching last known location...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.d(TAG, "Last known location is null. Waiting for updates.");
                        if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Last location null, waiting for live updates...");
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error trying to get last GPS location", e);
                    binding.tvLocationCity2.setText("Failed to get location");
                    if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Failed to get last location.");
                });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!sharedPreferences.getString(KEY_LOCATION_MODE, "auto").equals("auto")) {
            stopLocationUpdates();
            return;
        }
        if (requestingLocationUpdates) return;

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        requestingLocationUpdates = true;
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        if (binding.tvDirectionText != null) binding.tvDirectionText.setText("Updating location (GPS)...");
    }

    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
        }
    }
}
<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" class="logo" width="120"/>

# AlayaApp

**AlayaApp** is a feature-rich Android app designed to help users plan, customize, and navigate detailed itineraries in Baguio City. The app leverages real-time location services and user preferences to provide tailored recommendations for tourist spots, food places, and other points of interest.

---

## ✨ Features

### 1. Smart Itinerary Generation

- **Personalized Plans:** Create itineraries based on your current or manually chosen location.
- **Custom Stops:** Select the number of stops and preferred categories (Tourist Spot, Food, Shopping, Park, Museum, Cafe).
- **Time Windows:** Plans consider your trip date, time range, and each location’s opening hours.


### 2. Detailed Place Information

- **Rich Details:** View descriptions, ratings, reviews, and images for each place.
- **Opening Hours:** See daily open/close times, with alerts for closed days.
- **Best Time to Visit:** Get suggestions for optimal visiting hours.


### 3. Interactive Maps \& Navigation

- **Google Maps Integration:** Visualize all recommended places and your itinerary on an interactive map.
- **Directions:** Get walking, taxi, or two-wheeler routes between stops, with real-time travel estimates.
- **Manual Location Picker:** Search or tap on the map to set your starting location.


### 4. User Profile \& History

- **Profile Management:** Edit your name, birthday, contact number, and change your password securely.
- **Trip History:** Save, view, and delete past itineraries, with previews and the ability to revisit each plan.


### 5. Offline Resilience

- **Firestore Integration:** All user data and places are stored in Firebase Firestore and Realtime Database, with offline support for trip saving.
- **Graceful Error Handling:** The app provides clear feedback when network or permission issues occur.

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (latest version recommended)
- **Google Maps API Key** (add to `local.properties` as `MAPS_API_KEY`)
- **Firebase Project:** Set up Firebase and add your `google-services.json` to the `app/` directory.


### Installation

1. Clone the repository.
2. Open in Android Studio.
3. Add your `local.properties` and `google-services.json`.
4. Build and run on an emulator or Android device.

---

## 🛠 Usage

- **Set Location:** Use GPS or manually select your starting point.
- **Customize Plan:** Choose the number and type of stops, then generate your itinerary.
- **Explore:** Tap on any place for details, directions, and map view.
- **Save Trips:** Save your favorite plans and review your travel history anytime.

---

## 🏗 Technologies Used

| Technology | Purpose |
| :-- | :-- |
| Android SDK (Kotlin/Java) | Core app development |
| Firebase (Firestore, Realtime DB) | Data storage, authentication, offline sync |
| Google Maps SDK | Maps and navigation |
| Material Design Components | UI/UX |
| Glide | Image loading |
| MVVM Architecture | App structure, state management |


---

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your proposed changes.

---

## 📄 License

This project is licensed under the MIT License.

---

## 🙏 Acknowledgments

- Google Maps Platform
- Firebase
- Android Jetpack Libraries

---

*For more details, see the full project documentation and codebase.*

<div style="text-align: center">⁂</div>

[^1]: project_summary.md


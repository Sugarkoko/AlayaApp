# AlayaApp

**AlayaApp** is an Android application designed for travelers and explorers, providing personalized itinerary planning and location-based recommendations—currently optimized for the Baguio City region.

---

## Table of Contents

- [Features](#features)
- [Technologies](#technologies)
- [Installation](#installation)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Screenshots](#screenshots)
- [Support](#support)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- **Location-Based Recommendations:** Discover nearby attractions, restaurants, and landmarks based on your current GPS location or a manually set point.
- **Itinerary Planning:** Generate and customize travel plans with stops, categories, and durations.
- **User Authentication:** Secure sign-up, login, and password management using Firebase Authentication.
- **Trip History:** Save and revisit your past itineraries.
- **Interactive Maps:** View places and itineraries on an interactive map with custom info windows.
- **Time \& Date Selection:** Set trip start and end times for personalized itinerary suggestions.
- **Region Support:** Currently optimized for Baguio City, Philippines, with plans to expand.

---

## Technologies

- **Android Studio** (Kotlin/Java)
- **Firebase**
    - Authentication
    - Firestore (Database)
    - Storage
- **Google Maps API**
- **Material Design Components**
- **Glide** (Image Loading)
- **SwipeRefreshLayout** (Pull-to-Refresh)
- **SharedPreferences** (Local Storage)
- **ViewBinding**

---

## Installation

1. **Clone the Repository:**

```bash
git clone https://github.com/YourUsername/AlayaApp.git
```

2. **Open in Android Studio:**
    - Open the project in Android Studio.
    - Sync Gradle dependencies.
3. **Configure Firebase:**
    - Add your `google-services.json` file to the `app` directory.
    - Set up your Google Maps API key in `local.properties` as `MAPS_API_KEY`.
4. **Build and Run:**
    - Build the project and run it on your Android device or emulator.

---

## Usage

1. **Launch the App:** Start with the welcome/splash screen.
2. **Set Your Location:** Choose between GPS or manual location selection.
3. **Set Trip Date \& Time:** Select your trip date and time range.
4. **Browse Places:** View recommended places near your location.
5. **Generate Itinerary:** Create a custom itinerary based on your preferences.
6. **Save \& View:** Save your itinerary and revisit it anytime in your trip history.
7. **Explore Maps:** Visualize your itinerary on an interactive map.

---

## Project Structure

```
AlayaApp/
├── .idea/                # IDE configuration files
├── app/
│   ├── build.gradle.kts  # App build configuration
│   ├── google-services.json # Firebase configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/alayaapp/
│   │   │   │   ├── HomeActivity.java         # Main home screen
│   │   │   │   ├── ItinerariesActivity.java  # Itinerary management
│   │   │   │   ├── MapsActivity.java         # Interactive map
│   │   │   │   ├── ManualLocationPickerActivity.java # Manual location selection
│   │   │   │   ├── ProfileActivity.java      # User profile
│   │   │   │   ├── ChangePasswordActivity.java # Password management
│   │   │   │   └── ...                       # Other activities and utilities
│   │   │   ├── res/                          # Layouts, drawables, strings, etc.
│   │   │   └── AndroidManifest.xml
│   │   └── androidTest/                      # Instrumented tests
│   └── proguard-rules.pro
└── .gitignore            # Git ignore rules
```


---

## Screenshots

*Coming soon!*

---

## Support

For questions or issues, please open an issue on GitHub or contact the project maintainers.

---

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request.

---

## License

This project is licensed under the MIT License.

```markdown
MIT License

Copyright (c) 2025 Sumakses Team

Permission is hereby granted...
```


---

**Happy Traveling with AlayaApp!** 🚀

---


<div style="text-align: center">⁂</div>


AlayaApp
AlayaApp is a comprehensive Android application designed to help users plan, customize, and navigate itineraries in Baguio City. The app provides personalized recommendations for tourist spots, food places, and other points of interest, leveraging real-time location services and user preferences.

Features
1. Smart Itinerary Generation
Personalized Plans: Generate itineraries based on your current location or a manually selected start point.

Custom Stops: Choose the number of stops and preferred categories (e.g., Tourist Spot, Food, Shopping, Park, Museum, Cafe).

Time Windows: Plans respect your selected trip date and time range, as well as each location’s opening hours.

2. Detailed Place Information
Rich Details: View descriptions, ratings, reviews, and images for each place.

Opening Hours: See daily opening and closing times, with warnings for closed days.

Best Time to Visit: Suggestions for optimal visiting hours are provided.

3. Interactive Maps & Navigation
Google Maps Integration: Visualize all recommended places and your itinerary on an interactive map.

Directions: Get walking, taxi, or two-wheeler routes between stops, with real-time travel estimates.

Manual Location Picker: Search or tap on the map to set your starting location.

4. User Profile & History
Profile Management: Edit your name, birthday, contact number, and change your password securely.

Trip History: Save, view, and delete past itineraries, with detailed previews and the ability to revisit each plan.

5. Offline Resilience
Firestore Integration: All user data and places are stored in Firebase Firestore and Realtime Database, with offline support for trip saving.

Graceful Error Handling: The app provides clear feedback when network or permission issues occur.

Getting Started
Prerequisites
Android Studio (latest version recommended)

Google Maps API Key (add to local.properties as MAPS_API_KEY)

Firebase Project: Set up Firebase and add your google-services.json to the app/ directory.

Installation
Clone the repository.

Open in Android Studio.

Add your local.properties and google-services.json.

Build and run on an emulator or Android device.

Usage
Set Location: Use GPS or manually select your starting point.

Customize Plan: Choose the number and type of stops, then generate your itinerary.

Explore: Tap on any place for details, directions, and map view.

Save Trips: Save your favorite plans and review your travel history anytime.

Technologies Used
Android SDK (Kotlin/Java)

Firebase (Firestore, Realtime Database, Auth, Storage)

Google Maps SDK

Material Design Components

Glide (Image Loading)

MVVM Architecture with LiveData & ViewModel

Contributing
Contributions are welcome! Please fork the repository and submit a pull request with your proposed changes.

License
This project is licensed under the MIT License.

Acknowledgments
Google Maps Platform

Firebase

Android Jetpack Libraries

For more details, see the full project documentation and codebase.

AlayaApp

Overview

AlayaApp is an Android application designed to enhance user experience with features like itinerary planning, location-based services, and user profile management. It leverages Google Maps for navigation and Firebase for authentication, analytics, real-time database, and storage functionalities. The app provides a modern user interface with Material Design components and supports a range of Android devices running API levels 24 to 35.

Features





User Authentication: Secure login and signup using Firebase Authentication.



Itinerary Management: Create, view, and manage travel itineraries with detailed logs and ratings.



Location Services: Integration with Google Maps for location picking, place details, and route navigation.



Profile Management: Update user profiles and manage passwords with a user-friendly interface.



Custom UI Components: Includes custom dialogs, bottom navigation, and place cards for a seamless experience.



Responsive Design: Supports multiple device resolutions and densities levels for optimal display.

Project Structure





Root Directory:





.gitignore: Ignores build files, IDE configurations, and local properties.



build.gradle.kts: Configures Gradle plugins and settings for the project.



gradle.properties: Defines JVM arguments and AndroidX settings.



settings.gradle.kts: Manages plugin and dependency repositories, including Google, Maven Central, and JitPack.



gradlew & gradlew.bat: Gradle wrapper scripts for Unix and Windows.



gradle-wrapper.jar & gradle-wrapper.properties: Gradle wrapper configuration for consistent builds.



App Module:





app/build.gradle.kts: Configures the Android application with dependencies like Firebase, Google Maps, and Glide.



app/src/main/AndroidManifest.xml: Declares permissions for internet and location access, and app components.



Layouts: XML files for activities (e.g., activity_welcome_page.xml, activity_maps.xml) and custom components like dialogs and cards.



Resources: Includes drawables, fonts, menus, and values (e.g., colors, strings, themes).



Tests: Instrumented tests (ExampleInstrumentedTest.java) and unit tests (ExampleUnitTest.java) for validation.

Dependencies





Firebase: Analytics, Authentication, Realtime Database, Firestore, and Storage.



Google Play Services: Maps and Location APIs for navigation and geolocation.



AndroidX Libraries: AppCompat, Material, Activity, and ConstraintLayout for UI components.



Third-Party: Glide for image loading and CircleImageView for profile images.



Testing: JUnit and Espresso for unit and UI testing.

Setup Instructions





Clone the Repository:

git clone <repository-url>
cd AlayaApp



Configure Google Maps API Key:





Obtain an API key from the Google Cloud Console with "Maps SDK for Android" enabled.



Update the com.google.android.geo.API_KEY value in app/src/main/AndroidManifest.xml with your key.



Set Up Firebase:





Ensure the google-services.json file in the app directory is correctly configured with your Firebase project details.



Verify Firebase services (Analytics, Authentication, Database, Firestore, Storage) are enabled in the Firebase Console.



Install Dependencies:





Ensure you have Android Studio and the Android SDK installed.



Run ./gradlew build to download dependencies and build the project.



Run the App:





Open the project in Android Studio.



Select a device or emulator (API 24 or higher) from the device list.



Click "Run" to deploy the app.

Build Configuration





Compile SDK: 35



Min SDK: 24



Target SDK: 34



Java Version: 1.8



Gradle Version: 8.11.1



Android Gradle Plugin: 8.9.2



NDK Version: 26.3.11579264

Supported Devices

The app is tested on a variety of devices, including:





Google Pixel series (Pixel 5, 6, 6a, 7, 7a, 8, 8a, 9, 9 Pro, 9 Pro XL, 9a)



Samsung Galaxy series (S21, S22 Ultra, S23, S23 Ultra, S24, A15, A35, Note9, Z Fold5/6, Flip 6)



Motorola (moto g 5G, moto g play, razr plus 2024)



Other brands like OnePlus, OPPO, FUJITSU, and SHARP.



Screen densities: 240dpi to 600dpi, with resolutions up to 1440x3088.

Testing





Unit Tests: Run ./gradlew test to execute local unit tests (e.g., ExampleUnitTest.java).



Instrumented Tests: Run ./gradlew connectedAndroidTest to execute tests on a device/emulator (e.g., ExampleInstrumentedTest.java).

Contributing





Fork the repository.



Create a new branch (git checkout -b feature-branch).



Make changes and commit (git commit -m "Add feature").



Push to the branch (git push origin feature-branch).



Create a Pull Request on GitHub.

License

This project is licensed under the Apache License, Version 2.0. See the LICENSE file for details.

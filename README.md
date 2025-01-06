## Features

- **Real-Time Location Tracking**: Displays the user's current location on the map using Google FusedLocationProvider.
- **POI (Point of Interest) Search**: Search nearby points of interest (e.g., cafes, restaurants, fast food) with selectable radius options.
- **Route Guidance**: Provides walking routes between the user's location and selected destinations.
- **Dynamic UI**: Implements responsive and animated buttons for intuitive user interaction.
- **Time Overlay**: Displays the current time as an overlay on the map.
- **Compass and Tracking Modes**: Supports compass mode and real-time tracking for a seamless navigation experience.

## Technologies Used

- **Android Compose**: Modern toolkit for building native UIs.
- **TMap API**: Used for map rendering, marker management, and route calculations.
- **Google Fused Location Provider**: Provides location services with high accuracy.

## Prerequisites

1. Android Studio installed on your development machine.
2. A valid TMap API key from SKT Developers.
3. A wearable OS emulator or device for testing.

## Getting Started

1. Open the project in Android Studio.

2. Add your TMap API key:
   - Navigate to `BuildConfig` and set your API key in `TMAP_API_KEY`.

3. Build and run the app on a Wear OS emulator or device.

## Permissions

The app requires the following permissions to function properly:

- **Location Access**: To fetch the user's current location and display it on the map.

Ensure you grant these permissions during runtime.

## Code Overview

### MainActivity.kt
The `MainActivity` handles:

- Initializing the TMapView and configuring map settings.
- Managing location updates with Google FusedLocationProvider.
- Handling user interactions such as searching for POIs and selecting destinations.

Key Components:

1. **Location Handling**: Ensures real-time updates of the user's location and dynamically updates map markers.
2. **Composable Functions**:
   - `MapView`: Displays the TMapView inside a Compose UI.
   - `CenterRowView`: Contains buttons for POI search and location tracking.
   - `DestinationDetail`: Shows destination details and remaining distance.




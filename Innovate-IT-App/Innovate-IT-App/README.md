# Umbilo Temple Frontend

An Android application for the Umbilo Temple with features for event management, calendar, donations, and more.

## Features

- User Authentication (Email/Password, Google, Facebook)
- Events Management
- Calendar View
- Donations
- Document Management
- Firewalking Event Details

## Setup Instructions

### Prerequisites

- Android Studio
- JDK 11 or higher
- Android SDK

### Getting Started

1. Clone the repository
2. Open the project in Android Studio
3. Sync the project with Gradle files

### Configuration

#### Google Sign-In

1. Create a project in the [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project
   - Use the package name `com.example.umbilotemplefrontend`
   - Download the `google-services.json` file and place it in the app directory
3. Enable Google Sign-In in the Firebase Authentication section
4. Get your Web Client ID from the Firebase project settings
5. Update the `google_web_client_id` in `strings.xml` with your Web Client ID

```xml
<string name="google_web_client_id">YOUR_GOOGLE_WEB_CLIENT_ID</string>
```

#### Facebook Login

1. Create a new app on the [Facebook Developer Portal](https://developers.facebook.com/)
2. Set up the Facebook Login product
3. Configure the OAuth redirect URI
4. Get your Facebook App ID and Client Token
5. Update the following values in `strings.xml`:

```xml
<string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
<string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
<string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
```

### Building the App

1. Build the app using Android Studio
2. Run on an emulator or physical device

## Project Structure

- `app/src/main/java/com/example/umbilotemplefrontend/`
  - `activities/`: All app screens
  - `adapters/`: RecyclerView adapters
  - `models/`: Data models
  - `utils/`: Utility classes

## Testing

The app includes both unit tests and instrumentation tests:

- Unit tests: `app/src/test/`
- Instrumentation tests: `app/src/androidTest/`

## License

This project is proprietary and confidential.

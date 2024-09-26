# BZU Map Navigator

## Overview
The BZU Map Navigator is an Android application designed to assist users in navigating Birzeit University (BZU) campus. The app displays the user's current location and allows them to select two department locations to find the shortest path between them. Additionally, the app categorizes various locations on campus, such as mosques, cafeterias, and departments, making it easy for users to identify points of interest.
<p align="center">
  <img src="https://github.com/Yazan-Abdalmutee/BZU-Map-Navigator/blob/master/image1.jfif" alt="Image 1" width="150"/>
  <img src="https://github.com/Yazan-Abdalmutee/BZU-Map-Navigator/blob/master/image2.jfif" alt="Image 2" width="150"/>
  <img src="https://github.com/Yazan-Abdalmutee/BZU-Map-Navigator/blob/master/image3.jfif" alt="Image 3" width="150"/>
  <img src="https://github.com/Yazan-Abdalmutee/BZU-Map-Navigator/blob/master/image4.jfif" alt="Image 4" width="150"/>
  <img src="https://github.com/Yazan-Abdalmutee/BZU-Map-Navigator/blob/master/image5.jfif" alt="Image 5" width="150"/>
  <img src="https://github.com/Yazan-Abdalmutee/BZU-Map-Navigator/blob/master/image6.jfif" alt="Image 6" width="150"/>
</p>
## Features
- **Current Location Display:** Shows the user's current location on the map.
- **Shortest Path Calculation:** Users can select two department locations, and the app calculates and displays the shortest path between them.
- **Location Categories:** Displays different categories of locations, including mosques, cafeterias, and departments.
- **Interactive Map:** Utilizes the Google Maps API to provide an interactive mapping experience.
- **Custom Markers:** Allows users to set markers on the map for better navigation.

## Technologies Used
- **Programming Language:** Java
- **Development Environment:** Android Studio
- **API:** Google Maps API for map integration
- **Data Structure:** Uses HashMap for storing location data

## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Yazan-Abdalmutee/BZU-Map-Navigator/tree/master
2. Open the project in Android Studio.

3. Locate the AndroidManifest.xml file and find the line that specifies the API key.
4. Replace **YOUR_API_KEY_HERE** with your actual API key in the following format:
```bash
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY_HERE"/>

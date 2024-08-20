package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView coordinatesTextView;
    private Marker marker;
    private Marker postionMarker;
    Map<String, String> locationMap = new HashMap<>();
    private Map<String, double[]> coordinates = new HashMap<>();
    private Map<String, List<String>> graph = new HashMap<>();
    private List<Polyline> polylineList = new ArrayList<>();
    private Map<String, List<Marker>> nameToMarkersMap = new HashMap<>();
    private Map<String, Boolean> markersVisibility = new HashMap<>();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coordinatesTextView = findViewById(R.id.coordinatesTextView);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        findViewById(R.id.markerMenuButton).setOnClickListener(this::showMarkerBottomSheet);


        // Initialize location client and request
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2000); // Update every 10 seconds
        locationRequest.setFastestInterval(1000); // Fastest update every 5 seconds
        findViewById(R.id.fromToButton).setOnClickListener(this::showFromToDialog);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationMarker(location);
                }
            }
        };


        initialiseValues();


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
// Set the map center
        LatLng center = new LatLng(31.95906, 35.18198);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 12)); // Adjust zoom level as needed

// Define the bounds
//        LatLngBounds bounds = new LatLngBounds(
//                new LatLng(31.95602, 35.178), // Southwest corner
//                new LatLng(31.96474, 35.18643)     // Northeast corner
//        );

        LatLngBounds bounds = new LatLngBounds(
                new LatLng(31.95583, 35.178), // Southwest corner
                new LatLng(31.96474, 35.18645)     // Northeast corner  left right
        );
        addMarker();

// Prevent the user from panning outside the specified bounds
        mMap.setLatLngBoundsForCameraTarget(bounds);

// Optionally, set the minimum and maximum zoom levels
        mMap.setMinZoomPreference(15.0f); // Adjust the minimum zoom level as needed
        mMap.setMaxZoomPreference(20.0f); // Adjust the maximum zoom level as needed

        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.bzu);

        if (bitmapDescriptor != null) {
            GroundOverlayOptions overlayOptions = new GroundOverlayOptions()
                    .image(bitmapDescriptor)
                    .positionFromBounds(bounds);

            mMap.addGroundOverlay(overlayOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
        } else {
            Log.e("MapError", "BitmapDescriptor is null");
        }

        addMarkersFromJson();

        mMap.setOnMapClickListener(latLng -> {
            if (marker != null) {
                marker.remove();
            }
            marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Clicked Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            coordinatesTextView.setText(String.format("Lat: %.6f, Lng: %.6f", latLng.latitude, latLng.longitude));
        });

        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        // Request location updates
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
                }
            }
        }
    }
//31.6086673
//    35.040515

    private void updateLocationMarker(Location location) {

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (postionMarker == null) {
            postionMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Your Position").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        } else {
            postionMarker.setPosition(latLng);
        }
//        31.95906, 35.18198



        coordinatesTextView.setText(String.format("Lat: %.6f, Lng: %.6f", latLng.latitude, latLng.longitude));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    private void addMarkersFromJson() {
        try {
            InputStreamReader reader = new InputStreamReader(getAssets().open("data2.json"));
            Gson gson = new Gson();
            Type listType = new TypeToken<List<MarkerData>>() {}.getType();
            List<MarkerData> markers = gson.fromJson(reader, listType);

            double latShiftSW = 31.95602 - 31.95583; // Southwest latitude difference
            double lngShiftSW = 35.178 - 35.178;     // Southwest longitude difference

            double latShiftNE = 31.96474 - 31.96474; // Northeast latitude difference
            double lngShiftNE = 35.18643 - 35.18645; // Northeast longitude difference

            for (MarkerData markerData : markers) {
                LatLng point = new LatLng(markerData.location[0], markerData.location[1]); // Example point
//                double newLat = point.latitude - latShiftSW;
//                double newLng = point.longitude - lngShiftSW;
//                LatLng position = new LatLng(newLat, newLng);
                LatLng position = new LatLng(markerData.location[0], markerData.location[1]);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title(markerData.title)
                        .snippet(markerData.image); // Set the image name as the snippet

                // Set the marker image


                Marker marker = mMap.addMarker(markerOptions);

                // Add marker to the list based on its name
                if (!nameToMarkersMap.containsKey(markerData.category)) {
                    nameToMarkersMap.put(markerData.category, new ArrayList<>());
                }
                nameToMarkersMap.get(markerData.category).add(marker);

                // Initially visible
                markersVisibility.put(markerData.category, false);
                marker.setVisible(false);

            }
        } catch (Exception e) {
            Log.e("MapError", "Error loading markers from JSON", e);
        }
    }
    private void showMarkerBottomSheet(View view) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_layout, null);
// Set up checkboxes for each name
        CheckBox checkDepartment = bottomSheetView.findViewById(R.id.checkBuild1);
        CheckBox checkCafe = bottomSheetView.findViewById(R.id.checkBuild2);
        CheckBox checkBuildings = bottomSheetView.findViewById(R.id.checkBuild3);
        CheckBox checkMosques = bottomSheetView.findViewById(R.id.checkBuild4);
        CheckBox checkPrinter = bottomSheetView.findViewById(R.id.checkBuild5);
        CheckBox checkShops = bottomSheetView.findViewById(R.id.checkBuild6);
        CheckBox checkPhoto = bottomSheetView.findViewById(R.id.checkBuild7);
        CheckBox checkEnter = bottomSheetView.findViewById(R.id.checkBuild8);

// Initialize checkbox states based on name visibility
// Initialize checkbox states based on name visibility (default to unchecked)
        checkDepartment.setChecked(false);
        checkCafe.setChecked(false);
        checkBuildings.setChecked(false);
        checkMosques.setChecked(false);
        checkPrinter.setChecked(false);
        checkShops.setChecked(false);
        checkPhoto.setChecked(false);
        checkEnter.setChecked(false);
        checkDepartment.setChecked(markersVisibility.getOrDefault("Department", false));
        checkCafe.setChecked(markersVisibility.getOrDefault("Cafe", false));
        checkBuildings.setChecked(markersVisibility.getOrDefault("Buildings", false));
        checkMosques.setChecked(markersVisibility.getOrDefault("Mosques", false));
        checkPrinter.setChecked(markersVisibility.getOrDefault("Printer", false));
        checkShops.setChecked(markersVisibility.getOrDefault("Shops", false));
        checkPhoto.setChecked(markersVisibility.getOrDefault("Photo", false));
        checkEnter.setChecked(markersVisibility.getOrDefault("Enter", false));


// Set checkbox listeners
        checkDepartment.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkerVisibility("Department", isChecked));
        checkCafe.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkerVisibility("Cafe", isChecked));
        checkBuildings.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkerVisibility("Buildings", isChecked));
        checkMosques.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkerVisibility("Mosques", isChecked));
        checkPrinter.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkerVisibility("Printer", isChecked));
        checkShops.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkerVisibility("Shops", isChecked));
        checkPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkerVisibility("Photo", isChecked));
        checkEnter.setOnCheckedChangeListener((buttonView, isChecked) -> updateMarkerVisibility("Enter", isChecked));


        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void updateMarkerVisibility(String name, boolean isVisible) {
        // Update visibility for all markers with the given name
        if (nameToMarkersMap.containsKey(name)) {
            for (Marker marker : nameToMarkersMap.get(name)) {
                marker.setVisible(isVisible);
            }
        }
        markersVisibility.put(name, isVisible);
    }

    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private final View infoWindowView;

        CustomInfoWindowAdapter() {
            LayoutInflater inflater = getLayoutInflater();
            infoWindowView = inflater.inflate(R.layout.info_window_layout, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            // Returning null will use the default info window
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            // Populate the custom info window layout
            TextView titleTextView = infoWindowView.findViewById(R.id.info_window_title);
            ImageView imageView = infoWindowView.findViewById(R.id.info_window_image);

            // Set the title
            titleTextView.setText(marker.getTitle());

            String imageName = marker.getSnippet(); // Get the image name from snippet
            if (imageName != null && !imageName.isEmpty()) {
                int imageResId = getResources().getIdentifier(imageName, "drawable", getPackageName());
                if (imageResId != 0) {
                    imageView.setImageResource(imageResId);
                } else {
                    imageView.setImageDrawable(null);
                    Log.e("MapError", "Drawable resource not found for image: " + imageName);
                }
            } else {
                imageView.setImageDrawable(null);
            }

            return infoWindowView;
        }
    }

    private static class MarkerData {
        double[] location;
        String category;
        String title;
        String image; // Image name in drawable folder

    }










    //-----------=======================================================================================
    // List to keep track of polyline references

    private void showFromToDialog(View view) {
        // Create a dialog for selecting categories and markers
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_select_from_to);

        // Get references to the spinners
        Spinner fromCategorySpinner = dialog.findViewById(R.id.fromCategorySpinner);
        Spinner fromSpinner = dialog.findViewById(R.id.fromSpinner);
        Spinner toCategorySpinner = dialog.findViewById(R.id.toCategorySpinner);
        Spinner toSpinner = dialog.findViewById(R.id.toSpinner);

        // Sample categories for demonstration
        String[] categories = { "كلية", "مصلى", "كافتيريا", "محلات", "بناية" ,"مدخل الجامعة"};

        // Set up adapters for categories
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        fromCategorySpinner.setAdapter(categoryAdapter);
        toCategorySpinner.setAdapter(categoryAdapter);

        // Map categories to marker names
        Map<String, List<String>> categoryToMarkersMap = new HashMap<>();
        categoryToMarkersMap.put("مصلى", Arrays.asList("مصلى العلوم", "مصلى الهندسة", "مصلى تكنلوجيا المعلومات","مصلى الحقوق","مصلى الاداب"));

// Buildings
        categoryToMarkersMap.put("بناية", Arrays.asList(
                "مجلس الطلبة",
                "بنك فلسطين",
                "مبنى الاستراحة وصالة الشطرنج",
                "إسكان رياض للطالبات",
                "معهد صناعات الأدوية",
                "المكتبة الرئيسية",
                "متحف الجامعة",
                "متحف الجامعة",
                "عيادة الجامعة",
                "مبنى مجمع الطلبة",
                "مبنى التسجيل",
                "مبنى إدارة الجامعة",
                "مبنى رئاسة الجامعة",
                "مسرح نسيب عزيز شاهين",
                "قاعة الشهيد كمال ناصر"
                ));

// Cafes
        categoryToMarkersMap.put("كافتيريا", Arrays.asList(
                "كافتيريا العم أبو أحمد",
                "كافتيريا الزعيم",
                "كافتيريا التمريض",
                "كافتيريا التجارة",
                "كافتيريا العلوم",
                "كافتيريا المركزية",
                "كافتيريا الآداب"
        ));

// Enter
        categoryToMarkersMap.put("مدخل الجامعة", Arrays.asList(
                "المدخل الشرقي",
                "المدخل الجنوبي",
                "المدخل الغربي"
        ));


// Shops
        categoryToMarkersMap.put("محلات", Arrays.asList(
                "سوبر ماركت الشني",
                "البوك ستور"
        ));

// Department
        categoryToMarkersMap.put("كلية", Arrays.asList(
                "كلية العلوم",
                "كلية الهندسة",
                "كلية التجارة",
                "ملحق التجارة",
                "مبنى الدراسات العليا",
                "معهد الحقوق",
                "مبنى الحقوق والإدارة العامة",
                "كلية التربية",
                "مبنى زعني القبة الزرقاء",
                "كلية الآي تي",
                "كلية التنمية",
                "كلية الإعلام",
                "مبنى دراسات المرأة",
                "كلية التمريض",
                "كلية الفنون",
                "الكلية الجديدة",
                "مبنى التربية الرياضية",
                "كلية الآداب",
                "مبنى مشاغل الهندسة"
        ));

        // Update spinners based on selected category for "From"
        fromCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                List<String> markers = categoryToMarkersMap.getOrDefault(selectedCategory, new ArrayList<>());

                ArrayAdapter<String> markerAdapter = new ArrayAdapter<>(dialog.getContext(), android.R.layout.simple_spinner_dropdown_item, markers);
                fromSpinner.setAdapter(markerAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when no category is selected
            }
        });

        // Update spinners based on selected category for "To"
        toCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                List<String> markers = categoryToMarkersMap.getOrDefault(selectedCategory, new ArrayList<>());

                ArrayAdapter<String> markerAdapter = new ArrayAdapter<>(dialog.getContext(), android.R.layout.simple_spinner_dropdown_item, markers);
                toSpinner.setAdapter(markerAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when no category is selected
            }
        });

        // Set up the select button
        dialog.findViewById(R.id.selectButton).setOnClickListener(v -> {
            String fromMarker = fromSpinner.getSelectedItem().toString();
            String toMarker = toSpinner.getSelectedItem().toString();
            // Draw a path between the selected markers
            clearPaths();

            drawPathBetweenMarkers(locationMap.get(fromMarker), locationMap.get(toMarker));
            dialog.dismiss();
        });

        // Set up the clear button
        dialog.findViewById(R.id.button_clear).setOnClickListener(v -> {
            clearPaths();  // Clear all paths drawn on the map
        });

        dialog.show();
    }




        private void drawPathBetweenMarkers(String from, String to) {


            List<String> path = findShortestPath(from, to);
            drawShortestPath(path);
//            calculateDistance(from,to);
        }




    private void clearPaths() {
        // Remove all polylines from the map
        for (Polyline polyline : polylineList) {
            polyline.remove();
        }
        polylineList.clear();  // Clear the list of polyline references
    }
    private void addEdge(String nodeA, String nodeB) {
        graph.computeIfAbsent(nodeA, k -> new ArrayList<>()).add(nodeB);
        graph.computeIfAbsent(nodeB, k -> new ArrayList<>()).add(nodeA);
    }

    // Dijkstra's algorithm to find the shortest path
    private List<String> findShortestPath(String start, String end) {
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(node -> node.distance));
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> visited = new HashSet<>();

        distances.put(start, 0.0);
        queue.add(new Node(start, 0.0));

        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();
            String currentNodeName = currentNode.name;

            if (visited.contains(currentNodeName)) {
                continue;
            }

            visited.add(currentNodeName);

            if (currentNodeName.equals(end)) {
                break;
            }

            for (String neighbor : graph.getOrDefault(currentNodeName, new ArrayList<>())) {
                if (!visited.contains(neighbor)) {
                    double newDist = distances.get(currentNodeName) + haversine(coordinates.get(currentNodeName), coordinates.get(neighbor));
                    if (newDist < distances.getOrDefault(neighbor, Double.MAX_VALUE)) {
                        distances.put(neighbor, newDist);
                        previous.put(neighbor, currentNodeName);
                        queue.add(new Node(neighbor, newDist));
                    }
                }
            }
        }

        // Reconstruct the path
        List<String> path = new ArrayList<>();
        for (String at = end; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);

        if (!path.get(0).equals(start)) {
            return null;  // No path found
        }

        return path;
    }

    // Haversine formula to calculate the distance between two latitude-longitude points
    private double haversine(double[] startCoords, double[] endCoords) {
        final double R = 6371.0; // Earth radius in kilometers
        double dLat = Math.toRadians(endCoords[0] - startCoords[0]);
        double dLon = Math.toRadians(endCoords[1] - startCoords[1]);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(startCoords[0])) * Math.cos(Math.toRadians(endCoords[0])) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Draw the shortest path on the map and calculate the total distance
    private double drawShortestPath(List<String> path) {
        if (path == null || path.size() == 0) {
            return -1;  // Return -1 if no path found or path is invalid
        }

        double totalDistance = 0.0;

        for (int i = 0; i < path.size() - 1; i++) {
            String pointA = path.get(i);
            String pointB = path.get(i + 1);
            double[] startCoords = coordinates.get(pointA);
            double[] endCoords = coordinates.get(pointB);
            totalDistance += haversine(startCoords, endCoords);

            // Draw the polyline for the segment
            LatLng startLatLng = new LatLng(startCoords[0], startCoords[1]);
            LatLng endLatLng = new LatLng(endCoords[0], endCoords[1]);
            drawPolyline(startLatLng, endLatLng);
        }

        return totalDistance;
    }


    // Draw polyline between two points on the map
    private void drawPolyline(LatLng start, LatLng end) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(start)
                .add(end)
                .color(Color.GREEN)  // Set the color to green
                .width(10);  // Set the width of the polyline

        Polyline polyline = mMap.addPolyline(polylineOptions);
        polylineList.add(polyline);  // Add the polyline to the list
    }


    // Node class for the priority queue in Dijkstra's algorithm
    private static class Node {
        String name;
        double distance;

        Node(String name, double distance) {
            this.name = name;
            this.distance = distance;
        }
    }
    void addMarker()
    {
// Add markers for each coordinate
//        for (Map.Entry<String, double[]> entry : coordinates.entrySet()) {
//            String markerName = entry.getKey();
//            double[] position = entry.getValue();
//
//            MarkerOptions markerOptions = new MarkerOptions()
//                    .position(new LatLng(position[0], position[1]))
//                    .title(markerName);
//
//            mMap.addMarker(markerOptions);
//        }
    }
    private void initialiseValues()
    {
        coordinates.put("D1", new double[]{31.958499, 35.183705});
        coordinates.put("D2", new double[]{31.958499, 35.183250});
        coordinates.put("D3", new double[]{31.958499, 35.182581});
        coordinates.put("D4", new double[]{31.958510, 35.181919});
        coordinates.put("C1", new double[]{31.958523, 35.181488});
        coordinates.put("C2", new double[]{31.958611, 35.181283});
        coordinates.put("D5", new double[]{31.958498, 35.181082});
        coordinates.put("D7", new double[]{31.958499, 35.183042});
        coordinates.put("D8", new double[]{31.958499, 35.182581});
        coordinates.put("D10", new double[]{31.958855, 35.181084});
        coordinates.put("C3", new double[]{31.959238, 35.181098});
        coordinates.put("D16", new double[]{31.959791, 35.181302});
        coordinates.put("D17E", new double[]{31.959731, 35.181252});
        coordinates.put("D17", new double[]{31.959818, 35.180831});
        coordinates.put("O1", new double[]{31.959208, 35.182030});
        coordinates.put("D15", new double[]{31.959683, 35.182091});
        coordinates.put("D19", new double[]{31.960096, 35.182168});
        coordinates.put("C4", new double[]{31.958948,35.182932});
        coordinates.put("C5", new double[]{31.959065,35.182067});
        coordinates.put("A1", new double[]{31.960393,35.181679});
        coordinates.put("A2", new double[]{31.960699,35.181992});
        coordinates.put("D23", new double[]{31.961038,35.182442});
        coordinates.put("C6", new double[]{31.961144,35.182610});
        coordinates.put("D31", new double[]{31.961438,35.182255});
        coordinates.put("D27", new double[]{31.961341,35.182887});
        coordinates.put("C7", new double[]{31.961473,35.183095});
        coordinates.put("D26", new double[]{31.961287,35.183512});
        coordinates.put("C8", new double[]{31.961848,35.183510});
        coordinates.put("C9", new double[]{31.961836,35.184331});
        coordinates.put("D29", new double[]{31.961564,35.184338});
        coordinates.put("D32", new double[]{31.962234,35.184434});
        coordinates.put("C10", new double[]{31.961286,35.184180});
        coordinates.put("D25", new double[]{31.961141,35.184234});
        coordinates.put("C11", new double[]{31.960775,35.183509});
        coordinates.put("D22", new double[]{31.960668,35.183624});
        coordinates.put("D18", new double[]{31.960366,35.182954});
        coordinates.put("C12", new double[]{31.959847,35.182180});
        coordinates.put("C13", new double[]{31.959879,35.182123});
        coordinates.put("C14", new double[]{31.959892,35.182408});
        coordinates.put("C15", new double[]{31.959685,35.182649});
        coordinates.put("C16", new double[]{31.959512,35.182754});
        coordinates.put("D14", new double[]{31.959510,35.183243});
        coordinates.put("C17", new double[]{31.960001,35.182428});
        coordinates.put("C18", new double[]{31.961891,35.182880});
        coordinates.put("D33", new double[]{31.962082,35.182889});
        coordinates.put("C19", new double[]{31.961914,35.182210});
        coordinates.put("D35", new double[]{31.962055,35.182057});
        coordinates.put("C20", new double[]{31.962044,35.83376});
        coordinates.put("C21", new double[]{31.962330,35.183349});
        coordinates.put("C22", new double[]{31.962542,35.183143});
        coordinates.put("C23", new double[]{31.962818,35.182509});
        coordinates.put("D34", new double[]{31.962719,35.182474});
        coordinates.put("C20-", new double[]{31.962089,35.183375});
        coordinates.put("C24", new double[]{31.959381,35.181120});
        coordinates.put("D11", new double[]{31.959355,35.180686});
        coordinates.put("C25", new double[]{31.959223,35.181535});
        coordinates.put("M16", new double[]{31.959401,35.181695});
        coordinates.put("I", new double[]{31.959776,35.181476});
        coordinates.put("A23", new double[]{31.960906,35.182576});
        coordinates.put("I1", new double[]{31.958405,35.183717});
        coordinates.put("I2", new double[]{31.95843,35.183251});
        coordinates.put("I3", new double[]{31.958326,35.182576});
        coordinates.put("I7", new double[]{31.958601,35.183037});
        coordinates.put("I8", new double[]{31.958788,35.182287});
        coordinates.put("C26", new double[]{31.958519,35.182283});
        coordinates.put("I4", new double[]{31.958370,35.181914});
        coordinates.put("C27", new double[]{31.959032,35.181049});
        coordinates.put("I10", new double[]{31.959031,35.181188});
        coordinates.put("I15", new double[]{31.959656,35.182193});
        coordinates.put("I9", new double[]{31.958789,35.181736});
        coordinates.put("I27", new double[]{31.961201,35.183252});
        coordinates.put("IE", new double[]{31.961850,35.185012});
        coordinates.put("C28", new double[]{31.958326,35.184137});
        coordinates.put("C29", new double[]{31.957969,35.184638});
        coordinates.put("C30", new double[]{31.957740,35.184805});
        coordinates.put("IS", new double[]{31.957220,35.184999});
        coordinates.put("C31", new double[]{31.958600,35.180532});
        coordinates.put("IW", new double[]{31.958536,35.180183});
        coordinates.put("I29", new double[]{31.961515,35.184471});
        coordinates.put("C32", new double[]{31.961728,35.183369});
        coordinates.put("I30", new double[]{31.961701,35.183529});
        coordinates.put("C33", new double[]{31.960104,35.182562});
        coordinates.put("I19", new double[]{31.960166,35.182507});
        coordinates.put("C34", new double[]{31.958584,35.180310});
        coordinates.put("C35", new double[]{31.959080,35.180031});
        coordinates.put("I12A", new double[]{31.959226,35.179965});
        coordinates.put("I12B", new double[]{31.959367,35.179986});
        coordinates.put("C36", new double[]{31.960862,35.182202});
        coordinates.put("I28", new double[]{31.961085,35.181646});
        coordinates.put("C37", new double[]{31.960983,35.183805});
        coordinates.put("C38", new double[]{31.960588,35.184316});
        coordinates.put("C39", new double[]{31.960428,35.184698});
        coordinates.put("C40", new double[]{31.960897,35.184904});
        coordinates.put("I24", new double[]{31.960894,35.185759});
        coordinates.put("C41", new double[]{31.961425,35.185038});










        addEdge("D1", "D2");
        addEdge("D2", "D7");
        addEdge("D7", "D3");
        addEdge("D7", "D8");
        addEdge("D8", "D4");
        addEdge("D4", "C1");
        addEdge("C1", "C2");
        addEdge("C2", "D5");
        addEdge("C2", "D10");
        addEdge("D10", "C3");
        addEdge("C3", "D16");
        addEdge("C3", "D17E");
        addEdge("D17E", "D17");
        addEdge("D17E", "D16");
        addEdge("C3", "O1");
        addEdge("O1", "D15");
        addEdge("D15", "D19");
        addEdge("D1", "C4");
        addEdge("C4", "C5");
        addEdge("C5", "O1");
        addEdge("D16", "A1");
        addEdge("A1", "A2");
        addEdge("A2", "D23");
        addEdge("D23", "C6");
        addEdge("C6", "D31");
        addEdge("C6", "D27");
        addEdge("D27", "C7");
        addEdge("C7", "D26");
        addEdge("C7", "C8");
        addEdge("C8", "C9");
        addEdge("C9", "D32");
        addEdge("C9", "D29");
        addEdge("D29", "C10");
        addEdge("C10", "D25");
        addEdge("C10", "C11");
        addEdge("C11", "D22");
        addEdge("C11", "D18");
        addEdge("D18", "C12");
        addEdge("D15", "C12");
        addEdge("C13", "C12");
        addEdge("C13", "D19");
        addEdge("C12", "C14");
        addEdge("C17", "C12");
        addEdge("C14", "C15");
        addEdge("C15", "C16");
        addEdge("C16", "D14");
        addEdge("D18", "C17");
        addEdge("C17", "C14");
        addEdge("C8", "C18");
        addEdge("C18", "D33");
        addEdge("C18", "C19");
        addEdge("C19", "D35");
        addEdge("C8", "C20-");
        addEdge("C20-", "C21");
        addEdge("C21", "C22");
        addEdge("C22", "C23");
        addEdge("C23", "D34");
        addEdge("C3", "C24");
        addEdge("C24", "D17E");
        addEdge("C24", "D11");
        addEdge("C3", "C25");
        addEdge("C25", "O1");
        addEdge("C25", "M16");
        addEdge("D16", "I");
        addEdge("D23", "A23");
        addEdge("D1", "I1");
        addEdge("D2", "I2");
        addEdge("D7", "I7");
        addEdge("D8", "I3");
        addEdge("D8", "C26");
        addEdge("C26", "D4");
        addEdge("C26", "I8");
        addEdge("D4", "I4");
        addEdge("D10", "C27");
        addEdge("C27", "C3");
        addEdge("C27", "I10");
        addEdge("D15", "I15");
        addEdge("C1", "I9");
        addEdge("C7", "I27");
        addEdge("C9", "IE");
        addEdge("D1", "C28");
        addEdge("C28", "C29");
        addEdge("C29", "C30");
        addEdge("C30", "IS");
        addEdge("D5", "C31");
        addEdge("C31", "IW");
        addEdge("D29", "I29");
        addEdge("C7", "C32");
        addEdge("C32", "C8");
        addEdge("C32", "C18");
        addEdge("C32", "I30");
        addEdge("C17", "C33");
        addEdge("C33", "D18");
        addEdge("C33", "I19");
        addEdge("IW", "C34");
        addEdge("C34", "C31");
        addEdge("C34", "C35");
        addEdge("C35", "I12A");
        addEdge("C35", "I12B");
        addEdge("I12A", "I12B");
        addEdge("A2", "C36");
        addEdge("C36", "D23");
        addEdge("C36", "I28");
        addEdge("C11", "C37");
        addEdge("C37", "C10");
        addEdge("C37", "C38");
        addEdge("C38", "C39");
        addEdge("C39", "C40");
        addEdge("C40", "I24");
        addEdge("IE", "C41");
        addEdge("C40", "C41");




        locationMap.put("مصلى العلوم", "D5");
        locationMap.put("مصلى الهندسة", "D11");
        locationMap.put("مصلى الاي تي", "I29");
        locationMap.put("مصلى الحقوق", "D18");
        locationMap.put("مصلى الاداب", "A23");
        locationMap.put("بنك فلسطين", "I19");
        locationMap.put("إسكان رياض للطالبات", "I24");
        locationMap.put("معهد صناعات الأدوية", "I34");
        locationMap.put("المكتبة الرئيسية", "I8");
        locationMap.put("متحف الجامعة", "I7");
        locationMap.put("عيادة الجامعة", "I4");
        locationMap.put("مبنى مجمع الطلبة", "I16");
        locationMap.put("مبنى التسجيل", "I15");
        locationMap.put("مبنى إدارة الجامعة", "I10");
        locationMap.put("مبنى رئاسة الجامعة", "D14");
        locationMap.put("مسرح نسيب عزيز شاهين", "D32");
        locationMap.put("قاعة الشهيد كمال ناصر", "I9");
        locationMap.put("كافتيريا العم أبو أحمد", "I12A");
        locationMap.put("كافتيريا الزعيم", "I12B");
        locationMap.put("كافتيريا التمريض", "D33");
        locationMap.put("كافتيريا التجارة", "I2");
        locationMap.put("كافتيريا العلوم", "D5");
        locationMap.put("كافتيريا المركزية", "I");
        locationMap.put("كافتيريا الآداب", "A23");
        locationMap.put("المدخل الشرقي", "IE");
        locationMap.put("المدخل الجنوبي", "IS");
        locationMap.put("المدخل الغربي", "IW");
        locationMap.put("سوبر ماركت الشني", "D19");
        locationMap.put("البوك ستور", "I19");
        locationMap.put("مصلى الهندسة", "D11");
        locationMap.put("مصلى تكنلوجيا المعلومات", "I29");
        locationMap.put("مصلى الحقوق", "D18");
        locationMap.put("مصلى الاداب", "A23");
        locationMap.put("كلية العلوم", "D5");
        locationMap.put("كلية الهندسة", "D11");
        locationMap.put("كلية التجارة", "I2");
        locationMap.put("ملحق التجارة", "I1");
        locationMap.put("مبنى الدراسات العليا", "I3");
        locationMap.put("معهد الحقوق", "I15");
        locationMap.put("مبنى الحقوق والإدارة العامة", "D18");
        locationMap.put("كلية التربية", "D22");
        locationMap.put("مبنى زعني القبة الزرقاء", "D25");
        locationMap.put("كلية الآي تي", "I29");
        locationMap.put("كلية التنمية", "D26");
        locationMap.put("كلية الإعلام", "I30");
        locationMap.put("مبنى دراسات المرأة", "I27");
        locationMap.put("كلية التمريض", "D33");
        locationMap.put("كلية الفنون", "D35");
        locationMap.put("الكلية الجديدة", "D31");
        locationMap.put("مبنى التربية الرياضية", "I28");
        locationMap.put("كلية الآداب", "A23");
        locationMap.put("مبنى مشاغل الهندسة", "D17");
        locationMap.put("مجلس الطلبة", "I");
        locationMap.put("مبنى الاستراحة وصالة الشطرنج", "I");

    }

}

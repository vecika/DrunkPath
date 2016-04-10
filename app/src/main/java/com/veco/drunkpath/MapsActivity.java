package com.veco.drunkpath;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.veco.drunkpath.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.veco.drunkpath.R.layout.activity_drawer;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap myMap;

    private ToggleButton track;
    private TextView tvDistance;
    private Button btnReset;

    private LocationListener locationListener;

    private LocationManager locationManager;

    private long difference;

    private int refreshRate;

    private List<LatLng> list;

    private long tStartTime;
    private long tEndTime;

    private float speed;
    private float distanceTraveled;

    private SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.TIME_STAMP_FORMAT);


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_drawer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorBackground));
                toolbar.setTitleTextColor(Color.BLACK);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        refreshRate = Constants.DEFAULT_REFRESH_RATE;

        list = new ArrayList<>();

        track = (ToggleButton) findViewById(R.id.toggleButton);
        tvDistance = (TextView) findViewById(R.id.textview_distance);
        btnReset = (Button) findViewById(R.id.btn_reset);

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(MapsActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Reset?")
                        .setMessage("Are you sure you want to reset current tracking?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                reset();
                                if (!track.isChecked())
                                    track.setText(R.string.start_tracking);
                                Toast.makeText(getApplicationContext(), "Tracking reset", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        showUI();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, R.string.gps_enabled, Toast.LENGTH_SHORT).show();
        } else {
            showGPSDisabledAlertToUser();
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if (track.isChecked()) {
                    LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 17));
                    Marker marker = myMap.addMarker(new MarkerOptions().position(newLocation));

                    distanceTraveled = 0;

                    if (marker != null) {
                        tvDistance.setText(R.string.tracking);
                        tvDistance.setTextColor(Color.RED);
                    }

                    myMap.clear();

                    PolylineOptions line = new PolylineOptions();
                    line.width(5);
                    line.color(Color.rgb(255, 125, 32));
                    list.add(newLocation);
                    line.addAll(list);

                    myMap.addPolyline(line);

                    drawingIcon();

                    Location currLocation = new Location("currLocation");
                    Location lastLocation = new Location("lastLocation");
                    gettingDistance(line, currLocation, lastLocation);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Ok
            }

            @Override
            public void onProviderEnabled(String provider) {
                // Ok
            }

            @Override
            public void onProviderDisabled(String provider) {
                // Ok
            }
        };

        track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = track.isChecked();
                if (on) {
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        trackingStarted();
                    } else
                        showGPSDisabledAlertToUser();
                } else {
                    trackingStopped();

                    tEndTime = System.currentTimeMillis();
                    difference = tEndTime - tStartTime;
                    speed = (distanceTraveled / (difference / 1000)) * Constants.CONVERSION_M_KM;

                    String speedDecimal = String.format("%.2f", speed);
                    String distanceDecimal = String.format("%.2f", distanceTraveled);

                    Fragment fragmentResult = new FragmentResult();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.sve, fragmentResult).addToBackStack("main").commit();

                    Bundle bundle = new Bundle();
                    bundle.putString("distance", distanceDecimal);
                    bundle.putString("speed", speedDecimal);
                    bundle.putLong("time", difference);
                    fragmentResult.setArguments(bundle);
                }
            }
        });

        updateRefreshRate();
    }


    private void trackingStarted() {
        tStartTime = System.currentTimeMillis();
        Toast.makeText(getApplicationContext(), "Tracking started", Toast.LENGTH_SHORT).show();
        tvDistance.setText(R.string.searching_for_gps);
        tvDistance.setTextColor(Color.YELLOW);
    }

    private void trackingStopped() {
        Toast.makeText(getApplicationContext(), "Tracking stopped", Toast.LENGTH_SHORT).show();
        tvDistance.setText(R.string.inactive);
        tvDistance.setTextColor(Color.GREEN);
        track.setText(R.string.txt_continue);
    }


    private void drawingIcon() {
        LatLng iconDrunk = new LatLng((list.get(list.size() - 1).latitude), (list.get(list.size() - 1).longitude));
        myMap.addMarker(new MarkerOptions()
                .position(iconDrunk)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_drunk))
                .title(getString(R.string.here)));
    }

    public void showUI() {
        track.setVisibility(View.VISIBLE);
        tvDistance.setVisibility(View.VISIBLE);
        btnReset.setVisibility(View.VISIBLE);
    }


    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(getString(R.string.gps_disabled_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.go_to_settings),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton(getString(R.string.exit_application),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void reset() {
        myMap.clear();
        distanceTraveled = 0;
        list.clear();
        difference = 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        showUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private float gettingDistance(PolylineOptions line, Location currLocation, Location lastLocation){

        for (int i = 1; i < line.getPoints().size(); i++) {

            LatLng startLocation = new LatLng(line.getPoints().get(0).latitude, line.getPoints().get(0).longitude);
            myMap.addMarker(new MarkerOptions().position(startLocation));

            currLocation.setLatitude(line.getPoints().get(i).latitude);
            currLocation.setLongitude(line.getPoints().get(i).longitude);

            lastLocation.setLatitude(line.getPoints().get(i - 1).latitude);
            lastLocation.setLongitude(line.getPoints().get(i - 1).longitude);

            distanceTraveled += lastLocation.distanceTo(currLocation);
        }

        return distanceTraveled;
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        myMap = googleMap;
        showUI();
    }


    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getApplicationContext(), "Connection lost, please reconnect", Toast.LENGTH_SHORT).show();
        Log.i("2:", "Location service suspended, please reconnect.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_hybrid) {
            myMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (id == R.id.nav_normal) {
            myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (id == R.id.nav_satellite) {
            myMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else if (id == R.id.nav_about) {
            Fragment fragmentAbout = new FragmentAbout();
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.sve, fragmentAbout).addToBackStack("main").commit();
        } else {
            if (id == R.id.nav_refresh) {
                setRefreshRate();
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;

    }

    private void setRefreshRate() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.set_refresh_rate));
        alert.setMessage(getString(R.string.refresh_rate_message));

        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setIcon(R.drawable.icon_time);

        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("\t Current: " + refreshRate / 1000 + " seconds");
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        input.requestFocus();

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (input.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Refresh rate unchanged.", Toast.LENGTH_SHORT).show();
                } else {
                    int newRefreshRate = Integer.valueOf(input.getText().toString());
                    refreshRate = (newRefreshRate * 1000);
                    updateRefreshRate();
                    Toast.makeText(getApplicationContext(), "Refresh rate changed to " + newRefreshRate+ " second(s).", Toast.LENGTH_SHORT).show();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancel
            }
        });
        alert.show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Close application?")
                .setMessage("Are you sure you want to close application?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void updateRefreshRate(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager.requestLocationUpdates("gps", refreshRate, 5, locationListener);

    }
}







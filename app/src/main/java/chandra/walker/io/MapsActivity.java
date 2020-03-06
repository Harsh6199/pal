package chandra.walker.io;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.firestore.GeoPoint;

import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private final static int ALL_PERMISSIONS_RESULT = 101;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private MarkerManager markerManager;
    private Context context = this;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        locationManager = new LocationManager(this, MapsActivity.this);

        getPermission();


    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getPermission() {
        PermissionManger permissionManger = new PermissionManger(this, MapsActivity.this);
        permissionManger.getLocationPermission(() -> {
            locationManager.getLocationUpdates(l -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    updateCurrentLocation(l);
                }
                return null;
            });
            return null;
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void updateCurrentLocation(Location location) {
        Log.d("location:", location.getLatitude() + " " + location.getLongitude());
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        FirebaseManager.updateCurrentUserLocation(new GeoPoint(currentLocation.latitude, currentLocation.longitude));
        FirebaseManager.queryNearBy(location, (snapShot) -> {
            queryNearBy((Map<String, Object>) snapShot);
            return null;
        });
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 13));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == ALL_PERMISSIONS_RESULT) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationManager.getLocationUpdates(l -> {
                    updateCurrentLocation(l);
                    return null;
                });
            } else {
                Log.d("Permission manager", "Location permission denied by user");
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        markerManager = new MarkerManager(mMap);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void queryNearBy(Map<String, Object> snapShot) {
        GeoPoint location = (GeoPoint) snapShot.get("geoPoint");
        String id = (String) snapShot.get("id");
        assert location != null;
        Log.d("Geo query doc entered", "lat: " + location.getLatitude() + "lng: " + location.getLongitude() + "photoUrl: " + snapShot.get("photoUrl"));
        markerManager.addMarker(new LatLng(location.getLatitude(), location.getLongitude()), id);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onMarkerClick(Marker marker) {
        String id = (String) marker.getTag();
        assert id != null;
        if (id.equals(CurrentUser.uuid)) {
            Intent intent = new Intent(this, FriendsListActivity.class);
            startActivity(intent);
        } else {
            FirebaseManager.getUserInfo(id).thenAcceptAsync(user -> {
                FirebaseManager.getFriendShipStatus(id).thenAcceptAsync(status -> {
                    FragmentManager fm = getSupportFragmentManager();
                    ProfileViewFragment profileViewFragment = ProfileViewFragment.newInstance(user, status);
                    profileViewFragment.show(fm, id);
                });
            });
        }
        return true;
    }
}

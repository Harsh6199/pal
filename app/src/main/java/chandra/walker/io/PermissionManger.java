package chandra.walker.io;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;

class PermissionManger {

    private final static int ALL_PERMISSIONS_RESULT = 101;
    private Context context;
    private Activity activity;

    PermissionManger(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void getLocationPermission(Callable<Void> getLocationUpdates) {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(ACCESS_FINE_LOCATION);
        ArrayList<String> permissionsToRequest = findUnAskedPermissions(permissions);
        if (context.checkSelfPermission(ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (permissionsToRequest.size() > 0)
                activity.requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        } else {
            Log.d("Permission manager", "Permisson already granted");
            try {
                getLocationUpdates.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    CompletableFuture<Boolean> getPhonePermission() {
        CompletableFuture<Boolean> done = new CompletableFuture<>();
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(CALL_PHONE);
        ArrayList<String> permissionsToRequest = findUnAskedPermissions(permissions);
        if (context.checkSelfPermission(CALL_PHONE) != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            if (permissionsToRequest.size() > 0)
                activity.requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        } else {
            Log.d("Permission manager", "Permisson already granted");
            done.complete(true);
        }
        return done;
    }

}

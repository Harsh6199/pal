package chandra.walker.io;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {

    private String url;
    private MarkerOptions markerOptions;
    private GoogleMap map;
    private String id;

    ImageLoadTask(String url, MarkerOptions markerOptions, GoogleMap map, String id) {
        this.url = url;
        this.markerOptions = markerOptions;
        this.map = map;
        this.id = id;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        Log.d("Do in background", this.id + ": id url: " + this.url);
        if (this.url == null || this.url.isEmpty()) return null;
        try {
            Log.d("excusting", "started");
            URL urlConnection = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlConnection
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            Log.d("excusting", "done");
            return myBitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);
        if (result != null) {
            Bitmap returnedBitmap = getCroppedBitmap(result);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(returnedBitmap));
        }
        map.addMarker(markerOptions).setTag(id);
    }

    private Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
//         canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                50, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output;
    }

}


class MarkerCreater {
    static void getMarkerBitmapFromView(MarkerOptions markerOptions, GoogleMap map, String id, String photoUrl) {
        new ImageLoadTask(photoUrl,
                markerOptions, map, id).execute();
    }
}

class MarkerManager {
    private GoogleMap map;

    MarkerManager(GoogleMap map) {
        this.map = map;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void addMarker(LatLng currentLocation, String id) {
        if (!id.equals("current location")) {
            FirebaseManager.getUserInfo(id).thenAcceptAsync(user -> {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(currentLocation)
                        .title(id);
                MarkerCreater.getMarkerBitmapFromView(markerOptions, map, id, user.photoUrl);
            });
        } else {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(currentLocation)
                    .title(id);
            MarkerCreater.getMarkerBitmapFromView(markerOptions, map, id, "");
        }
        map.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
    }
}
package chandra.walker.io;

import android.os.Build;

import androidx.annotation.RequiresApi;

class CurrentUser {
    static String email, name, uuid, photoUrl, phoneNumber;

    static void setUser(String name, String email, String uuid, String photoUrl, Boolean isNew, String phone) {
        CurrentUser.name = name;
        CurrentUser.email = email;
        CurrentUser.uuid = uuid;
        CurrentUser.photoUrl = photoUrl;
        CurrentUser.phoneNumber = phone;
        if (isNew) {
            FirebaseManager.createUser();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static void setUser(String name, String email, String uuid, String photoUrl, Boolean isNew) {
        CurrentUser.name = name;
        CurrentUser.email = email;
        CurrentUser.uuid = uuid;
        CurrentUser.photoUrl = photoUrl;
        FirebaseManager.getFriendRequestList();
        if (isNew) {
            FirebaseManager.createUser();
        }
    }
}

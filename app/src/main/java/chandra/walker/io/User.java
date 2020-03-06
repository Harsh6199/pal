package chandra.walker.io;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;

class User implements Serializable {
    String name, id, photoUrl, phoneNumber;
    Boolean isFriend;
    GeoPoint location;

    User(String name, String id, String photoUrl, GeoPoint location, String phone) {
        this.name = name;
        this.id = id;
        this.photoUrl = photoUrl;
        this.location = location;
        this.phoneNumber = phone;
    }
}

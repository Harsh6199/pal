package chandra.walker.io;

import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.imperiumlabs.geofirestore.GeoFirestore;
import org.imperiumlabs.geofirestore.GeoQuery;
import org.imperiumlabs.geofirestore.listeners.GeoQueryDataEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


/**
 * user: {
 * {uid}: {
 * name: String,
 * email: String,
 * l: GeoLocation,
 * g: GeoHash,
 * photoUrl: String,
 * phNo: String
 * social: {
 * people: {
 * requestSent: Array[id],
 * requestReceived: Array[id],
 * friends: Array[id],
 * }
 * }
 * }
 * }
 */

class FirebaseManager {
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static GeoFirestore geoFirestore = new GeoFirestore(db.collection("user"));

    static void updateCurrentUserLocation(GeoPoint location) {
        geoFirestore.setLocation(CurrentUser.uuid, location);
    }

    static void createUser() {
        Map<String, Object> userLocation = new HashMap<>();
        userLocation.put("name", CurrentUser.name);
        userLocation.put("email", CurrentUser.email);
        userLocation.put("id", CurrentUser.uuid);
        userLocation.put("photoUrl", CurrentUser.photoUrl);
        userLocation.put("phNo", CurrentUser.phoneNumber);
        db.collection("user").document(CurrentUser.uuid).set(userLocation).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Pushed ot db", ">>>>");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }

    static void queryNearBy(Location location, Function<Object, Object> updateLocation) {
        GeoQuery geoQuery = geoFirestore.queryAtLocation(
                new GeoPoint(
                        location.getLatitude(),
                        location.getLongitude()),
                1);

        geoQuery.addGeoQueryDataEventListener(new GeoQueryDataEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onDocumentEntered(DocumentSnapshot documentSnapshot, GeoPoint geoPoint) {
                Log.d("In field", "name: " + documentSnapshot.getData() + " photoURl: " + documentSnapshot.get("photoUrl"));
                Map<String, Object> map = new HashMap<>();
                map.put("id", documentSnapshot.getId());
                map.put("geoPoint", geoPoint);
                updateLocation.apply(map);
            }

            @Override
            public void onDocumentExited(DocumentSnapshot documentSnapshot) {
            }

            @Override
            public void onDocumentMoved(DocumentSnapshot documentSnapshot, GeoPoint geoPoint) {
            }

            @Override
            public void onDocumentChanged(DocumentSnapshot documentSnapshot, GeoPoint geoPoint) {
            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(Exception e) {
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static CompletableFuture<User> getUserInfo(String id) {
        CompletableFuture<User> completableFuture = new CompletableFuture<>();
        db.collection("user").document(id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String name = (String) documentSnapshot.get("name"),
                        id = documentSnapshot.getId(),
                        photoUrl = (String) documentSnapshot.get("photoUrl"),
                        phone = (String) documentSnapshot.get("phNo");
                GeoPoint geoPoint = (GeoPoint) documentSnapshot.get("l");
                User user = new User(name, id, photoUrl, geoPoint, phone);
                Log.d("Got user", "user" + user.name);
                Log.d("Got user", "user" + user.name + geoPoint.getLongitude());
                completableFuture.complete(user);
            }
        });
        return completableFuture;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static CompletableFuture<List<String>> getFriendsList(String id) {
        CompletableFuture<List<String>> completableFuture = new CompletableFuture<>();
        db.collection("user")
                .document(id)
                .collection("social")
                .document("people").get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        List<String> friendsId = (List<String>) documentSnapshot.get("friends");
                        Log.d("Got friends", "list: " + friendsId);
                        completableFuture.complete(friendsId);
                    }
                });
        return completableFuture;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static CompletableFuture<Boolean> updateFriendsList(String userId, String friendId) {
        CompletableFuture<Boolean> done = new CompletableFuture<>();
        DocumentReference people = db.collection("user")
                .document(userId)
                .collection("social")
                .document("people");
        people
                .update("friends", FieldValue.arrayUnion(friendId))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Log.e("Failed to add as friend", String.valueOf(task.getException()));
                            String reason = String.valueOf(task.getException());
                            if (reason.contains("No document to update:")) {
                                Map<String, Object> friends = new HashMap<>();
                                List<String> friendsList = new ArrayList<>();
                                friendsList.add(friendId);
                                friends.put("friends", friendsList);
                                people.set(friends).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        done.complete(task.isSuccessful());
                                    }
                                });
                            } else {
                                done.complete(task.isSuccessful());
                            }
                        } else {
                            done.complete(task.isSuccessful());
                        }
                    }
                });
        return done;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static CompletableFuture<Boolean> isFollowing(String otherId) {
        CompletableFuture<Boolean> isFollowing = new CompletableFuture<>();
        DocumentReference people = db.collection("user")
                .document(CurrentUser.uuid)
                .collection("social")
                .document("people");
        people.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot snapshot = task.getResult();
                    List<String> friendsList = (List<String>) snapshot.get("friends");
                    List<String> requestList = (List<String>) snapshot.get("requestSent");
                    if (friendsList != null && requestList != null) {
                        isFollowing.complete(friendsList.contains(otherId) || requestList.contains(otherId));
                    }
                }
                isFollowing.complete(false);
            }
        });
        return isFollowing;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static CompletableFuture<List<String>> getFriendRequestList() {
        CompletableFuture<List<String>> userId = new CompletableFuture<>();
        db.collection("user")
                .document(CurrentUser.uuid)
                .collection("social")
                .document("people").get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult().get("requestReceived") != null) {
                            userId.complete(new ArrayList<String>((Collection<? extends String>) Objects.requireNonNull(task.getResult().get("requestReceived"))));
                        } else {
                            userId.complete(new ArrayList<>());
                        }
                    }
                });
        return userId;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    static CompletableFuture<Boolean> _addAsFriend(String friendId) {
        CompletableFuture<Boolean> done = new CompletableFuture<>();
        Log.d("_add as friend", "starting");
        DocumentReference currentUserRef = db.collection("user")
                .document(friendId)
                .collection("social")
                .document("people");
        currentUserRef
                .update("requestReceived", FieldValue.arrayUnion(CurrentUser.uuid))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d("_add as friend", "added");
                        if (!task.isSuccessful()) {
                            Log.e("Failed to add as friend", String.valueOf(task.getException()));
                            String reason = String.valueOf(task.getException());
                            if (reason.contains("No document to update:")) {
                                Map<String, Object> friends = new HashMap<>();
                                List<String> friendsList = new ArrayList<>();
                                friendsList.add(CurrentUser.uuid);
                                friends.put("requestReceived", friendsList);
                                currentUserRef.set(friends).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        done.complete(task.isSuccessful());
                                    }
                                });
                            } else {
                                done.complete(task.isSuccessful());
                            }
                        }
                    }
                });
        return done;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static CompletableFuture<Boolean> sendFriendRequest(String friendId) {
        CompletableFuture<Boolean> done = new CompletableFuture<>();
        DocumentReference currentUserRef = db.collection("user")
                .document(CurrentUser.uuid)
                .collection("social")
                .document("people");
        currentUserRef
                .update("requestSent", FieldValue.arrayUnion(friendId))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.e("Failed to add as friend", String.valueOf(task.getException()));
                            String reason = String.valueOf(task.getException());
                            if (reason.contains("No document to update:")) {
                                Map<String, Object> friends = new HashMap<>();
                                List<String> friendsList = new ArrayList<>();
                                friendsList.add(friendId);
                                friends.put("requestSent", friendsList);
                                currentUserRef.set(friends).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        _addAsFriend(friendId).thenAcceptAsync((d) -> {
                                            done.complete(task.isSuccessful());
                                        });

                                    }
                                });
                            } else {
                                _addAsFriend(friendId).thenAcceptAsync((d) -> {
                                    done.complete(task.isSuccessful());
                                });
                            }
                        }
                    }
                });
        return done;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static CompletableFuture<Boolean> acceptFriendRequest(String friendId) {
        CompletableFuture<Boolean> done = new CompletableFuture<>();
        DocumentReference friendSocialList = db.collection("user")
                .document(friendId)
                .collection("social")
                .document("people");
        DocumentReference userSocialList = db.collection("user")
                .document(CurrentUser.uuid)
                .collection("social")
                .document("people");

        updateFriendsList(friendId, CurrentUser.uuid);
        friendSocialList.update("requestSent", FieldValue.arrayRemove(CurrentUser.uuid));
        userSocialList.update("requestReceived", FieldValue.arrayRemove(friendId));
        updateFriendsList(CurrentUser.uuid, friendId).thenAcceptAsync((isCompleted) -> {
            System.out.println(">>>Added as frnd");
            done.complete(isCompleted);
        });
        return done;
    }

    static void logOut() {
        FirebaseAuth.getInstance().signOut();
    }
}

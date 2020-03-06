package chandra.walker.io;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.GeoPoint;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


class FriendsListAdapter extends RecyclerView.Adapter<FriendsListAdapter.ViewHolder> {
    private ArrayList<User> users;
    private Context context;
    private Activity activity;

    FriendsListAdapter(ArrayList<User> listdata, Context context, Activity activity) {
        this.users = listdata;
        this.context = context;
        this.activity = activity;
    }

    @Override
    public FriendsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.frinds_list_adapter, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Log.d("update on ", position + " updatin");
        final User currentUser = users.get(position);
        holder.name.setText(currentUser.name);
        Picasso.with(context)
                .load(currentUser.photoUrl)
                .into(holder.imageView);
        holder.direction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Invoking ", currentUser.location + "");
                invokeMap(currentUser.location);
            }
        });
        holder.phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                invokePhone(currentUser.phoneNumber);
            }
        });
        holder.optionsLayout.setVisibility(currentUser.isFriend ? View.VISIBLE : View.GONE);
        holder.addFriend.setVisibility(currentUser.isFriend ? View.GONE : View.VISIBLE);
        holder.addFriend.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                Toast.makeText(context, "Accepting request", Toast.LENGTH_SHORT).show();
                FirebaseManager.acceptFriendRequest(currentUser.id).thenAcceptAsync((done) -> {
                    currentUser.isFriend = true;
                    users.remove(position);
                    users.add(position, currentUser);
                    FriendsListActivity.refreshList((FriendsListActivity) context, position);
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    @SuppressLint("MissingPermission")
    private void invokePhone(String phNo) {
        PermissionManger permissionManger = new PermissionManger(context, activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            permissionManger.getPhonePermission().thenAcceptAsync((done) -> {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + phNo));
                context.startActivity(intent);
            });
        }
    }

    private void invokeMap(GeoPoint geoPoint) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + geoPoint.getLatitude() + "," + geoPoint.getLongitude());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        context.startActivity(mapIntent);
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView name;
        LinearLayout optionsLayout;
        ImageButton direction, phone, addFriend;

        ViewHolder(View itemView) {
            super(itemView);
            this.imageView = itemView.findViewById(R.id.imageView);
            this.name = itemView.findViewById(R.id.name);
            this.optionsLayout = itemView.findViewById(R.id.options_layout);
            this.direction = itemView.findViewById(R.id.direction);
            this.phone = itemView.findViewById(R.id.phone);
            this.addFriend = itemView.findViewById(R.id.accept_friend);
        }
    }
}
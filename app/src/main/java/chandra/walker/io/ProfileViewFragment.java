package chandra.walker.io;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;

import com.squareup.picasso.Picasso;

public class ProfileViewFragment extends DialogFragment implements View.OnClickListener {

    private User user;

    ProfileViewFragment() {

    }

    static ProfileViewFragment newInstance(User user, Boolean isFollowing) {
        ProfileViewFragment frag = new ProfileViewFragment();
        Bundle args = new Bundle();
        args.putSerializable("user", user);
        args.putBoolean("isFollowing", isFollowing);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button addAsFriend = view.findViewById(R.id.add_as_friend);
        addAsFriend.setOnClickListener(this);
        ImageView profileView = view.findViewById(R.id.profile);
        TextView name = view.findViewById(R.id.name);
        User user = (User) getArguments().getSerializable("user");
        Boolean isFollowing = getArguments().getBoolean("isFollowing");
        if (isFollowing) {
            addAsFriend.setEnabled(false);
            addAsFriend.setText("Following");
        }

        System.out.println("Got user object" + user.name);
        name.setText(user.name);
        Picasso.with(getContext()).load(user.photoUrl).into(profileView);
        this.user = user;

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View view) {
        dismiss();
        int id = view.getId();
        if (id == R.id.add_as_friend) {
            Log.d("Add as friend", "To be added as friend" + user.id);
            FirebaseManager.sendFriendRequest(user.id).thenAcceptAsync((done) -> {
                Log.d("Added as Friend", "Success" + done);
            });
        }
    }
}

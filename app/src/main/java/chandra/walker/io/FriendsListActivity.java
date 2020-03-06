package chandra.walker.io;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FriendsListActivity extends AppCompatActivity {

    ArrayList<User> friendsLists;
    RecyclerView recyclerView;
    FriendsListAdapter friendsListAdapter;

    static void refreshList(FriendsListActivity friendsListActivity, int pos) {
        Log.d("Item refrished", "done");
        friendsListActivity.friendsListAdapter.notifyItemChanged(pos);
        friendsListActivity.friendsListAdapter.notifyDataSetChanged();
        friendsListActivity.recyclerView.notify();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        recyclerView = findViewById(R.id.friendsList);
        recyclerView.setHasFixedSize(true);
        friendsLists = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsListAdapter = new FriendsListAdapter(friendsLists, this, FriendsListActivity.this);
        recyclerView.setAdapter(friendsListAdapter);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        FirebaseManager.getFriendsList(CurrentUser.uuid).thenAcceptAsync(this::getFriendList);
        FirebaseManager.getFriendRequestList().thenAcceptAsync(this::getFriendRequestList);

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void getFriendList(List<String> friendsIds) {
        for (String id : friendsIds) {
            FirebaseManager.getUserInfo(id).thenAcceptAsync((user) -> {
                user.isFriend = true;
                friendsLists.add(user);
                friendsListAdapter.notifyItemInserted(friendsLists.size());
                friendsListAdapter.notifyDataSetChanged();
                recyclerView.notify();
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void getFriendRequestList(List<String> requestId) {
        for (String id : requestId) {
            FirebaseManager.getUserInfo(id).thenAcceptAsync((user) -> {
                user.isFriend = false;
                friendsLists.add(user);
                friendsListAdapter.notifyItemInserted(friendsLists.size());
                friendsListAdapter.notifyDataSetChanged();
                recyclerView.notify();
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.logout, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            Intent intent = new Intent(this, SplashScreen.class);
            intent.putExtra("logout", true);
            startActivity(intent);

            return (true);
        }
        return (super.onOptionsItemSelected(item));
    }

}

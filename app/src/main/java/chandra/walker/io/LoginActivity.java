package chandra.walker.io;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().hide();

        findViewById(R.id.signInButton).setOnClickListener(this);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mAuth = FirebaseAuth.getInstance();

        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.move_to_up);
        Animation animation2 = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.move_to_donw);
        SignInButton signInButton = findViewById(R.id.signInButton);
        signInButton.startAnimation(animation);
        TextView textView = findViewById(R.id.textView2);
        textView.startAnimation(animation2);

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onStart() {
        super.onStart();
        try {
            Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
            updateUI(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateUI(boolean isNewUser) {
        if (isNewUser) {
            getPhoneNumber().thenAcceptAsync((phoneNumber) -> {
                CurrentUser.setUser(mAuth.getCurrentUser().getDisplayName(),
                        mAuth.getCurrentUser().getEmail(),
                        mAuth.getUid(), mAuth.getCurrentUser().getPhotoUrl().toString(), isNewUser, phoneNumber);
                startMapActivity();
            });
        } else {
            CurrentUser.setUser(mAuth.getCurrentUser().getDisplayName(),
                    mAuth.getCurrentUser().getEmail(),
                    mAuth.getUid(), mAuth.getCurrentUser().getPhotoUrl().toString(), isNewUser);
            startMapActivity();
        }
    }

    private void startMapActivity() {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                assert account != null;
                firebaseAuthWithGoogle(account);
            } catch (Exception e) {
                Log.w("Failed to sign in", "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("Google sign in ", "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("Google sign in::", "signInWithCredential:success");
                            updateUI(task.getResult().getAdditionalUserInfo().isNewUser());
                        } else {
                            Log.w("Google sign in::", "signInWithCredential:failure", task.getException());
                        }

                        // ...
                    }
                });
    }

    public void signIn(View view) {
        Log.d("Trying to signin", "Started");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.signInButton) {
            signIn(view);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    CompletableFuture<String> getPhoneNumber() {
        CompletableFuture<String> phoneNumber = new CompletableFuture<>();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
        alert.setMessage("Phone number");
        alert.setTitle("Enter your phone number");
        alert.setView(edittext);
        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(DialogInterface dialog, int whichButton) {
                String phNo = edittext.getText().toString();
                phoneNumber.complete(phNo);
            }
        });
        alert.setCancelable(false);
        alert.show();
        setFinishOnTouchOutside(false);
        return phoneNumber;
    }
}

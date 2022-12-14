package com.example.javaceadminapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity2 extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{
    FirebaseAuth firebaseAuth;
    DrawerLayout drawerLayout;
    FirebaseFirestore firestore;
    Dialog progressDialog;


    List<ExercisesModel> exercisesModels = new ArrayList<>();
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    ExercisesAdapter exercisesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Javace");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        drawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        progressDialog = new Dialog(MainActivity2.this);
        progressDialog.setContentView(R.layout.loading_progressbar);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setBackgroundDrawableResource(R.drawable.progressbar_background);
        progressDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);


        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();



        recyclerView = findViewById(R.id.exerciseRecycler);
        recyclerView.setHasFixedSize(true);
        layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);


        fetchExercises();

    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchExercises();
    }
    void deleteExercise(int index) {
        progressDialog.show();

        firestore.collection("Exercises").document(exercisesModels.get(index).geteId()).delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MainActivity2.this, "Exercise deleted", Toast.LENGTH_SHORT).show();
                        fetchExercises();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity2.this, "Unable to delete exercise", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void fetchExercises() {
        progressDialog.show();
        firestore.collection("Exercises").orderBy("exercise_score", Query.Direction.ASCENDING).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                exercisesModels.clear();
                progressDialog.dismiss();

                for (DocumentSnapshot documentSnapshot: task.getResult()) {
                    ExercisesModel exercisesModel = new ExercisesModel(documentSnapshot.getString("eId"),
                            documentSnapshot.getString("exercise_title"),
                            documentSnapshot.getString("exercise_instruction"),
                            documentSnapshot.getString("exercise_content"),
                            documentSnapshot.getString("exercise_score"));
                    exercisesModels.add(exercisesModel);
                }
                exercisesAdapter = new ExercisesAdapter(MainActivity2.this, exercisesModels);
                recyclerView.setAdapter(exercisesAdapter);
                exercisesAdapter.notifyItemInserted(exercisesModels.size());

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        String uid = firebaseUser.getUid();
        firestore.collection("Admins").document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult().exists()) {
                    progressDialog.dismiss();
                } else {
                    firebaseAuth.signOut();
                    redirectActivity(MainActivity2.this, MainActivity.class);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity2.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.students:
                redirectActivity(MainActivity2.this, Student.class);
                break;
            case R.id.quiz:
                redirectActivity(MainActivity2.this, Category.class);
                break;
            case R.id.exercises:
                redirectActivity(MainActivity2.this, ExerciseActivity.class);
                break;
            case R.id.forum:
                redirectActivity(MainActivity2.this, Forum.class);
                break;
            case R.id.feedback:
                redirectActivity(MainActivity2.this, Feedback.class);
                break;
            case R.id.admin:
                redirectActivity(MainActivity2.this, Admin.class);
                break;
            case R.id.progress:
                redirectActivity(MainActivity2.this, Progress.class);
                break;
            case R.id.resources:
                redirectActivity(MainActivity2.this, Resources.class);
                break;
            case R.id.logout:
                firebaseAuth.signOut();
                redirectActivity(MainActivity2.this, MainActivity.class);

        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    public void redirectActivity(Activity activity, Class pointClass) {

        Intent intent = new Intent(activity,pointClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }


}
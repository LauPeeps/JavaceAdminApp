package com.example.javaceadminapp;

import static com.example.javaceadminapp.Category.category_index;
import static com.example.javaceadminapp.Category.category_list;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.collection.ArrayMap;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Sets extends AppCompatActivity {

    RecyclerView setsView;
    Button addSetBtn;
    SetsAdapter adapter;
    FirebaseFirestore firestore;
    Dialog progressDialog, addPage;
    EditText topicTitle,topicContent;
    Button addTopicBtn;


    public static List<String> idOfSets = new ArrayList<>();
    public static int set_index = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sets);

        Toolbar toolbar = findViewById(R.id.setstoolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Submodules");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setsView = findViewById(R.id.sets_recycler);
        addSetBtn = findViewById(R.id.addSetBtn);

        progressDialog = new Dialog(Sets.this);
        progressDialog.setContentView(R.layout.loading_progressbar);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setBackgroundDrawableResource(R.drawable.progressbar_background);
        progressDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        addPage = new Dialog(Sets.this);
        addPage.setContentView(R.layout.add_topic_page);
        addPage.setCancelable(true);
        addPage.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        topicTitle = addPage.findViewById(R.id.titleText);
        topicContent = addPage.findViewById(R.id.titleContent);

        addTopicBtn = addPage.findViewById(R.id.addTopicBtn);
        addSetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                topicTitle.getText().clear();
                topicContent.getText().clear();
                addPage.show();
            }
        });

        addTopicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (topicTitle.getText().toString().isEmpty()) {
                    topicTitle.setError("Please enter topic title");
                    return;
                } if (topicContent.getText().toString().isEmpty()) {
                    topicContent.setError("Please enter topic content");
                    return;
                }
                addSet(topicTitle.getText().toString(), topicContent.getText().toString());
            }
        });





        firestore = FirebaseFirestore.getInstance();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        setsView.setLayoutManager(linearLayoutManager);

        fetchSets();




    }

    private void fetchSets() {

        idOfSets.clear();

        progressDialog.show();

        firestore.collection("Quiz").document(category_list.get(category_index).getId())
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        long noOfSets = (long) documentSnapshot.get("Submodule");

                        for (int i = 1; i <= noOfSets; i++) {
                            idOfSets.add(documentSnapshot.getString("Submodule" + String.valueOf(i) + "_Id"));

                        }
                        category_list.get(category_index).setSetBase(documentSnapshot.getString("Base"));
                        category_list.get(category_index).setNoOfSets(String.valueOf(noOfSets));

                        adapter = new SetsAdapter(idOfSets);
                        setsView.setAdapter(adapter);

                        progressDialog.dismiss();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(Sets.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();

                    }
                });


    }

    private void addSet(String title, String content) {
        addPage.dismiss();
        progressDialog.show();

        String current_category_id = category_list.get(category_index).getId();
        String current_questionNo = category_list.get(category_index).getSetBase();

        Map<String, Object> question_data = new ArrayMap<>();
        question_data.put("QNO", "0");

        firestore.collection("Quiz").document(current_category_id)
                .collection(current_questionNo).document("Question_List")
                .set(question_data).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Map<String, Object> category_doc = new ArrayMap<>();
                        category_doc.put("BASE", String.valueOf(Integer.parseInt(current_questionNo) + 1));
                        category_doc.put("Submodule" + String.valueOf(idOfSets.size() + 1) + "_Id", current_questionNo);
                        category_doc.put("Submodule", idOfSets.size() + 1);


                        firestore.collection("Quiz").document(current_category_id)
                                .update(category_doc)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {

                                        idOfSets.add(current_questionNo);
                                        adapter.notifyItemInserted(idOfSets.size());

                                        Map<String, Object> topic_data = new ArrayMap<>();
                                        topic_data.put("Topic_Title", title);
                                        topic_data.put("Topic_Content", content);
                                        firestore.collection("Quiz").document(current_category_id)
                                                .collection(current_questionNo).document("Topic_List").set(topic_data)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        category_list.get(category_index).setNoOfSets(String.valueOf(idOfSets.size()));
                                                        category_list.get(category_index).setSetBase(String.valueOf(Integer.parseInt(current_questionNo) + 1));

                                                        adapter.notifyItemInserted(idOfSets.size());
                                                        progressDialog.dismiss();
                                                    }
                                                });

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e){

                                        Toast.makeText(Sets.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        progressDialog.dismiss();

                                    }
                                });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(Sets.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
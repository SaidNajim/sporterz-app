package com.example.sporterz_mobile.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sporterz_mobile.R;
import com.example.sporterz_mobile.adapters.UserAdapter;
import com.example.sporterz_mobile.models.User;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    private EditText searchInput;
    private ImageButton searchButton;
    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private DatabaseReference usersRef;
    private ProgressBar progressBar;
    private TextView noUsersFoundText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(com.example.sporterz_mobile.R.layout.fragment_explore, container, false);

        searchInput = view.findViewById(com.example.sporterz_mobile.R.id.seach_username_input);
        searchButton = view.findViewById(com.example.sporterz_mobile.R.id.search_user_btn);
        userRecyclerView = view.findViewById(com.example.sporterz_mobile.R.id.search_user_recycler_view);
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(getActivity(), userList);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        userRecyclerView.setAdapter(userAdapter);
        progressBar = view.findViewById(com.example.sporterz_mobile.R.id.progress_bar);
        noUsersFoundText = view.findViewById(R.id.no_users_found_text);

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        // Load all users by default
        loadAllUsers();

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchText = searchInput.getText().toString().trim();
                if (!searchText.isEmpty()) {
                    progressBar.setVisibility(View.VISIBLE);
                    searchUsers(searchText);
                } else {
                    Toast.makeText(getActivity(), "Please enter a username to search",
                            Toast.LENGTH_SHORT).show();
                    userList.clear();
                    userAdapter.notifyDataSetChanged();
                }
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String searchText = s.toString().trim();
                if (searchText.isEmpty()) {
                    userList.clear();
                    userAdapter.notifyDataSetChanged();
                }
            }
        });

        return view;
    }

    private void searchUsers(final String searchText) {
        usersRef.orderByChild("username").startAt(searchText).endAt(searchText + "\uf8ff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        userList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String userId = snapshot.getKey();
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                user.setUserId(userId);
                                userList.add(user);
                            }
                        }
                        progressBar.setVisibility(View.GONE);
                        if (userList.isEmpty()) {
                            noUsersFoundText.setVisibility(View.VISIBLE);
                        } else {
                            noUsersFoundText.setVisibility(View.GONE);
                        }
                        userAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getActivity(), "Error: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void loadAllUsers() {
        progressBar.setVisibility(View.VISIBLE);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        String userId = snapshot.getKey();
                        user.setUserId(userId);
                        userList.add(user);
                    }
                }
                progressBar.setVisibility(View.GONE);
                userAdapter.notifyDataSetChanged();
                noUsersFoundText.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getActivity(), "Error loading users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
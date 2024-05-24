package com.example.helloworld.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import com.example.helloworld.R;

public class ProfilePage extends Fragment {
    private SwipeRefreshLayout swipeRefreshLayout;

    public static ProfilePage newInstance() {
        return new ProfilePage();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.profile_page, container, false);
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        swipeRefreshLayout.setRefreshing(true);

        refreshData();

        return rootView;
    }

    private void refreshData() {

        swipeRefreshLayout.setRefreshing(false);
    }
}

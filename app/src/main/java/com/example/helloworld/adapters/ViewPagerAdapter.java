package com.example.helloworld.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.helloworld.pages.HomePage;
import com.example.helloworld.pages.PlaylistPage;
import com.example.helloworld.pages.ProfilePage;
import com.example.helloworld.pages.VideosPage;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {
    private static final int NUM_PAGES = 4;

    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return HomePage.newInstance();
            case 1:
                return VideosPage.newInstance();
            case 2:
                return PlaylistPage.newInstance();
            case 3:
                return ProfilePage.newInstance();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }
}

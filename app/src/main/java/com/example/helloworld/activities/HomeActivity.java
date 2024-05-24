package com.example.helloworld.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.example.helloworld.R;
import com.example.helloworld.adapters.ViewPagerAdapter;
import com.example.helloworld.utils.AppUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle = null;
    private NavigationView navigationView;
    private BottomNavigationView bottomNav;
    private ViewPager viewPager;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this);

        setContentView(R.layout.activity_home);
        drawerLayout = findViewById(R.id.drawer_layout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDefaultDisplayHomeAsUpEnabled(true);

            toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open_hint, R.string.drawer_close_hint) {
                public void onDrawerClosed(View view) {
                    supportInvalidateOptionsMenu();
                }
                public void onDrawerOpened(View drawerView) {
                    supportInvalidateOptionsMenu();
                }
            };
            toggle.setDrawerIndicatorEnabled(true);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            if(item.getItemId() == R.id.drawer_home){
                viewPager.setCurrentItem(0);
                bottomNav.setSelectedItemId(bottomNav.getMenu().getItem(0).getItemId());
            } else if(item.getItemId() == R.id.drawer_videos){
                viewPager.setCurrentItem(1);
                bottomNav.setSelectedItemId(bottomNav.getMenu().getItem(1).getItemId());
            } else if(item.getItemId() == R.id.drawer_playlist) {
                viewPager.setCurrentItem(2);
                bottomNav.setSelectedItemId(bottomNav.getMenu().getItem(2).getItemId());
            } else if(item.getItemId() == R.id.drawer_profile) {
                viewPager.setCurrentItem(3);
                bottomNav.setSelectedItemId(bottomNav.getMenu().getItem(3).getItemId());
            } else if(item.getItemId() == R.id.drawer_about){
                showAboutDialog();
            } else if(item.getItemId() == R.id.drawer_website){
                AppUtils.openLink(this,"https://graphesee.com");
            } else if(item.getItemId() == R.id.drawer_facebook){
                AppUtils.openFacebook(this, "https://www.facebook.com/graphsee");
            } else if(item.getItemId() == R.id.drawer_youtube){
                AppUtils.openYoutube(this, "https://www.youtube.com/@madebygoogle");
            } else if(item.getItemId() == R.id.drawer_share_link){
                AppUtils.shareLink(this, "https://play.google.com/store/apps/details?id=" + getPackageName());
            } else if(item.getItemId() == R.id.drawer_rate_us){
                AppUtils.rate(this);
            } else if(item.getItemId() == R.id.drawer_feedback){
                AppUtils.feedback(this);
            }
            drawerLayout.close();
            return true;
        });

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnApplyWindowInsetsListener(null);
        bottomNav.setOnItemSelectedListener(item -> {
            if(item.getItemId() == R.id.nav_home){
                viewPager.setCurrentItem(0);
                navigationView.getMenu().findItem(R.id.drawer_home).setChecked(true);
            } else if(item.getItemId() == R.id.nav_videos){
                viewPager.setCurrentItem(1);
                navigationView.getMenu().findItem(R.id.drawer_videos).setChecked(true);
            } else if(item.getItemId() == R.id.nav_playlist){
                viewPager.setCurrentItem(2);
                navigationView.getMenu().findItem(R.id.drawer_playlist).setChecked(true);
            } else if(item.getItemId() == R.id.nav_profile){
                viewPager.setCurrentItem(3);
                navigationView.getMenu().findItem(R.id.drawer_profile).setChecked(true);
            }
            return true;
        });

        viewPager = findViewById(R.id.view_pager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position == 0){
                    navigationView.getMenu().findItem(R.id.drawer_home).setChecked(true);
                } else if(position == 1){
                    navigationView.getMenu().findItem(R.id.drawer_videos).setChecked(true);
                } else if(position == 2){
                    navigationView.getMenu().findItem(R.id.drawer_playlist).setChecked(true);
                } else if(position == 3){
                    navigationView.getMenu().findItem(R.id.drawer_profile).setChecked(true);
                }
                bottomNav.setSelectedItemId(bottomNav.getMenu().getItem(position).getItemId());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPager.setCurrentItem(0);
        navigationView.getMenu().findItem(R.id.drawer_home).setChecked(true);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if(drawerLayout.isOpen()){
            drawerLayout.close();
        } else if(viewPager.getCurrentItem() != 0) {
            viewPager.setCurrentItem(0);
            navigationView.getMenu().findItem(R.id.drawer_home).setChecked(true);
            bottomNav.setSelectedItemId(bottomNav.getMenu().getItem(0).getItemId());
        } else {
            showExitDialog();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        interstitialAd.show(HomeActivity.this);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(toggle != null) {
            toggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (toggle != null) {
            toggle.onConfigurationChanged(newConfig);
        }
    }

    private void showAboutDialog(){
        final Dialog aboutDialog = new Dialog(this);
        aboutDialog.setContentView(R.layout.about_dialog);
        Button closeButton = aboutDialog.findViewById(R.id.about_dialog_close_btn);
        closeButton.setOnClickListener(v -> aboutDialog.dismiss());
        aboutDialog.show();
    }

    private void showExitDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.exit_dialog_title));
        builder.setMessage(getResources().getString(R.string.exit_dialog_descriptions));
        builder.setPositiveButton(getResources().getString(R.string.yes_btn), (dialog, which) -> finish());
        builder.setNegativeButton(getResources().getString(R.string.no_btn), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
/*
 * Copyright 2013 Yu AOKI
 */

package com.aokyu.dev.sample.network;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;

public class NetworkStateActivity extends Activity {

    private boolean mTransactionAllowed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.state_screen);

        mTransactionAllowed = true;
        showNetworkStateFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTransactionAllowed = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTransactionAllowed = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mTransactionAllowed = false;
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.network_state, menu);
        return true;
    }

    private void showNetworkStateFragment() {
        FragmentManager manager = getFragmentManager();

        NetworkStateFragment fragment = 
                (NetworkStateFragment) manager.findFragmentByTag(NetworkStateFragment.TAG);
        if (fragment == null) {
            fragment = NetworkStateFragment.newInstance();
        }

        showFragment(fragment);
    }

    private void showFragment(Fragment fragment) {
        if (!isFragmentTransactionAllowed()) {
            return;
        }

        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.container_view, fragment);
        transaction.commit();
    }

    public boolean isFragmentTransactionAllowed() {
        return mTransactionAllowed;
    }
}

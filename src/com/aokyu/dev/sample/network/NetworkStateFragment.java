/*
 * Copyright 2013 Yu AOKI
 */

package com.aokyu.dev.sample.network;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class NetworkStateFragment extends Fragment {

    /* package */ static final String TAG = NetworkStateFragment.class.getSimpleName();

    private ListView mStateView;
    private StateAdapter mAdapter;

    private Switch mWifiSwitch;
    private Switch mMobileSwitch;

    private Context mContext;
    private ConnectivityManager mConnectivityManager;
    private ConnectivityActionReceiver mReceiver;

    private WifiManager mWifiManager;

    public NetworkStateFragment() {}

    public static NetworkStateFragment newInstance() {
        NetworkStateFragment fragment = new NetworkStateFragment();
        return fragment;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity != null) {
            mContext = activity.getApplicationContext();
        }
    }

    private Context getContext() {
        return mContext;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        mReceiver = new ConnectivityActionReceiver(this);

        if (mContext != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mReceiver, filter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.state_panel, container, false);
        setupViews(contentView);
        return contentView;
    }

    private void setupViews(View rootView) {
        mStateView = (ListView) rootView.findViewById(R.id.state_view);
        mAdapter = new StateAdapter(mContext);
        mStateView.setAdapter(mAdapter);

        mWifiSwitch = (Switch) rootView.findViewById(R.id.wifi_switch);
        mWifiSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                mWifiManager.setWifiEnabled(isChecked);
            }
        });

        mMobileSwitch = (Switch) rootView.findViewById(R.id.mobile_switch);
        mMobileSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                try {
                    Class<?> clazz = Class.forName(ConnectivityManager.class.getName());
                    Method method = clazz.getMethod("setMobileDataEnabled", boolean.class);
                    method.invoke(mConnectivityManager, isChecked);
                    return;
                } catch (ClassNotFoundException e) {
                } catch (IllegalArgumentException e) {
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                } catch (NoSuchMethodException e) {
                }

                Toast.makeText(mContext, R.string.mobile_error_messasge, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWifiSwitch() {
        NetworkInfo info = getWifiInfo();
        boolean available = info.isAvailable();
        mWifiSwitch.setChecked(available);
    }

    private void updateMobileSwitch() {
        NetworkInfo info = getMobileInfo();
        boolean available = info.isAvailable();
        mMobileSwitch.setChecked(available);
    }

    private NetworkInfo getWifiInfo() {
        return mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

    private NetworkInfo getMobileInfo() {
        return mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateWifiSwitch();
        updateMobileSwitch();

        // Initialize.
        NetworkInfo[] info = mConnectivityManager.getAllNetworkInfo();
        mAdapter.set(info);
    }

    private void onConnectivityChanged(NetworkInfo[] info) {
        if (mAdapter != null) {
            updateWifiSwitch();
            updateMobileSwitch();
            mAdapter.set(info);
        }
    }

    private static final class ConnectivityActionReceiver extends BroadcastReceiver {

        private WeakReference<NetworkStateFragment> mFragment;
        private ConnectivityManager mManager;

        public ConnectivityActionReceiver(NetworkStateFragment fragment) {
            mFragment = new WeakReference<NetworkStateFragment>(fragment);
            Context context = fragment.getContext();
            mManager = (ConnectivityManager) context.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo[] info = mManager.getAllNetworkInfo();
            dispatchConnectivityChanged(info);
        }

        private void dispatchConnectivityChanged(NetworkInfo[] info) {
            NetworkStateFragment fragment = mFragment.get();
            if (fragment != null) {
                fragment.onConnectivityChanged(info);
            }
        }
    }

    private static final class NetworkInfoList extends ArrayList<NetworkInfo> {

        private static final long serialVersionUID = -4337598532395589575L;

        public NetworkInfoList(NetworkInfo[] info) {
            super(Arrays.asList(info));
        }

        public boolean addAll(NetworkInfo[] info) {
            return super.addAll(Arrays.asList(info));
        }
    }

    private static final class StateAdapter extends BaseAdapter {

        private NetworkInfoList mList;
        private LayoutInflater mInflater;

        private static final String NO_STATE = "-----";
        private static final String NO_EXTRA = "-----";
        private static final String NO_REASON = "-----";

        private static final class ViewCache {
            public final TextView typeView;
            public final TextView subtypeView;
            public final TextView stateView;
            public final TextView detailedStateView;
            public final TextView extraView;
            public final TextView reasonView;
            public final TextView availabilityView;
            public final TextView connectedView;
            public final TextView connectingView;
            public final TextView failoverView;
            public final TextView roamingView;

            public ViewCache(View rootView) {
                stateView = (TextView) rootView.findViewById(R.id.state_view);
                detailedStateView = (TextView) rootView.findViewById(R.id.detailed_state_view);
                typeView = (TextView) rootView.findViewById(R.id.type_view);
                subtypeView = (TextView) rootView.findViewById(R.id.subtype_view);
                extraView = (TextView) rootView.findViewById(R.id.extra_view);
                reasonView = (TextView) rootView.findViewById(R.id.reason_view);
                availabilityView = (TextView) rootView.findViewById(R.id.availability_view);
                connectedView = (TextView) rootView.findViewById(R.id.connected_view);
                connectingView = (TextView) rootView.findViewById(R.id.connecting_view);
                failoverView = (TextView) rootView.findViewById(R.id.failover_view);
                roamingView = (TextView) rootView.findViewById(R.id.roaming_view);
            }
        }

        public StateAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }

        /* package */ void set(NetworkInfo[] info) {
            if (info == null) {
                mList = null;
                notifyDataSetChanged();
                return;
            }

            if (mList != null) {
                mList.clear();
                mList.addAll(info);
            } else {
                mList = new NetworkInfoList(info);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mList != null) {
                int size = mList.size();
                return size;
            } else {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            if (mList != null) {
                NetworkInfo info = mList.get(position);
                return info;
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = newView(position, parent);
            }

            ViewCache cache = (ViewCache) view.getTag();
            bindView(position, cache);

            return view;
        }

        private View newView(int position, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.state_list_item, parent, false);
            ViewCache cache = new ViewCache(view);
            view.setTag(cache);
            return view;
        }

        private void bindView(int position, ViewCache cache) {
            NetworkInfo info = mList.get(position);

            NetworkInfo.State state = info.getState();
            if (state != null) {
                cache.stateView.setText(state.toString());
            } else {
                cache.stateView.setText(NO_STATE);
            }

            NetworkInfo.DetailedState detailedState = info.getDetailedState();
            if (detailedState != null) {
                cache.detailedStateView.setText(detailedState.toString());
            } else {
                cache.detailedStateView.setText(NO_STATE);
            }

            String typeName = info.getTypeName();
            cache.typeView.setText(typeName);

            String subtypeName = info.getSubtypeName();
            cache.subtypeView.setText(subtypeName);

            String extra = info.getExtraInfo();
            if (extra != null) {
                cache.extraView.setText(extra);
            } else {
                cache.extraView.setText(NO_EXTRA);
            }

            String reason = info.getReason();
            if (reason != null) {
                cache.reasonView.setText(reason);
            } else {
                cache.reasonView.setText(NO_REASON);
            }

            boolean availability = info.isAvailable();
            cache.availabilityView.setText(String.valueOf(availability));

            boolean connected = info.isConnected();
            cache.connectedView.setText(String.valueOf(connected));

            boolean connecting = info.isConnectedOrConnecting();
            cache.connectingView.setText(String.valueOf(connecting));

            boolean failover = info.isFailover();
            cache.failoverView.setText(String.valueOf(failover));

            boolean roaming = info.isRoaming();
            cache.roamingView.setText(String.valueOf(roaming));
        }
    }
}

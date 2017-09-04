/*
 * Copyright (c) 2014. Pokevian Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pokevian.app.smartfleet.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.pokevian.app.smartfleet.BuildConfig;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.util.ImageUtils;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

import java.util.ArrayList;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static final String STATE_PREVIOUS_POSITION = "previous_navigation_drawer_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
    private int mPreviousPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;
    private int mBadgeIndex = -1;


    public static final int DRAWER_MENU_LOG_OUT = 1000;
    public static final int DRAWER_MENU_USER_INFO = 1100;
    public static final int DRAWER_MENU_GOOLE_PLUS_SELECTED = 1001;
    public static final int DRAWER_MENU_FACEBOOK_SELECTED = 1002;
    public static final int DRAWER_MENU_KAKAO_TALK_SELECTED = 1003;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mPreviousPosition = savedInstanceState.getInt(STATE_PREVIOUS_POSITION);
            mFromSavedInstanceState = true;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);

        SettingsStore settingsStore = SettingsStore.getInstance();

        view.findViewById(R.id.user_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallbacks != null) {
                    mCallbacks.onNavigationDrawerItemSelected(DRAWER_MENU_USER_INFO);
                }
            }
        });

        view.findViewById(R.id.btn_gp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallbacks != null) {
                    mCallbacks.onNavigationDrawerItemSelected(DRAWER_MENU_GOOLE_PLUS_SELECTED);
                }
            }
        });

        view.findViewById(R.id.btn_fb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallbacks != null) {
                    mCallbacks.onNavigationDrawerItemSelected(DRAWER_MENU_FACEBOOK_SELECTED);
                }
            }
        });


        String accountEmail = settingsStore.getLoginId();
        TextView emailText = (TextView) view.findViewById(R.id.account_email);
        emailText.setText(accountEmail);

        String accountName = settingsStore.getAccountName();
        TextView nameText = (TextView) view.findViewById(R.id.account_name);
        nameText.setText(accountName);

        mDrawerListView = (ListView) view.findViewById(R.id.drawer_list);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        String[] menuTitles = getResources().getStringArray(R.array.menu_items);
        TypedArray menuIcons = getResources().obtainTypedArray(R.array.menu_icons);
        ArrayList<DrawerMenuItem> menuItems = new ArrayList<>();

        for (int i = 0; i < menuTitles.length; i++) {
            menuItems.add(new DrawerMenuItem(menuTitles[i], menuIcons.getResourceId(i, -1)));
        }

        menuIcons.recycle();

        MenuAdapter adapter = new MenuAdapter(getActivity(), menuItems);

        mDrawerListView.setAdapter(adapter);
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);

        PackageInfo pkgInfo = getPackageInfo();
        if (pkgInfo != null) {
            ((TextView) view.findViewById(R.id.version)).setText(String.format("v%s", pkgInfo.versionName));
        }

        if (savedInstanceState != null) {
            mBadgeIndex = savedInstanceState.getInt("badge-index");
        }
        if (mBadgeIndex >= 0) {
            updateBadge(view.findViewById(R.id.account_image));
        }

        if (!BuildConfig.INCLUDE_USER_CHANNEL) {
            view.findViewById(R.id.ll_channel).setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        getView().removeCallbacks(mLoadAccountImageRunnable);

        super.onDestroyView();
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    public void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                0,  /* "open drawer" description for accessibility */
                0  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()

                // Show selected item title
                if (mDrawerListView != null) {
                    DrawerMenuItem item = (DrawerMenuItem) mDrawerListView.getItemAtPosition(mCurrentSelectedPosition);
                    getActionBar().setTitle(item.title);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()

                // Show app name
                getActionBar().setTitle(R.string.app_name);
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);

            // only once
            mUserLearnedDrawer = true;
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // load account image
//        mLoadAccountImageRunnable.run();
    }

    protected void updateAccountInfo() {
        TextView tv = (TextView) getView().findViewById(R.id.account_name);
        String accountName = SettingsStore.getInstance().getAccountName();
        if (!TextUtils.isEmpty(accountName)) {
            tv.setText(accountName);
        }
    }

    private Runnable mLoadAccountImageRunnable = new Runnable() {
        public void run() {
            if (isDetached() || isRemoving() || !isAdded()) return;

            String url = SettingsStore.getInstance().getAccountImageUrl();
            if (!TextUtils.isEmpty(url)) {
                final int s = (int) dp2px(42);

                RequestQueue requestQueue = VolleySingleton.getInstance().getRequestQueue();
                requestQueue.add(
                        new ImageRequest(url,
                                new Response.Listener<Bitmap>() {
                                    @Override
                                    public void onResponse(Bitmap image) {
                                        Bitmap scaled = Bitmap.createScaledBitmap(image, s, s, true);
                                        Bitmap masked = ImageUtils.masking(getResources(),
                                                new BitmapDrawable(getResources(), scaled),
                                                R.drawable.ic_account_image_mask);

                                        ImageView accountImg = (ImageView) getView().findViewById(R.id.account_image);
                                        accountImg.setImageBitmap(masked);
                                    }
                                },
                                s, s, null,
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        if (isDetached() || isRemoving() || !isAdded()) return;

                                        getView().postDelayed(mLoadAccountImageRunnable, 5000);
                                    }
                                }));
            }
        }
    };

    public void selectPreviousSegment() {
        selectItem(mPreviousPosition);
    }

    private void selectItem(int position) {
        mPreviousPosition = mCurrentSelectedPosition;
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);

            // Show selected item title
            DrawerMenuItem item = (DrawerMenuItem) mDrawerListView.getItemAtPosition(position);
            getActionBar().setTitle(item.title);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
        outState.putInt("badge-index", mBadgeIndex);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mDrawerLayout != null && !isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);

            if (BuildConfig.DEBUG) {
            } else {
                MenuItem item = menu.findItem(R.id.action_log_report);
                item.setVisible(false);
            }

            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void notifyDataSetChanged() {
        if (mDrawerListView != null) {
            mDrawerListView.post(new Runnable() {
                @Override
                public void run() {
                    ((MenuAdapter) mDrawerListView.getAdapter()).notifyDataSetChanged();
                }
            });
        }
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }

    private ActionBar getActionBar() {
        return ((BaseActivity) getActivity()).getSupportActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int position);
    }

    private PackageInfo getPackageInfo() {
        PackageInfo pkgInfo = null;
        try {
            PackageManager pm = getActivity().getPackageManager();
            pkgInfo = pm.getPackageInfo(getActivity().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
        return pkgInfo;
    }

    protected void updateBadge(int index) {
        TypedArray badge = getResources().obtainTypedArray(R.array.badge_green);
        int length = badge.length() - 1;
        mBadgeIndex = index > length ? length : index;
        ((ImageView) mFragmentContainerView.findViewById(R.id.account_image)).setImageResource(badge.getResourceId(mBadgeIndex, 0));
        badge.recycle();
    }

    private void updateBadge(View view) {
        TypedArray badge = getResources().obtainTypedArray(R.array.badge_green);
        ((ImageView) view).setImageResource(badge.getResourceId(mBadgeIndex, 0));
        badge.recycle();
    }


    class DrawerMenuItem {

        public int icon;
        public String title;

        DrawerMenuItem() {
        }

        DrawerMenuItem(String title, int icon) {
            this.title = title;
            this.icon = icon;
        }

    }

    public class MenuAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;
        private ArrayList<DrawerMenuItem> mItems;

        public MenuAdapter(Context context, ArrayList<DrawerMenuItem> items) {
            mInflater = LayoutInflater.from(context);
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.menu_item, null);
            }

            ImageView menuIcon = (ImageView) convertView.findViewById(R.id.row_icon);
            menuIcon.setImageResource(mItems.get(position).icon);

            TextView menuTitle = (TextView) convertView.findViewById(R.id.row_title);
            menuTitle.setText(mItems.get(position).title);

            if (position == 0) {
                boolean hasNoti = SettingsStore.getInstance().hasNewNoti();
                convertView.findViewById(R.id.row_badge).setVisibility(hasNoti ? View.VISIBLE : View.GONE);
            }

            return convertView;
        }

    }


    int dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

}

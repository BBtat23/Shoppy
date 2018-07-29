package com.example.bea.shoppy;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.bea.shoppy.analytics.AnalyticsApplication;
import com.example.bea.shoppy.data.ShoppyContract;
import com.example.bea.shoppy.widget.WidgetProvider;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor> {
    private GoogleSignInClient mGoogleSignInClient;
    /**
     * Identifier for the food data loader
     */
    private static final int FOOD_LOADER = 0;

    /**
     * Adapter for the ListView
     */
    ShoppyCursorAdapter mCursorAdapter;

    private static Context mContext;
    private ArrayList<String> mFoodList = new ArrayList<>();
    private ListView mListView;
    private ShoppyCursorAdapter mAdapter;
    Cursor mCursor;
    private static Tracker mTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplicationContext();
        mTracker = application.getDefaultTracker();

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ShoppingListActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });
        // Find the ListView which will be populated with the food data
        ListView foodListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        foodListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of food data in the Cursor.
        // There is no food data yet (until the loader finishes) so pass in null for the Cursor.
        mCursorAdapter = new ShoppyCursorAdapter(this, null);
        foodListView.setAdapter(mCursorAdapter);

        // Setup the item click listener
        foodListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create new intent to go to {@link EditorActivity}
                Intent intent = new Intent(ShoppingListActivity.this, EditorActivity.class);

                // Form the content URI that represents the specific food that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link ShoppyEntry#CONTENT_URI}.
                // For example, the URI would be "content://com.example.android.shoppy/shoppy/2"
                // if the shoppy with ID 2 was clicked on.
                Uri currentFoodUri = ContentUris.withAppendedId(ShoppyContract.ShoppyEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentFoodUri);

                // Launch the {@link EditorActivity} to display the data for the current food.
                startActivity(intent);
            }
        });

        mContext = this;
        mListView = (ListView) findViewById(R.id.widgetListView);
        getFoodList();

        // Kick off the loader
        getLoaderManager().initLoader(FOOD_LOADER, null, this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // [START configure_signin]
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        // [END configure_signin]
        // [START build_client]
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // [END build_client]
    }

    public void addTodoItem(final String s) {

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                ContentValues values = new ContentValues();
                values.put(ShoppyContract.ShoppyEntry.COL_TODO_TEXT, s);

                final Uri uri = mContext.getContentResolver().insert(ShoppyContract.ShoppyEntry.CONTENT_URI, values);

                ShoppingListActivity a = (ShoppingListActivity) mContext;
                if(uri != null) {
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "New task created", Toast.LENGTH_LONG).show();
                            getFoodList();
                            // this will send the broadcast to update the appwidget
                            WidgetProvider.sendRefreshBroadcast(mContext);
                        }
                    });
                } else {
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Something went wrong, task cannot be created.", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                return null;

            }
        }.execute();
    }

    private void setTodoItems(ArrayList items) {
        mFoodList = items;
    }

    public void getFoodList() {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                Cursor cursor = null;

                try {

                    cursor = mContext.getContentResolver().query(
                            ShoppyContract.ShoppyEntry.CONTENT_URI,
                            null,
                            null,
                            null,
                            ShoppyContract.ShoppyEntry._ID + " DESC"
                    );
                }
                catch (Exception e) {

                }


                final ArrayList<String> items = new ArrayList<>();
                while(cursor.moveToNext()) {
                    String item = String.valueOf(cursor.getColumnIndex("name"));
                    items.add(item);
                }

                cursor.close();

                ShoppingListActivity a = (ShoppingListActivity) mContext;
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTodoItems(items);
                    }
                });


                return null;

            }
        }.execute();


    }


    /**
     * Helper method to insert hardcoded food data into the database. For debugging purposes only.
     */
    private void insertFood() {
        // Create a ContentValues object where column names are the keys,
        // and Melon food attributes are the values.
        ContentValues values = new ContentValues();
        values.put(ShoppyContract.ShoppyEntry.COLUMN_FOOD_NAME, "Melon");

        // Insert a new row for Melon into the provider using the ContentResolver.
        // Use the {@link ShoppyEntry#CONTENT_URI} to indicate that we want to insert
        // into the food database table.
        // Receive the new content URI that will allow us to insert Melon data.
        Uri newUri = getContentResolver().insert(ShoppyContract.ShoppyEntry.CONTENT_URI, values);
    }

    /**
     * Helper method to delete all food in the database.
     */
    private void deleteAllFood() {
        int rowsDeleted = getContentResolver().delete(ShoppyContract.ShoppyEntry.CONTENT_URI, null, null);
        Log.v("CatalogActivity", rowsDeleted + " rows deleted from food database");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertFood();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllFood();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            signOut();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // [START signOut]
    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // [START_EXCLUDE]
                        updateUI(null);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END signOut]
    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account == null) {
            Intent signInToMain = new Intent(this, SignInActivity.class);
            startActivity(signInToMain);
        } else {
            Toast.makeText(this,"Logout fails!!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                ShoppyContract.ShoppyEntry._ID,
                ShoppyContract.ShoppyEntry.COLUMN_FOOD_NAME};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                ShoppyContract.ShoppyEntry.CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link ShoppyCursorAdapter} with this new cursor containing updated food data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }
}


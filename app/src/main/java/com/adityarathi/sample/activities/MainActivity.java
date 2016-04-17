package com.adityarathi.sample.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.JsonReader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.adityarathi.fastscroll.views.FastScrollRecyclerView;
import com.adityarathi.sample.R;
import com.adityarathi.sample.RecyclerItemClickListener;
import com.adityarathi.sample.adapters.SearchListAdapter;
import com.adityarathi.sample.objects.UserObject;
import com.rey.material.widget.ProgressView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static final String RESULTS_FETCH_MEMBERS_URL = "http://demo.codeofaninja.com/tutorials/json-example-with-php/index.php";

    private static final String TAG_RESULTS = "Users";
    private static final String TAG_FIELD_1 = "firstname";
    private static final String TAG_FIELD_2 = "lastname";
    private static final String TAG_FIELD_3 = "username";

    @Bind(R.id.recycler_view) FastScrollRecyclerView mRecyclerView;

    private SearchListAdapter mAdapter;
    private List<UserObject> mModels;

    private LinkedHashMap<UserObject, String> searchableStringDataMap;
    private ArrayList<UserObject> userObjectArrayList = new ArrayList<>();

    public ProgressDialog progressDialog;

    private String listJsonString;

    private boolean noNetworkFlag = false;

    private JsonReader jsonReader;
    private final int loadingFrequency = 5;
    private boolean allLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //initializing recycler view with no data in adapter
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mModels = new ArrayList<>();
        mAdapter = new SearchListAdapter(MainActivity.this, mModels, 0, !allLoaded);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.scrollToPosition(0);
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(position < mAdapter.getContentItemCount())
                    Toast.makeText(MainActivity.this,"Tapped on "+mAdapter.getItem(position).getFirst_name(),Toast.LENGTH_SHORT).show();
                else{
                    loadMore();
                }
            }
        }));
        //recycler initialized

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading...");
        progressDialog.setCancelable(false);

        getDataFromURL();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.menu_1:
                setupRecyclerView(0);
                break;
            case R.id.menu_2:
                setupRecyclerView(1);
                break;
            case R.id.menu_3:
                setupRecyclerView(2);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        final List<UserObject> filteredModelList = filter(mModels, query);
        mAdapter.animateTo(filteredModelList);
        mRecyclerView.scrollToPosition(0);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private List<UserObject> filter(List<UserObject> models, String query) {
        query = query.toLowerCase();

        final List<UserObject> filteredModelList = new ArrayList<>();
        for (UserObject model : models) {
            final String text = model.getFirst_name().toLowerCase()+" "
                    +model.getLast_name().toLowerCase()+" "
                    +model.getUser_name().toLowerCase();
            if (text.contains(query)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }

    public void getDataFromURL() {
        class GetData extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... params) {

                URL obj = null;
                String result = null;
                InputStream inputStream = null;
                try {
                    obj = new URL(RESULTS_FETCH_MEMBERS_URL);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    //add request header
                    con.setRequestProperty("Content-Type", "application/json");
                    inputStream = con.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 16);
                    jsonReader = new JsonReader(bufferedReader);

                    jsonReader.beginObject();
                    if(jsonReader.nextName().equals(TAG_RESULTS)){
                        jsonReader.beginArray();
                        int k=0;
                        while (jsonReader.hasNext()&& k < loadingFrequency){
                            userObjectArrayList.add(getUserObject(jsonReader));
                            ++k;
                        }
                    }

                } catch (Exception e) {
                } finally {
                    try {
                        if (inputStream != null) inputStream.close();
                    } catch (Exception squish) {
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                progressDialog.dismiss();
                if(!noNetworkFlag)
                    setupRecyclerView(0);
                else
                    Toast.makeText(MainActivity.this, "Check Internet connection and try again.", Toast.LENGTH_LONG).show();


            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setTitle("Loading...");
                progressDialog.show();
                if (!isNetworkAvailable())
                    noNetworkFlag = true;

            }
        }
        GetData g = new GetData();
        g.execute();
    }

    private void loadMore() {

        class LoadMoreData extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... params) {

                URL obj = null;
                String result = null;

                try {
                    int k=0;
                    while (jsonReader.hasNext()&& k < loadingFrequency){
                        userObjectArrayList.add(getUserObject(jsonReader));
                        ++k;
                    }

                    if(!jsonReader.hasNext()){
                        allLoaded = true;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                if(!noNetworkFlag)
                    setupRecyclerView(0);

            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (!isNetworkAvailable())
                    noNetworkFlag = true;
                else
                    noNetworkFlag = false;

            }
        }

        LoadMoreData g = new LoadMoreData();
        g.execute();

    }

    public UserObject getUserObject(JsonReader reader) throws IOException {
        String firstName = "", lastName = "", userName = "";

        reader.beginObject();
        while (reader.hasNext()) {
            String s = reader.nextName();
            switch (s){
                case TAG_FIELD_1:
                    firstName = reader.nextString();
                    break;
                case TAG_FIELD_2:
                    lastName = reader.nextString();
                    break;
                case TAG_FIELD_3:
                    userName = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new UserObject(firstName, lastName, userName);
    }

    private void setupRecyclerView(int filterType) {

        mModels = new ArrayList<>();

        searchableStringDataMap = new LinkedHashMap<>();

        for (UserObject userObject : userObjectArrayList) {
            String s = "";
            switch (filterType){
                case 0:
                    s = userObject.getFirst_name();
                    break;
                case 1:
                    s = userObject.getLast_name();
                    break;
                case 2:
                    s = userObject.getUser_name();
                    break;
            }
            searchableStringDataMap.put(userObject, s);
        }

        searchableStringDataMap = sortHashMapByValuesD(searchableStringDataMap);
        final ArrayList<UserObject> mapKeys = new ArrayList(searchableStringDataMap.keySet());
        int k = 0;
        for (String s : searchableStringDataMap.values()){
            mModels.add(new UserObject(mapKeys.get(k).getFirst_name(), mapKeys.get(k).getLast_name(), mapKeys.get(k).getUser_name()));
            ++k;
        }

        mAdapter = new SearchListAdapter(MainActivity.this, mModels, filterType, !allLoaded);
        mRecyclerView.setAdapter(mAdapter);

    }

    public LinkedHashMap sortHashMapByValuesD(HashMap passedMap) {
        List mapKeys = new ArrayList(passedMap.keySet());
        List mapValues = new ArrayList(passedMap.values());
        Collections.sort(mapValues, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        LinkedHashMap sortedMap = new LinkedHashMap();

        Iterator valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Object val = valueIt.next();
            Iterator keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                Object key = keyIt.next();
                String comp1 = passedMap.get(key).toString();
                String comp2 = val.toString();

                if (comp1.equals(comp2)) {
                    passedMap.remove(key);
                    mapKeys.remove(key);
                    sortedMap.put(key, val);
                    break;
                }

            }

        }
        return sortedMap;
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


}



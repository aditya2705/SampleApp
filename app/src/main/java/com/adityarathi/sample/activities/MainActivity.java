package com.adityarathi.sample.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.JsonReader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.adityarathi.sample.R;
import com.adityarathi.sample.RecyclerItemClickListener;
import com.adityarathi.sample.adapters.UserRecyclerViewAdapter;
import com.adityarathi.sample.objects.UserObject;

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
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static final String RESULTS_FETCH_MEMBERS_URL = "http://demo.codeofaninja.com/tutorials/json-example-with-php/index.php";

    private static final String TAG_RESULTS = "Users";
    private static final String TAG_FIELD_1 = "firstname";
    private static final String TAG_FIELD_2 = "lastname";
    private static final String TAG_FIELD_3 = "username";

    private static final int FILTER_BY_FIRST_NAME = 0;
    private static final int FILTER_BY_LAST_NAME = 1;
    private static final int FILTER_BY_USER_NAME = 2;

    private static final int LOADING_FREQUENCY = 5;

    private static final int CONNECTION_TIMEOUT = 20000;

    @Bind(R.id.recycler_view) RecyclerView mRecyclerView;
    @Bind(R.id.btn_retry) AppCompatButton retryButton;
    @Bind(R.id.btn_next) AppCompatButton nextButton;
    @Bind(R.id.btn_previous) AppCompatButton previousButton;

    private UserRecyclerViewAdapter mAdapter;
    private List<UserObject> mModels;

    private LinkedHashMap<UserObject, String> searchableStringDataMap;
    private ArrayList<UserObject> userObjectArrayList = new ArrayList<>();
    private ArrayList<UserObject> displayArrayList = new ArrayList<>();

    public ProgressDialog progressDialog;

    private boolean noNetworkFlag = false;

    private JsonReader jsonReader;
    private boolean allLoaded = false;
    private int currentPage = 0;
    private int lastPagePointer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //initializing recycler view with no data in adapter
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(MainActivity.this,mAdapter.getItem(position).getFirst_name()+
                        " "+mAdapter.getItem(position).getLast_name(),Toast.LENGTH_SHORT).show();
            }
        }));

        //progress dialog instantiated
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading...");
        progressDialog.setCancelable(false);

        //fetch from URL directly on activity launch
        getDataFromURL();

    }

    @OnClick(R.id.btn_next)
    public void onNextClick() {

        if(!allLoaded) {
            ++currentPage;
            loadMore();
        }else if(currentPage<lastPagePointer){
            displayArrayList = new ArrayList<>();
            int startPointer = (currentPage+1)*LOADING_FREQUENCY;
            int k=0;
            while (k<LOADING_FREQUENCY && startPointer<userObjectArrayList.size()){
                displayArrayList.add(userObjectArrayList.get(startPointer++));
                ++k;
            }
            setupRecyclerView(FILTER_BY_FIRST_NAME);
            ++currentPage;
        }
    }

    @OnClick(R.id.btn_previous)
    public void onPreviousClick() {
        if(currentPage>0) {
            displayArrayList = new ArrayList<>();
            int startPointer = (currentPage-1)*LOADING_FREQUENCY;
            int k=0;
            while (k<LOADING_FREQUENCY && startPointer<userObjectArrayList.size()){
                displayArrayList.add(userObjectArrayList.get(startPointer++));
                ++k;
            }
            setupRecyclerView(FILTER_BY_FIRST_NAME);
            --currentPage;
        }
    }

    @OnClick(R.id.btn_retry)
    public void onRetryClick(){
        retryButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.VISIBLE);
        previousButton.setVisibility(View.VISIBLE);
        if(jsonReader!=null)
            loadMore();
        else
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
                setupRecyclerView(FILTER_BY_FIRST_NAME);
                break;
            case R.id.menu_2:
                setupRecyclerView(FILTER_BY_LAST_NAME);
                break;
            case R.id.menu_3:
                setupRecyclerView(FILTER_BY_USER_NAME);
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
                    con.setConnectTimeout(CONNECTION_TIMEOUT);
                    inputStream = con.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 16);

                    //json reader for controlling loading
                    jsonReader = new JsonReader(bufferedReader);

                    jsonReader.beginObject();
                    if(jsonReader.nextName().equals(TAG_RESULTS)){
                        jsonReader.beginArray();
                        int k=0;
                        while (jsonReader.hasNext()&& k < LOADING_FREQUENCY){
                            displayArrayList.add(getUserObject(jsonReader));
                            ++k;
                        }
                    }

                }catch (Exception e) {


                }finally {
                    try {
                        if (inputStream != null) inputStream.close();
                    } catch (Exception squish) {}
                }
                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                progressDialog.dismiss();
                if(!noNetworkFlag)
                    setupRecyclerView(FILTER_BY_FIRST_NAME);
                else
                    Toast.makeText(MainActivity.this, "Check Internet connection and try again.", Toast.LENGTH_LONG).show();

                if(displayArrayList.size()!=0)
                    userObjectArrayList.addAll(displayArrayList);
                else {
                    retryButton.setVisibility(View.VISIBLE);
                    nextButton.setVisibility(View.INVISIBLE);
                    previousButton.setVisibility(View.INVISIBLE);
                }


            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (!isNetworkAvailable())
                    noNetworkFlag = true;
                else {
                    progressDialog.setTitle("Loading...");
                    progressDialog.show();
                }

            }
        }
        GetData g = new GetData();
        g.execute();
    }

    private void loadMore() {

        class LoadMoreData extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... params) {

                displayArrayList = new ArrayList<>();
                String result = null;

                try {
                    int k=0;
                    while (jsonReader.hasNext()&& k < LOADING_FREQUENCY){
                        displayArrayList.add(getUserObject(jsonReader));
                        ++k;
                    }

                    if(!jsonReader.hasNext()){
                        allLoaded = true;
                        lastPagePointer = currentPage;
                    }

                }catch (Exception e) {

                }

                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                progressDialog.dismiss();
                if(!noNetworkFlag)
                    setupRecyclerView(FILTER_BY_FIRST_NAME);

                if(displayArrayList.size()!=0)
                    userObjectArrayList.addAll(displayArrayList);

            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (!isNetworkAvailable())
                    noNetworkFlag = true;
                else {
                    noNetworkFlag = false;
                    progressDialog.setTitle("Loading...");
                    progressDialog.show();
                }

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

        for (UserObject userObject : displayArrayList) {
            String s = "";
            switch (filterType){
                case FILTER_BY_FIRST_NAME:
                    s = userObject.getFirst_name();
                    break;
                case FILTER_BY_LAST_NAME:
                    s = userObject.getLast_name();
                    break;
                case FILTER_BY_USER_NAME:
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

        mAdapter = new UserRecyclerViewAdapter(MainActivity.this, mModels);
        mRecyclerView.setAdapter(mAdapter);

    }

    //sorting the user objects linkedhashmap
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

    //simple checking for network
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


}



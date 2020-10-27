package com.nsc.covidscore;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.graphics.Color;

import com.android.volley.RequestQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.nsc.covidscore.api.RequestSingleton;
import com.nsc.covidscore.api.Requests;
import com.nsc.covidscore.api.VolleyJsonCallback;
import com.nsc.covidscore.room.CovidSnapshot;
import com.nsc.covidscore.room.CovidSnapshotWithLocationViewModel;
import com.nsc.covidscore.room.Location;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private Location currentLocation;
    private LiveData<Location> liveLocation;
    private List<Location> savedLocations = new ArrayList<>();
    // so that we only set one observer on Room Location
    private boolean roomLocationObserved = false;
    private LiveData<CovidSnapshot> liveCovidSnapshot;
    private CovidSnapshot currentSnapshot;
    // so that we only set one observer on Room CovidSnapshot
    private boolean roomCovidSnapshotObserved = false;

    private CovidSnapshotWithLocationViewModel vm;
    private RequestQueue queue;
    private RequestSingleton requestManager;

    private TextView tempDisplayTextView;
    public LineChart riskTrends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestManager = RequestSingleton.getInstance(this.getApplicationContext());
        queue = requestManager.getRequestQueue();
        tempDisplayTextView = findViewById(R.id.hello_world);

        //Draws graph for risks vs. group size relationship
        riskTrends = findViewById(R.id.lineGraph);
        LineDataSet riskDataSet1 = new LineDataSet(dataSet1(),"Risk vs. Group size");
        ArrayList<ILineDataSet> riskTrendDataSet = new ArrayList<>();
        riskTrendDataSet.add(riskDataSet1);
        LineData riskData = new LineData(riskTrendDataSet);


        riskDataSet1.setCircleColor(Color.BLACK);
        riskDataSet1.setAxisDependency(YAxis.AxisDependency.LEFT);

        riskTrends.setData(riskData);
        XAxis xAxis = riskTrends.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        riskTrends.getDescription().setEnabled(true);
        Description description = new Description();
        description.setText("Group Size vs. Risk at your location");
        description.setTextSize(15f);
        description.setPosition(0f,0f);
        riskTrends.invalidate();

        // Access to Room Database
        vm = new ViewModelProvider(this).get(CovidSnapshotWithLocationViewModel.class);

        // Set Room Data to local variables, if saved
        currentSnapshot = vm.getSavedCovidSnapshot();
        currentLocation = vm.getSavedLocation();

        // These variables will hold latest copies of Room rows
        liveCovidSnapshot = vm.getLatestCovidSnapshot();
        liveLocation = vm.getLatestLocation();

        // These functions will set observers on those, in case they change
        setRoomCovidSnapshotObserved();
        setRoomLocationObserved();

        // Attempts to save CovidSnapshot to DB whenever the local variable is changed
        // - if the fields aren't fully set, it will not insert
        if (currentSnapshot == null) {
            currentSnapshot = new CovidSnapshot();
        }
        currentSnapshot.setListener(e -> {
            if (currentSnapshot.hasFieldsSet()) {
                saveSnapshotToRoom();
            }
        });

        // temp test data - remove
        Location tempLocation = new Location("king", "washington");
        currentLocation = tempLocation;

        if (currentLocation == null) {
            // there is no previously saved location
            // TODO: pop up dialog here?
        }

        // Attempts to save Location to DB whenever the local variable is changed
        // - if the fields aren't fully set, it will not insert
        if (currentLocation.hasFieldsSet()) {
            saveLocationToRoom();
        }
        currentLocation.setListener(e -> {
            saveLocationToRoom();
        });

        makeApiCalls(tempLocation);

        Log.d(TAG,"onCreate invoked");
    }

    //Array of dummy data to be deleted after we can plug in the real data
    private ArrayList<Entry> dataSet1()
    {
        ArrayList<Entry> riskVals = new ArrayList<Entry>();

        riskVals.add(new Entry(10f, 1f));
        riskVals.add(new Entry(100f,  10f));
        riskVals.add(new Entry(1f,   0.1f));

        return riskVals;
    };


    @Override
    protected void onStop () {
        super.onStop();
        if (queue != null) {
            requestManager.getRequestQueue().cancelAll(TAG);
        }
        Log.d(TAG, "onStop invoked");
    }

    /**
     * This sets an observer on the Room function that returns the most recent addition to the db
     * When the new CovidSnapshot comes through, save to local variable and display to user
     */
    private void setRoomCovidSnapshotObserved() {
        // Set Listener for Current Covid Snapshot - Add Else - Dialog
        if (!roomCovidSnapshotObserved && liveCovidSnapshot != null) {
            liveCovidSnapshot.observe(this, new Observer<CovidSnapshot>() {
                @Override
                public void onChanged(@Nullable final CovidSnapshot covidSnapshotFromDb) {
                    // update cached version of snapshot
                    currentSnapshot = liveCovidSnapshot.getValue() != null ? liveCovidSnapshot.getValue() : currentSnapshot;
                    if (covidSnapshotFromDb != null || (currentSnapshot != null && currentSnapshot.hasFieldsSet())) {
                        currentSnapshot = covidSnapshotFromDb == null ? covidSnapshotFromDb : currentSnapshot;
                        // TODO: set textfields here! - vv this is temporary vv
                        if (currentLocation == null) { // this shouldn't be hit because currentLocation shouldn't be null
                            currentLocation = liveLocation.getValue();
                            //tempDisplayTextView.setText("Most Recent Snapshot:\n" + currentSnapshot.toString());
                        }// else {
                            //tempDisplayTextView.setText("Most Recent Location: id: \n" + currentLocation.getLocationId() + ", " + currentLocation.toApiFormat()
                                    //+ "\nMost Recent Snapshot: \n" + currentSnapshot.toString());
                        //}
                        Log.e(TAG, "CovidSnapshot Room listener invoked");
                    }
                    else if (covidSnapshotFromDb == null) {
                        Log.e(TAG, "Observer returned null CovidSnapshot");
                        // run API call, if location is saved
                    }
                }
            });
            roomCovidSnapshotObserved = true;
        }
    }

    /**
     * This sets an observer on the Room function that returns the most recent addition to the db
     * When the new Location comes through, ?
     */
    private void setRoomLocationObserved() {
        // Set Listener for Location
        if (!roomLocationObserved && liveLocation != null) {
            liveLocation.observe(this, new Observer<Location>() {
                @Override
                public void onChanged(@Nullable final Location locationFromDb) {
                    // update cached version of location
                    currentLocation = liveLocation.getValue() != null ? liveLocation.getValue() : currentLocation;
                    if ((locationFromDb != null && !Location.alreadyInRoom(locationFromDb, savedLocations))
                        || (currentLocation != null && currentLocation.hasFieldsSet())) {
                        currentLocation = locationFromDb != null ? locationFromDb : currentLocation;
                        savedLocations.add(currentLocation);
                        currentSnapshot.setLocationId(currentLocation.getLocationId());
                        Log.e(TAG, "Locally saved location set to :" + currentLocation.toApiFormat());
                    } else {
                        Log.d(TAG, "Location not saved locally: " + locationFromDb.toApiFormat());
                        // no location is saved
                        // pop up dialog
                    }
                }
            });
        }
        roomLocationObserved = true;
    }

    private void makeApiCalls(Location location) {
        Requests.getCounty(this, location.toApiFormat(), new VolleyJsonCallback() {
            @Override
            public void getJsonData(JSONObject response) throws JSONException {
                if (currentSnapshot == null) { currentSnapshot = new CovidSnapshot(); }
                // Add Location to Database (if not already present)
                if (!currentLocation.hasSameData(location)) {
                    vm.insertLocation(location);
                }
                JSONObject stats = (JSONObject) response.get("stats");
                Integer confirmed = (Integer) stats.get("confirmed");
                Integer deaths = (Integer) stats.get("deaths");
                // TODO: calculate better estimate of active cases
                Integer activeCounty = confirmed - deaths;
                currentSnapshot.setCountyActiveCount(activeCounty);
                if (currentSnapshot.hasFieldsSet()) {
                        saveSnapshotToRoom();
                }
                Log.d(TAG, "getJsonData: county " + response);
            }

            @Override
            public void getJsonException(Exception exception) {}

            @Override
            public void getString(String response) {

            }
        });
        Requests.getState(this, location.toApiFormat(), new VolleyJsonCallback() {
            @Override
            public void getJsonData(JSONObject response) throws JSONException {
                if (currentSnapshot == null) { currentSnapshot = new CovidSnapshot(); }
                // Add Location to Database (if not already present)
                if (!currentLocation.hasSameData(location)) {
                    vm.insertLocation(location);
                }                Integer activeState = (Integer) response.get("active");
                currentSnapshot.setStateActiveCount(activeState);
                if (currentSnapshot.hasFieldsSet()) {
                    saveSnapshotToRoom();
                }
                Log.d(TAG, "getJsonData: state " + response);
            }

            @Override
            public void getJsonException(Exception exception) {}

            @Override
            public void getString(String response) {}
        });
        Requests.getCountyHistorical(this, location.toApiFormat(), "30", new VolleyJsonCallback() {
            @Override
            public void getJsonData(JSONObject response) {
                Log.d(TAG, "getJsonData: countyHistorical " + response);
            }

            @Override
            public void getJsonException(Exception exception) {}

            @Override
            public void getString(String response) {}
        });
        Requests.getUSHistorical(this, "1", new VolleyJsonCallback() {
            @Override
            public void getJsonData(JSONObject response) throws JSONException, IOException {
                if (currentSnapshot == null) { currentSnapshot = new CovidSnapshot(); }
                // Add Location to Database (if not already present)
                if (!currentLocation.hasSameData(location)) {
                    vm.insertLocation(location);
                }
                JSONObject timeline = response.getJSONObject("timeline");
                HashMap<String, Integer> totalMap = new ObjectMapper().readValue((timeline.get("cases")).toString(), HashMap.class);

                Integer totalCountry = 0;
                for (Object value : totalMap.values()) {
                    totalCountry = (Integer) value;
                }
                Integer deathCountry = 0;
                HashMap<String, Integer> deathMap = new ObjectMapper().readValue((timeline.get("deaths")).toString(), HashMap.class);
                for (Object value : deathMap.values()) {
                    deathCountry = (Integer) value;
                }
                Integer recoveredCountry = 0;
                HashMap<String, Integer> recoveredMap = new ObjectMapper().readValue((timeline.get("recovered")).toString(), HashMap.class);
                for (Object value : recoveredMap.values()) {
                    recoveredCountry = (Integer) value;
                }
                currentSnapshot.setCountryActiveCount(totalCountry - deathCountry - recoveredCountry);
                if (currentSnapshot.hasFieldsSet()) {
                    saveSnapshotToRoom();
                }
                Log.d(TAG, "getJsonData: country " + response);
            }

            @Override
            public void getJsonException(Exception exception) {}

            @Override
            public void getString(String response) {}
        });
        Requests.getCountyPopulation(this, location.toApiFormat(), new VolleyJsonCallback() {
            @Override
            public void getJsonData(JSONObject response) {}

            @Override
            public void getJsonException(Exception exception) {}

            @Override
            public void getString(String response) {
                if (currentSnapshot == null) { currentSnapshot = new CovidSnapshot(); }
                vm.insertLocation(location);
                currentSnapshot.setCountyTotalPopulation(Integer.parseInt(response));
                if (currentSnapshot.hasFieldsSet()) {
                    saveSnapshotToRoom();
                }
                Log.d(TAG, "getStringData: County " + response);
            }
        });
        Requests.getStatePopulation(this, location.toApiFormat(), new VolleyJsonCallback() {
            @Override
            public void getJsonData(JSONObject response) {}

            @Override
            public void getJsonException(Exception exception) {}

            @Override
            public void getString(String response) {
                if (currentSnapshot == null) { currentSnapshot = new CovidSnapshot(); }
                vm.insertLocation(location);
                currentSnapshot.setStateTotalPopulation(Integer.parseInt(response));
                if (currentSnapshot.hasFieldsSet()) {
                    saveSnapshotToRoom();
                }
                Log.d(TAG, "getStringData: State  " + response);
            }
        });

        Requests.getCountryPopulation(this, new VolleyJsonCallback() {
            @Override
            public void getJsonData(JSONObject response) {}

            @Override
            public void getJsonException(Exception exception) {}

            @Override
            public void getString(String response) {
                if (currentSnapshot == null) { currentSnapshot = new CovidSnapshot(); }
                vm.insertLocation(location);
                currentSnapshot.setCountryTotalPopulation(Integer.parseInt(response));
                if (currentSnapshot.hasFieldsSet()) {
                    saveSnapshotToRoom();
                }
                Log.d(TAG, "getStringData: Country " + response);
            }
        });
    }

    public void saveSnapshotToRoom() {
        if (currentSnapshot != null && currentSnapshot.hasFieldsSet()) {
            // make sure to set LocationIdFK on Snapshot to current LocationIdPK
            if (currentSnapshot.getLocationId() == null || currentSnapshot.getLocationId() == 0) {
                if (currentLocation.getLocationId() == null) {
                    saveLocationToRoom();
                    currentLocation = liveLocation.getValue();
                }
                currentSnapshot.setLocationId(currentLocation != null ? currentLocation.getLocationId() : -1);

            }
            Calendar calendar = Calendar.getInstance();
            currentSnapshot.setLastUpdated(calendar);
            vm.insertCovidSnapshot(currentSnapshot);
        } else {
            Log.e(TAG, "Incomplete Snapshot: " + currentSnapshot.toString());
        }
        Log.d(TAG, "saveSnapshotToRoom invoked");
    }

    public void saveLocationToRoom() {
        if (currentLocation != null && currentLocation.hasFieldsSet()) {
            Calendar calendar = Calendar.getInstance();
            currentLocation.setLastUpdated(calendar);
            vm.insertLocation(currentLocation);
        } else {
            Log.e(TAG, "Incomplete Location:  " + currentLocation.toApiFormat());
        }
        Log.d(TAG, "saveLocationToRoom invoked");
    }
}

package com.nsc.covidscore.room;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.nsc.covidscore.room.CovidSnapshot;
import com.nsc.covidscore.room.CovidSnapshotWithLocationRepository;
import com.nsc.covidscore.room.Location;

import java.util.List;

public class CovidSnapshotWithLocationViewModel extends AndroidViewModel {

    private CovidSnapshotWithLocationRepository repo;

    public CovidSnapshotWithLocationViewModel(Application application) {
        super(application);
        repo = new CovidSnapshotWithLocationRepository(application);
    }

    public Integer insertLocation(Location location) { return repo.insertLocation(location); }

    public void insertCovidSnapshot(CovidSnapshot covidSnapshot) { repo.insertCovidSnapshot(covidSnapshot); }

    public LiveData<CovidSnapshot> getLatestCovidSnapshotByLocation(Location location) { return repo.getLatestCovidSnapshotByLocation(location); }

    public LiveData<CovidSnapshot> getLatestCovidSnapshot() { return repo.getLatestSnapshot(); }

    public LiveData<Location> getLatestLocation() { return repo.getLatestLocation(); }

    public LiveData<List<Location>> getAllLocations() { return repo.getAllLocations(); }

}


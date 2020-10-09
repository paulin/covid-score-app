package com.nsc.covidscore;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CovidSnapshotDao {

    @Query("SELECT * FROM covid_snapshot")
    LiveData<List<CovidSnapshot>> getAll();

    @Query("SELECT * FROM covid_snapshot WHERE covid_snapshot_id = :id LIMIT 1")
    LiveData<CovidSnapshot> findById(Integer id);

    @Query("SELECT * FROM covid_snapshot WHERE location_id = :locationId ORDER BY last_updated_room LIMIT 1")
    LiveData<CovidSnapshot> findLatestByLocationId(Integer locationId);

    @Query("SELECT * FROM covid_snapshot ORDER BY last_updated_room DESC LIMIT 1")
    LiveData<CovidSnapshot> findLatest();

}

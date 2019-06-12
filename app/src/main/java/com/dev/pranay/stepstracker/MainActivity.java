package com.dev.pranay.stepstracker;

import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements ResultCallback<DataReadResult>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, CompoundButton.OnCheckedChangeListener {

    private static final String FIELD_STEPS = "steps";
    private static final String TAG = "stepcounter";
    private static final int REQUEST_OAUTH = 121;
    private GoogleApiClient mGoogleApiClient;
    private List<StepsData> list;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SwitchMaterial toggle = findViewById(R.id.sToggle);
        toggle.setOnCheckedChangeListener(this);

        // Create a Google Fit Client instance with default user account.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)  // Required for SensorsApi calls
                // Optional: specify more APIs used with additional calls to addApi
                .useDefaultAccount()
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
    }

    public void populateTable(boolean revFlag) {
        ListView listView = findViewById(R.id.lvHistory);
        StepsAdapter mStepsAdapter = new StepsAdapter(this, list, revFlag);
        listView.setAdapter(mStepsAdapter);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Connected");

        new GetTodayStepCount().execute();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endTime = calendar.getTimeInMillis();

        calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -14);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 1);
        long startTime = calendar.getTimeInMillis();


        DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
        Log.i(TAG, "Range Start: " + df.format(startTime));
        Log.i(TAG, "Range End: " + df.format(endTime));

        DataReadRequest dataReadRequest = new DataReadRequest.Builder()
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .bucketByTime(1, TimeUnit.DAYS)
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .build();
        PendingResult<DataReadResult> pendingResult = Fitness.HistoryApi.readData(mGoogleApiClient, dataReadRequest);
        pendingResult.setResultCallback(this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.i(TAG, "Connection failed. Cause: " + connectionResult.toString());
        try {
            connectionResult.startResolutionForResult(this, REQUEST_OAUTH);
        } catch (IntentSender.SendIntentException e) {
            Log.i(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OAUTH && resultCode == RESULT_OK) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResult(DataReadResult dataReadResult) {
        list = new ArrayList<>();
        list.add(new StepsData("Date", "Steps"));
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, dataReadResult.getBuckets().size() + "");
            for (Bucket bucket : dataReadResult.getBuckets()) {
                list.add(processDataset(bucket));
            }
        }
        populateTable(true);
    }

    private StepsData processDataset(Bucket bucket) {
        Date d = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
        String date = DateFormat.getDateInstance().format(d);
        String steps = "0";
        List<DataSet> dataSets = bucket.getDataSets();
        for (DataSet dataSet : dataSets) {
            for (DataPoint dataPoint : dataSet.getDataPoints()) {
                for (Field field : dataPoint.getDataType().getFields()) {
                    if (field.getName().equals(FIELD_STEPS)) {
                        steps = dataPoint.getValue(field).toString();
                    }
                }
            }
        }
        Log.i(TAG, "Date: " + date + "\tValue: " + steps);

        return new StepsData(date, steps);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked)
            populateTable(false);
        else
            populateTable(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                new GetTodayStepCount().execute();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class GetTodayStepCount extends AsyncTask<Void, Void, Long> {


        @Override
        protected Long doInBackground(Void... voids) {
            Log.i(TAG, "doInBackground called");
            long steps = 0;

            PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal(mGoogleApiClient, DataType.TYPE_STEP_COUNT_DELTA);
            DailyTotalResult totalResult = result.await(30, TimeUnit.SECONDS);
            if (totalResult.getStatus().isSuccess()) {
                DataSet totalSet = totalResult.getTotal();
                steps = totalSet.isEmpty()
                        ? 0
                        : totalSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
            } else {
                Log.w(TAG, "There was a problem getting the step count.");
            }

            Log.i(TAG, "Total steps: " + steps);

            return steps;
        }

        @Override
        protected void onPostExecute(Long steps) {
            super.onPostExecute(steps);
            TextView today = findViewById(R.id.tvToday);
            today.setText(steps.toString());
        }
    }
}

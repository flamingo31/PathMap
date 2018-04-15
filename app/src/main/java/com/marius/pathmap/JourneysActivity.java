package com.marius.pathmap;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.marius.pathmap.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class JourneysActivity extends AppCompatActivity {

    private ListView listJourneys;
    private ArrayList<String> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journeys);

        listJourneys = (ListView) findViewById(R.id.journeysList);

        arrayList = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for(int i = 0, j = 0; i < User.getInstance().getStartTime().size() && j < User.getInstance().getEndTime().size(); i++, j++){
            arrayList.add(i + ". " + dateFormat.format(User.getInstance().getStartTime().get(i)) + " - " + dateFormat.format(User.getInstance().getEndTime().get(j)));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),R.layout.list_item_journey, R.id.textView, arrayList);
        listJourneys.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
}

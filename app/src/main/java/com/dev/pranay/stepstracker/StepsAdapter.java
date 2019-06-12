package com.dev.pranay.stepstracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class StepsAdapter extends ArrayAdapter<StepsData> {

    private Context context;
    private List<StepsData> list;
    private boolean revFlag;

    public StepsAdapter(Context context, List<StepsData> list, Boolean revFlag) {
        super(context, 0, list);
        this.context = context;
        this.list = list;
        this.revFlag = revFlag;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if(view == null){
            view = LayoutInflater.from(context).inflate(R.layout.row_item, parent, false);
        }
        int pos = position;
        if(revFlag && pos != 0){
            pos = list.size()-position;
        }
        StepsData stepsData = list.get(pos);
        TextView date = view.findViewById(R.id.tvDate);
        TextView steps = view.findViewById(R.id.tvSteps);
        date.setText(stepsData.getDate());
        steps.setText(stepsData.getSteps());
        return view;
    }
}

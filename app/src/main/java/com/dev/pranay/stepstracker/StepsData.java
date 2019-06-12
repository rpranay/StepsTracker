package com.dev.pranay.stepstracker;

public class StepsData {
    private String date;
    private String steps;

    public StepsData(String date, String steps){
        this.date = date;
        this.steps = steps;
    }

    public String getDate() {
        return this.date;
    }

    public String getSteps() {
        return this.steps;
    }
}

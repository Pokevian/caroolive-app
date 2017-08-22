package com.pokevian.app.smartfleet.model;

/**
 * Created by ian on 2016-02-25.
 */
public class ScoreRank extends Rank {
    private int harshSpeedScoreStar;
    private int idlingScoreStar;
    private int normalSpeedScoreStar;
    private int ecoSpeedScoreStar;
    private int overSpeedScoreStar;
    private int highLoadScoreStar;
    private int fuelCutScoreStar;

    public ScoreRank() {
    }

    public int getHarshSpeedScoreStar() {
        return harshSpeedScoreStar;
    }

    public void setHarshSpeedScoreStar(int harshSpeedScoreStar) {
        this.harshSpeedScoreStar = harshSpeedScoreStar;
    }

    public int getIdlingScoreStar() {
        return idlingScoreStar;
    }

    public void setIdlingScoreStar(int idlingScoreStar) {
        this.idlingScoreStar = idlingScoreStar;
    }

    public int getNormalSpeedScoreStar() {
        return normalSpeedScoreStar;
    }

    public void setNormalSpeedScoreStar(int normalSpeedScoreStar) {
        this.normalSpeedScoreStar = normalSpeedScoreStar;
    }

    public int getEcoSpeedScoreStar() {
        return ecoSpeedScoreStar;
    }

    public void setEcoSpeedScoreStar(int ecoSpeedScoreStar) {
        this.ecoSpeedScoreStar = ecoSpeedScoreStar;
    }

    public int getOverSpeedScoreStar() {
        return overSpeedScoreStar;
    }

    public void setOverSpeedScoreStar(int overSpeedScoreStar) {
        this.overSpeedScoreStar = overSpeedScoreStar;
    }

    public int getHighLoadScoreStar() {
        return highLoadScoreStar;
    }

    public void setHighLoadScoreStar(int highLoadScoreStar) {
        this.highLoadScoreStar = highLoadScoreStar;
    }

    public int getFuelCutScoreStar() {
        return fuelCutScoreStar;
    }

    public void setFuelCutScoreStar(int fuelCutScoreStar) {
        this.fuelCutScoreStar = fuelCutScoreStar;
    }
}

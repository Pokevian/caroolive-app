package com.pokevian.app.smartfleet.model;

import java.io.Serializable;

/**
 * Created by ian on 2016-02-25.
 */
public class Rank implements Serializable {

    private String memberNo;
    private int tripCount;
    private int runTime;
    private float runDistance;
    private float fuelConsumption;
    private float fuelEconomy;
    private int harshAccelCount;
    private int harshBrakeCount;
    private float idlingTime;
    private float normalSpeedTime;
    private float ecoSpeedTime;
    private float overSpeedTime;
    private float fuelCutTime;
    private float highLoadTime;
    private float drivingPoint;
    private float drivingScore;
    private int drivingLevel;
    private int scoreCount;

    private Float fuelCost;
    private Float fuelCostSaved;
    private Float fuelCostSaveTk;
    private Float fuelCostTk;

    private String loginId;
    private String memberNm;
    private String email;
    private String localCd;
    private String localCdName;

    private String carNo;
    private String carMakerCd;
    private String carMakerName;
    private String carModelCd;
    private String carModelName;
    private String carSrcName;
    private String carFuelCd;

    private Float maxSpeed;
    private int drivingScoreRanking;
    private int harshSpeedRanking;
    private int ecoSpeedRanking;
    private int fuelCutRanking;

    private Integer memberDrvPt;		// 누적 driving point
    private Integer memberLevPt;		// 누적 level point
    private Integer memberRankPt;		// 누적 ranking point
    private Integer memberEvtPt;		// 누적 event point
    private Integer memberPtAcc;		// 누적 적립 point
    private Integer memberPtUse;		// 누적 사용 point
    private Integer memberPtRes;		// 누적 잔여 point
    private Integer memberPtExp;		// 소멸예정 point


    public Rank() {
    }

    public String getMemberNo() {
        return memberNo;
    }

    public void setMemberNo(String memberNo) {
        this.memberNo = memberNo;
    }

    public int getTripCount() {
        return tripCount;
    }

    public void setTripCount(int tripCount) {
        this.tripCount = tripCount;
    }

    public int getRunTime() {
        return runTime;
    }

    public void setRunTime(int runTime) {
        this.runTime = runTime;
    }

    public float getRunDistance() {
        return runDistance;
    }

    public void setRunDistance(float runDistance) {
        this.runDistance = runDistance;
    }

    public float getFuelConsumption() {
        return fuelConsumption;
    }

    public void setFuelConsumption(float fuelConsumption) {
        this.fuelConsumption = fuelConsumption;
    }

    public float getFuelEconomy() {
        return fuelEconomy;
    }

    public void setFuelEconomy(float fuelEconomy) {
        this.fuelEconomy = fuelEconomy;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public int getHarshAccelCount() {
        return harshAccelCount;
    }

    public void setHarshAccelCount(int harshAccelCount) {
        this.harshAccelCount = harshAccelCount;
    }

    public int getHarshBrakeCount() {
        return harshBrakeCount;
    }

    public void setHarshBrakeCount(int harshBrakeCount) {
        this.harshBrakeCount = harshBrakeCount;
    }

    public float getIdlingTime() {
        return idlingTime;
    }

    public void setIdlingTime(float idlingTime) {
        this.idlingTime = idlingTime;
    }

    public float getNormalSpeedTime() {
        return normalSpeedTime;
    }

    public void setNormalSpeedTime(float normalSpeedTime) {
        this.normalSpeedTime = normalSpeedTime;
    }

    public float getEcoSpeedTime() {
        return ecoSpeedTime;
    }

    public void setEcoSpeedTime(float ecoSpeedTime) {
        this.ecoSpeedTime = ecoSpeedTime;
    }

    public float getOverSpeedTime() {
        return overSpeedTime;
    }

    public void setOverSpeedTime(float overSpeedTime) {
        this.overSpeedTime = overSpeedTime;
    }

    public float getFuelCutTime() {
        return fuelCutTime;
    }

    public void setFuelCutTime(float fuelCutTime) {
        this.fuelCutTime = fuelCutTime;
    }

    public float getHighLoadTime() {
        return highLoadTime;
    }

    public void setHighLoadTime(float highLoadTime) {
        this.highLoadTime = highLoadTime;
    }

    public float getDrivingPoint() {
        return drivingPoint;
    }

    public void setDrivingPoint(float drivingPoint) {
        this.drivingPoint = drivingPoint;
    }

    public float getDrivingScore() {
        return drivingScore;
    }

    public void setDrivingScore(float drivingScore) {
        this.drivingScore = drivingScore;
    }

    public int getDrivingLevel() {
        return drivingLevel;
    }

    public void setDrivingLevel(int drivingLevel) {
        this.drivingLevel = drivingLevel;
    }

    public int getScoreCount() {
        return scoreCount;
    }

    public void setScoreCount(int scoreCount) {
        this.scoreCount = scoreCount;
    }

    public int getDrivingScoreRanking() {
        return drivingScoreRanking;
    }

    public void setDrivingScoreRanking(int drivingScoreRanking) {
        this.drivingScoreRanking = drivingScoreRanking;
    }

    public int getHarshSpeedRanking() {
        return harshSpeedRanking;
    }

    public void setHarshSpeedRanking(int harshSpeedRanking) {
        this.harshSpeedRanking = harshSpeedRanking;
    }

    public int getEcoSpeedRanking() {
        return ecoSpeedRanking;
    }

    public void setEcoSpeedRanking(int ecoSpeedRanking) {
        this.ecoSpeedRanking = ecoSpeedRanking;
    }

    public int getFuelCutRanking() {
        return fuelCutRanking;
    }

    public void setFuelCutRanking(int fuelCutRanking) {
        this.fuelCutRanking = fuelCutRanking;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getMemberNm() {
        return memberNm;
    }

    public void setMemberNm(String memberNm) {
        this.memberNm = memberNm;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCarNo() {
        return carNo;
    }

    public void setCarNo(String carNo) {
        this.carNo = carNo;
    }

    public String getCarMakerCd() {
        return carMakerCd;
    }

    public void setCarMakerCd(String carMakerCd) {
        this.carMakerCd = carMakerCd;
    }

    public String getCarMakerName() {
        return carMakerName;
    }

    public void setCarMakerName(String carMakerName) {
        this.carMakerName = carMakerName;
    }

    public String getCarModelCd() {
        return carModelCd;
    }

    public void setCarModelCd(String carModelCd) {
        this.carModelCd = carModelCd;
    }

    public String getCarModelName() {
        return carModelName;
    }

    public void setCarModelName(String carModelName) {
        this.carModelName = carModelName;
    }

    public Float getFuelCost() {
        return fuelCost;
    }

    public void setFuelCost(Float fuelCost) {
        this.fuelCost = fuelCost;
    }

    public Float getFuelCostSaved() {
        return fuelCostSaved;
    }

    public void setFuelCostSaved(Float fuelCostSaved) {
        this.fuelCostSaved = fuelCostSaved;
    }

    public Float getFuelCostSaveTk() {
        return fuelCostSaveTk;
    }

    public void setFuelCostSaveTk(Float fuelCostSaveTk) {
        this.fuelCostSaveTk = fuelCostSaveTk;
    }

    public Float getFuelCostTk() {
        return fuelCostTk;
    }

    public void setFuelCostTk(Float fuelCostTk) {
        this.fuelCostTk = fuelCostTk;
    }

    public String getLocalCdName() {
        return localCdName;
    }

    public void setLocalCdName(String localCdName) {
        this.localCdName = localCdName;
    }

    public String getCarFuelCd() {
        return carFuelCd;
    }

    public void setCarFuelCd(String carFuelCd) {
        this.carFuelCd = carFuelCd;
    }

    public String getCarFuelName() {
        if ("gasoline".equals(carFuelCd)) {
            return "휘발유";
        } else if ("diesel".equals(carFuelCd)) {
            return "경유";
        } else if ("lpg".equals(carFuelCd)) {
            return "LPG";
        }

        return null;
    }

    public String getLocalCd() {
        return localCd;
    }

    public void setLocalCd(String localCd) {
        this.localCd = localCd;
    }

    public String getCarSrcName() {
        return carSrcName;
    }

    public void setCarSrcName(String carSrcName) {
        this.carSrcName = carSrcName;
    }

    public void setMaxSpeed(Float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public Integer getMemberDrvPt() {
        return memberDrvPt;
    }

    public void setMemberDrvPt(Integer memberDrvPt) {
        this.memberDrvPt = memberDrvPt;
    }

    public Integer getMemberLevPt() {
        return memberLevPt;
    }

    public void setMemberLevPt(Integer memberLevPt) {
        this.memberLevPt = memberLevPt;
    }

    public Integer getMemberRankPt() {
        return memberRankPt;
    }

    public void setMemberRankPt(Integer memberRankPt) {
        this.memberRankPt = memberRankPt;
    }

    public Integer getMemberEvtPt() {
        return memberEvtPt;
    }

    public void setMemberEvtPt(Integer memberEvtPt) {
        this.memberEvtPt = memberEvtPt;
    }

    public Integer getMemberPtAcc() {
        return memberPtAcc;
    }

    public void setMemberPtAcc(Integer memberPtAcc) {
        this.memberPtAcc = memberPtAcc;
    }

    public Integer getMemberPtUse() {
        return memberPtUse;
    }

    public void setMemberPtUse(Integer memberPtUse) {
        this.memberPtUse = memberPtUse;
    }

    public Integer getMemberPtRes() {
        return memberPtRes;
    }

    public void setMemberPtRes(Integer memberPtRes) {
        this.memberPtRes = memberPtRes;
    }

    public Integer getMemberPtExp() {
        return memberPtExp;
    }

    public void setMemberPtExp(Integer memberPtExp) {
        this.memberPtExp = memberPtExp;
    }
}

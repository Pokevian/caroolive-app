package com.pokevian.app.smartfleet.model;

import java.util.HashMap;
import java.util.Map;

public class EcoData {
	private String 	           date;
	private String 	           carNo;
	private String             carSrcCd;
	private String 	           tripNo;
	private int 	           cnt;
	private int 	           drivingTime;
	private float 	           drivingDistance;
	private float 	           fuelConsumption;
	private int 	           maxSpeed;
	private float 	           avgSpeed;
	private int 	           harshAccelCount;
	private int 	           harshDecelCount;
	                           
	private float 	           timeInIdelTime;
	private float 	           lowSpeedTime;        // 0~40
	private float 	           normalSpeedTime;		// 40~60, 80~100
	private float 	           economyTime;			// 60~80
	private float 	           overSpeedTime;		// 100~@
	private float 	           fuelCutTime;
	private float 	           timeInAccel;
	                           
	private int 	           overMaxSpeedCount;
	private int 	           maxEngineRpmCount;
	private int 	           oilPrice;
	private String 	           drivingDay;
	private String 	           drivingHour;
	private String 	           drivingMin;
	
	private HashMap<String, String> analysisData;
	
	public EcoData() {
		//
	}
	
	public String getCarNo() {
		return carNo;
	}

	public void setCarNo(String carNo) {
		this.carNo = carNo;
	}

	public String getCarSrcCd() {
        return carSrcCd;
    }

    public void setCarSrcCd(String carSrcCd) {
        this.carSrcCd = carSrcCd;
    }

    public String getTripNo() {
        return tripNo;
    }

    public void setTripNo(String tripNo) {
        this.tripNo = tripNo;
    }

    public int getCnt() {
		return cnt;
	}

	public void setCnt(int cnt) {
		this.cnt = cnt;
	}

	public void setDrivingTime(Integer drivingTime) {
		this.drivingTime = drivingTime;
	}
	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getDrivingTime() {
		return drivingTime;
	}

	public void setDrivingTime(int drivingTime) {
		this.drivingTime = drivingTime;
	}

	public float getDrivingDistance() {
		return drivingDistance;
	}
	
	public float getFuelConsumption() {
		return fuelConsumption;
	}

	public void setFuelConsumption(float fuelConsumption) {
		this.fuelConsumption = fuelConsumption;
	}

	public void setDrivingDistance(float drivingDistance) {
		this.drivingDistance = drivingDistance;
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(int maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public float getAvgSpeed() {
		return avgSpeed;
	}

	public void setAvgSpeed(float avgSpeed) {
		this.avgSpeed = avgSpeed;
	}

	public int getHarshAccelCount() {
		return harshAccelCount;
	}

	public void setHarshAccelCount(int harshAccelCount) {
		this.harshAccelCount = harshAccelCount;
	}

	public int getHarshDecelCount() {
		return harshDecelCount;
	}

	public void setHarshDecelCount(int harshDecelCount) {
		this.harshDecelCount = harshDecelCount;
	}

	public float getTimeInIdelTime() {
		return timeInIdelTime;
	}

	public void setTimeInIdelTime(float timeInIdelTime) {
		this.timeInIdelTime = timeInIdelTime;
	}

	public float getLowSpeedTime() {
		return lowSpeedTime;
	}

	public void setLowSpeedTime(float lowSpeedTime) {
		this.lowSpeedTime = lowSpeedTime;
	}
	
	public float getNormalSpeedTime() {
		return normalSpeedTime;
	}

	public void setNormalSpeedTime(float normalSpeedTime) {
		this.normalSpeedTime = normalSpeedTime;
	}

	public float getEconomyTime() {
		return economyTime;
	}

	public void setEconomyTime(float economyTime) {
		this.economyTime = economyTime;
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

	public float getOverMaxSpeedCount() {
		return overMaxSpeedCount;
	}

	public void setOverMaxSpeedCount(int overMaxSpeedCount) {
		this.overMaxSpeedCount = overMaxSpeedCount;
	}

	public int getMaxEngineRpmCount() {
		return maxEngineRpmCount;
	}

	public void setMaxEngineRpmCount(int maxEngineRpmCount) {
		this.maxEngineRpmCount = maxEngineRpmCount;
	}
	
	public float getTimeInAccel() {
		return timeInAccel;
	}

	public void setTimeInAccel(float timeInAccel) {
		this.timeInAccel = timeInAccel;
	}

	public HashMap<String, String> getAnalysisData() {
		return analysisData;
	}

	public void setAnalysisData(HashMap<String, String> analysisData) {
		this.analysisData = analysisData;
	}

	public int getOilPrice() {
		return oilPrice;
	}

	public void setOilPrice(int oilPrice) {
		this.oilPrice = oilPrice;
	}

	public String getDrivingMin() {
		return drivingMin;
	}

	public void setDrivingMin(String drivingMin) {
		this.drivingMin = drivingMin;
	}
	
	public String getDrivingDay() {
		return drivingHour;
	}

	public void setDrivingDay(String drivingDay) {
		this.drivingDay = drivingDay;
	}
	
	public String getDrivingHour() {
		return drivingHour;
	}

	public void setDrivingHour(String drivingHour) {
		this.drivingHour = drivingHour;
	}
	
	/**
	 * 에코-포인트 계산 항목들을 맵으로부터 셋팅한다. 
	 * @param map
	 */
	public void setFromMap(Map map) {
	    drivingTime = Integer.parseInt(map.get("drivingTime").toString());
	    drivingDistance = Float.parseFloat(map.get("drivingDistance").toString());
	    fuelConsumption = Float.parseFloat(map.get("fuelConsumption").toString());
	    harshAccelCount = Integer.parseInt(map.get("harshAccelCount").toString());
	    harshDecelCount = Integer.parseInt(map.get("harshDecelCount").toString());
	    fuelCutTime = Float.parseFloat(map.get("fuelCutTime").toString());
	    timeInIdelTime = Float.parseFloat(map.get("timeInIdelTime").toString());
	    lowSpeedTime = Float.parseFloat(map.get("lowSpeedTime").toString());
	    normalSpeedTime = Float.parseFloat(map.get("normalSpeedTime").toString());
	    economyTime = Float.parseFloat(map.get("economyTime").toString());
	    overSpeedTime = Float.parseFloat(map.get("overSpeedTime").toString());
	    timeInAccel = Float.parseFloat(map.get("timeInAccel").toString());
	}
	
	@Override
	public String toString() {
		return "EcoData [date=" + date + ", carNo=" + carNo + ", cnt=" + cnt
		        + ", carSrcCd=" + carSrcCd
				+ ", drivingTime=" + drivingTime + ", drivingDistance="
				+ drivingDistance + ", fuelConsumption=" + fuelConsumption
				+ ", maxSpeed=" + maxSpeed + ", avgSpeed=" + avgSpeed
				+ ", harshAccelCount=" + harshAccelCount + ", harshDecelCount="
				+ harshDecelCount + ", timeInIdelTime=" + timeInIdelTime
				+ ", lowSpeedTime=" + lowSpeedTime + ", normalSpeedTime="
				+ normalSpeedTime + ", economyTime=" + economyTime
				+ ", overSpeedTime=" + overSpeedTime + ", fuelCutTime="
				+ fuelCutTime + ", overMaxSpeedCount=" + overMaxSpeedCount
				+ ", maxEngineRpmCount=" + maxEngineRpmCount + ", timeInAccel="
				+ timeInAccel + ", oilPrice=" + oilPrice + ", drivingDay="
				+ drivingDay + ", drivingHour=" + drivingHour + ", drivingMin="
				+ drivingMin + ", analysisData=" + analysisData + "]";
	}

}
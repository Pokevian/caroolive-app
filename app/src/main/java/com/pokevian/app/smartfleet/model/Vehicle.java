/*
 * Copyright (c) 2014. Pokevian Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pokevian.app.smartfleet.model;

import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.caroo.common.model.TwoState;
import com.pokevian.caroo.common.smart.model.SmartCar;
import com.pokevian.caroo.common.smart.model.SmartCarModel;
import com.pokevian.lib.obd2.defs .FuelType;


import java.io.File;
import java.io.Serializable;

public class Vehicle implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private String vehicleId;
    private String plateNo;
    private String activeCode = TwoState.Y.name();
    private VehicleModel model = new VehicleModel();
    private String vin;
    private Integer prodYear = 0; // FIXME
    private String imageUrl;
    private Integer odometer = 0;
    private Integer engineDistance = 0;
    private String isgCode = TwoState.N.name();

    private String obdAddress;
    private String obdProtocol;
    private int obdConnectionMethod = 0;
    private String elmVer;

    private File imageFile;

    public Vehicle() {
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }

    public String getActiveCode() {
        return activeCode;
    }

    public void setActiveCode(String activeCode) {
        this.activeCode = activeCode;
    }

    public VehicleModel getModel() {
        return model;
    }

    public void setModel(VehicleModel model) {
        this.model = model;
    }

    public FuelType getFuelType() {
        return model.getFuelType();
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public Integer getProdYear() {
        return prodYear;
    }

    public void setProdYear(Integer prodYear) {
        this.prodYear = prodYear;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getOdometer() {
        return odometer;
    }

    public void setOdometer(Integer odometer) {
        this.odometer = odometer;
    }

    public Integer getEngineDistance() {
        return engineDistance;
    }

    public void setEngineDistance(Integer engineDistance) {
        this.engineDistance = engineDistance;
    }

    public String getIsgCode() {
        return isgCode;
    }

    public void setIsgCode(String isgCode) {
        this.isgCode = isgCode;
    }

    public String getObdAddress() {
        return obdAddress;
    }

    public void setObdAddress(String obdAddress) {
        this.obdAddress = obdAddress;
    }

    public String getObdProtocol() {
        return obdProtocol;
    }

    public void setObdProtocol(String obdProtocol) {
        this.obdProtocol = obdProtocol;
    }

    public int getObdConnectionMethod() {
        return obdConnectionMethod;
    }

    public void setObdConnectionMethod(int obdConnectionMethod) {
        this.obdConnectionMethod = obdConnectionMethod;
    }

    public String getElmVer() {
        return elmVer;
    }

    public void setElmVer(String elmVer) {
        this.elmVer = elmVer;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    @Override
    public Vehicle clone() {
        return new Vehicle(this);
    }

    @Override
    public String toString() {
        return "Vehicle [vehicleId=" + vehicleId + ", plateNo=" + plateNo
                + ", activeCode=" + activeCode + ", model=" + model + ", vin="
                + vin + ", prodYear=" + prodYear + ", isg=" + isgCode +", imageUrl=" + imageUrl
                + ", odometer=" + odometer + ", engineDistance="
                + engineDistance + ", obdAddress=" + obdAddress
                + ", obdProtocol=" + obdProtocol + ", obdConnectionMethod="
                + obdConnectionMethod  + ", elmVer=" + elmVer
                + ", imageFile=" + imageFile + "]";
    }

    public Vehicle(Vehicle other) {
        this.vehicleId = other.vehicleId;
        this.plateNo = other.plateNo;
        this.activeCode = other.activeCode;
        this.model = new VehicleModel(other.model);
        this.vin = other.vin;
        this.prodYear = other.prodYear;
        this.imageUrl = other.imageUrl;
        this.odometer = other.odometer;
        this.engineDistance = other.engineDistance;
        this.isgCode = other.isgCode;
        this.obdAddress = other.obdAddress;
        this.obdProtocol = other.obdProtocol;
        this.obdConnectionMethod = other.obdConnectionMethod;
        this.imageFile = other.imageFile;
        this.elmVer = other.elmVer;
    }

    public Vehicle(SmartCar sc) {
        if (sc != null) {
            vehicleId = sc.getCarNo();
            plateNo = sc.getCarPlateNo();
            activeCode = sc.getActiveYn().name();
            model = new VehicleModel(sc.getCarModel());
            vin = sc.getVin();
            prodYear = sc.getYear();
            isgCode = sc.getIsgYn().name();
            if (sc.getCarImageFile() != null) {
                imageUrl = ServerUrl.SERVICE_SERVER_BASE_URL + sc.getCarImageFile();
            }
            odometer = sc.getOdometer();
            engineDistance = sc.getEngineDistance();
            obdAddress = sc.getObdMac();
            if ("0".equals(obdAddress)) {
                obdAddress = null;
            }
            obdProtocol = String.valueOf(sc.getProtocol());
            elmVer = sc.getElmVer();
        }
    }

    public SmartCar toServerBean(String accountId) {
        SmartCar sc = new SmartCar();
        sc.setMemberNo(accountId);
        sc.setCarNo(vehicleId);
        sc.setCarPlateNo(plateNo);
        sc.setActiveYn(TwoState.valueOf(activeCode));
        sc.setVin(vin);
        sc.setYear(prodYear);
        sc.setIsgYn(TwoState.valueOf(isgCode));
        sc.setOdometer(odometer);
        sc.setEngineDistance(engineDistance);
        sc.setObdMac(obdAddress);
        try {
            sc.setProtocol(Integer.valueOf(obdProtocol));
        } catch (NumberFormatException e) {

        }
        sc.setElmVer(elmVer);

        if (model != null) {
            SmartCarModel scm = new SmartCarModel();
            sc.setCarModel(scm);
            scm.setCarModelNo((int) model.getModelId());
            /* DO NOT SET MODEL PARAMETERS
            scm.setCarMakerCd(model.getVehicleMakerTag());
			scm.setCarModelNameCd(model.getModelCode());
			scm.setCarSrcCd(model.getVehicleFuelTag());
			scm.setCarTypeCd(model.getTypeCode());
			scm.setEngineDisplacement(model.getDisplacement());
			scm.setFuelEconomy(model.getFuelEconomy());*/
        }

        return sc;
    }

    public boolean isHybrid() {
        VehicleModel model = getModel();
        if (model != null) {
            return "hybrid".equals(model.getEngineCode());
        } else {
            return false;
        }
    }

}

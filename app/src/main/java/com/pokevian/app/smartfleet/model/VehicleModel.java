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

import com.pokevian.caroo.common.smart.model.SmartCarModel;
import com.pokevian.lib.obd2.defs.FuelType;

import org.apache.log4j.Logger;

import java.io.Serializable;


public class VehicleModel implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private long modelId;
    private String makerCode;
    private String makerName;
    private String typeCode;
    private String typeName;
    private String modelCode;
    private String modelName;
    private String fuelCode;
    private String engineName;
    private String engineCode;
    private int displacement;
    private float fuelEconomy;
    private int releaseYear;
    private int discontinuedYear;

    public VehicleModel() {
    }

    public long getModelId() {
        return modelId;
    }

    public void setModelId(long modelId) {
        this.modelId = modelId;
    }

    public String getMakerCode() {
        return makerCode;
    }

    public void setMakerCode(String makerCode) {
        this.makerCode = makerCode;
    }

    public String getMakerName() {
        return makerName;
    }

    public void setMakerName(String makerName) {
        this.makerName = makerName;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getFuelCode() {
        return fuelCode;
    }

    public void setFuelCode(String fuelCode) {
        this.fuelCode = fuelCode;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getEngineCode() {
        return engineCode;
    }

    public void setEngineCode(String engineCode) {
        this.engineCode = engineCode;
    }

    public int getDisplacement() {
        return displacement;
    }

    public void setDisplacement(int engineDisplacement) {
        this.displacement = engineDisplacement;
    }

    public float getFuelEconomy() {
        return fuelEconomy;
    }

    public void setFuelEconomy(float fuelEconomy) {
        this.fuelEconomy = fuelEconomy;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public int getDiscontinuedYear() {
        return discontinuedYear;
    }

    public void setDiscontinuedYear(int discontinuedYear) {
        this.discontinuedYear = discontinuedYear;
    }

    @Override
    public VehicleModel clone() {
        return new VehicleModel(this);
    }

    @Override
    public String toString() {
        return "VehicleModel{" +
                "modelId=" + modelId +
                ", makerCode='" + makerCode + '\'' +
                ", typeCode='" + typeCode + '\'' +
                ", modelCode='" + modelCode + '\'' +
                ", fuelCode='" + fuelCode + '\'' +
                ", engineCode='" + engineCode + '\'' +
                ", displacement=" + displacement +
                ", releaseYear=" + releaseYear +
                ", discontinuedYear=" + discontinuedYear +
                '}';
    }

    public VehicleModel(VehicleModel other) {
        this.modelId = other.modelId;
        this.makerCode = other.makerCode;
        this.makerName = other.makerName;
        this.typeCode = other.typeCode;
        this.typeName = other.typeName;
        this.modelCode = other.modelCode;
        this.modelName = other.modelName;
        this.fuelCode = other.fuelCode;
        this.engineCode = other.engineCode;
        this.engineName = other.engineName;
        this.displacement = other.displacement;
        this.fuelEconomy = other.fuelEconomy;
        this.releaseYear = other.releaseYear;
        this.discontinuedYear = other.discontinuedYear;

    }

    public VehicleModel(SmartCarModel scm) {
        if (scm != null) {
            modelId = scm.getCarModelNo();
            makerCode = scm.getCarMakerCd();
            makerName = scm.getCarMakerName();
            typeCode = scm.getCarTypeCd();
            typeName = scm.getCarTypeName();
            modelCode = scm.getCarModelNameCd();
            modelName = scm.getCarModelName();
            engineCode = scm.getCarSrcCd();
            engineName = scm.getCarSrcName();
            fuelCode = scm.getCarFuelCd();
            displacement = scm.getEngineDisplacement();
            fuelEconomy = scm.getFuelEconomy();
            releaseYear = scm.getReleaseYear();
            discontinuedYear = scm.getDiscontinuedYear();
        }
    }

    public FuelType getFuelType() {
        if ("diesel".equals(fuelCode)) {
            return FuelType.DIESEL;
        } else if ("lpg".equals(fuelCode)) {
            return FuelType.LPG;
        } else if ("electric".equals(fuelCode)) {
            return FuelType.ELECTRIC;
        } else { // gasoline
            return FuelType.GASOLINE;
        }

    }

}

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

import com.pokevian.caroo.common.model.TwoState;
import com.pokevian.caroo.common.model.code.JoinRouteCd;
import com.pokevian.caroo.common.smart.model.SmartMember;

import java.io.Serializable;
import java.util.Date;

public class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accountId;
    private String loginId;
    private String nickName;
    private Date birthday;
    private String countryCode;
    private String regionCode;
    private String sexCode;
    private String joinCode;
    private String activeCode;
    private AuthTarget authTarget;
    private String appVer;

    public Account() {
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getSexCode() {
        return sexCode;
    }

    public void setSexCode(String sexCode) {
        this.sexCode = sexCode;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public String getActiveCode() {
        return activeCode;
    }

    public void setActiveCode(String activeCode) {
        this.activeCode = activeCode;
    }

    public AuthTarget getAuthTarget() {
        return authTarget;
    }

    public void setAuthTarget(AuthTarget authTarget) {
        this.authTarget = authTarget;
    }

    public String getAppVer() {
        return appVer;
    }

    public void setAppVer(String appVer) {
        this.appVer = appVer;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId='" + accountId + '\'' +
                ", loginId='" + loginId + '\'' +
                ", authTarget='" + authTarget + '\'' +
                ", appVer='" + appVer + '\'' +
                '}';
    }

    public Account(SmartMember sm) {
        if (sm != null) {
            accountId = sm.getMemberNo();
            loginId = sm.getLoginId();
            nickName = sm.getMemberNm();
            birthday = sm.getBirthDate();
            countryCode = sm.getCountryCd();
            regionCode = sm.getLocalCd();
            sexCode = sm.getSexCd();
            joinCode = sm.getJoinRouteCd();
            activeCode = sm.getActiveYn().name();

            if (JoinRouteCd.google.name().equals(joinCode)) {
                authTarget = AuthTarget.GOOGLE;
            } else if (JoinRouteCd.facebook.name().equals(joinCode)) {
                authTarget = AuthTarget.FACEBOOK;
            } else {
                authTarget = AuthTarget.NONE;
            }
            appVer = sm.getAppVer();
        }
    }

    public SmartMember toServerBean() {
        SmartMember sm = new SmartMember();
        sm.setMemberNo(accountId);
        sm.setLoginId(loginId);
        sm.setMemberNm(nickName);
        sm.setEmail(loginId);
        sm.setSexCd(sexCode);
        sm.setBirthDate(birthday);
        sm.setCountryCd(countryCode);
        sm.setLocalCd(regionCode);
        sm.setJoinRouteCd(joinCode);
        sm.setActiveYn(TwoState.valueOf(activeCode));
        sm.setAppVer(appVer);
        return sm;
    }

}

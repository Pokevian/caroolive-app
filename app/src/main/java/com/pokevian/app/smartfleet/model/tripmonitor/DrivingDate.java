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

package com.pokevian.app.smartfleet.model.tripmonitor;

import java.util.List;

public class DrivingDate {
    String loginId;
    String mod_id;
    String cmd;
    Data data;
    String reg_id;
    String memberNo;

    DrivingDate() {
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getModId() {
        return mod_id;
    }

    public void setModId(String modId) {
        this.mod_id = modId;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getRegId() {
        return reg_id;
    }

    public void setRegId(String regId) {
        this.reg_id = regId;
    }

    public String getMemberNo() {
        return memberNo;
    }

    public void setMemberNo(String memberNo) {
        this.memberNo = memberNo;
    }

    public class Data {
        List<UserEcoDate> IsSelectUserEcoDate;

        Data() {

        }

        public List<UserEcoDate> getIsSelectUserEcoDate() {
            return IsSelectUserEcoDate;
        }

        public void setIsSelectUserEcoDate(List<UserEcoDate> isSelectUserEcoDate) {
            this.IsSelectUserEcoDate = isSelectUserEcoDate;
        }

    }

    ;


    public class UserEcoDate {
        String date;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }


    }

    ;
}

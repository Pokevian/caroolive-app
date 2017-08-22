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

public class NoticeCategory {
    public String cmd;
    public String mod_id;
    public String board_tp_cd;
    public BoardInfo boardInfo;
    public List<CategoryData> categoryList;
    public String reg_id;
    public String board_no;


    NoticeCategory() {
    }

    public String getMod_id() {
        return mod_id;
    }

    public List<CategoryData> getCategoryList() {
        return categoryList;
    }

    public BoardInfo getBoardInfo() {
        return boardInfo;
    }

    public String getBoard_tp_cd() {
        return board_tp_cd;
    }

    public String getBoard_no() {
        return board_no;
    }

    public String getReg_id() {
        return reg_id;
    }


    public class CategoryData {

        public String REG_DATE;
        public String REG_ID;
        public String BOARD_NO;
        public String MOD_DATE;
        public String MOD_ID;
        public String CATEGORY_NM;
        public String ORDER_SEQ;
        public String ARTICLE_CNT;
        public String CATEGORY_NO;

        public CategoryData() {

        }


        public String getREG_DATE() {
            return REG_DATE;
        }

        public String getREG_ID() {
            return REG_ID;
        }

        public String getBOARD_NO() {
            return BOARD_NO;
        }

        public String getMOD_DATE() {
            return MOD_DATE;
        }

        public String getMOD_ID() {
            return MOD_ID;
        }

        public String getORDER_SEQ() {
            return ORDER_SEQ;
        }

        public String getCATEGORY_NM() {
            return CATEGORY_NM;
        }

        public String getARTICLE_CNT() {
            return ARTICLE_CNT;
        }

        public String getCATEGORY_NO() {
            return CATEGORY_NO;
        }

    }

    public class BoardInfo {

        public String MEM_COMMENT_YN;
        public String FILE_UPLOAD_YN;
        public String USE_YN;
        public String BOARD_TP_CD;
        public String REG_ID;
        public String SMS_REPLY_YN;
        public String CATEGORY_YN;
        public String MEM_WRITE_YN;
        public String REG_DATE;
        public String BOARD_NM;
        public String BOARD_ID;
        public String EMAIL_REPLY_YN;
        public String BOARD_NO;
        public String MOD_DATE;
        public String MOD_ID;
        public String EDITER_YN;

        public String getMEM_COMMENT_YN() {
            return MEM_COMMENT_YN;
        }

        public String getFILE_UPLOAD_YN() {
            return FILE_UPLOAD_YN;
        }

        public String getUSE_YN() {
            return USE_YN;
        }

        public String getBOARD_TP_CD() {
            return BOARD_TP_CD;
        }

        public String getREG_ID() {
            return REG_ID;
        }

        public String getSMS_REPLY_YN() {
            return SMS_REPLY_YN;
        }

        public String getCATEGORY_YN() {
            return CATEGORY_YN;
        }

        public String getMEM_WRITE_YN() {
            return MEM_WRITE_YN;
        }

        public String getREG_DATE() {
            return REG_DATE;
        }

        public String getBOARD_NM() {
            return BOARD_NM;
        }

        public String getBOARD_ID() {
            return BOARD_ID;
        }

        public String getEMAIL_REPLY_YN() {
            return EMAIL_REPLY_YN;
        }

        public String getBOARD_NO() {
            return BOARD_NO;
        }

        public String getMOD_DATE() {
            return MOD_DATE;
        }

        public String getMOD_ID() {
            return MOD_ID;
        }

        public String getEDITER_YN() {
            return EDITER_YN;
        }

    }
}

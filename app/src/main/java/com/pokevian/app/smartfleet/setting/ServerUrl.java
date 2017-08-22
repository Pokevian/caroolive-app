/*
 * Copyright (c) 2015. Pokevian Ltd.
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

package com.pokevian.app.smartfleet.setting;

/**
 * Created by dg.kim on 2015-03-31.
 */
public class ServerUrl {

    // Server URL
    public static final String SERVICE_SERVER_BASE_URL;
    public static final String GATHERING_SERVER_BASE_URL;


    static {
        SERVICE_SERVER_BASE_URL = "http://14.63.216.111:9801";
        GATHERING_SERVER_BASE_URL = "http://14.63.216.111:9701";
    }

    // REST API
    public static final String DATA_API = SERVICE_SERVER_BASE_URL + "/smart.json";
    public static final String SIGN_IN_API = SERVICE_SERVER_BASE_URL + "/mbrLogin.do";
    public static final String SESSION_CLOSED_API = SERVICE_SERVER_BASE_URL + "/index.do"; // represented by web log-in page
    public static final String DATA_UPLOAD_API = GATHERING_SERVER_BASE_URL + "/smart.json";

    public static final String GET_RANKING_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/stats/getRanking.do";
    public static final String GET_RANKING_TOP10_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/stats/getDrivingScoreRankingTop10.do";
    public static final String GET_RANKING_TOP10_ECO_SPEED_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/stats/getEcoSpeedRankingTop10.do";
    public static final String GET_RANKING_TOP10_FUEL_CUT_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/stats/getFuelCutRankingTop10.do";
    public static final String GET_RANKING_TOP10_HARSH_SPEED_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/stats/getHarshSpeedRankingTop10.do";
    public static final String GET_PATTERN_SCORESTAR_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/stats/getScoreStar.do";
    public static final String GET_RANKING_BEST_TRIP_LIST_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/ranking/getBestTripRankingList.do";
    public static final String GET_RANKING_BEST_TRIP_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/ranking/getBestTripMyRanking.do";
    public static final String GET_RANKING_TOP10_LEVEL_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/stats/getLevelRankingTop10.do";
    public static final String GET_RANKING_TOP10_POINT_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/stats/getPointRankingTop10.do";
    public static final String GET_RANKING_TOP10_FUEL_SAVE_API = SERVICE_SERVER_BASE_URL + "/api/v2/trip/stats/getFuelSaveRankingTop10.do";

    // Web Page URL
    public static final String HOME_URL = SERVICE_SERVER_BASE_URL + "/m/view.do?cmd=vDashboard";
    public static final String WEEKLY_RANKING_URL = SERVICE_SERVER_BASE_URL + "/m/view.do?cmd=vWeekRanking";
    public static final String GET_DRIVING_DATE_URL = SERVICE_SERVER_BASE_URL + "/m/trip.do?cmd=qSelectUserDrivingTripDate";
    public static final String CHART_DRIVING_STATISTICS_URL = SERVICE_SERVER_BASE_URL + "/m/chart.do?cmd=vDrivingStatisticsChart";
    public static final String CHART_ECOPOINT_URL = SERVICE_SERVER_BASE_URL + "/m/chart.do?cmd=vEcoChart";
    public static final String CHART_FUELECONOMY_URL = SERVICE_SERVER_BASE_URL + "/m/chart.do?cmd=vFuelEconomyChart";
    public static final String CHART_FUELCOST_URL = SERVICE_SERVER_BASE_URL + "/m/chart.do?cmd=vFuelCostsChart";
    public static final String CHART_DRIVINGRATE_URL = SERVICE_SERVER_BASE_URL + "/m/chart.do?cmd=vDrivingRatiosChart";
    public static final String VIDEO_LIST_URL = SERVICE_SERVER_BASE_URL + "/m/drvYtVideo.do?cmd=vVideoMain";
    public static final String NOTICE_CATEGORY_LIST_URL = SERVICE_SERVER_BASE_URL + "/m/board.do?cmd=qSelectCategoryList";
    public static final String NOTICE_LIST_URL = SERVICE_SERVER_BASE_URL + "/m/board.do?cmd=vBoard";

    public static String UNREACHABLE_URL = "file:///android_asset/error_webview.html";

    public static final String CONTRACT_DETAIL_PERSONAL = SERVICE_SERVER_BASE_URL + "/web/contractDetail_01.html";
    public static final String CONTRACT_DETAIL_SERVICE = SERVICE_SERVER_BASE_URL + "/web/contractDetail_02.html";
    public static final String CONTRACT_DETAIL_LOCATION = SERVICE_SERVER_BASE_URL + "/web/contractDetail_03.html";

    public static final String EVENT_PAGE_URL = SERVICE_SERVER_BASE_URL + "/m/view.do?cmd=vEventObd";
    public static final String MY_POINT_URL = SERVICE_SERVER_BASE_URL + "/m/view.do?cmd=vMemberPointLog";
    public static final String CAR_STATUS_CHECK_URL = SERVICE_SERVER_BASE_URL + "/m/view.do?cmd=vCarStatusCheck";
    public static final String DRIVING_PATTERN_URL = SERVICE_SERVER_BASE_URL + "/m/view.do?cmd=vDrivingPattern";
    public static final String MEMBER_RANKING_URL = SERVICE_SERVER_BASE_URL + "/m/view.do?cmd=vMemberRanking";
    public static final String HALL_OF_FAME = SERVICE_SERVER_BASE_URL + "/m/view.do?cmd=vHallOfFamePoint";
    public static final String DIAGNOSTICS_URL = SERVICE_SERVER_BASE_URL + "/m/view.do?cmd=vCarDiagList";

}

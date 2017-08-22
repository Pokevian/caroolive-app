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

package com.pokevian.app.smartfleet.request;

import com.android.volley.Response.Listener;
import com.pokevian.app.smartfleet.model.tripmonitor.NoticeCategory;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.volley.GsonRequest;
import com.pokevian.app.smartfleet.volley.VolleyListener;

public class GetNoticeCategoryRequest extends GsonRequest<NoticeCategory> {

    public GetNoticeCategoryRequest(final VolleyListener<NoticeCategory> listener) {
        super(ServerUrl.NOTICE_CATEGORY_LIST_URL, NoticeCategory.class, listener);

        setListener(new Listener<NoticeCategory>() {
            public void onResponse(NoticeCategory response) {
                listener.onResponse(response);
            }
        });
    }

}

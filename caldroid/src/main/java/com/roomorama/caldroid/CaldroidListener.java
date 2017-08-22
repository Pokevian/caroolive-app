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

package com.roomorama.caldroid;

import java.util.Date;

import android.view.View;

/**
 * CaldroidListener inform when user clicks on a valid date (not within disabled
 * dates, and valid between min/max dates)
 * 
 * The method onChangeMonth is optional, user can always override this to listen
 * to month change event
 * 
 * @author thomasdao
 * 
 */
public abstract class CaldroidListener {
	/**
	 * Inform client user has clicked on a date
	 * @param date
	 * @param view
	 */
	public abstract void onSelectDate(Date date, View view);

	
	/**
	 * Inform client user has long clicked on a date
	 * @param date
	 * @param view
	 */
	public void onLongClickDate(Date date, View view) {
		// Do nothing
	}

	
	/**
	 * Inform client that calendar has changed month
	 * @param month
	 * @param year
	 */
	public void onChangeMonth(int month, int year) {
		// Do nothing
	};

	
	/**
	 * Inform client that CaldroidFragment view has been created and views are
	 * no longer null. Useful for customization of button and text views
	 */
	public void onCaldroidViewCreated() {
		// Do nothing
	}
}

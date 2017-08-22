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

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * MonthPagerAdapter holds 4 fragments, which provides fragment for current
 * month, previous month and next month. The extra fragment helps for recycle
 * fragments.
 * 
 * @author thomasdao
 * 
 */
public class MonthPagerAdapter extends FragmentPagerAdapter {

	private ArrayList<DateGridFragment> fragments;

	// Lazily create the fragments
	public ArrayList<DateGridFragment> getFragments() {
		if (fragments == null) {
			fragments = new ArrayList<DateGridFragment>();
			for (int i = 0; i < getCount(); i++) {
				fragments.add(new DateGridFragment());
			}
		}
		return fragments;
	}

	public void setFragments(ArrayList<DateGridFragment> fragments) {
		this.fragments = fragments;
	}

	public MonthPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		DateGridFragment fragment = getFragments().get(position);
		return fragment;
	}

	@Override
	public int getCount() {
		// We need 4 gridviews for previous month, current month and next month,
		// and 1 extra fragment for fragment recycle
		return CaldroidFragment.NUMBER_OF_PAGES;
	}

}

/*  Copyright 2012
 *	Lorenzo Braghetto monossido@lorenzobraghetto.com
 *      This file is part of SpeakBird <https://github.com/monossido/SpeakBird>
 *      
 *      SpeakBird is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *      
 *      SpeakBird is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *      
 *      You should have received a copy of the GNU General Public License
 *      along with SpeakBird  If not, see <http://www.gnu.org/licenses/>.
 *      
 */
package com.lorenzobraghetto.speakbird.View;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.lorenzobraghetto.speakbird.R;

public class Tweet extends SherlockFragmentActivity implements
		TitlesFragment.OnItemSelectedListener {

	private boolean mDualFragments;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TitlesFragment frag;
		try {
			setContentView(R.layout.fragments);

			frag = (TitlesFragment) getSupportFragmentManager()
					.findFragmentById(R.id.titles_frag);
		} catch (Exception e) {
			frag = null;
		}

		if (frag == null) {
			final ActionBar bar = getSupportActionBar();
			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

			Tab tab1 = bar
					.newTab()
					.setText(getString(R.string.mentions))
					.setTabListener(
							new TabsAdapterT<Mentions>(this, Mentions.class,
									"0"));

			Tab tab2 = bar
					.newTab()
					.setText(getString(R.string.messages))
					.setTabListener(
							new TabsAdapterT<Mentions>(this, Mentions.class,
									"1"));

			if (getIntent().getExtras() != null
					&& getIntent().getExtras().getBoolean("messages")) {
				bar.addTab(tab1, false);
				bar.addTab(tab2, true);
			} else {
				bar.addTab(tab1);
				bar.addTab(tab2);
			}

			if (savedInstanceState != null) {
				bar.setSelectedNavigationItem(savedInstanceState.getInt("tab",
						0));
			}
		} else {

			Mentions fragm = (Mentions) getSupportFragmentManager()
					.findFragmentById(R.id.content_frag);
			// Messages fragM = (Messages) getSupportFragmentManager()
			// .findFragmentById(R.id.content_frag);
			if (fragm != null)
				mDualFragments = true;
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", getSupportActionBar()
				.getSelectedNavigationIndex());
	}

	private static class TabsAdapterT<T extends SherlockFragment> implements
			ActionBar.TabListener {
		private Fragment mFragment;
		private final SherlockFragmentActivity mActivity;
		private final Class<T> mClass;
		private String tag;

		/**
		 * Constructor used each time a new tab is created.
		 * 
		 * @param activity
		 *            The host Activity, used to instantiate the fragment
		 * @param tag
		 *            The identifier tag for the fragment
		 * @param clz
		 *            The fragment's Class, used to instantiate the fragment
		 */
		public TabsAdapterT(SherlockFragmentActivity activity, Class<T> clz,
				String tags) {
			mActivity = activity;
			mClass = clz;
			tag = tags;
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			// Check if the fragment is already initialized
			if (mFragment == null) {
				// If not, instantiate and add it to the activity
				mFragment = Fragment.instantiate(mActivity, mClass.getName());
				ft.add(android.R.id.content, mFragment, tag);
			} else {
				// If it exists, simply attach it in order to show it
				ft.attach(mFragment);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (mFragment != null) {
				// Detach the fragment, because another one is being attached
				ft.detach(mFragment);
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// User selected the already selected tab. Usually do nothing.
		}
	}

	public void onItemSelected(int category, int position) {
		if (!mDualFragments) {

		} else {
			Log.v("SPEAKBIRD", "mentions4?");

			// If showing both fragments, directly update the ContentFragment
			Mentions frag = (Mentions) getSupportFragmentManager()
					.findFragmentById(R.id.content_frag);
			frag.updateContentAndRecycleBitmap(category, position);
		}
	}

}

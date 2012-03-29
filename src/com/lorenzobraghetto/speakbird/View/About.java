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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.lorenzobraghetto.speakbird.R;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class About extends SherlockFragmentActivity {
private ViewPager mViewPager;
private TabsAdapter mTabsAdapter;

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    mViewPager = new ViewPager(this);
	    mViewPager.setId(R.id.pager);
	    setContentView(mViewPager);
	    final ActionBar bar = getSupportActionBar();
	    bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	
	    mTabsAdapter = new TabsAdapter(this, mViewPager);
	    mTabsAdapter.addTab(bar.newTab().setText("About"),
	            AboutFragment.class, null);
	    mTabsAdapter.addTab(bar.newTab().setText("Changelog"),
	            ChangelogFragment.class, null);
	    mTabsAdapter.addTab(bar.newTab().setText("License"),
	            LicenseFragment.class, null);
	
	    if (savedInstanceState != null) {
	        bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
	    }
	}
	
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
	}
	
	public static class AboutFragment extends SherlockFragment {
    	@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.dialoginfo, container, false);
			return v;
    	}
    }
	
	public static class ChangelogFragment extends SherlockFragment {
    	@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.changelog, container, false);
			return v;
    	}
    }
    
    public static class LicenseFragment extends SherlockFragment {
    	@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.license, container, false);
			return v;
    	}
    }

}
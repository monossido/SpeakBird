package com.lorenzobraghetto.speakbird.View;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;


import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.lorenzobraghetto.speakbird.R;
import com.lorenzobraghetto.speakbird.View.TabsAdapter.TabInfo;

public class Tweet extends SherlockFragmentActivity {

		public void onCreate(Bundle savedInstanceState) {
		    super.onCreate(savedInstanceState);

		    final ActionBar bar = getSupportActionBar();
		    bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		    Tab tab1 = bar.newTab().setText(getString(R.string.mentions))
		    .setTabListener(new TabsAdapterT<Mentions>(this, Mentions.class));

		    Tab tab2 = bar.newTab().setText(getString(R.string.messages))
		    .setTabListener(new TabsAdapterT<Messages>(this, Messages.class));

		    if(getIntent().getExtras()!=null && getIntent().getExtras().getBoolean("messages"))
	    	{
	    		bar.addTab(tab1, false);
	    		bar.addTab(tab2, true);
	    	}else
	    	{
	    		bar.addTab(tab1);
	    		bar.addTab(tab2);
	    	}
		
		    if (savedInstanceState != null) {
		        bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
		    }
		}
		
		protected void onSaveInstanceState(Bundle outState) {
		    super.onSaveInstanceState(outState);
		    outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
		}
		
		 private static class TabsAdapterT<T extends SherlockFragment> implements ActionBar.TabListener { 
		                 private Fragment mFragment; 
		                 private final SherlockFragmentActivity mActivity; 
		                 private final Class<T> mClass; 
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
		                 public TabsAdapterT(SherlockFragmentActivity activity, 
		 Class<T> clz) { 
		                         mActivity = activity; 
		                         mClass = clz; 
		                 } 

		                 public void onTabSelected(Tab tab, FragmentTransaction ft) {
		                     // Check if the fragment is already initialized
		                     if (mFragment == null) {
		                         // If not, instantiate and add it to the activity
		                         mFragment = Fragment.instantiate(mActivity, mClass.getName());
		                         ft.add(android.R.id.content, mFragment, null);
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

	}

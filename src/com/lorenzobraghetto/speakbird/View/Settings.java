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
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.lorenzobraghetto.speakbird.R;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;


public class Settings extends SherlockPreferenceActivity
{
	 @Override
     protected void onCreate(Bundle savedInstanceState) {
             super.onCreate(savedInstanceState);
             
             addPreferencesFromResource(R.xml.preference);
             
             ActionBar actionBar = getSupportActionBar();
             actionBar.setDisplayHomeAsUpEnabled(true);
             
             SharedPreferences prefs = PreferenceManager
                     .getDefaultSharedPreferences(getBaseContext());
             
             ListPreference prefBtn = (ListPreference) findPreference("listPref");
             final CheckBoxPreference musicPrefs = (CheckBoxPreference) findPreference("onMusic");
             
             final CheckBoxPreference speakNotifing = (CheckBoxPreference) findPreference("speakNotifing");
             
             final CheckBoxPreference automaticSAP = (CheckBoxPreference) findPreference("automaticSAP");
             
             final CheckBoxPreference notificationSpeaking = (CheckBoxPreference) findPreference("notificationSpeaking");
             
             final ListPreference language = (ListPreference) findPreference("language");

        	 final CheckBoxPreference automaticNotification = (CheckBoxPreference) findPreference("automaticNotification");
             
             prefBtn.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					if(arg1.toString().compareTo("notification")==0)
					{
						speakNotifing.setEnabled(true);
						automaticSAP.setEnabled(true);
						musicPrefs.setEnabled(true);
						automaticNotification.setEnabled(false);
					}
					else
					{
						speakNotifing.setEnabled(false);
						musicPrefs.setEnabled(false);
						automaticSAP.setEnabled(false);
						automaticNotification.setEnabled(true);
					}
			        
					return true;
				}
             });
             
             String type = prefs.getString("listPref", "speakASpull");
             if(type.compareTo("speakASpull")==0)	
             {
            	 speakNotifing.setEnabled(false);
            	 musicPrefs.setEnabled(false);
            	 automaticSAP.setEnabled(false);
             }
        	 OnPreferenceChangeListener resetSync = new OnPreferenceChangeListener() {

				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					Account account = new Account("Speakbird Twitter Account", "com.lorenzobraghetto.speakbird.account");
					ContentResolver.addPeriodicSync(account, "com.lorenzobraghetto.speakbird.content", new Bundle(), Long.parseLong(arg1.toString())*60);
					
					return true;
				}
        		 
        	 };
             
        	 EditTextPreference updateInterval = (EditTextPreference) findPreference("updateInterval");
        	 updateInterval.setOnPreferenceChangeListener(resetSync); 
        	 
        	 

	}
	 
	 @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        // Handle item selection
	        switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, Main.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	        }
	    }
	
}
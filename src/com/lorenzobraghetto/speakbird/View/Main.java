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

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.lorenzobraghetto.speakbird.R;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class Main extends SherlockActivity {
    
	private SharedPreferences prefs;
	private Context mContext;
	private Dialog dialog;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mContext=this;
		dialog = new Dialog(mContext);

        
		prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
		
		String lastTweetUser = prefs.getString("lastTweetUser", "");
		if(lastTweetUser.length()>0)
		{
			((RelativeLayout)findViewById(R.id.relativeLayout2)).setVisibility(0);
			((TextView)findViewById(R.id.user)).setText("@"+lastTweetUser);
		}
        
       
        
        Button button1 = (Button)findViewById(R.id.button1);  
        button1.setOnClickListener(new OnClickListener() {    

		public void onClick(View arg0) {
			Intent mentions = new Intent(getBaseContext(), Mentions.class);
			startActivity(mentions);
		}
        
       });
        Button button2 = (Button)findViewById(R.id.button2);  
        button2.setOnClickListener(new OnClickListener() {    

		public void onClick(View arg0) {
			Intent settings = new Intent(getBaseContext(), Settings.class);
			startActivity(settings);
		}
        
       });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.xml.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.info:
			 dialog.setContentView(R.layout.dialoginfo);
			 try {
				((TextView)dialog.findViewById(R.id.version)).setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 dialog.setTitle("Info");
			 dialog.show();
			 return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
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
package com.lorenzobraghetto.speakbird.Logic;


import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;



	public class DeleteReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(context);
			Editor editor = settings.edit();
			
			if(intent.getExtras().getBoolean("fromMentions")==true)
			{
	    		editor.putString("lastTweet", intent.getExtras().getLong("lastTweet")+"");
	    		editor.putString("lastTweetUser", intent.getExtras().getString("lastTweetUser")+"");
	    		context.stopService(new Intent(context,TwitterService.class));
			}else
			{
	    		editor.putString("lastTweetMessage", intent.getExtras().getLong("lastTweetMessage")+"");
	    		editor.putString("lastTweetMessageUser", intent.getExtras().getString("lastTweetMessageUser")+"");
				context.stopService(new Intent(context,TwitterServiceM.class));
			}
    		editor.commit();
			
		}

	
	}
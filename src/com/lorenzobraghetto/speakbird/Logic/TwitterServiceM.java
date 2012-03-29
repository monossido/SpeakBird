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

import java.util.HashMap;
import java.util.Locale;

import com.lorenzobraghetto.speakbird.R;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;
import android.widget.Toast;

public class TwitterServiceM extends TwitterService
{
	private ResponseList<DirectMessage> messages;
	private String myText1;
    private int result;
	private int newTweet;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		mContext=this;
		
		Log.v("SPEAKBIRD","Service started");
		
		if((mTts !=null && mTts.isSpeaking()) || !isNetworkAvailable())
			return 0;
		warns=false;
		mNotificationManager = (NotificationManager) getSystemService(ns);
		
		settings = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
		editor = settings.edit();

		
		if(intent!=null && intent.getExtras()!=null && intent.getExtras().getBoolean("notificationToSpeak", false))//From notification?
		{
			if(((SpeakBirdApplication)getApplication()).isBusy())
				return 0;
			else
			{
				((SpeakBirdApplication)getApplication()).setBusy(true);
				mTts = new TextToSpeech(this, this);
				mNotificationManager.cancel(4);
			 	return START_STICKY;
			}
		}
		
	      //check for saved log in details..  
        boolean autenticated = checkForSavedLogin();  
        audiom = (AudioManager) getSystemService(AUDIO_SERVICE);
                
         if(!autenticated )//First start?
        	 return 0;

         new TwitterBackgroundM().execute(intent);
        
		return START_STICKY;
		
	}
	
	
	private class TwitterBackgroundM extends AsyncTask<Intent, Void, Void>
	{

		@Override
		protected Void doInBackground(Intent... intents) {

	        
			String lastTweet = "";
			
			lastTweet = settings.getString("lastTweetMessage", "");

			Paging since =new Paging();
	        if(lastTweet.length()!=0)
	        	since = new Paging(Long.parseLong(lastTweet));

			try {
				messages = twitter.getDirectMessages(since);
			} catch (TwitterException e) {
				e.printStackTrace();
				stopSelf();
				return null;
			}

	    	boolean exit = false;
	    	newTweet=0;
	        for(; !exit && newTweet<messages.size(); newTweet++)
	        {
	        	if(lastTweet.compareTo(messages.get(newTweet).getId()+"")==0)
	        		exit=true;
	        }
	    	if(lastTweet.length()==0)//if there are some problems or if it's the first time that the service is running
	    	{
	    		lastTweet = messages.get(0).getId()+"";
	    		editor.putString("lastTweetMessage", messages.get(0).getId()+"");
	    		editor.commit();
	    		newTweet=0;
	    	}

	    	if(newTweet>0)//There is almost one tweet
			{
	    		boolean automaticNotification = settings.getBoolean("automaticNotification", false);
				if(settings.getString("listPref", "notification").compareTo("speakASpull")==0 
						&& (!automaticNotification || (automaticNotification && audiom.getRingerMode() != AudioManager.RINGER_MODE_SILENT && audiom.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE))
						|| (settings.getBoolean("onMusic", false) && audiom.isMusicActive())
						|| (settings.getBoolean("automaticSAP", false) && audiom.isWiredHeadsetOn()))//Speak as pull o notifica?
					mTts = new TextToSpeech(getApplicationContext(), TwitterServiceM.this);
				else
				{
					warns = settings.getBoolean("speakNotifing", false);
					if(warns)
						mTts = new TextToSpeech(getApplicationContext(), TwitterServiceM.this);
	        		int icon = R.drawable.icon;
	        		CharSequence tickerText = getString(R.string.wSpeaking);
	        		long when = System.currentTimeMillis();

	        		Notification notificationspeak = new Notification(icon, tickerText, when);
	        		
	        		Context context = getApplicationContext();
	        		CharSequence contentTitle = "SpeakBird notification";
	        		
	        		String sMessage;
	        		if(newTweet>1)
	        			sMessage=newTweet+" "+getString(R.string.messages).toLowerCase();
	        		else
	        			sMessage=newTweet+" "+getString(R.string.message);
	        		
	        		CharSequence contentText = getString(R.string.wSpeaking)+" - "+sMessage;
	        		
	        		Bundle toService = new Bundle();
	        	    toService.putBoolean("notificationToSpeak", true);

	        		Intent notificationSpeak = new Intent(getApplicationContext(), TwitterServiceM.class);
	        		notificationSpeak.putExtras(toService);
	        		
	        		PendingIntent contentIntent = PendingIntent.getService(context, 1234567, notificationSpeak, PendingIntent.FLAG_CANCEL_CURRENT);

	        		notificationspeak.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	        		
	        		final int HELLO_ID = 4;
	        		
	        		Intent deleteNotification = new Intent();
	        		deleteNotification.setClass(getApplicationContext(), DeleteReceiver.class);
	        		deleteNotification.putExtra("lastTweetMessage", messages.get(0).getId());
	        		deleteNotification.putExtra("lastTweetMessageUser", messages.get(0).getSender().getScreenName());
	        		deleteNotification.putExtra("fromMentions", false);

	        		
	        		notificationspeak.deleteIntent = PendingIntent.getBroadcast(context, 123456, deleteNotification, PendingIntent.FLAG_CANCEL_CURRENT);

	        		mNotificationManager.notify(HELLO_ID, notificationspeak);
				}
			}else
				stopSelf();
			return null;
			
		}
		
	}
	
	@Override
	public void onInit(int arg0) {
		Locale locale = getLocale();

        if(mTts.isLanguageAvailable(locale)>0)
        {   mTts.setLanguage(locale);
        	if(warns)
        	{
        		result = -2;
        		result = mTts.speak(getString(R.string.newm), TextToSpeech.QUEUE_ADD, null);
        		while(result!=0)
        			mTts.shutdown();
        		return;
        	}
        	//Notification while speaking?
        	if(settings.getBoolean("notificationSpeaking", false))
        	{
        		
        		int icon = R.drawable.icon;
        		CharSequence tickerText = getString(R.string.isSpeaking);
        		long when = System.currentTimeMillis();

        		Notification notification = new Notification(icon, tickerText, when);
        		
        		Context context = getApplicationContext();
        		CharSequence contentTitle = "SpeakBird notification";
        		CharSequence contentText = getString(R.string.isSpeakingC);
        		
        		Intent deleteNotification = new Intent();
        		deleteNotification.setClass(getApplicationContext(), DeleteReceiver.class);
        		deleteNotification.putExtra("lastTweetMessage", messages.get(0).getId());
        		deleteNotification.putExtra("lastTweetMessageUser", messages.get(0).getSender().getScreenName());
        		deleteNotification.putExtra("fromMentions", false);

        		PendingIntent contentIntent = PendingIntent.getBroadcast(context, 123456,
        				deleteNotification, PendingIntent.FLAG_CANCEL_CURRENT);

        		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        		
        		final int HELLO_ID = 3;
        		
        		notification.flags |= Notification.FLAG_ONGOING_EVENT;


        		mNotificationManager.notify(HELLO_ID, notification);
        	}
        	
        	if(newTweet>1)
        		mTts.speak(getString(R.string.haveMS)+" "+newTweet+" "+getString(R.string.haveMMS), TextToSpeech.QUEUE_ADD, null);

            for(int z=newTweet-1; z>=0; z--)
            {
            	mTts.setOnUtteranceCompletedListener(this);
            	HashMap<String, String> myHashAlarm = new HashMap();
            	if(z==0)
            		myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
            	        "end of wakeup message ID");
	            myText1 = getString(R.string.messageBy)+" "+ messages.get(z).getSender().getScreenName() +" \" "+messages.get(z).getText();

            	mTts.speak(myText1, TextToSpeech.QUEUE_ADD, myHashAlarm);
            }
            
            editor.putString("lastTweetMessage", messages.get(0).getId()+"");
    		editor.putString("lastTweetMessageUser", messages.get(0).getSender().getScreenName());
    		editor.commit();
            
        }else{
        	Toast.makeText(getApplicationContext(), "There was a problem with yout TTS Locale, maybe it's missing?", Toast.LENGTH_SHORT).show();
        }
		
	}
	
	public void onDestroy()
	{
		mNotificationManager.cancel(3);
		if(mTts!=null)
		{
			mTts.stop();
	        mTts.shutdown();
		}
        twitter.shutdown();
		((SpeakBirdApplication)getApplication()).setBusy(false);

	}
    
}
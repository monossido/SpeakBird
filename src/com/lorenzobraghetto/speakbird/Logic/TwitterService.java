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

public class TwitterService extends Service implements OnInitListener, OnUtteranceCompletedListener
{
	protected Twitter twitter;
	private ResponseList<Status> mentions;
	private String myText1;
	protected TextToSpeech mTts;
	protected String CONSUMER_KEY = "CaLz8BjfUQdFZ19i0Ni5mA";
	protected String CONSUMER_SECRET = "2Djfy4vFEMeZ4ft7vC1EakzPwrtSHVkmBigJdrZg";
    private int result;
    protected SharedPreferences settings;
	private int newTweet;
	protected String ns = NOTIFICATION_SERVICE;
	protected NotificationManager mNotificationManager;
	protected SharedPreferences.Editor editor;
	protected boolean warns;
	protected static Context mContext;
	protected AudioManager audiom;
	
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
				mNotificationManager.cancel(2);
			 	return START_STICKY;
			}
		}
		
	      //check for saved log in details..  
        boolean autenticated = checkForSavedLogin();  
        audiom = (AudioManager) getSystemService(AUDIO_SERVICE);
                
        if(!autenticated )//First start?
        	 return 0;

         new TwitterBackground().execute(intent);
        
		return START_STICKY;
		
	}
	
	private class TwitterBackground extends AsyncTask<Intent, Void, Void>
	{

		@Override
		protected Void doInBackground(Intent... intents) {

			String lastTweet = "";
			
			lastTweet = settings.getString("lastTweet", "");

			Paging since =new Paging();
	        if(lastTweet.length()!=0)
	        	since = new Paging(Long.parseLong(lastTweet));

			try {
				mentions = twitter.getMentions(since);
			} catch (TwitterException e) {
				e.printStackTrace();
				stopSelf();
				return null;
			}
			
			boolean exit = false;
	    	newTweet=0;
	        for(; !exit && newTweet<mentions.size(); newTweet++)
	        {
	        	if(lastTweet.compareTo(mentions.get(newTweet).getId()+"")==0)
	        		exit=true;
	        }
	    	if(lastTweet.length()==0)//if there are some problems or if it's the first time that the service is running
	    	{
	    		lastTweet = mentions.get(0).getId()+"";
	    		editor.putString("lastTweet", mentions.get(0).getId()+"");
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
					mTts = new TextToSpeech(getApplicationContext(), TwitterService.this);
				else
				{
					warns = settings.getBoolean("speakNotifing", false);
					if(warns)
						mTts = new TextToSpeech(getApplicationContext(), TwitterService.this);
	        		int icon = R.drawable.icon;
	        		CharSequence tickerText = getString(R.string.wSpeaking);
	        		long when = System.currentTimeMillis();

	        		Notification notificationspeak = new Notification(icon, tickerText, when);
	        		
	        		Context context = getApplicationContext();
	        		CharSequence contentTitle = "SpeakBird notification";
	        		
	        		String sMentions;
	        		if(newTweet>1)
	        			sMentions=newTweet+" "+getString(R.string.mentions).toLowerCase();
	        		else
	        			sMentions=newTweet+" "+getString(R.string.mention);
	        		
	        		CharSequence contentText = getString(R.string.wSpeaking)+" - "+sMentions;
	        		
	        		Bundle toService = new Bundle();
	        	    toService.putBoolean("notificationToSpeak", true);

	        		Intent notificationSpeak = new Intent(getApplicationContext(), TwitterService.class);
	        		notificationSpeak.putExtras(toService);
	        		
	        		PendingIntent contentIntent = PendingIntent.getService(context, 1234567, notificationSpeak, PendingIntent.FLAG_CANCEL_CURRENT);

	        		notificationspeak.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	        		
	        		final int HELLO_ID = 2;
	        		
	        		Intent deleteNotification = new Intent();
	        		deleteNotification.setClass(getApplicationContext(), DeleteReceiver.class);
	        		deleteNotification.putExtra("lastTweet", mentions.get(0).getId());
	        		deleteNotification.putExtra("lastTweetUser", mentions.get(0).getUser().getScreenName());
	        		deleteNotification.putExtra("fromMentions", true);
	        		
	        		notificationspeak.deleteIntent = PendingIntent.getBroadcast(context, 123456, deleteNotification, PendingIntent.FLAG_CANCEL_CURRENT);

	        		mNotificationManager.notify(HELLO_ID, notificationspeak);
				}
			}else
				stopSelf();
			return null;
			
		}
		
	}
	
	public static boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
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
        		deleteNotification.putExtra("fromMentions", true);
        		deleteNotification.putExtra("lastTweet", mentions.get(0).getId());
        		deleteNotification.putExtra("lastTweetUser", mentions.get(0).getUser().getScreenName());
        		
        		PendingIntent contentIntent = PendingIntent.getBroadcast(context, 123456,
        				deleteNotification, PendingIntent.FLAG_CANCEL_CURRENT);

        		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        		
        		final int HELLO_ID = 1;
        		
        		notification.flags |= Notification.FLAG_ONGOING_EVENT;


        		mNotificationManager.notify(HELLO_ID, notification);
        	}
        	
        	if(newTweet>1)
        		mTts.speak(getString(R.string.have)+" "+newTweet+" "+getString(R.string.haveM), TextToSpeech.QUEUE_ADD, null);

            for(int z=newTweet-1; z>=0; z--)
            {
            	mTts.setOnUtteranceCompletedListener(this);
            	HashMap<String, String> myHashAlarm = new HashMap();
            	if(z==0)
            		myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
            	        "end of wakeup message ID");
	            myText1 = getString(R.string.mentionedBy)+" "+ mentions.get(z).getUser().getScreenName() +" \" "+mentions.get(z).getText();

            	mTts.speak(myText1, TextToSpeech.QUEUE_ADD, myHashAlarm);
            }
            
            editor.putString("lastTweet", mentions.get(0).getId()+"");
    		editor.putString("lastTweetUser", mentions.get(0).getUser().getScreenName());
    		editor.commit();
            
        }else{
        	Toast.makeText(getApplicationContext(), "There was a problem with yout TTS Locale, maybe it's missing?", Toast.LENGTH_SHORT).show();
        }
		
	}
	
	public void onUtteranceCompleted(String uttId)	{
		stopSelf();
		
	}
	
	public void onDestroy()
	{
		mNotificationManager.cancel(1);
		if(mTts!=null)
		{
			mTts.stop();
	        mTts.shutdown();
		}
        twitter.shutdown();
		((SpeakBirdApplication)getApplication()).setBusy(false);

	}
	
	protected boolean checkForSavedLogin() {  
   	 // Get Access Token and persist it  
   	 AccessToken a = getAccessToken();  
   	 if (a==null) 
   		 {
   		 	return false; //if there are no credentials stored then return to usual activity  
   		 }
   	  
   	 // initialize Twitter4J  
   	 twitter = new TwitterFactory().getInstance();  
   	 twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);  
   	 twitter.setOAuthAccessToken(a);  
   	((SpeakBirdApplication)getApplication()).setTwitter(twitter);  
   	   
   	 //startFirstActivity();  
   	 //finish();  
   	 return true;
   	}
    
    private AccessToken getAccessToken() {
		SharedPreferences settings = getSharedPreferences("Auth", MODE_PRIVATE);
		String token = settings.getString("accessTokenToken", "");
		String tokenSecret = settings.getString("accessTokenSecret", "");
		if (token!=null && tokenSecret!=null && !"".equals(tokenSecret) && !"".equals(token)){
			return new AccessToken(token, tokenSecret);
		}
		return null;
	}

    protected Locale getLocale()
    {
    	String locale = settings.getString("language", "");
    	if(locale.length()==0)
    		return Locale.getDefault();
    	if(locale.compareTo("UK")==0)
    		return Locale.UK;
    	else if(locale.compareTo("US")==0)
    		return Locale.US;
    	else if(locale.compareTo("FRENCH")==0)
    		return Locale.FRENCH;
    	else if(locale.compareTo("ITALIAN")==0)
    		return Locale.ITALIAN;
    	else if(locale.compareTo("SPANISH")==0)
    		return new Locale("es");
    	else if(locale.compareTo("GERMAN")==0)
    		return Locale.GERMAN;
		return Locale.getDefault();

    }
    
}
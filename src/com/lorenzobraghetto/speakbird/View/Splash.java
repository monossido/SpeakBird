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
 
 import java.util.Locale;

import com.lorenzobraghetto.speakbird.R;
import com.lorenzobraghetto.speakbird.Logic.SpeakBirdApplication;

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import twitter4j.auth.AccessToken;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
 
 
 
public class Splash extends Activity implements OnInitListener {
       
 
		static final private int TTS_CHECK_CODE = 1;
		private TextToSpeech mTts;
		private SharedPreferences authsettings;
		private String CONSUMER_KEY = "CaLz8BjfUQdFZ19i0Ni5mA";
		private String CONSUMER_SECRET = "2Djfy4vFEMeZ4ft7vC1EakzPwrtSHVkmBigJdrZg";
		private String CALLBACK_URL =  "x-latify-oauth-twitter://callback";
		private OAuthProvider provider;
		private CommonsHttpOAuthConsumer consumer;
		private Context context;
		private AccessToken a;
		private ImageView image3;
		private ImageView image4;
 
        /** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle icicle) {
        	super.onCreate(icicle);
           	setContentView(R.layout.splashscreen);
           	
        	context = this;
        	
    		authsettings = getSharedPreferences("Auth", MODE_PRIVATE);
    		
        	
			if (getIntent()!=null && this.getIntent().getData()!=null)//from browser
				return;
        	
			if(getIntent().getAction().equals("com.lorenzobraghetto.speakbird.Sync.LOGIN"))//from android settings
			{
				CALLBACK_URL = "x-latify-oauth-twitter://callbackfromsync";
				((SpeakBirdApplication)getApplication()).setExtraAccount(getIntent().getExtras());

			}                	
			
            
            //check for saved log in details..  
            checkForSavedLogin(); 
            
            getConsumerProvider(); 
            
			//Check TTS
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(checkIntent, TTS_CHECK_CODE); 

        	}
        
        
		public void onInit(int arg0) {
            if(mTts.isLanguageAvailable(Locale.UK)>0)
            {      
            	//TTS
            }
            mTts.shutdown();
			
		}
		
		@Override
		public void onResume()
		{
			super.onResume();

			if (getIntent()!=null && this.getIntent().getData()!=null){
				Uri uri = getIntent().getData();
				if (uri != null && uri.toString().startsWith("x-latify-oauth-twitter")) {
		            getConsumerProvider();  

					
				    String verifier = uri.getQueryParameter("oauth_verifier");  
					provider = new DefaultOAuthProvider("https://api.twitter.com/oauth/request_token", "https://api.twitter.com/oauth/access_token", "https://api.twitter.com/oauth/authorize");
					setConsumerProvider();


					try {

						// this will populate token and token_secret in consumer
						provider.retrieveAccessToken(consumer, verifier);
		
						// Get Access Token and persist it
						AccessToken a = new AccessToken(consumer.getToken(), consumer.getTokenSecret());
						storeAccessToken(a);
						
						//accountsync
						Account account = new Account("Speakbird Twitter Account", "com.lorenzobraghetto.speakbird.account");
						AccountManager am = AccountManager.get(this);
						boolean accountCreated = am.addAccountExplicitly(account, null, null);
						ContentResolver.setSyncAutomatically(account, "com.lorenzobraghetto.speakbird.content", true);

						
						Bundle extras = ((SpeakBirdApplication)getApplication()).getExtraAccount();
						if(uri.toString().contains("fromsync"))
						{
							if (extras != null) {
							 if (accountCreated) {  //Pass the new account back to the account manager
							  AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
							  Bundle result = new Bundle();
							  result.putString(AccountManager.KEY_ACCOUNT_NAME, "Speakbird Twitter Account");
							  result.putString(AccountManager.KEY_ACCOUNT_TYPE, "com.lorenzobraghetto.speakbird.account");
							  response.onResult(result);
							 }
				             finish();
							 startActivity(new Intent("android.settings.SYNC_SETTINGS"));
				             finish();
							 return;
							}
						}
						Toast.makeText(this, getString(R.string.newaccount), Toast.LENGTH_LONG).show();

			            Intent mainIntent = new Intent(Splash.this,Main.class);
		                finish();
		    			startActivity(mainIntent);
		
					} catch (Exception e) {
						//Log.e(APP, e.getMessage());
						e.printStackTrace();
						Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
						Log.v("SPEAKBIRD","SPEAKBIRD errore2 "+e.getMessage());

					}
				}

			}
			
		}
		
		private void checkForSavedLogin() {  
			 // Get Access Token and persist it  
			 a = getAccessToken();  
			 
		}
		
		private void setConsumerProvider() {
			if (provider!=null){
				((SpeakBirdApplication)getApplication()).setProvider(provider);
			}
			if (consumer!=null){
				((SpeakBirdApplication)getApplication()).setConsumer(consumer);
			}
		}
		private AccessToken getAccessToken() {
			String token = authsettings.getString("accessTokenToken", "");
			String tokenSecret = authsettings.getString("accessTokenSecret", "");
			if (token!=null && tokenSecret!=null && !"".equals(tokenSecret) && !"".equals(token)){
				return new AccessToken(token, tokenSecret);
			}
			return null;
		}
	    
	@Override
	    protected void onActivityResult(
	        int requestCode, int resultCode, Intent data) {
	    if (requestCode == TTS_CHECK_CODE) {
			 if(resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS || a==null)//Something is missing
			 {
				 setContentView(R.layout.dialogcheck);
				 image3 = (ImageView)findViewById(R.id.imageView3);
				 image4 = (ImageView)findViewById(R.id.imageView4);
				 
		        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
		   				 image4.setImageResource(R.drawable.whitetts);
		   				 image4.setOnClickListener(null);
	
		        }else
		        {
					 image4.setOnClickListener(new OnClickListener() {
						 public void onClick(View v) {
					            Intent installIntent = new Intent();
					            installIntent.setAction(
					                TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
					            startActivity(installIntent); 		
						 }
	
					 });
		        }
		        if(a!=null)
	        	{
					 image3.setImageResource(R.drawable.whitetweet);
	
	        	}else { 
					 image3.setOnClickListener(new OnClickListener() {
						 public void onClick(View v) {
							 Log.v("SPEAKBIRD","SPEAKBIRD clicked");
							 	new askOauthTask().execute();  		
						 }
	
					 });
	
		        }
			 }else
			 {
				 Intent main = new Intent(context, Main.class);
				 finish();
				 startActivity(main);
			 }
	    }
	}
	        
		private void storeAccessToken(AccessToken a) {
			SharedPreferences.Editor editor = authsettings.edit();
			editor.putString("accessTokenToken", a.getToken());
			editor.putString("accessTokenSecret", a.getTokenSecret());
			editor.commit();
		}
		
		private void getConsumerProvider() {
			OAuthProvider p = ((SpeakBirdApplication)getApplication()).getProvider();
			if (p!=null){
				provider = p;
			}
			CommonsHttpOAuthConsumer c = ((SpeakBirdApplication)getApplication()).getConsumer();
			if (c!=null){
				consumer = c;
			}
		}
		
		private class askOauthTask extends AsyncTask<Void, Void, Boolean>
		{
			ProgressBar progress;
			String authUrl;
			
			@Override
			protected void onPreExecute()
			{
				progress = (ProgressBar) findViewById(R.id.progressBar1);
				progress.setVisibility(0);
			}
			
			@Override
			protected Boolean doInBackground(Void... arg0) {
				try {

					consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
					provider = new DefaultOAuthProvider("https://api.twitter.com/oauth/request_token", "https://api.twitter.com/oauth/access_token", "https://api.twitter.com/oauth/authorize");
					
					provider.setOAuth10a(true);
					authUrl = provider.retrieveRequestToken(consumer, CALLBACK_URL);
					setConsumerProvider();
					
				} catch (Exception e) {
					Log.v("SPEAKBIRD","SPEAKBIRD, errore1"+e);
					return false;
				}
				return true;
			}
			
			@Override 
			protected void onPostExecute(Boolean result)
			{
				if(result)
				{
					Toast.makeText(context, R.string.autorize, Toast.LENGTH_LONG).show();
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
					progress.setVisibility(8);
				}else
				{
					Toast.makeText(context, R.string.autorizeerror, Toast.LENGTH_LONG).show();
				}
			}
			
		}
        
}
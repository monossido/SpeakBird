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

import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import twitter4j.Twitter;
import android.app.Application;
import android.os.Bundle;

public class SpeakBirdApplication extends Application{
	
	private Bundle extra;
	private boolean busy;

	@Override
    public void onCreate() {
		super.onCreate();
		
	}

	private Twitter twitter;
	/**
	 * @return the twitter
	 */
	public Twitter getTwitter() {
		return twitter;
	}
	
	public void setBusy(boolean b)
	{
		busy=b;
	}

	
	public boolean isBusy()
	{
		return busy;
	}
	
	/**
	 * @param twitter the twitter to set
	 */
	public void setTwitter(Twitter twitter) {
		this.twitter = twitter;
	}

	private OAuthProvider provider;
	private CommonsHttpOAuthConsumer consumer;


	/**
	 * @param provider the provider to set
	 */
	public void setProvider(OAuthProvider provider) {
		this.provider = provider;
	}

	/**
	 * @return the provider
	 */
	public OAuthProvider getProvider() {
		return provider;
	}

	/**
	 * @param consumer the consumer to set
	 */
	public void setConsumer(CommonsHttpOAuthConsumer consumer) {
		this.consumer = consumer;
	}

	/**
	 * @return the consumer
	 */
	public CommonsHttpOAuthConsumer getConsumer() {
		return consumer;
	}
	
	public void setExtraAccount(Bundle extra)
	{
		this.extra = extra;
	}

	public Bundle getExtraAccount()
	{
		return extra;
	}

}

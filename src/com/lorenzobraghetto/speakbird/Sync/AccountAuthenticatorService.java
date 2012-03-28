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
package com.lorenzobraghetto.speakbird.Sync;

import com.lorenzobraghetto.speakbird.View.Splash;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
 
/**
 * Authenticator service that returns a subclass of AbstractAccountAuthenticator in onBind()
 */
public class AccountAuthenticatorService extends Service {
private static AccountAuthenticatorImpl sAccountAuthenticator = null;
 
public AccountAuthenticatorService() {
	super();
}
 
public IBinder onBind(Intent intent) {
	IBinder ret = null;
	if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
		ret = getAuthenticator().getIBinder();
	return ret;
	}
 
private AccountAuthenticatorImpl getAuthenticator() {
	if (sAccountAuthenticator == null)
		sAccountAuthenticator = new AccountAuthenticatorImpl(this);
	return sAccountAuthenticator;
	}
 
private static class AccountAuthenticatorImpl extends AbstractAccountAuthenticator {
	private Context mContext;
	
	public AccountAuthenticatorImpl(Context context) {
		super(context);
		mContext = context;
		}
	
	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options)
			throws NetworkErrorException {
		Bundle reply = new Bundle();
		
		Intent i = new Intent(mContext, Splash.class);
		i.setAction("com.lorenzobraghetto.speakbird.Sync.LOGIN");
		i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		reply.putParcelable(AccountManager.KEY_INTENT, i);
   
		return reply;
		}
 
	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
		return null;
		}
	
	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		return null;
		}
	
	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
		return null;
		}
	
	@Override
	public String getAuthTokenLabel(String authTokenType) {
		return null;
		}
 
	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
		return null;
		}
	
	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
		return null;
		}
	
	@Override
	public Bundle getAccountRemovalAllowed(
	        AccountAuthenticatorResponse response, Account account)
	        throws NetworkErrorException {
	    Bundle result = super.getAccountRemovalAllowed(response, account);

	    if (result != null && result.containsKey(AccountManager.KEY_BOOLEAN_RESULT)
	            && !result.containsKey(AccountManager.KEY_INTENT)) {
	        final boolean removalAllowed = result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT);

	        if (removalAllowed) {
	    		SharedPreferences authsettings = mContext.getSharedPreferences("Auth", MODE_PRIVATE);
				SharedPreferences.Editor editor = authsettings.edit();
				editor.putString("accessTokenToken", "");
				editor.putString("accessTokenSecret", "");
				editor.commit();
	        }
	    }

	    return result;
	}
	}
}
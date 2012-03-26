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

import com.lorenzobraghetto.speakbird.Logic.TwitterService;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class SpeakService extends Service {
	private static SyncAdapterImpl sSyncAdapter = null;
 
public SpeakService() {
	 super();
 }
 
private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
	private Context mContext;
 
	public SyncAdapterImpl(Context context) {
		super(context, true);
		mContext = context;
		SharedPreferences prefs = PreferenceManager
		          .getDefaultSharedPreferences(context);
		String time = prefs.getString("updateInterval", "5");
		Account account = new Account("Speakbird Twitter Account", "com.lorenzobraghetto.speakbird.account");
		ContentResolver.addPeriodicSync(account, "com.lorenzobraghetto.speakbird.content", new Bundle(), Long.parseLong(time)*60);
	}
 
	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		try {
			SpeakService.performSync(mContext, account, extras, authority, provider, syncResult);
		} catch (OperationCanceledException e) {
		}
	}
	
	@Override
	public void onSyncCanceled()
	{
		
	}
 }
 
@Override
public IBinder onBind(Intent intent) {
	IBinder ret = null;
	ret = getSyncAdapter().getSyncAdapterBinder();
	return ret;
	}
 
private SyncAdapterImpl getSyncAdapter() {
	if (sSyncAdapter == null)
		sSyncAdapter = new SyncAdapterImpl(this);
	return sSyncAdapter;
	}
 
 private static void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
	throws OperationCanceledException {		
		Intent intent = new Intent(context, TwitterService.class);
		context.startService(intent);
	}
}
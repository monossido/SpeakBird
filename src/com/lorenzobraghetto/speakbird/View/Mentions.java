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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.lorenzobraghetto.speakbird.R;
import com.lorenzobraghetto.speakbird.Logic.SpeakBirdApplication;

public class Mentions extends SherlockFragment implements OnInitListener, OnUtteranceCompletedListener
{
	protected Twitter twitter;
	protected ResponseList<Status> mentions;
	protected String CONSUMER_KEY = "CaLz8BjfUQdFZ19i0Ni5mA";
	protected String CONSUMER_SECRET = "2Djfy4vFEMeZ4ft7vC1EakzPwrtSHVkmBigJdrZg";
	protected MentionsAdapter adapter;
	protected TextToSpeech mTts;
	protected SharedPreferences settings;
	protected NotificationManager mNotificationManager;
	protected int controls;
	protected int clickedMention;
	private ProgressDialog dialogP;
	protected int numeroTweet;
	private ListView listView;
	protected Context mContext;
	protected boolean speaking;
	protected PullToRefreshListView list;
	private boolean nottop;
	protected View v;
	private boolean started;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mContext = getSherlockActivity();

		mNotificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

		started = false;

		nottop = false;

		controls = 0;
		speaking = false;

		settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		checkForSavedLogin();

		getAccessToken();

		numeroTweet = 20;
		if (isNetworkAvailable())
			new mentionsProgress().execute();
		else
			Toast.makeText(mContext, "Connection not avaible", Toast.LENGTH_SHORT).show();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.mentions, container, false);

		setHasOptionsMenu(true);

		list = (PullToRefreshListView) v.findViewById(R.id.mentionsListView);
		list.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh(PullToRefreshBase refreshView) {
				new mentionsUpdate(false).execute();
			}
		});

		listView = list.getRefreshableView();

		listView.setAdapter(adapter);
		listView.setOnItemClickListener(onClickMention);
		list.setOnScrollListener(EndlessScrollListener);
		if (!started)
			listView.setDividerHeight(0);
		return v;
	}

	protected void initializeActivity(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.xml.menu2, menu);
		MenuItem pausa = menu.getItem(2);
		pausa.setVisible(speaking);
		MenuItem top = menu.getItem(1);
		top.setVisible(nottop);
		MenuItem controlsM = menu.getItem(0);
		if (controls == 1)
			controlsM.setIcon(R.drawable.controlli);
		else
			controlsM.setIcon(R.drawable.controlliuno);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.controlli:
			switch (controls) {
			case 0:
				item.setIcon(R.drawable.controlli);
				controls = 1;
				Toast.makeText(mContext, R.string.controllerAll, Toast.LENGTH_SHORT).show();

				break;
			case 1:
				item.setIcon(R.drawable.controlliuno);
				controls = 0;
				Toast.makeText(mContext, R.string.controller, Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.pause:
			speaking = false;
			getSherlockActivity().invalidateOptionsMenu();
			mTts.stop();
			mTts.shutdown();
			mNotificationManager.cancel(1);
			reDrawList(-1);
			return true;
		case R.id.top:
			listView.setSelectionFromTop(0, 0);
			nottop = false;
			getSherlockActivity().invalidateOptionsMenu();
			return true;
		case android.R.id.home:
			// app icon in action bar clicked; go home
			Intent intent = new Intent(mContext, Main.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	OnScrollListener EndlessScrollListener = new OnScrollListener() {

		private int visibleThreshold = 0;
		private int previousTotal = 0;
		private boolean loading = true;

		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (started)
			{
				if (firstVisibleItem > 1)
					nottop = true;
				else
					nottop = false;

				getSherlockActivity().invalidateOptionsMenu();

				if (loading) {
					if (totalItemCount > previousTotal) {
						loading = false;
						previousTotal = totalItemCount;
					}
				}
				if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
					new mentionsUpdate(true).execute();
					loading = true;
				}
			}
		}

		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}
	};

	private class mentionsUpdate extends AsyncTask<Void, Boolean, Boolean>
	{
		RelativeLayout progress;
		boolean fromScroll;

		public mentionsUpdate(boolean fromScroll) {
			super();
			this.fromScroll = fromScroll;
		}

		@Override
		protected void onPreExecute()
		{
			if (fromScroll)
			{
				progress = (RelativeLayout) v.findViewById(R.id.progress);
				progress.setVisibility(0);
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(list.getLayoutParams());
				params.addRule(RelativeLayout.ABOVE, R.id.progress);
				params.addRule(RelativeLayout.BELOW, R.id.actionbar);
				params.addRule(RelativeLayout.CENTER_HORIZONTAL, 1);
				params.setMargins((int) (10 * mContext.getResources().getDisplayMetrics().density), (int) (5 * mContext.getResources().getDisplayMetrics().density), (int) (10 * mContext.getResources().getDisplayMetrics().density), 0);
				list.setLayoutParams(params);
			}
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {
			Paging paging = new Paging();
			paging.count(10);
			paging.maxId(mentions.get(mentions.size() - 1).getId());
			ResponseList<twitter4j.Status> mentionsNew;

			try {
				mentionsNew = twitter.getMentions(paging);
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			if (mentionsNew == null)
			{
				Toast.makeText(mContext, "Connection not avaible", Toast.LENGTH_SHORT).show();
				return false;
			}
			for (int i = 1; i < mentionsNew.size(); i++)//aggiorno mentions e adapter
			{
				mentions.add(mentionsNew.get(i));
				adapter.add(mentionsNew.get(i));
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			if (result)
			{
				adapter.notifyDataSetChanged();
				if (fromScroll)
					progress.setVisibility(8);
				numeroTweet = numeroTweet + 10;

				list.onRefreshComplete();
			} else
			{
				if (fromScroll)
					progress.setVisibility(8);
				list.onRefreshComplete();
				Toast.makeText(mContext, getString(R.string.errorconnection), Toast.LENGTH_SHORT).show();
			}

		}

	}

	private class mentionsProgress extends AsyncTask<Void, Void, Boolean>
	{

		@Override
		protected void onPreExecute()
		{
			dialogP = ProgressDialog.show(mContext, "",
					"Loading. Please wait...", true);

		}

		@Override
		protected Boolean doInBackground(Void... params) {
			checkForSavedLogin();

			Paging count = new Paging();
			count.count(numeroTweet);
			try {
				mentions = twitter.getMentions(count);
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;

			}
			if (mentions == null)
			{
				getSherlockActivity().runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(mContext, getString(R.string.errorconnection), Toast.LENGTH_SHORT).show();
					}
				});
				return false;
			} else if (mentions.size() == 0)
			{
				getSherlockActivity().runOnUiThread(new Runnable() {

					public void run() {
						Toast.makeText(mContext, getString(R.string.errormentions), Toast.LENGTH_SHORT).show();
					}
				});
				getSherlockActivity().finish();
				return false;
			}

			ArrayList<Object> data = new ArrayList<Object>();

			for (int i = 0; i < mentions.size(); i++) {
				twitter4j.Status p = mentions.get(i);

				data.add(p);
			}

			adapter = new MentionsAdapter(
					mContext,
					data,
					mentions.get(0).getCreatedAt().getTime(), -1, "mentions");

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			if (result)
			{
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(mContext);
				Editor editor = settings.edit();
				editor.putString("lastTweet", mentions.get(0).getId() + "");
				editor.putString("lastTweetUser", mentions.get(0).getUser().getScreenName());
				editor.commit();

				if (dialogP != null)
					dialogP.cancel();

				list = (PullToRefreshListView) v.findViewById(R.id.mentionsListView);
				list.setOnRefreshListener(new OnRefreshListener() {

					@Override
					public void onRefresh(PullToRefreshBase refreshView) {
						new mentionsUpdate(false).execute();

					}
				});

				listView = list.getRefreshableView();

				listView.setAdapter(adapter);
				listView.setOnItemClickListener(onClickMention);
				list.setOnScrollListener(EndlessScrollListener);
				started = true;

				DisplayMetrics metrics = new DisplayMetrics();
				getSherlockActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
				float logicalDensity = metrics.density;

				listView.setDividerHeight((int) (1 * logicalDensity + 0.5f));
			} else if (mentions.size() != 0)
			{
				if (dialogP != null)
					dialogP.cancel();
				Toast.makeText(mContext, getString(R.string.errorconnection), Toast.LENGTH_SHORT).show();
			}

		}

	}

	protected final OnItemClickListener onClickMention = new OnItemClickListener()
	{
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
			if (mTts == null || !mTts.isSpeaking())
			{
				clickedMention = position;
				mTts = new TextToSpeech(mContext, Mentions.this);
			}

		}
	};

	protected void reDrawList(int position)
	{
		ArrayList<Object> data = new ArrayList<Object>();

		for (int z = 0; z < mentions.size(); z++) {
			twitter4j.Status p = mentions.get(z);

			data.add(p);
		}
		adapter = new MentionsAdapter(
				mContext,
				data,
				mentions.get(0).getCreatedAt().getTime(), position, "mentions");

		int index = listView.getFirstVisiblePosition();
		View v = listView.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();

		listView.setAdapter(adapter);

		listView.setSelectionFromTop(index, top);
	}

	protected AccessToken getAccessToken() {
		SharedPreferences settings = mContext.getSharedPreferences("Auth", mContext.MODE_PRIVATE);
		String token = settings.getString("accessTokenToken", "");
		String tokenSecret = settings.getString("accessTokenSecret", "");
		if (token != null && tokenSecret != null && !"".equals(tokenSecret) && !"".equals(token)) {
			return new AccessToken(token, tokenSecret);
		}
		return null;
	}

	protected boolean checkForSavedLogin() {
		// Get Access Token and persist it  
		AccessToken a = getAccessToken();
		if (a == null)
		{
			return false; //if there are no credentials stored then return to usual activity  
		}

		// initialize Twitter4J  
		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		twitter.setOAuthAccessToken(a);
		((SpeakBirdApplication) getSherlockActivity().getApplication()).setTwitter(twitter);

		return true;
	}

	public void onInit(int arg0) {
		Locale localeI = getLocaleMentions();
		if (mTts.isLanguageAvailable(localeI) > 0)
		{
			mTts.setLanguage(localeI);
			speaking = true;
			getSherlockActivity().invalidateOptionsMenu();

			if (settings.getBoolean("notificationSpeaking", false))
			{
				int icon = R.drawable.icon;
				CharSequence tickerText = getString(R.string.isSpeaking);
				long when = System.currentTimeMillis();

				Notification notification = new Notification(icon, tickerText, when);

				CharSequence contentTitle = "SpeakBird notification";
				CharSequence contentText = getString(R.string.isSpeaking);

				PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
						new Intent(), 0);

				notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);

				final int HELLO_ID = 1;

				notification.flags |= Notification.FLAG_ONGOING_EVENT;

				mNotificationManager.notify(HELLO_ID, notification);
			}
			reDrawList(clickedMention - 1);
			String myText1 = "Menzionato da " + mentions.get(clickedMention - 1).getUser().getScreenName() + " \" " + mentions.get(clickedMention - 1).getText();

			HashMap<String, String> myHashAlarm = new HashMap();

			mTts.setOnUtteranceCompletedListener(this);

			myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
					"end of wakeup message ID");
			mTts.speak(myText1, TextToSpeech.QUEUE_ADD, myHashAlarm);

		}

	}

	public void onUtteranceCompleted(String arg0) {
		if (controls == 1 && clickedMention > 1)
		{
			clickedMention--;
			getSherlockActivity().runOnUiThread(new Runnable() {
				public void run() {
					reDrawList(clickedMention - 1);
				}
			});
			String myText1 = "Menzionato da " + mentions.get(clickedMention - 1).getUser().getScreenName() + " \" " + mentions.get(clickedMention - 1).getText();

			HashMap<String, String> myHashAlarm = new HashMap();

			mTts.setOnUtteranceCompletedListener(this);

			myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
					"end of wakeup message ID");
			mTts.speak(myText1, TextToSpeech.QUEUE_ADD, myHashAlarm);
		} else
		{
			getSherlockActivity().runOnUiThread(new Runnable() {
				public void run() {
					speaking = false;
					getSherlockActivity().invalidateOptionsMenu();
					reDrawList(-1);
					mNotificationManager.cancelAll();
				}
			});
			mTts.stop();
			mTts.shutdown();
		}
	}

	protected Locale getLocaleMentions()
	{
		String localeS = settings.getString("language", "");

		if (localeS.length() == 0)
			return Locale.getDefault();
		if (localeS.compareTo("UK") == 0)
			return Locale.UK;
		else if (localeS.compareTo("US") == 0)
			return Locale.US;
		else if (localeS.compareTo("FRENCH") == 0)
			return Locale.FRANCE;
		else if (localeS.compareTo("ITALIAN") == 0)
			return Locale.ITALY;
		else if (localeS.compareTo("SPANISH") == 0)
			return new Locale("spa", "ESP");
		else if (localeS.compareTo("GERMAN") == 0)
			return Locale.GERMANY;
		return Locale.getDefault();

	}

	public boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

}

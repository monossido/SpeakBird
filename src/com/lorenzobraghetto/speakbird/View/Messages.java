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

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.TwitterException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.lorenzobraghetto.speakbird.R;

public class Messages extends Mentions
{
	private ResponseList<DirectMessage> messages;
	private ProgressDialog dialogPM;
	private ListView listViewM;
	private PullToRefreshListView listM;
	private View v;
	private boolean startedM;
	private boolean nottop;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		initializeActivity(savedInstanceState);
		Log.v("SPEAKBIRD", "onCreate");
		mContext = getSherlockActivity();

		mNotificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

		nottop = false;
		startedM = false;

		controls = 0;
		speaking = false;

		settings = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		checkForSavedLogin();

		getAccessToken();

		numeroTweet = 20;
		if (isNetworkAvailable())
			new messagesProgress().execute();
		else
			Toast.makeText(mContext, "Connection not avaible", Toast.LENGTH_SHORT).show();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.mentions, container, false);

		setHasOptionsMenu(true);

		listM = (PullToRefreshListView) v.findViewById(R.id.mentionsListView);
		listM.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh(PullToRefreshBase refreshView) {
				new messagesUpdate(false).execute();
			}
		});

		listViewM = listM.getRefreshableView();

		listViewM.setAdapter(adapter);
		listViewM.setOnItemClickListener(onClickMention);
		listM.setOnScrollListener(EndlessScrollListenerM);
		if (!startedM)
			listViewM.setDividerHeight(0);
		return v;

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
			listViewM.setSelectionFromTop(0, 0);
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

	OnScrollListener EndlessScrollListenerM = new OnScrollListener() {

		private int visibleThreshold = 0;
		private int previousTotal = 0;
		private boolean loading = true;

		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (startedM)
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
					new messagesUpdate(true).execute();
					loading = true;
				}
			}
		}

		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}
	};

	public class messagesUpdate extends AsyncTask<Void, Boolean, Boolean>
	{
		RelativeLayout progress;
		boolean fromScroll;

		public messagesUpdate(boolean fromScroll) {
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
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(listM.getLayoutParams());
				params.addRule(RelativeLayout.ABOVE, R.id.progress);
				params.addRule(RelativeLayout.BELOW, R.id.actionbar);
				params.addRule(RelativeLayout.CENTER_HORIZONTAL, 1);
				params.setMargins((int) (10 * mContext.getResources().getDisplayMetrics().density), (int) (5 * mContext.getResources().getDisplayMetrics().density), (int) (10 * mContext.getResources().getDisplayMetrics().density), 0);
				listM.setLayoutParams(params);
			}
		}

		@Override
		protected Boolean doInBackground(Void... arg0) {
			Paging paging = new Paging();
			paging.count(10);
			paging.maxId(messages.get(messages.size() - 1).getId());
			ResponseList<twitter4j.DirectMessage> messagesNew;

			try {
				messagesNew = twitter.getDirectMessages(paging);
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			if (messagesNew == null)
			{
				Toast.makeText(mContext, "Connection not avaible", Toast.LENGTH_SHORT).show();
				return false;
			}
			for (int i = 1; i < messagesNew.size(); i++)//aggiorno mentions e adapter
			{
				messages.add(messagesNew.get(i));
				adapter.add(messagesNew.get(i));
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

				listM.onRefreshComplete();
			} else
			{
				if (fromScroll)
					progress.setVisibility(8);
				listM.onRefreshComplete();
				Toast.makeText(mContext, getString(R.string.errorconnection), Toast.LENGTH_SHORT).show();
			}

		}

	}

	private class messagesProgress extends AsyncTask<Void, Void, Boolean>
	{

		@Override
		protected void onPreExecute()
		{
			dialogPM = ProgressDialog.show(mContext, "",
					"Loading. Please wait...", true);

		}

		@Override
		protected Boolean doInBackground(Void... params) {
			checkForSavedLogin();

			Paging count = new Paging();
			count.count(numeroTweet);
			try {
				messages = twitter.getDirectMessages(count);
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;

			}
			if (messages == null)
			{
				getSherlockActivity().runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(mContext, getString(R.string.errorconnection), Toast.LENGTH_SHORT).show();
					}
				});
				return false;
			} else if (messages.size() == 0)
			{
				getSherlockActivity().runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(mContext, getString(R.string.errormessages), Toast.LENGTH_SHORT).show();
					}
				});
				getSherlockActivity().finish();
				return false;
			}

			ArrayList<Object> data = new ArrayList<Object>();

			for (int i = 0; i < messages.size(); i++) {
				twitter4j.DirectMessage p = messages.get(i);

				data.add(p);
			}

			adapter = new MentionsAdapter(
					mContext,
					data,
					messages.get(0).getCreatedAt().getTime(), -1, "messages");

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
				editor.putString("lastTweetMessage", messages.get(0).getId() + "");
				editor.putString("lastTweetMessageUser", messages.get(0).getSender().getScreenName());
				editor.commit();

				if (dialogPM != null)
					dialogPM.cancel();

				listM = (PullToRefreshListView) v.findViewById(R.id.mentionsListView);
				listM.setOnRefreshListener(new OnRefreshListener() {
					@Override
					public void onRefresh(PullToRefreshBase refreshView) {
						new messagesUpdate(false).execute();

					}
				});

				listViewM = listM.getRefreshableView();

				listViewM.setAdapter(adapter);
				listViewM.setOnItemClickListener(onClickMention);
				listM.setOnScrollListener(EndlessScrollListenerM);
				startedM = true;

				DisplayMetrics metrics = new DisplayMetrics();
				getSherlockActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
				float logicalDensity = metrics.density;

				listViewM.setDividerHeight((int) (1 * logicalDensity + 0.5));
			} else if (messages.size() != 0)
			{
				if (dialogPM != null)
					dialogPM.cancel();
				Toast.makeText(mContext, getString(R.string.errorconnection), Toast.LENGTH_SHORT).show();
			}

		}

	}

	@Override
	protected void reDrawList(int position)
	{
		ArrayList<Object> data = new ArrayList<Object>();

		for (int z = 0; z < messages.size(); z++) {
			twitter4j.DirectMessage p = messages.get(z);

			data.add(p);
		}
		adapter = new MentionsAdapter(
				mContext,
				data,
				messages.get(0).getCreatedAt().getTime(), position, "messages");

		int index = listViewM.getFirstVisiblePosition();
		View v = listViewM.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();

		listViewM.setAdapter(adapter);

		listViewM.setSelectionFromTop(index, top);
	}

	@Override
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
			String myText1 = "Menzionato da " + messages.get(clickedMention - 1).getSender().getScreenName() + " \" " + messages.get(clickedMention - 1).getText();

			HashMap<String, String> myHashAlarm = new HashMap();

			mTts.setOnUtteranceCompletedListener(this);

			myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
					"end of wakeup message ID");
			mTts.speak(myText1, TextToSpeech.QUEUE_ADD, myHashAlarm);

		}

	}

	@Override
	public void onUtteranceCompleted(String arg0) {
		if (controls == 1 && clickedMention > 1)
		{
			clickedMention--;
			getSherlockActivity().runOnUiThread(new Runnable() {
				public void run() {
					reDrawList(clickedMention - 1);
				}
			});
			String myText1 = "Menzionato da " + messages.get(clickedMention - 1).getSender().getScreenName() + " \" " + messages.get(clickedMention - 1).getText();

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

}

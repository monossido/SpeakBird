package com.lorenzobraghetto.speakbird.View;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.lorenzobraghetto.speakbird.R;

public class TitlesFragment extends SherlockListFragment {

	OnItemSelectedListener mListener;
	private boolean mDualFragments;
	private int mCurPosition;

	/**
	 * Container Activity must implement this interface and we ensure that it
	 * does during the onAttach() callback
	 */
	public interface OnItemSelectedListener {
		public void onItemSelected(int category, int position);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Check that the container activity has implemented the callback
		// interface
		try {
			mListener = (OnItemSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnItemSelectedListener");
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Mentions frag = (Mentions) getFragmentManager().findFragmentById(
				R.id.content_frag);
		// Messages fragM = (Messages) getFragmentManager().findFragmentById(
		// R.id.content_frag);
		if (frag != null)
			mDualFragments = true;

		populateTitles();
		ListView lv = getListView();
		lv.setCacheColorHint(Color.TRANSPARENT); // Improves scrolling
													// performance

		if (mDualFragments) {
			// Highlight the currently selected item
			lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			// Enable drag and dropping

		}

		if (mDualFragments)
			selectPosition(mCurPosition);

	}

	public void populateTitles() {
		String[] items = new String[2];
		items[0] = getString(R.string.mention);
		items[1] = getString(R.string.message);
		// Convenience method to attach an adapter to ListFragment's ListView
		setListAdapter(new ArrayAdapter<String>(getActivity(),
				R.layout.title_list_item, items));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Send the event to the host activity via OnItemSelectedListener
		// callback
		mListener.onItemSelected(0, position);
		mCurPosition = position;
	}

	/** Called to select an item from the listview */
	public void selectPosition(int position) {
		// Only if we're showing both fragments should the item be "highlighted"
		if (mDualFragments) {
			ListView lv = getListView();
			lv.setItemChecked(position, true);
		}
		// Calls the parent activity's implementation of the
		// OnItemSelectedListener
		// so the activity can pass the event to the sibling fragment as
		// appropriate
		mListener.onItemSelected(0, position);
	}
}

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

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.lorenzobraghetto.speakbird.R;

import twitter4j.Status;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MentionsAdapter extends BaseAdapter{

	ArrayList<Object> itemList;

	private Context context;
    private LayoutInflater inflater;
    private long first;
    private int itemColored;

    public MentionsAdapter(Context context,ArrayList<Object> itemList, long first, int itemColored) {
        super();
        this.context=context;
        this.itemList = itemList;
        
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.first = first;
        this.itemColored = itemColored;
}

    public void add(Object item)
    {
    	itemList.add(item);
    }
    
    public int getCount() {
        // TODO Auto-generated method stub
        return itemList.size();
    }

    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return itemList.get(position);
    }

    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static class ViewHolder
    {
        ImageView imgViewLogo;
        TextView txtViewNick;
        TextView txtViewText;
        TextView txtViewTime;
        RelativeLayout background;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        
        ViewHolder holder;
        if(convertView==null)
        {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.mentionslist, null);

            holder.imgViewLogo = (ImageView) convertView.findViewById(R.id.personImage);
            holder.txtViewNick = (TextView) convertView.findViewById(R.id.personName);
            holder.txtViewText = (TextView) convertView.findViewById(R.id.testo);
            holder.txtViewTime = (TextView) convertView.findViewById(R.id.time);
            holder.background = (RelativeLayout) convertView.findViewById(R.id.layout);

            convertView.setTag(holder);
        }
        else
            holder=(ViewHolder)convertView.getTag();

        Status bean = (Status) itemList.get(position);
        
        long d = bean.getCreatedAt().getTime();
        long now = System.currentTimeMillis();
        
        long elapsed = now-d;
        int minutes = (int) ((elapsed / (1000*60)) % 60);
        int hours   = (int) ((elapsed / (1000*60*60)) % 24);
        int days = (int) ((elapsed / (1000*60*60*24)));
        
        UrlImageViewHelper.setUrlDrawable(holder.imgViewLogo, bean.getUser().getProfileImageURL()+"");

        holder.txtViewNick.setText("@"+bean.getUser().getScreenName());
        holder.txtViewText.setText(bean.getText());
        if(days>0 && hours>0 && minutes>0)
        	holder.txtViewTime.setText(days+"d "+hours+"h "+minutes+"m ");
        else if(hours>0 && minutes>0)
        	holder.txtViewTime.setText(hours+"h "+minutes+"m ");
        else
        	holder.txtViewTime.setText(minutes+"m ");

        if(position == itemColored)
        {
        	holder.background.setBackgroundColor(0xffa4a4a4);

        }else if(first == d)
        {
        	holder.background.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.sfondotweetunonove));
        }else
        {
        	holder.background.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.sfondotweetnove));
        }

        return convertView;
    }

}
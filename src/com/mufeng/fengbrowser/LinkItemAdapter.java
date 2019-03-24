package com.mufeng.fengbrowser;

import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LinkItemAdapter extends BaseAdapter {
	/**
	 * 显示联想到的网址成员
	 */
	private LayoutInflater inflater = null;
	private LinkedList<ListItemInfo> links = null;

	public LinkItemAdapter(Context context, LinkedList<ListItemInfo> links) {
		inflater = LayoutInflater.from(context);
		this.links = links;
	}

	public int getCount() {
		if (links == null) {
			return 0;
		} else {
			return links.size();
		}
	}

	public Object getItem(int position) {
		if (links == null || links.size() <= position) {
			return null;
		} else {
			return links.get(position);
		}
	}

	public long getItemId(int position) {
		return position;
	}

	@SuppressLint("InflateParams")
	public View getView(int position, View convertView, ViewGroup parent) {
		if (links == null || links.size() <= position) {
			return convertView;
		}

		MyHolder holder = null;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_link_item, null);
			holder = new MyHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (MyHolder) convertView.getTag();
		}

		ListItemInfo item = links.get(position);
		holder.titleText.setText(item.getTitle());
		holder.urlText.setText(item.getUrl());

		return convertView;
	}

	private class MyHolder {
		private TextView titleText = null;
		private TextView urlText = null;
		public MyHolder(View view) {
			titleText = (TextView) view.findViewById(R.id.tv_link_title);
			urlText = (TextView) view.findViewById(R.id.tv_link_url);
		}
	}

}

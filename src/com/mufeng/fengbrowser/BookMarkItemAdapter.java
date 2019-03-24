package com.mufeng.fengbrowser;

import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class BookMarkItemAdapter extends BaseAdapter {
	/**
	 * 显示书签成员
	 */
	private LayoutInflater inflater = null;
	private LinkedList<ListItemInfo> bookMarks = null;

	public BookMarkItemAdapter(Context context,
			LinkedList<ListItemInfo> bookMarks) {
		inflater = LayoutInflater.from(context);
		this.bookMarks = bookMarks;
	}

	public int getCount() {
		return bookMarks.size();
	}

	public Object getItem(int position) {
		return bookMarks.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	@SuppressLint("InflateParams")
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView titleText = null;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_show_bookmark_item,null);
			titleText = (TextView) convertView.findViewById(R.id.tv_bookmark_title);
			convertView.setTag(titleText);
		} else {
			titleText = (TextView) convertView.getTag();
		}

		ListItemInfo item = bookMarks.get(position);
		titleText.setText(item.getTitle());

		return convertView;
	}

}

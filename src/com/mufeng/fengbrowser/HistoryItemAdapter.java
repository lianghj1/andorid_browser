package com.mufeng.fengbrowser;

import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class HistoryItemAdapter extends BaseAdapter {
	/**
	 * 显示历史记录成员
	 */
	private LayoutInflater inflater = null;
	private LinkedList<ListItemInfo> historys = null;

	public HistoryItemAdapter(Context context,
			LinkedList<ListItemInfo> historys) {
		inflater = LayoutInflater.from(context);
		this.historys = historys;
	}

	public int getCount() {
		return historys.size();
	}

	public Object getItem(int position) {
		return historys.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	@SuppressLint("InflateParams")
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView titleText = null;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_show_history_item,null);
			titleText = (TextView) convertView.findViewById(R.id.tv_history_title);
			convertView.setTag(titleText);
		} else {
			titleText = (TextView) convertView.getTag();
		}

		ListItemInfo item = historys.get(position);
		titleText.setText(item.getTitle());

		return convertView;
	}

}

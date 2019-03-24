package com.mufeng.fengbrowser;

import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.PopupWindow;

public class LinkPopupWindow extends PopupWindow {
	/**
	 * 从书签及历史记录列表中读取联想到的网址，显示出来，最多显示十个，优先显示书签中的内容
	 */
	private static LinkedList<ListItemInfo> links = null;
	private ListView linkListView = null;
	private LinkItemAdapter adapter = null;

	private Activity context = null;
	private View view = null;

	private final int MAX_SIZE = 10;

	private boolean isInitOver = false;
	private Handler handler = null;

	public LinkPopupWindow(Activity context, String msg) {
		this.context = context;

		initView();
		initData();
		updateLinks(msg);
		initListView();
	}

	@SuppressLint("InflateParams")
	private void initView() {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(R.layout.link_popup_window, null);
		setContentView(view);

		linkListView = (ListView) view.findViewById(R.id.lv_link);
	}

	@SuppressLint("HandlerLeak")
	private void initData() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (adapter != null && links != null) {
					if (links.size() == 0) {
						dismiss();
					} else {
						linkListView.setVisibility(View.GONE);
						adapter.notifyDataSetChanged();
						linkListView.setVisibility(View.VISIBLE);
					}
				}
			}
		};
		// 设置高度和宽度
		setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
		// 设置弹出窗口可点击
		// setFocusable(true);
		setFocusable(false);
		setOutsideTouchable(true);
		setTouchable(true);

		setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);// 解决输入法被遮挡的问题

		// 刷新状态
		update();
		// 设置背景
		setBackgroundDrawable(new ColorDrawable(0xb0000000));// 缺少这句话的话，无法响应返回键
	}

	private void initListView() {
		new Thread(new Runnable() {
			public void run() {
				waitForSetLinks();// 未加载完成，等待

				// 加载完毕，显示
				adapter = new LinkItemAdapter(context, links);
				linkListView.setAdapter(adapter);

				linkListView.setOnItemClickListener(new OnItemClickListener() {

					public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
						ListItemInfo item = links.get(position);
						BrowserActivity.loadUrl(item.getUrl());
						dismiss();
					}
				});
			}
		}).start();

	}

	public void updateLinks(final String msg) {
		// 分别从书签及历史记录中查找匹配，最多显示MAX_SIZE个
		new Thread(new Runnable() {
			public void run() {
				isInitOver = false;

				if (links == null) {
					links = new LinkedList<ListItemInfo>();
				} else {
					links.clear();
				}

				if (msg != null && !msg.equals("")) {
					LinkedList<ListItemInfo> bookMarks = ShowBookMarkActivity.getList();
					LinkedList<ListItemInfo> historys = ShowHistoryActivity.getList();

					int linksSize = 0;

					int bookMarksSize = 0;
					if (bookMarks != null) {
						bookMarksSize = bookMarks.size();
					}
					int historysSize = 0;
					if (historys != null) {
						historysSize = historys.size();
					}
					// 从书签中查找
					for (int i = 0; linksSize < MAX_SIZE
							&& i < bookMarksSize; ++i) {
						ListItemInfo item = bookMarks.get(i);
						if (item.getTitle().contains(msg)
								|| item.getUrl().contains(msg)) {// 包含输入的信息
							if (!links.contains(item)) {// 不在列表中
								links.add(item);
								++linksSize;
							}
						}
					}
					// 从历史记录中查找
					for (int i = 0; linksSize < MAX_SIZE
							&& i < historysSize; ++i) {
						ListItemInfo item = historys.get(i);
						if (item.getTitle().contains(msg)
								|| item.getUrl().contains(msg)) {// 包含输入的信息
							if (!links.contains(item)) {// 不在列表中
								links.add(item);
								++linksSize;
							}
						}
					}
				}
				isInitOver = true;
				if (handler != null) {
					handler.sendMessage(new Message());
				}
			}
		}).start();
	}

	// public void updateList(String msg) {
	// this.msg = msg;
	// setLinks();
	//
	// // new Thread(new Runnable() {
	// // public void run() {
	// // waitForSetLinks();
	// //
	// // if (handler != null) {
	// // handler.sendMessage(new Message());
	// // }
	// // }
	// // }).start();
	//
	// }

	private void waitForSetLinks() {// 等待setLinks方法加载完毕
		while (!isInitOver) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// @Override
	// public void showAsDropDown(View anchor) {
	// // showAsDropDown(anchor, anchor.getWidth(), anchor.getHeight());
	// showAsDropDown(anchor, 0, 0);
	// }

}

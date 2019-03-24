package com.mufeng.fengbrowser;

import com.mufeng.fengbrowser.util.SysApplicationUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;

public class MyMenuPopupWindow extends PopupWindow {
	/**
	 * 浏览器底部菜单按钮，点击弹出选项小窗口
	 */
	private Activity context = null;
	private View view = null;
	private Button bookMarkButton = null;
	private Button addBookMarkButton = null;
	private Button historyButton = null;
	private Button emailButton = null;
	private Button downloadButton = null;
	private Button powerOffButton = null;

	private AddBookMarkPopupWindow addBookMarkPopupWindow = null;

	public MyMenuPopupWindow(Activity context) {
		this.context = context;

		initView();
		findView();
		addListener();
		initData();
	}

	@SuppressLint("InflateParams")
	private void initView() {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);//不同点是LayoutInflater是用来找res/layout/下的xml布局文件
		view = inflater.inflate(R.layout.menu_popup_window, null);
		setContentView(view);
	}

	private void findView() {
		bookMarkButton = (Button) view.findViewById(R.id.bt_menu_bookmark);
		addBookMarkButton = (Button) view
				.findViewById(R.id.bt_menu_addbookmark);
		historyButton = (Button) view.findViewById(R.id.bt_menu_history);
		emailButton = (Button) view.findViewById(R.id.bt_menu_email);
		downloadButton = (Button) view.findViewById(R.id.bt_menu_download);
		powerOffButton = (Button) view.findViewById(R.id.bt_menu_poweroff);
	}

	private void addListener() {
		bookMarkButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				dismiss();
				startActivity(ShowBookMarkActivity.class);
			}
		});

		addBookMarkButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				dismiss();
				String title = BrowserActivity.getCurrentTitle();
				String url = BrowserActivity.getCurrentUrl();
				if (addBookMarkPopupWindow == null) {
					addBookMarkPopupWindow = new AddBookMarkPopupWindow(context,title, url);
				} else {
					addBookMarkPopupWindow.setText(title, url);
				}
				// 居中显示
				addBookMarkPopupWindow.showAtLocation(
						context.getWindow().getDecorView(), Gravity.CENTER, 0,0);
			}
		});

		historyButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				dismiss();
				startActivity(ShowHistoryActivity.class);
			}
		});

		emailButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				dismiss();
				startActivity(SendEmailActivity.class);
			}
		});

		downloadButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				dismiss();
				startActivity(ShowDownloadActivity.class);
			}
		});

		powerOffButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				dismiss();
				SysApplicationUtil.getInstance().exitApp();
			}
		});
	}

	private void startActivity(Class<?> cls) {
		Intent intent = new Intent(getContentView().getContext(), cls);
		getContentView().getContext().startActivity(intent);
	}

	private void initData() {
		// 设置高度和宽度
		setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
		// 设置弹出窗口可点击
		setFocusable(true);
		setOutsideTouchable(true);
		setTouchable(true);

		// 刷新状态
		update();
		// 设置背景
		setBackgroundDrawable(new ColorDrawable(0xb0000000));// 缺少这句话的话，无法响应
	}

	@Override
	public void showAsDropDown(View anchor) {//相对于主activity控件的位置
		if (isShowing()) {
			dismiss();
		} else {
			showAsDropDown(anchor, 0, anchor.getHeight() >> 1);
		}
	}

}

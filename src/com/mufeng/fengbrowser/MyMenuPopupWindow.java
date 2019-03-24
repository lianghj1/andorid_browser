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
	 * ������ײ��˵���ť���������ѡ��С����
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
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);//��ͬ����LayoutInflater��������res/layout/�µ�xml�����ļ�
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
				// ������ʾ
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
		// ���ø߶ȺͿ��
		setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
		// ���õ������ڿɵ��
		setFocusable(true);
		setOutsideTouchable(true);
		setTouchable(true);

		// ˢ��״̬
		update();
		// ���ñ���
		setBackgroundDrawable(new ColorDrawable(0xb0000000));// ȱ����仰�Ļ����޷���Ӧ
	}

	@Override
	public void showAsDropDown(View anchor) {//�������activity�ؼ���λ��
		if (isShowing()) {
			dismiss();
		} else {
			showAsDropDown(anchor, 0, anchor.getHeight() >> 1);
		}
	}

}

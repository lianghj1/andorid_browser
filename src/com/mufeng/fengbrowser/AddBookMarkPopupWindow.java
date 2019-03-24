package com.mufeng.fengbrowser;

import com.mufeng.fengbrowser.util.ShowToastUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;

public class AddBookMarkPopupWindow extends PopupWindow {
	/**
	 * �����ǩ���棬Ϊ��������������ⲿ�����ݲ���Ӱ�쵽�����ڣ�Ҳ����Ӧ���ؼ�������ͨ�������ȡ������ť�ͷ�ҳ��
	 */
	private Activity context = null;
	private View view = null;
	private EditText titleText = null;
	private EditText urlText = null;
	private Button addButton = null;
	private Button cancelButton = null;

	// private final float HALF_ALPHA = 0.5f;
	// private final float DEFAULT_ALPHA = 1.0f;

	public AddBookMarkPopupWindow(Activity context, String title, String url) {
		this.context = context;

		initView();
		findView();
		setText(title, url);
		addListener();
		initData();
	}

	@SuppressLint("InflateParams")
	private void initView() {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(R.layout.add_bookmark_item_popup_window, null);
		setContentView(view);
	}

	private void findView() {
		titleText = (EditText) view.findViewById(R.id.et_bookmark_title);
		urlText = (EditText) view.findViewById(R.id.et_bookmark_url);
		addButton = (Button) view.findViewById(R.id.bt_bookmark_add);
		cancelButton = (Button) view.findViewById(R.id.bt_bookmark_cancel);
	}

	public void setText(String title, String url) {
		titleText.setText(title);
		urlText.setText(url);
	}

	private void addListener() {
		addButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				String title = titleText.getText().toString();
				String url = urlText.getText().toString();

				if (title == null || title.equals("")) {
					ShowToastUtil.showToast(context, "���������");
				} else {
					ShowToastUtil.showToast(context, "��ӳɹ�");
					ShowBookMarkActivity.addNewBookMarkItem(title, url);
					dismiss();
				}
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				dismiss();
			}
		});

	}

	private void initData() {
		// ���ø߶ȺͿ��
		setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
		// ���õ������ڿɵ��
		setFocusable(true);
		// setOutsideTouchable(true);
		setTouchable(true);

		// ˢ��״̬
		update();
	}

	@Override
	public void dismiss() {
		super.dismiss();
	}

	@Override
	public void showAtLocation(View parent, int gravity, int x, int y) {
		super.showAtLocation(parent, gravity, x, y);
		
	}

}

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
	 * 添加书签界面，为弹窗，点击窗口外部的内容不会影响到本窗口，也不响应返回键，仅能通过点击“取消”按钮释放页面
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
					ShowToastUtil.showToast(context, "请输入标题");
				} else {
					ShowToastUtil.showToast(context, "添加成功");
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
		// 设置高度和宽度
		setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
		// 设置弹出窗口可点击
		setFocusable(true);
		// setOutsideTouchable(true);
		setTouchable(true);

		// 刷新状态
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

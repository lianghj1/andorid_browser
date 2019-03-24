package com.mufeng.fengbrowser;

import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebSettings.LayoutAlgorithm;
import com.tencent.smtt.sdk.WebView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class X5WebView extends WebView {
	/**
	 * 继承tbs webview的自定义的webview，x5内核
	 */
	public X5WebView(Context context) {
		super(context);
		setBackgroundColor(85621);
	}

	public X5WebView(Context context, AttributeSet set) {
		super(context, set);
		initWebViewSettings();
		this.getView().setClickable(true);
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void initWebViewSettings() {
		// 初始化WebViewSettings
		WebSettings webSettings = this.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webSettings.setAllowFileAccess(true);
		// 自动适应屏幕
		webSettings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
		webSettings.setLoadWithOverviewMode(true);
		// 支持缩放
		webSettings.setSupportZoom(true);
		// 缩放按钮
		webSettings.setBuiltInZoomControls(true);
		// 扩大比例的缩放
		webSettings.setUseWideViewPort(true);

		webSettings.setSupportMultipleWindows(true);
		webSettings.setAppCacheEnabled(true);
		webSettings.setDomStorageEnabled(true);
		webSettings.setGeolocationEnabled(true);
		webSettings.setPluginState(WebSettings.PluginState.ON_DEMAND);
		webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

		// 保存信息
		webSettings.setSaveFormData(true);
		webSettings.setSavePassword(false);// 明文保存密码，有风险，关闭
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		// 显示当前使用的内核为系统内核还是X5内核
		boolean flag = super.drawChild(canvas, child, drawingTime);
		canvas.save();
		Paint paint = new Paint();
		paint.setColor(0x7fff0000);
		paint.setTextSize(24.f);
		paint.setAntiAlias(true);

		// if (getX5WebViewExtension() != null) {
		// canvas.drawText(this.getContext().getPackageName() + "-pid:"
		// + android.os.Process.myPid(), 10, 15, paint);
		// canvas.drawText("X5 Core:" + QbSdk.getTbsVersion(this.getContext()),
		// 10, 100, paint);
		// } else {
		// canvas.drawText(this.getContext().getPackageName() + "-pid:"
		// + android.os.Process.myPid(), 10, 15, paint);
		// canvas.drawText("Sys Core", 10, 100, paint);
		// }
		// canvas.drawText(Build.MANUFACTURER, 10, 150, paint);
		// canvas.drawText(Build.MODEL, 10, 200, paint);
		canvas.restore();

		return flag;
	}

}

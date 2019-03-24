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
	 * �̳�tbs webview���Զ����webview��x5�ں�
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
		// ��ʼ��WebViewSettings
		WebSettings webSettings = this.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webSettings.setAllowFileAccess(true);
		// �Զ���Ӧ��Ļ
		webSettings.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
		webSettings.setLoadWithOverviewMode(true);
		// ֧������
		webSettings.setSupportZoom(true);
		// ���Ű�ť
		webSettings.setBuiltInZoomControls(true);
		// �������������
		webSettings.setUseWideViewPort(true);

		webSettings.setSupportMultipleWindows(true);
		webSettings.setAppCacheEnabled(true);
		webSettings.setDomStorageEnabled(true);
		webSettings.setGeolocationEnabled(true);
		webSettings.setPluginState(WebSettings.PluginState.ON_DEMAND);
		webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

		// ������Ϣ
		webSettings.setSaveFormData(true);
		webSettings.setSavePassword(false);// ���ı������룬�з��գ��ر�
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		// ��ʾ��ǰʹ�õ��ں�Ϊϵͳ�ں˻���X5�ں�
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

package com.mufeng.fengbrowser.util;

import android.app.Activity;
import android.widget.Toast;

public class ShowToastUtil {
	/**
	 * 用户类，用以显示Toast
	 */

	public static void showToast(Activity activity, String msg) {
		Toast.makeText(activity.getApplicationContext(), msg,
				Toast.LENGTH_SHORT).show();
	}

}

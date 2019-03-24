package com.mufeng.fengbrowser.util;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class OpenFileUtil {
	/**
	 * 工具类，用以打开文件
	 */

	public static void openFile(Activity activity, File file, String mimeType) {// 打开文件
		if (!file.exists()) {// 文件不存在
			return;
		}
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(file), mimeType);
		try {
			activity.startActivity(intent);
		} catch (Exception e) {// 未安装对应的软件
			intent.setDataAndType(Uri.fromFile(file), "*/*");
			activity.startActivity(intent);
		}
	}

}

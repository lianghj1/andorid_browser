package com.mufeng.fengbrowser.util;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class OpenFileUtil {
	/**
	 * �����࣬���Դ��ļ�
	 */

	public static void openFile(Activity activity, File file, String mimeType) {// ���ļ�
		if (!file.exists()) {// �ļ�������
			return;
		}
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(file), mimeType);
		try {
			activity.startActivity(intent);
		} catch (Exception e) {// δ��װ��Ӧ�����
			intent.setDataAndType(Uri.fromFile(file), "*/*");
			activity.startActivity(intent);
		}
	}

}

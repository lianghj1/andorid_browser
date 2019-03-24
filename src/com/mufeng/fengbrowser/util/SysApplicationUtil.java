package com.mufeng.fengbrowser.util;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.Application;

public class SysApplicationUtil extends Application {
	/**
	 * �����࣬���ڽ�������activity���˳�����
	 */

	private List<Activity> mList = new LinkedList<Activity>();
	private static SysApplicationUtil instance;

	public SysApplicationUtil() {
	}

	public synchronized static SysApplicationUtil getInstance() {
		if (instance == null) {
			instance = new SysApplicationUtil();
		}

		return instance;
	}

	public void addActivity(Activity activity) {
		mList.add(activity);
	}

	public void exitApp() {
		for (Activity activity : mList) {
			if (activity != null) {
				activity.finish();
			}
		}

		// System.exit(0);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		System.gc();// ��������
	}

}

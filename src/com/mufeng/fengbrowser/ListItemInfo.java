package com.mufeng.fengbrowser;

import java.io.Serializable;

public class ListItemInfo implements Serializable {
	/**
	 * ���л��������ڴ洢��ǩ����ʷ��¼����Ϣ
	 */
	private static final long serialVersionUID = 3551696623716837997L;
	private String title = null;
	private String url = null;

	public ListItemInfo(String title, String url) {
		this.title = title;
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public String toString() {
		return title + ";" + url;
	}

	// ����equals�����������ж�Ԫ���Ƿ��ظ�
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ListItemInfo) {
			ListItemInfo item = (ListItemInfo) obj;
			if (title.equals(item.title) && url.equals(item.url)) {
				return true;
			}
		}
		return false;
	}

}

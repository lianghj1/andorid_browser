package com.mufeng.fengbrowser;

import java.io.Serializable;

public class ListItemInfo implements Serializable {
	/**
	 * 序列化对象，用于存储书签、历史记录的信息
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

	// 重载equals方法，才能判断元素是否重复
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

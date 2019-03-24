package com.mufeng.fengbrowser;

import java.io.Serializable;

public class DownloadFileInfo implements Serializable {
	/**
	 * 序列化对象，存储文件的信息
	 */
	private static final long serialVersionUID = 8654149223031367038L;

	private String url = null;
	private String savePath = null;
	private String fileName = null;
	private String mimeType = null;

	public DownloadFileInfo(String url, String savePath, String fileName,
			String mimeType) {
		this.url = url;
		this.savePath = savePath;
		this.fileName = fileName;
		this.mimeType = mimeType;
	}

	public String toString() {
		return url + ";" + savePath + ";" + fileName + ";" + mimeType;
	}

	public String getUrl() {
		return url;
	}

	public String getSavePath() {
		return savePath;
	}

	public String getFileName() {
		return fileName;
	}

	public String getMimeType() {
		return mimeType;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DownloadFileInfo) {
			DownloadFileInfo tmp = (DownloadFileInfo) obj;
			return url.equals(tmp.url) && savePath.equals(tmp.savePath)
					&& fileName.equals(tmp.fileName)
					&& mimeType.equals(tmp.mimeType);
		}
		return false;
	}

}

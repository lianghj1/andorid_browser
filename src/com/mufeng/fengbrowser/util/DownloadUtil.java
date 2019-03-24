package com.mufeng.fengbrowser.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import com.mufeng.fengbrowser.ShowDownloadActivity.MyHandler;

import android.os.Message;

public class DownloadUtil {
	/**
	 * 用以多线程断点续传下载文件 不断返回下载进度
	 */

	// TODO remove result
	private String link = null;// 下载链接
	private String savePath = null;// 保存路径
	private String fileName = null;// 文件名
	private int threadNum = 0; // 下载时启用的线程数
	private CountDownLatch latch = null; // 活动中的线程数目

	private int fileSize = 0; // 文件总大小
	private int blockSize = 0; // 各线程分配到的文件大小
	private int downloadSize = 0;

	private DownloadThread[] threads = null; // 线程对象数组，用于执行下载
	private RandomAccessFile randomAccessFile = null; // 生成临时文件

	private int[] startPos = null;// 每个线程需要下载的资源的起始位置
	private int[] endPos = null;// 每个线程需要下载的资源的终止位置
	private URL url = null;
	private boolean isRunning = false;// 标记程序是否正在运行
	private boolean isWaiting = false;// 标记程序是否正在暂停下载
	private boolean isEnding = false;// 标记程序本次下载是否已结束
	private File tmp_info = null; // 储存断点信息的文件对象
	private File download_file = null;// 下载的文件对象

	private MyHandler handler = null;

	public final static int NORMAL_MSG = 1;// 下载正常
	public final static int EXCEPTION_MSG = 2;// 下载出错
	public final static int URL_ERROR_MSG = 3;// URL链接不正确或不支持该协议
	public final static String MSG_KEY = "size";

	public DownloadUtil(String link, String savePath, String fileName,
			int threadNum, MyHandler handler) {
		// 传入URL链接、文件保存路径、文件名、线程数
		this.link = link;
		this.savePath = savePath;
		this.fileName = fileName;
		this.threadNum = threadNum;
		this.handler = handler;
		threads = new DownloadThread[this.threadNum];
		startPos = new int[this.threadNum];
		endPos = new int[this.threadNum];

		File file = new File(savePath);
		if (!file.exists()) {
			file.mkdirs();
		}

		isWaiting = true;
		isRunning = false;
		isEnding = true;
	}

	public void startDownload() {
		// 开始下载
		FileInputStream in_tmpfile = null;
		FileOutputStream out_save = null;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		DataInputStream in_info = null;
		DataOutputStream out_info = null;
		HttpURLConnection urlCon = null;

		latch = new CountDownLatch(threadNum); // 计数器初始化

		isEnding = false;

		try {
			url = new URL(link);
			urlCon = (HttpURLConnection) url.openConnection();

			// 设置连接超时时间为3秒
			urlCon.setConnectTimeout(3 * 1000);
			urlCon.setReadTimeout(3 * 1000);

			// 修改信息，否则读取大小会出错
			urlCon.setRequestProperty("Accept-Encoding", "identity");
			urlCon.connect();

			// 获取资源文件大小
			fileSize = urlCon.getContentLength();
			// 平均分配每个线程负责的资源，除法向上取整
			blockSize = (int) Math.ceil(fileSize * 1.0 / threadNum);

			// 创建下载文件对象、临时文件对象及断点信息文件对象
			download_file = new File(savePath + fileName);
			tmp_info = new File(savePath + fileName + "_info");

			// 标记是否为第一次下载
			boolean isFirst = true;

			if (download_file.exists() && tmp_info.exists()) {
				// 临时下载文件与临时信息文件存在，获取上次下载的断点信息
				fis = new FileInputStream(tmp_info);
				in_info = new DataInputStream(fis);
				for (int i = 0; i < threadNum; ++i) {
					startPos[i] = in_info.readInt();
					endPos[i] = in_info.readInt();
				}
				downloadSize = in_info.readInt();

				isFirst = false;
			} else {// 第一次下载
				randomAccessFile = new RandomAccessFile(savePath + fileName,
						"rw");

				// 设置本地文件大小
				randomAccessFile.setLength(fileSize);
				downloadSize = 0;
			}

			// 启动线程开始下载
			RandomAccessFile tmpFile = null;
			for (int i = 0; i < threadNum; ++i) {
				if (isFirst) {// 第一次下载，设置断点
					startPos[i] = i * blockSize;
					if (i == threadNum - 1) {
						// 最后一个线程的终止位置为文件末尾
						endPos[i] = fileSize;
					} else {
						endPos[i] = startPos[i] + blockSize - 1;
					}
				}
				// 每个线程使用一个RandomAccessFile进行下载
				tmpFile = new RandomAccessFile(savePath + fileName, "rw");
				// 定位到该线程的下载位置
				tmpFile.seek(startPos[i]);
				// 创建并启动线程
				threads[i] = new DownloadThread(i, tmpFile);
				threads[i].start();
			}

			isRunning = true;
			isWaiting = false;

			try {
				latch.await();// 正在下载，阻塞
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// 停止下载后的操作
			if (getCompleteRate() != 100) {
				// 下载未完成,更新info文件中的断点信息
				fos = new FileOutputStream(tmp_info);
				out_info = new DataOutputStream(fos);
				for (int i = 0; i < threadNum; ++i) {
					out_info.writeInt(startPos[i]);
					out_info.writeInt(endPos[i]);
				}
				out_info.writeInt(downloadSize);

				out_info.flush();
			}
		} catch (MalformedURLException e) {
			isRunning = false;

			Message message = new Message();
			message.what = URL_ERROR_MSG;
			handler.sendMessage(message);

			e.printStackTrace();
		} catch (IOException e) {
			isRunning = false;

			Message message = new Message();
			message.what = EXCEPTION_MSG;
			handler.sendMessage(message);

			e.printStackTrace();
		} finally {
			// 关闭流
			try {
				if (randomAccessFile != null) {
					randomAccessFile.close();
				}

				if (urlCon != null) {
					urlCon.disconnect();
				}

				if (in_info != null) {
					in_info.close();
				}
				if (fis != null) {
					fis.close();
				}
				if (out_info != null) {
					out_info.close();
				}
				if (fos != null) {
					fos.close();
				}
				if (in_tmpfile != null) {
					in_tmpfile.close();
				}
				if (out_save != null) {
					out_save.close();
				}

				isRunning = false;
				isEnding = true;
				if (isCompleted()) {
					// 下载完成，删除临时文件
					deleteTmpFile();
					// 发送信息
					Message message = new Message();
					message.what = NORMAL_MSG;
					message.getData().putInt(MSG_KEY, 100);
					handler.sendMessage(message);
				}

			} catch (IOException e) {
				isRunning = false;

				Message message = new Message();
				message.what = EXCEPTION_MSG;
				handler.sendMessage(message);

				e.printStackTrace();
			}
		}
	}

	public void deleteTmpFile() {// 删除临时文件
		new Thread(new Runnable() {
			public void run() {
				while (!isEnding) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (tmp_info != null && tmp_info.exists()) {
					tmp_info.delete();
				}
			}
		}).start();

	}

	public String getPath() {// 返回文件存放路径
		return savePath;
	}

	public String getFileName() {// 返回文件名
		return fileName;
	}

	public boolean isDownloading() {// 返回当前程序是否还在下载
		return isRunning;
	}

	public boolean isWaiting() {// 返回当前程序是否暂停
		return isWaiting;
	}

	public boolean isEnding() {// 返回当前程序是否已结束下载
		return isEnding;
	}

	private synchronized void addDownloadSize(int size) {// 更新已下载文件大小
		downloadSize += size;
	}

	private int getCompleteRate() {// 返回下载进度百分比
		// 先转为浮点数，避免溢出
		if (fileSize == 0) {
			return 0;
		}
		return (int) (downloadSize * 100.0 / fileSize);
	}

	private boolean isCompleted() {// 判断是否下载完毕
		int i;
		for (i = 0; i < threadNum; ++i) {
			if (startPos[i] < endPos[i])
				break;
		}

		return i >= threadNum || getCompleteRate() == 100;
	}

	public void suspendDownload() {// 停止下载
		isWaiting = true;
		isRunning = false;
	}

	// 下载线程类
	private class DownloadThread extends Thread {
		// 当前线程的编号
		private int index;
		// 当前线程需要下载的文件块
		private RandomAccessFile currentBlock = null;

		public DownloadThread(int index, RandomAccessFile currentBlock) {
			this.index = index;
			this.currentBlock = currentBlock;
		}

		public void run() {
			InputStream in = null;
			HttpURLConnection urlCon = null;
			try {
				URL url = new URL(link);
				urlCon = (HttpURLConnection) url.openConnection();

				if (startPos[index] > endPos[index]) {// 该线程已完成任务，结束
					return;
				}

				urlCon.setRequestProperty("Accept-Encoding", "identity");
				// 获取所要下载的资源段
				urlCon.setRequestProperty("RANGE",
						"bytes=" + startPos[index] + "-" + endPos[index]);
				urlCon.connect();

				in = urlCon.getInputStream();
				byte[] buffer = new byte[1024];
				int length = 0;

				while (isRunning && (length = in.read(buffer)) != -1) {
					startPos[index] += length;
					currentBlock.write(buffer, 0, length);
					addDownloadSize(length);

					// 发送信息
					Message message = new Message();
					message.what = NORMAL_MSG;
					message.getData().putInt(MSG_KEY, getCompleteRate());
					handler.sendMessage(message);
				}
			} catch (IOException e) {
				isRunning = false;

				Message message = new Message();
				message.what = EXCEPTION_MSG;
				handler.sendMessage(message);

				e.printStackTrace();
			} finally {
				// 关闭流
				try {
					if (urlCon != null) {
						urlCon.disconnect();
					}
					currentBlock.close();
					latch.countDown(); // 计数器自减
					if (in != null) {
						in.close();
					}
				} catch (IOException e1) {
					isRunning = false;

					Message message = new Message();
					message.what = EXCEPTION_MSG;
					handler.sendMessage(message);
					e1.printStackTrace();
				}
			}

		}
	}

}

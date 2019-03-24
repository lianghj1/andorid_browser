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
	 * ���Զ��̶߳ϵ����������ļ� ���Ϸ������ؽ���
	 */

	// TODO remove result
	private String link = null;// ��������
	private String savePath = null;// ����·��
	private String fileName = null;// �ļ���
	private int threadNum = 0; // ����ʱ���õ��߳���
	private CountDownLatch latch = null; // ��е��߳���Ŀ

	private int fileSize = 0; // �ļ��ܴ�С
	private int blockSize = 0; // ���̷߳��䵽���ļ���С
	private int downloadSize = 0;

	private DownloadThread[] threads = null; // �̶߳������飬����ִ������
	private RandomAccessFile randomAccessFile = null; // ������ʱ�ļ�

	private int[] startPos = null;// ÿ���߳���Ҫ���ص���Դ����ʼλ��
	private int[] endPos = null;// ÿ���߳���Ҫ���ص���Դ����ֹλ��
	private URL url = null;
	private boolean isRunning = false;// ��ǳ����Ƿ���������
	private boolean isWaiting = false;// ��ǳ����Ƿ�������ͣ����
	private boolean isEnding = false;// ��ǳ��򱾴������Ƿ��ѽ���
	private File tmp_info = null; // ����ϵ���Ϣ���ļ�����
	private File download_file = null;// ���ص��ļ�����

	private MyHandler handler = null;

	public final static int NORMAL_MSG = 1;// ��������
	public final static int EXCEPTION_MSG = 2;// ���س���
	public final static int URL_ERROR_MSG = 3;// URL���Ӳ���ȷ��֧�ָ�Э��
	public final static String MSG_KEY = "size";

	public DownloadUtil(String link, String savePath, String fileName,
			int threadNum, MyHandler handler) {
		// ����URL���ӡ��ļ�����·�����ļ������߳���
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
		// ��ʼ����
		FileInputStream in_tmpfile = null;
		FileOutputStream out_save = null;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		DataInputStream in_info = null;
		DataOutputStream out_info = null;
		HttpURLConnection urlCon = null;

		latch = new CountDownLatch(threadNum); // ��������ʼ��

		isEnding = false;

		try {
			url = new URL(link);
			urlCon = (HttpURLConnection) url.openConnection();

			// �������ӳ�ʱʱ��Ϊ3��
			urlCon.setConnectTimeout(3 * 1000);
			urlCon.setReadTimeout(3 * 1000);

			// �޸���Ϣ�������ȡ��С�����
			urlCon.setRequestProperty("Accept-Encoding", "identity");
			urlCon.connect();

			// ��ȡ��Դ�ļ���С
			fileSize = urlCon.getContentLength();
			// ƽ������ÿ���̸߳������Դ����������ȡ��
			blockSize = (int) Math.ceil(fileSize * 1.0 / threadNum);

			// ���������ļ�������ʱ�ļ����󼰶ϵ���Ϣ�ļ�����
			download_file = new File(savePath + fileName);
			tmp_info = new File(savePath + fileName + "_info");

			// ����Ƿ�Ϊ��һ������
			boolean isFirst = true;

			if (download_file.exists() && tmp_info.exists()) {
				// ��ʱ�����ļ�����ʱ��Ϣ�ļ����ڣ���ȡ�ϴ����صĶϵ���Ϣ
				fis = new FileInputStream(tmp_info);
				in_info = new DataInputStream(fis);
				for (int i = 0; i < threadNum; ++i) {
					startPos[i] = in_info.readInt();
					endPos[i] = in_info.readInt();
				}
				downloadSize = in_info.readInt();

				isFirst = false;
			} else {// ��һ������
				randomAccessFile = new RandomAccessFile(savePath + fileName,
						"rw");

				// ���ñ����ļ���С
				randomAccessFile.setLength(fileSize);
				downloadSize = 0;
			}

			// �����߳̿�ʼ����
			RandomAccessFile tmpFile = null;
			for (int i = 0; i < threadNum; ++i) {
				if (isFirst) {// ��һ�����أ����öϵ�
					startPos[i] = i * blockSize;
					if (i == threadNum - 1) {
						// ���һ���̵߳���ֹλ��Ϊ�ļ�ĩβ
						endPos[i] = fileSize;
					} else {
						endPos[i] = startPos[i] + blockSize - 1;
					}
				}
				// ÿ���߳�ʹ��һ��RandomAccessFile��������
				tmpFile = new RandomAccessFile(savePath + fileName, "rw");
				// ��λ�����̵߳�����λ��
				tmpFile.seek(startPos[i]);
				// �����������߳�
				threads[i] = new DownloadThread(i, tmpFile);
				threads[i].start();
			}

			isRunning = true;
			isWaiting = false;

			try {
				latch.await();// �������أ�����
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// ֹͣ���غ�Ĳ���
			if (getCompleteRate() != 100) {
				// ����δ���,����info�ļ��еĶϵ���Ϣ
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
			// �ر���
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
					// ������ɣ�ɾ����ʱ�ļ�
					deleteTmpFile();
					// ������Ϣ
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

	public void deleteTmpFile() {// ɾ����ʱ�ļ�
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

	public String getPath() {// �����ļ����·��
		return savePath;
	}

	public String getFileName() {// �����ļ���
		return fileName;
	}

	public boolean isDownloading() {// ���ص�ǰ�����Ƿ�������
		return isRunning;
	}

	public boolean isWaiting() {// ���ص�ǰ�����Ƿ���ͣ
		return isWaiting;
	}

	public boolean isEnding() {// ���ص�ǰ�����Ƿ��ѽ�������
		return isEnding;
	}

	private synchronized void addDownloadSize(int size) {// �����������ļ���С
		downloadSize += size;
	}

	private int getCompleteRate() {// �������ؽ��Ȱٷֱ�
		// ��תΪ���������������
		if (fileSize == 0) {
			return 0;
		}
		return (int) (downloadSize * 100.0 / fileSize);
	}

	private boolean isCompleted() {// �ж��Ƿ��������
		int i;
		for (i = 0; i < threadNum; ++i) {
			if (startPos[i] < endPos[i])
				break;
		}

		return i >= threadNum || getCompleteRate() == 100;
	}

	public void suspendDownload() {// ֹͣ����
		isWaiting = true;
		isRunning = false;
	}

	// �����߳���
	private class DownloadThread extends Thread {
		// ��ǰ�̵߳ı��
		private int index;
		// ��ǰ�߳���Ҫ���ص��ļ���
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

				if (startPos[index] > endPos[index]) {// ���߳���������񣬽���
					return;
				}

				urlCon.setRequestProperty("Accept-Encoding", "identity");
				// ��ȡ��Ҫ���ص���Դ��
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

					// ������Ϣ
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
				// �ر���
				try {
					if (urlCon != null) {
						urlCon.disconnect();
					}
					currentBlock.close();
					latch.countDown(); // �������Լ�
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

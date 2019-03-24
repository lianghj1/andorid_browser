package com.mufeng.fengbrowser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.mufeng.fengbrowser.util.DownloadUtil;
import com.mufeng.fengbrowser.util.OpenFileUtil;
import com.mufeng.fengbrowser.util.ShowToastUtil;
import com.mufeng.fengbrowser.util.SysApplicationUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ShowDownloadActivity extends Activity {
	/**
	 * ���ؽ��棬����ExpandableListView��ʾ������ �� �����ص��ļ�
	 */

	private static ShowDownloadActivity activity = null;

	private static ExpandableListView downloadListView = null;
	private List<String> groups = null;// groupPositionΪ0���ʾ�����У�Ϊ1���ʾ������
	private static List<List<DownloadFileInfo>> childs = null;
	private static List<DownloadFileInfo> downloadingChilds = null;
	private static List<DownloadUtil> downloadUtils = null;
	private static List<DownloadFileInfo> downloadedChilds = null;

	private final String DOWNLOADING_TITLE = "������";
	private final String DOWNLOADED_TITLE = "������";
	private final static int DOWNLOADING_GROUP = 0;
	private final static int DOWNLOADED_GROUP = 1;

	// �ļ����ص�Ĭ��Ŀ¼
	private final static String DOWNLOAD_PATH = BrowserActivity.FILE_DOWNLOAD_PATH;
	// ���������е��ļ�������ϵ��ļ���Ϣ
	private final static String INFO_PATH = BrowserActivity.SYSTEM_FILE_PATH;
	private final static String DOWNLOADING_INFO = "downloadingInfo.dat";
	private final static String DOWNLOADED_INFO = "downloadedInfo.dat";

	// ���ص�״̬
	private final static String RESULT_DOWNLOADING = "��������";
	private final static String RESULT_ERROR = "���س���";
	private final static String RESULT_URL_ERROR = "��Դ���ӳ���";
	private final static String RESULT_SUSPEND = "������ͣ";
	private final static String RESULT_WAITING = "�������������";

	private final static int THREAD_NUM = 5;// Ĭ�������߳���Ϊ5
	private static List<Integer> progress = null;// ���ؽ���
	private static List<String> status = null;// ����״̬

	private static ShowDownloadAdapter adapter = null;
	private static Handler updateAdapterHandler = null;// ���ڽ�����Ϣ��ˢ���б�
	private final static String MSG_START_DOWNLOAD = "��ʼ����";
	private final static String MSG_END_DOWNLOAD = "�������";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_download);
		SysApplicationUtil.getInstance().addActivity(this);

		onBackPressed();
		init();
	}

	@SuppressLint("HandlerLeak")
	private void init() {
		activity = ShowDownloadActivity.this;
		//ˢ���б�
		updateAdapterHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case DOWNLOADING_GROUP :
						downloadListView.expandGroup(DOWNLOADING_GROUP);
						break;
					case DOWNLOADED_GROUP :
						downloadListView.expandGroup(DOWNLOADED_GROUP);
					default :
						break;
				}
				adapter.notifyDataSetChanged();
			};
		};

		progress = new LinkedList<Integer>();
		status = new LinkedList<String>();
		downloadUtils = new LinkedList<DownloadUtil>();

		downloadListView = (ExpandableListView) findViewById(R.id.elv_download);
		groups = new ArrayList<String>();
		childs = new ArrayList<List<DownloadFileInfo>>();

		// ����������
		groups.add(DOWNLOADING_TITLE);
		groups.add(DOWNLOADED_TITLE);

		// �������С�Ŀ¼������Ŀ
		downloadingChilds = getInfoFromFile(INFO_PATH + DOWNLOADING_INFO);
		childs.add(downloadingChilds);

		int size = downloadingChilds.size();
		for (int i = 0; i < size; ++i) {
			progress.add(0);
			status.add(RESULT_WAITING);
			downloadUtils.add(null);
		}

		// �������ء�Ŀ¼������Ŀ
		downloadedChilds = getInfoFromFile(INFO_PATH + DOWNLOADED_INFO);
		childs.add(downloadedChilds);

		adapter = new ShowDownloadAdapter(this);
		downloadListView.setAdapter(adapter);
		downloadListView.setGroupIndicator(null);// ȡ��Ĭ�ϵ�ͼ��

		// ��Ӽ����¼�
		downloadListView.setOnChildClickListener(new OnChildClickListener() {

			public boolean onChildClick(ExpandableListView expandablelistview,
					View view, final int groupPosition, final int childPosition,
					long id) {

				final DownloadFileInfo info = childs.get(groupPosition)
						.get(childPosition);
				switch (groupPosition) {
					case DOWNLOADING_GROUP :

						if (downloadUtils.get(childPosition) == null) {// Ԥ������ʼ����
							preInitDownload(childPosition);
							startDownload(childPosition);
						} else if (downloadUtils.get(childPosition)
								.isDownloading()) {// ��ǰ�������أ������ͣ
							suspendDownload(childPosition);
						} else if (downloadUtils.get(childPosition)
								.isWaiting()) {// ��ǰΪ��ͣ״̬���������
							startDownload(childPosition);
						}

						break;
					case DOWNLOADED_GROUP :
						File file = new File(
								info.getSavePath() + info.getFileName());
						if (file.exists()) {// �ļ����ڣ����ļ�
							OpenFileUtil.openFile(ShowDownloadActivity.this,
									file, info.getMimeType());
						} else {
							Builder builder = new Builder(
									ShowDownloadActivity.this);
							builder.setTitle("ERROR");
							builder.setMessage("�ļ�������");
							builder.setPositiveButton("ȷ��",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											// �رմ���
											dialog.dismiss();
										}
									});
							builder.setCancelable(false);
							builder.create().show();
						}

						break;
				}

				return true;
			}
		});
		downloadListView
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {
						// ��ȡ���񵽼����ĳ�Ա��λ��
						final long packedPosition = downloadListView
								.getExpandableListPosition(position);
						final int groupPosition = ExpandableListView
								.getPackedPositionGroup(packedPosition);
						final int childPosition = ExpandableListView
								.getPackedPositionChild(packedPosition);// ��Ϊ-1�����ʾ���������飬�������ӳ�Ա

						if (childPosition != -1) {// ��ʾ���������ӳ�Ա
							Builder builder = new Builder(
									ShowDownloadActivity.this);
							builder.setTitle("Delete");
							builder.setMessage("�����Ƿ�Ҫɾ�����ļ�����¼��");

							builder.setPositiveButton("ȷ��",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											// ɾ���ļ�������
											if (groupPosition == DOWNLOADING_GROUP) {
												deleteDownloadingChild(
														childPosition);
											} else if (groupPosition == DOWNLOADED_GROUP) {
												deleteDownloadedChild(
														childPosition);
											}
											// �رմ���
											dialog.dismiss();
										}
									});

							builder.setNegativeButton("ȡ��",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();
										}
									});

							builder.setCancelable(false);
							builder.create().show();
						}

						return true;
					}
				});

	}

	private List<DownloadFileInfo> getInfoFromFile(String filePath) {
		// ���ļ��ж�ȡDownloadFileInfo��
		List<DownloadFileInfo> list = new ArrayList<DownloadFileInfo>();

		File file = new File(filePath);
		if (file.exists()) {
			FileInputStream fis = null;
			ObjectInputStream ois = null;

			try {
				fis = new FileInputStream(file);
				ois = new ObjectInputStream(fis);

				Object obj = null;
				while ((obj = ois.readObject()) != null) {
					list.add((DownloadFileInfo) obj);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {// �ر���
				try {
					if (ois != null) {
						ois.close();
					}
					if (fis != null) {
						fis.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return list;
	}

	private void deleteDownloadingChild(final int childPosition) {
		new Thread(new Runnable() {
			public void run() {
				DownloadFileInfo fileInfo = childs.get(DOWNLOADING_GROUP)
						.get(childPosition);
				DownloadUtil downloadUtil = downloadUtils.get(childPosition);
				File file = new File(
						fileInfo.getSavePath() + fileInfo.getFileName());

				// ����
				downloadingChilds.remove(childPosition);
				progress.remove(childPosition);
				status.remove(childPosition);
				downloadUtils.remove(childPosition);

				Message msg = new Message();
				msg.what = DOWNLOADING_GROUP;
				updateAdapterHandler.sendMessage(msg);

				// ��ֹͣ����
				if (downloadUtil != null) {
					downloadUtil.suspendDownload();
					while (!downloadUtil.isEnding()) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					// ɾ�����ع����е���ʱ�ļ�
					downloadUtil.deleteTmpFile();
				}
				// ɾ���ļ�
				if (file.exists()) {
					file.delete();
				}

			}
		}).start();

	}

	private void deleteDownloadedChild(final int childPosition) {
		new Thread(new Runnable() {
			public void run() {
				DownloadFileInfo fileInfo = childs.get(DOWNLOADED_GROUP)
						.get(childPosition);
				File file = new File(
						fileInfo.getSavePath() + fileInfo.getFileName());

				if (file.exists()) {
					file.delete();
				}
				// ����
				downloadedChilds.remove(childPosition);
				updateAdapterHandler.sendMessage(new Message());
			}
		}).start();
	}

	public static void addNewDownload(final String url, final String mimeType) {// ����µ���������
		try {
			URL link = new URL(url);
			String fileName = link.getPath();
			String savePath = DOWNLOAD_PATH;

			// ��ȡ�ļ���������ǰ�ļ��������ļ����ڣ�����Ӻ�׺
			fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
			fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);

			int tmp = 0;
			int index = fileName.lastIndexOf(".");
			// ��ȥ�ļ���׺���͵Ĳ���
			String tmpFileName = fileName.substring(0,
					index == -1 ? fileName.length() : index);
			// �ļ���׺����
			String type = fileName
					.substring(index == -1 ? fileName.length() - 1 : index);
			// ����ļ�����׺
			File file = new File(savePath + fileName);
			while (file.exists()) {
				++tmp;
				file = new File(
						savePath + tmpFileName + "(" + tmp + ")" + type);
			}
			fileName = file.getName();

			// �����б�
			DownloadFileInfo fileInfo = new DownloadFileInfo(url, savePath,
					fileName, mimeType);
			downloadingChilds.add(fileInfo);
			downloadUtils.add(null);
			progress.add(0);
			status.add(RESULT_WAITING);
			// Ԥ������ʼ����
			int childPosition = downloadingChilds.indexOf(fileInfo);
			preInitDownload(childPosition);
			startDownload(childPosition);

			ShowToastUtil.showToast(activity, MSG_START_DOWNLOAD);

			// ˢ�½����б�
			Message msg = new Message();
			msg.what = DOWNLOADING_GROUP;
			updateAdapterHandler.sendMessage(msg);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		// }
		// }).start();

	}

	public static void updateAllDownloadInfo() {// ���������м������ص��ļ���Ϣ
		updateDownloadInfo(downloadingChilds, INFO_PATH, DOWNLOADING_INFO);
		updateDownloadInfo(downloadedChilds, INFO_PATH, DOWNLOADED_INFO);
	}

	private static void updateDownloadInfo(final List<DownloadFileInfo> list,
			final String savePath, final String fileName) {// �����ļ���Ϣ��д�뵽�����ļ�
		new Thread(new Runnable() {
			public void run() {
				File dir = new File(savePath);
				if (!dir.exists()) {
					dir.mkdirs();
				}

				FileOutputStream fos = null;
				ObjectOutputStream oos = null;
				File file = new File(savePath + fileName);
				try {
					fos = new FileOutputStream(file);
					oos = new ObjectOutputStream(fos);

					int size = list.size();
					for (int i = 0; i < size; ++i) {
						oos.writeObject(list.get(i));
					}
					oos.writeObject(null);// ��ʾ�ѵ��ļ���β
					oos.flush();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (oos != null) {
							oos.close();
						}
						if (fos != null) {
							fos.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();

	}

	private static void preInitDownload(int childPosition) {// ��ʼ����ǰ��Ԥ����
		DownloadFileInfo info = downloadingChilds.get(childPosition);
		MyHandler handler = new MyHandler(DOWNLOADING_GROUP, childPosition);
		downloadUtils.set(childPosition, new DownloadUtil(info.getUrl(),
				info.getSavePath(), info.getFileName(), THREAD_NUM, handler));
	}

	private static void startDownload(final int childPosition) {// ��ʼ/��������
		new Thread(new Runnable() {
			public void run() {
				DownloadUtil downloadUtil = downloadUtils.get(childPosition);
				if (downloadUtil != null && downloadUtil.isWaiting()) {
					while (!downloadUtil.isEnding()) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					downloadUtils.get(childPosition).startDownload();
				}
			}
		}).start();

		status.set(childPosition, RESULT_DOWNLOADING);
	}

	private void suspendDownload(int childPosition) {// ��ͣ����
		DownloadUtil downloadUtil = downloadUtils.get(childPosition);
		if (downloadUtil != null && downloadUtil.isDownloading()) {
			downloadUtils.get(childPosition).suspendDownload();
			status.set(childPosition, RESULT_SUSPEND);
		}

	}

	public static void stopAllDownloading() {
		int size = downloadUtils.size();

		for (int i = 0; i < size; ++i) {
			if (downloadUtils.get(i) != null) {
				downloadUtils.get(i).suspendDownload();
			}
		}
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	@Override
	public void finish() {
		stopAllDownloading();
		updateAllDownloadInfo();
		super.finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return super.onOptionsItemSelected(item);
	}

	@SuppressLint("HandlerLeak")
	public static class MyHandler extends Handler {
		/**
		 * �Զ���Handler�࣬�������ཻ��
		 */
		private DownloadFileInfo fileInfo = null;
		public MyHandler(int groupPosition, int childPosition) {
			fileInfo = childs.get(groupPosition).get(childPosition);
		}

		@Override
		public void handleMessage(final Message msg) {
			int childPosition = downloadingChilds.indexOf(fileInfo);
			if (childPosition == -1) {
				return;
			}

			switch (msg.what) {
				case DownloadUtil.NORMAL_MSG :
					int num = msg.getData().getInt(DownloadUtil.MSG_KEY);

					if (num == 100) {// �������
						// ���½���
						progress.set(childPosition, num);
						// ����״̬
						DownloadUtil tmp = downloadUtils.get(childPosition);
						if (tmp != null && tmp.isDownloading()) {
							status.set(childPosition, RESULT_DOWNLOADING);
						} else if (tmp != null && tmp.isWaiting()) {
							status.set(childPosition, RESULT_SUSPEND);
						}

						// �����б�
						downloadingChilds.remove(fileInfo);// �Ƴ��б�
						downloadUtils.remove(childPosition);
						progress.remove(childPosition);
						status.remove(childPosition);
						downloadedChilds.add(0, fileInfo);// ��ӵ����б�
						adapter.notifyDataSetChanged();

						ShowToastUtil.showToast(activity, MSG_END_DOWNLOAD);

						downloadListView.expandGroup(DOWNLOADED_GROUP);// չ���������ء�
					} else {
						// ���½���
						progress.set(childPosition, num);
						// ����״̬
						DownloadUtil tmp = downloadUtils.get(childPosition);
						if (tmp != null && tmp.isDownloading()) {
							status.set(childPosition, RESULT_DOWNLOADING);
						} else if (tmp != null && tmp.isWaiting()) {
							status.set(childPosition, RESULT_SUSPEND);
						}
					}

					adapter.notifyDataSetChanged();// ǿ��ˢ��

					break;

				case DownloadUtil.EXCEPTION_MSG :
					status.set(childPosition, RESULT_ERROR);
					break;

				case DownloadUtil.URL_ERROR_MSG :
					status.set(childPosition, RESULT_URL_ERROR);
				default :
					break;
			}
			// }
			// });

		}
	}

	private class ShowDownloadAdapter extends BaseExpandableListAdapter {
		/**
		 * ��ʾ�����м�������ɵ��ļ�
		 */
		private Activity activity = null;
		private Bitmap fileIcon = null;

		public ShowDownloadAdapter(Activity activity) {
			this.activity = activity;
			fileIcon = BitmapFactory.decodeResource(
					this.activity.getResources(), R.drawable.file2);
		}

		// ��ȡ��Ԫ�ض���
		public Object getChild(int groupPosition, int childPosition) {
			return childs.get(groupPosition).get(childPosition);
		}

		// ��ȡ��Ԫ��id
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		// ����Ԫ�ز���ʾ
		/* ��Ϊ���������֣�������contextView.getTag()������һ���ʱ���������Ľ�ѵ!!! */
		@SuppressLint("InflateParams")
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View converView, ViewGroup parent) {

			String msg = null;
			switch (groupPosition) {
				case DOWNLOADING_GROUP :// �������С��ĳ�Ա
					DownloadingHolder downloadingHolder = null;
					// if (converView != null) {
					// downloadingHolder = (DownloadingHolder) converView
					// .getTag();
					// } else {
					converView = View.inflate(activity,
							R.layout.item_show_downloading_item, null);
					downloadingHolder = new DownloadingHolder(converView);

					// converView.setTag(downloadingHolder);
					// }

					// ��������
					downloadingHolder.mImage.setImageBitmap(fileIcon);
					msg = ((DownloadFileInfo) getChild(groupPosition,
							childPosition)).getFileName();
					downloadingHolder.fileNameText.setText(msg);
					msg = status.get(childPosition);
					//��ʾ���ص���Ŀ����
					downloadingHolder.statusText.setText(msg);
					if (!msg.equals(RESULT_WAITING)) {// �����ػ���ͣ״̬������ʾ����
						int num = progress.get(childPosition);
						downloadingHolder.progressText.setText(num + "%");
						downloadingHolder.progressBar.setProgress(num);
					}

					break;
				case DOWNLOADED_GROUP :// �������ء��ĳ�Ա
					DownloadedHolder downloadedHolder = null;
					// if (converView != null) {
					// downloadedHolder = (DownloadedHolder) converView
					// .getTag();
					// } else {
					converView = View.inflate(activity,
							R.layout.item_show_downloaded_item, null);
					downloadedHolder = new DownloadedHolder(converView);
					// converView.setTag(downloadedHolder);
					// }

					downloadedHolder.mImage.setImageBitmap(fileIcon);
					msg = ((DownloadFileInfo) getChild(groupPosition,
							childPosition)).getFileName();
					//��ʾ�����ص���Ŀ����
					downloadedHolder.fileNameText.setText(msg);

					break;

				default :
					break;
			}

			return converView;
		}

		public int getChildrenCount(int groupPosition) {
			return childs.get(groupPosition).size();
		}

		public Object getGroup(int groupPosition) {
			return groups.get(groupPosition);
		}

		public int getGroupCount() {
			return groups.size();
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			GroupHolder groupHolder = null;
			if (convertView != null) {
				groupHolder = (GroupHolder) convertView.getTag();
			} else {
				convertView = View.inflate(activity,
						R.layout.item_show_download_group, null);
				groupHolder = new GroupHolder(convertView);
				convertView.setTag(groupHolder);
			}

			String msg = groups.get(groupPosition);
			groupHolder.groupNameText.setText(msg);
			if (isExpanded) {
				groupHolder.mImage
						.setImageResource(R.drawable.ic_listview_down);
			} else {
				groupHolder.mImage.setImageResource(R.drawable.ic_listview_up);
			}

			return convertView;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isChildSelectable(int arg0, int arg1) {
			return true;
		}

		private class GroupHolder {
			/**
			 * �Զ������Ա�ĸ�����
			 */
			private TextView groupNameText = null;
			private ImageView mImage = null;
			public GroupHolder(View view) {
				groupNameText = (TextView) view
						.findViewById(R.id.tv_group_name);
				mImage = (ImageView) view.findViewById(R.id.iv_group_icon);
			}
		}
		private class DownloadingHolder {
			/**
			 * �Զ����������ļ��ĸ�����
			 */
			private ImageView mImage = null;//�ļ�ͼƬ
			private TextView fileNameText = null;
			private TextView statusText = null;
			private TextView progressText = null;
			private ProgressBar progressBar = null;
			public DownloadingHolder(View view) {
				mImage = (ImageView) view
						.findViewById(R.id.iv_downloading_item);
				fileNameText = (TextView) view
						.findViewById(R.id.tv_downloading_item);
				statusText = (TextView) view
						.findViewById(R.id.tv_downloading_status);
				progressText = (TextView) view
						.findViewById(R.id.tv_downloading_progress);
				progressBar = (ProgressBar) view
						.findViewById(R.id.pb_file_downloading);
			}
		}
		private class DownloadedHolder {
			/**
			 * �Զ����������ļ��ĸ�����
			 */
			private ImageView mImage = null;
			private TextView fileNameText = null;
			public DownloadedHolder(View view) {
				mImage = (ImageView) view.findViewById(R.id.iv_downloaded_item);
				fileNameText = (TextView) view
						.findViewById(R.id.tv_downloaded_item);
			}
		}

	}

}

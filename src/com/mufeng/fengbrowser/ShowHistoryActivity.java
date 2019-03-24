package com.mufeng.fengbrowser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

import com.mufeng.fengbrowser.util.SysApplicationUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

public class ShowHistoryActivity extends Activity {
	/**
	 * 从本地文件读取保存的历史记录信息，显示历史记录列表，点击历史记录项打开链接，长按可删除
	 */
	private static LinkedList<ListItemInfo> historys = null;
	private ListView historyListView = null;
	private HistoryItemAdapter adapter = null;

	private final static String HISTORYS_SAVE_PATH = BrowserActivity.SYSTEM_FILE_PATH;
	private final static String HISTORYS_INFO = "history.dat";

	private static boolean isInitOver = false;
	private static Handler handler = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_history);
		SysApplicationUtil.getInstance().addActivity(this);

		onBackPressed();
		init();
	}

	@SuppressLint("HandlerLeak")
	private void init() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {// 接收到信息就刷新
				if (adapter != null) {
					adapter.notifyDataSetChanged();
				}
			}
		};

		new Thread(new Runnable() {
			public void run() {

				historyListView = (ListView) findViewById(R.id.lv_history);
				historys = new LinkedList<ListItemInfo>();

				// 读取历史记录信息
				File fileInfo = new File(HISTORYS_SAVE_PATH + HISTORYS_INFO);
				FileInputStream fis = null;
				ObjectInputStream ois = null;
				if (fileInfo.exists()) {
					try {
						fis = new FileInputStream(fileInfo);
						ois = new ObjectInputStream(fis);
						Object obj = null;
						while ((obj = ois.readObject()) != null) {
							historys.add((ListItemInfo) obj);
						}

					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} finally {
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
				adapter = new HistoryItemAdapter(ShowHistoryActivity.this,
						historys);
				historyListView.setAdapter(adapter);

				historyListView
						.setOnItemClickListener(new OnItemClickListener() {
							// 点击打开该历史记录
							public void onItemClick(AdapterView<?> parent,
									View view, int position, long id) {
								ListItemInfo item = historys.get(position);
								BrowserActivity.loadUrl(item.getUrl());
								onBackPressed();
							}
						});
				historyListView.setOnItemLongClickListener(
						new OnItemLongClickListener() {

							public boolean onItemLongClick(
									AdapterView<?> parent, View view,
									final int position, long id) {

								Builder builder = new Builder(
										ShowHistoryActivity.this);
								builder.setTitle("Delete");
								builder.setMessage("请问是否要删除该历史记录？");
								builder.setPositiveButton("确定",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												// 删除记录并更新列表信息
												historys.remove(position);
												handler.sendMessage(
														new Message());
												// 关闭窗口
												dialog.dismiss();
											}
										});
								builder.setNegativeButton("取消",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												dialog.dismiss();
											}
										});

								builder.setCancelable(false);
								builder.create().show();

								return true;
							}
						});

				isInitOver = true;
			}
		}).start();

	}

	public static void addNewHistoryItem(final String title, final String url) {

		new Thread(new Runnable() {
			public void run() {
				while (!isInitOver) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				ListItemInfo item = new ListItemInfo(title, url);
				historys.add(0, item);
				// TODO
				// updateFileInfo();
				handler.sendMessage(new Message());
			}
		}).start();
	}

	public static void updateFileInfo() {
		new Thread(new Runnable() {
			public void run() {
				File file = new File(HISTORYS_SAVE_PATH);
				if (!file.exists()) {
					file.mkdirs();
				}

				file = new File(HISTORYS_SAVE_PATH + HISTORYS_INFO);
				FileOutputStream fos = null;
				ObjectOutputStream oos = null;

				try {
					fos = new FileOutputStream(file);
					oos = new ObjectOutputStream(fos);

					int size = historys.size();
					for (int i = 0; i < size; ++i) {
						oos.writeObject(historys.get(i));
					}
					oos.writeObject(null);// 表示当前已到结尾
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

	public static LinkedList<ListItemInfo> getList() {
		return historys;
	}

	@Override
	public void finish() {
		updateFileInfo();
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

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

}

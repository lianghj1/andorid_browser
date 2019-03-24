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

public class ShowBookMarkActivity extends Activity {
	/**
	 * 从本地文件读取保存的书签信息，显示书签列表，点击书签项打开链接，长按可删除
	 */
	private static LinkedList<ListItemInfo> bookMarks = null;
	private ListView bookMarkListView = null;
	private BookMarkItemAdapter adapter = null;

	private final static String BOOKMARKS_SAVE_PATH = BrowserActivity.SYSTEM_FILE_PATH;
	private final static String BOOKMARKS_INFO = "bookMarksInfo.dat";

	private static boolean isInitOver = false;
	private static Handler handler = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_bookmark);
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

				bookMarkListView = (ListView) findViewById(R.id.lv_bookMark);
				bookMarks = new LinkedList<ListItemInfo>();

				// 读取书签信息
				File fileInfo = new File(BOOKMARKS_SAVE_PATH + BOOKMARKS_INFO);
				FileInputStream fis = null;
				ObjectInputStream ois = null;
				if (fileInfo.exists()) {
					try {
						fis = new FileInputStream(fileInfo);
						ois = new ObjectInputStream(fis);

						Object obj = null;
						while ((obj = ois.readObject()) != null) {
							bookMarks.add((ListItemInfo) obj);
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

				adapter = new BookMarkItemAdapter(ShowBookMarkActivity.this,bookMarks);
				bookMarkListView.setAdapter(adapter);

				bookMarkListView.setOnItemClickListener(new OnItemClickListener() {
							// 点击打开该书签
							public void onItemClick(AdapterView<?> parent,
									View view, int position, long id) {
								ListItemInfo item = bookMarks.get(position);
								BrowserActivity.loadUrl(item.getUrl());
								onBackPressed();
							}
						});
				bookMarkListView.setOnItemLongClickListener(
						new OnItemLongClickListener() {

							public boolean onItemLongClick(AdapterView<?> parent, View view,final int position, long id) {

								Builder builder = new Builder(ShowBookMarkActivity.this);
								builder.setTitle("Delete");
								builder.setMessage("请问是否要删除该书签记录？");
								builder.setPositiveButton("确定",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												// 删除记录并更新列表及本地文件信息
												bookMarks.remove(position);
												handler.sendMessage(
														new Message());
												// updateFileInfo();
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

	public static void addNewBookMarkItem(final String title,
			final String url) {

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
				bookMarks.add(item);
				// updateFileInfo();
				handler.sendMessage(new Message());
			}
		}).start();

	}

	public static void updateFileInfo() {
		if (bookMarks == null || bookMarks.size() == 0) {
			return;
		}

		new Thread(new Runnable() {
			public void run() {
				File file = new File(BOOKMARKS_SAVE_PATH);
				if (!file.exists()) {
					file.mkdirs();
				}

				file = new File(BOOKMARKS_SAVE_PATH + BOOKMARKS_INFO);
				FileOutputStream fos = null;
				ObjectOutputStream oos = null;

				try {
					fos = new FileOutputStream(file);
					oos = new ObjectOutputStream(fos);

					int size = bookMarks.size();
					for (int i = 0; i < size; ++i) {
						oos.writeObject(bookMarks.get(i));
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
		return bookMarks;
	}

	@Override
	public void finish() {
		updateFileInfo();
		super.finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}
}

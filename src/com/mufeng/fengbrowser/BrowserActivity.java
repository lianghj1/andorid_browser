package com.mufeng.fengbrowser;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mufeng.fengbrowser.util.ShowToastUtil;
import com.mufeng.fengbrowser.util.SysApplicationUtil;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.QbSdk.PreInitCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class BrowserActivity extends Activity {
	/**
	 * 浏览器浏览界面
	 */

	private EditText linkEditText = null;
	private Button refreshButton = null;
	private ProgressBar progressBar = null;
	private FrameLayout x5WebViewParent = null;
	private static X5WebView x5WebView = null;
	private Button backButton = null;
	private Button forwardButton = null;
	private Button menuButton = null;
	private Button homeButton = null;
	// private Button multiWindowsButton = null; //TODO 多窗口功能按钮，暂未实现出来,可以等暑假完成

	private MyMenuPopupWindow menuPopupWindow = null;
	private LinkPopupWindow linkPopupWindow = null;

	private String currentTitle = null;// 当前网址的标题
	private boolean loadDone = true; // 记录当前页面是否已加载完毕
	private WebViewClient mWebViewClient = null;
	private WebChromeClient mWebChromeCLient = null;

	private final String HOME_PAGE = "file:///android_asset/homepage.html";
	private final String ERROR_PAGE = "file:///android_asset/time_out_error.html";

	public final static String FILE_DOWNLOAD_PATH = "//sdcard/MyBrowser/download/";//// 文件下载保存的根路径
	public final static String SYSTEM_FILE_PATH = "//data/data/com.mufeng.fengbrowser/info/";// 程序关键文件保存的根路径

	private final float disable = 0.3f; // 按钮不可用时的透明度
	private final float enable = 1.0f; // 按钮可用时的透明度
	private final String BAIDU = "https://www.baidu.com/s?wd="; // 搜索引擎为百度
	private final String HTTP = "http://";// 默认为http协议

	private long firstClickTime = 0;// 第一次点击返回键时的时间

	// 超时处理
	private final long TIME_OUT = 15000;// 超时时间为15秒
	private Timer timer = null;
	private Handler handler = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 启动x5内核
		if (!QbSdk.isTbsCoreInited()) {
			QbSdk.preInit(getApplicationContext(), null);
		}
		QbSdk.initX5Environment(getApplicationContext(), new PreInitCallback() {

			public void onViewInitFinished(boolean arg0) {
			}
			public void onCoreInitFinished() {
			}
		});

		// 在子线程启动书签及历史记录，这两个activity在执行onCreate方法时会将自己切入后台
		new Thread(new Runnable() {
			public void run() {
				// 启动下载界面
				startActivity(ShowDownloadActivity.class);
			}
		}).start();
		new Thread(new Runnable() {
			public void run() {
				// 启动书签界面
				startActivity(ShowBookMarkActivity.class);;
			}
		}).start();
		new Thread(new Runnable() {
			public void run() {
				// 启动历史记录界面
				startActivity(ShowHistoryActivity.class);
			}
		}).start();

		setContentView(R.layout.activity_browser);
		SysApplicationUtil.getInstance().addActivity(this);

		// runOnUiThread(new Runnable() { // 耗时的处理不放在主线程处理
		// public void run() {
		findView();

		initWebViewClient();
		initWebChromeClient();
		initWebView();
		addListener();

		initPage();
		initHandler();
		// }
		// });

	}

	private void startActivity(Class<?> cls) {
		Intent intent = new Intent(BrowserActivity.this, cls);
		startActivity(intent);
	}

	private void findView() {
		linkEditText = (EditText) findViewById(R.id.et_search_input_browser);
		refreshButton = (Button) findViewById(R.id.bt_refresh_browser);
		progressBar = (ProgressBar) findViewById(R.id.pb_web_loading_browser);
		x5WebViewParent = (FrameLayout) findViewById(R.id.x5_wv_parent_browser);
		backButton = (Button) findViewById(R.id.bt_back_browser);
		forwardButton = (Button) findViewById(R.id.bt_forward_browser);
		menuButton = (Button) findViewById(R.id.bt_menu_browser);
		homeButton = (Button) findViewById(R.id.bt_home_browser);
		// multiWindowsButton = (Button) findViewById(
		// R.id.bt_multi_windows_browser);
	}

	@SuppressLint("HandlerLeak")
	private void initHandler() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (x5WebView != null && x5WebView.getProgress() <= 50) {
					loadUrl(ERROR_PAGE);
				}
			}
		};
	}

	private void initWebView() {
		getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
				WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

		x5WebView = new X5WebView(this, null);
		x5WebViewParent.addView(x5WebView,
				new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT,
						FrameLayout.LayoutParams.MATCH_PARENT));

		// 解决WebView硬件加速导致页面渲染闪烁的问题
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			x5WebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		} else {
			x5WebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		}

		if (mWebViewClient != null) {
			x5WebView.setWebViewClient(mWebViewClient);
		}
		if (mWebChromeCLient != null) {
			x5WebView.setWebChromeClient(mWebChromeCLient);
		}

	}

	private void initWebViewClient() {
		mWebViewClient = new WebViewClient() {// 防止加载页面时调起系统浏览器

			public boolean shouldOverrideUrlLoading(WebView view, String url) {// 处理因为网页重定向而无法返回的问题
				return false;
			};

			@Override
			public void onPageStarted(WebView view, final String url,
					Bitmap map) {
				// TODO 隐藏输入法
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(linkEditText.getWindowToken(), 0);

				loadDone = false;
				linkEditText.clearFocus();// 清除网址栏当前的焦点

				refreshButton.setBackgroundResource(R.drawable.ic_stop);
				if (progressBar != null) {
					progressBar.setVisibility(ProgressBar.VISIBLE);// 显示页面加载进度条
				}

				if (timer != null) {
					timer.cancel();
					timer.purge();
				}

				// 使用一个计时器计时，以进行超时处理
				timer = new Timer();
				TimerTask task = new TimerTask() {

					@Override
					public void run() {
						Message msg = new Message();
						handler.sendMessage(msg);
						timer.cancel();
						timer.purge();

					}
				};
				timer.schedule(task, TIME_OUT);
				super.onPageStarted(view, url, map);


			}

			@Override
			public void onPageFinished(final WebView view, String url) {


				loadDone = true;
				refreshButton.setBackgroundResource(R.drawable.ic_refersh);
				if (progressBar != null) {
					progressBar.setVisibility(ProgressBar.GONE);// 隐藏页面加载进度条
				}

				// 更新标题
				currentTitle = x5WebView.getTitle();
				if (linkEditText != null) {
					linkEditText.setText(currentTitle);
				}
				changeButtonInfo(view);

				super.onPageFinished(view, url);

			}

			// 处理https请求
			@Override
			public void onReceivedSslError(WebView view,final SslErrorHandler handler, SslError error) {
				handler.proceed();// 等待证书响应
			}

		};
	}

	private void initWebChromeClient() {
		mWebChromeCLient = new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, final int progress) {
				super.onProgressChanged(view, progress);
				if (progressBar != null) {
					progressBar.setProgress(progress);
				}

				if (progress >= 50) {// 加载进度超过一半，则关闭计时器
					timer.cancel();
					timer.purge();
				}
			}

			@Override
			public void onReceivedTitle(WebView view, final String title) {
				super.onReceivedTitle(view, title);
				// 添加历史记录
				String url = x5WebView.getUrl();
				if (title != null && !title.equals("") && !url.equals(HOME_PAGE)&& !url.equals(ERROR_PAGE)) {
					// 标题不为空白时才记录，且主页及错误页面不记录
					ShowHistoryActivity.addNewHistoryItem(title, url);
				}
			}
		};
	}

	private void addListener() {// 添加监听事件
		// 网址栏焦点监听事件
		linkEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

			public void onFocusChange(View v, final boolean hasFocus) {
				if (hasFocus) {// 被点击时，更改图标，显示网址
					refreshButton.setBackgroundResource(R.drawable.ic_go);

					String url = x5WebView.getUrl();
					if (url.equals(HOME_PAGE) || url.equals(ERROR_PAGE)) {
						// 当前是主页或者是错误信息页，不显示网址
						linkEditText.setText(null);
					} else {
						linkEditText.setText(url);
						// 点击全选
						linkEditText.selectAll();
					}
				} else {// 没被点击时，显示标题，更改图标
					refreshButton.setBackgroundResource(R.drawable.ic_refersh);
					linkEditText.setText(x5WebView.getTitle());

					if (linkPopupWindow != null&& linkPopupWindow.isShowing()) {// 若linkPopupWindow正在显示，则隐藏
						linkPopupWindow.dismiss();
					}
				}
			}
		});

		// 网址栏编辑监听事件
		linkEditText.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				String msg = s.toString();
				if (linkEditText.isFocused()) {
					//编辑栏获得焦点的时候，显示推荐候选栏
					if (linkPopupWindow == null) {
						linkPopupWindow = new LinkPopupWindow(BrowserActivity.this, msg);
					} else {
						linkPopupWindow.updateLinks(msg);
					}
					linkPopupWindow.showAsDropDown(linkEditText);
				}

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		});

		// 网址栏回车按键监听事件
		linkEditText.setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					refreshButton.callOnClick();
					return true;
				} else {
					return false;
				}
			}
		});

		// 网址栏刷新按钮监听事件
		refreshButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				String msg = linkEditText.getText().toString();
				if (!msg.equals(currentTitle)) {// 加载新页面
					loadUrl(x5WebView, msg);
				} else if (loadDone) {// 刷新页面
					x5WebView.reload();
				} else {// 停止加载页面
					x5WebView.stopLoading();
				}
			}
		});

		// WebView下载监听事件
		x5WebView.setDownloadListener(new DownloadListener() {

			public void onDownloadStart(final String url, String userAgent,
					String contentDisposition, String mimeType,
					long contentLength) {
				Intent intent = new Intent(BrowserActivity.this,
						ShowDownloadActivity.class);
				startActivity(intent);
				ShowDownloadActivity.addNewDownload(url, mimeType);
			}
		});

		// 返回按钮点击监听事件
		backButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (x5WebView != null && x5WebView.canGoBack()) {
					x5WebView.goBack();
				}

			}
		});

		// 前进按钮点击监听事件
		forwardButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (x5WebView != null && x5WebView.canGoForward()) {
					x5WebView.goForward();
				}
			}
		});

		// 主页按钮点击监听事件
		homeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				loadUrl(x5WebView, HOME_PAGE);
			}
		});

		// 菜单按钮点击监听事件
		menuButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (menuPopupWindow == null) {
					menuPopupWindow = new MyMenuPopupWindow(BrowserActivity.this);
				}
				menuPopupWindow.showAsDropDown(menuButton);
			}
		});

		// 多窗口按钮点击监听事件
		// multiWindowsButton.setOnClickListener(new OnClickListener() {
		// public void onClick(View v) {
		// // TODO Auto-generated method stub
		//
		// }
		// });

	}

	public static void loadUrl(String url) {
		x5WebView.loadUrl(url);
	}

	private void loadUrl(final WebView view, final String msg) {

		if (view == null || msg == null || msg.equals("")) {
			return;
		}
		String url = null;
		if (isURL(msg)) {
			if (msg.indexOf("://") == -1) {
				url = HTTP + msg;// 默认补充为http协议
			} else {
				url = msg;
			}
		} else {
			url = BAIDU + msg;
		}

		view.loadUrl(url);

	}

	private void initPage() {
		// loadUrl(x5WebView, searchOrUrlMsg);

		loadUrl(x5WebView, HOME_PAGE);
		CookieSyncManager.createInstance(this);
		CookieSyncManager.getInstance().sync();
	}

	private boolean isURL(String msg) {// 判断用户输入的是搜索信息还是网址
		if (msg.indexOf(" ") == -1 && msg.indexOf(".") > 0
				&& msg.lastIndexOf(".") != msg.length() - 1
				&& !isContainsChinese(msg)) {
			// 粗略判断，不存在空格且存在小数点且小数点不在最末尾，且不包含中文，则认定为网址
			return true;
		} else {
			return false;
		}
	}

	private boolean isContainsChinese(String string) {
		// 判断字符串是否包含中文
		Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
		Matcher m = p.matcher(string);
		if (m.find()) {
			return true;
		}
		return false;
	}

	private void changeButtonInfo(WebView webView) {// 改变按钮的相关信息
		if (webView == null) {
			return;
		}

		// 后退按钮
		if (webView.canGoBack()) {
			backButton.setAlpha(enable);
			backButton.setEnabled(true);
		} else {
			backButton.setAlpha(disable);
			backButton.setEnabled(false);
		}

		// 前进按钮
		if (webView.canGoForward()) {
			forwardButton.setAlpha(enable);
			forwardButton.setEnabled(true);
		} else {
			forwardButton.setAlpha(disable);
			forwardButton.setEnabled(false);
		}

		// 主页按钮
		if (!webView.getUrl().equals(HOME_PAGE)) {
			homeButton.setEnabled(true);
		} else {
			homeButton.setEnabled(false);
		}

	}

	private void clickBackButtonExitApp() {
		long currClickTime = System.currentTimeMillis();
		if (currClickTime - firstClickTime >= 2000) {
			firstClickTime = System.currentTimeMillis();
			ShowToastUtil.showToast(BrowserActivity.this, "再次按返回键退出程序");
		} else {
			SysApplicationUtil.getInstance().exitApp();
		}
	}

	public static String getCurrentUrl() {
		return x5WebView.getUrl();
	}

	public static String getCurrentTitle() {
		return x5WebView.getTitle();
	}

	// public static void moveBrowserActivityToTop(Activity activity) {
	// // 将现在的activity带到栈顶，而不是新建
	// Intent intent = new Intent(activity, BrowserActivity.class);
	// intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	// activity.startActivity(intent);
	// }

	@Override
	public void onBackPressed() {
		if (x5WebView != null && x5WebView.getUrl().equals(HOME_PAGE)) {// 当前是主页，点击返回键退出程序
			clickBackButtonExitApp();
		} else if (x5WebView != null && x5WebView.canGoBack()) {// 不是主页且能返回，则返回
			x5WebView.goBack();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (x5WebView != null) {
			x5WebView.destroy();
		}
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

}

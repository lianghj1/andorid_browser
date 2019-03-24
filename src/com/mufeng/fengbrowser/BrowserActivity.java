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
	 * ������������
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
	// private Button multiWindowsButton = null; //TODO �ര�ڹ��ܰ�ť����δʵ�ֳ���,���Ե�������

	private MyMenuPopupWindow menuPopupWindow = null;
	private LinkPopupWindow linkPopupWindow = null;

	private String currentTitle = null;// ��ǰ��ַ�ı���
	private boolean loadDone = true; // ��¼��ǰҳ���Ƿ��Ѽ������
	private WebViewClient mWebViewClient = null;
	private WebChromeClient mWebChromeCLient = null;

	private final String HOME_PAGE = "file:///android_asset/homepage.html";
	private final String ERROR_PAGE = "file:///android_asset/time_out_error.html";

	public final static String FILE_DOWNLOAD_PATH = "//sdcard/MyBrowser/download/";//// �ļ����ر���ĸ�·��
	public final static String SYSTEM_FILE_PATH = "//data/data/com.mufeng.fengbrowser/info/";// ����ؼ��ļ�����ĸ�·��

	private final float disable = 0.3f; // ��ť������ʱ��͸����
	private final float enable = 1.0f; // ��ť����ʱ��͸����
	private final String BAIDU = "https://www.baidu.com/s?wd="; // ��������Ϊ�ٶ�
	private final String HTTP = "http://";// Ĭ��ΪhttpЭ��

	private long firstClickTime = 0;// ��һ�ε�����ؼ�ʱ��ʱ��

	// ��ʱ����
	private final long TIME_OUT = 15000;// ��ʱʱ��Ϊ15��
	private Timer timer = null;
	private Handler handler = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// ����x5�ں�
		if (!QbSdk.isTbsCoreInited()) {
			QbSdk.preInit(getApplicationContext(), null);
		}
		QbSdk.initX5Environment(getApplicationContext(), new PreInitCallback() {

			public void onViewInitFinished(boolean arg0) {
			}
			public void onCoreInitFinished() {
			}
		});

		// �����߳�������ǩ����ʷ��¼��������activity��ִ��onCreate����ʱ�Ὣ�Լ������̨
		new Thread(new Runnable() {
			public void run() {
				// �������ؽ���
				startActivity(ShowDownloadActivity.class);
			}
		}).start();
		new Thread(new Runnable() {
			public void run() {
				// ������ǩ����
				startActivity(ShowBookMarkActivity.class);;
			}
		}).start();
		new Thread(new Runnable() {
			public void run() {
				// ������ʷ��¼����
				startActivity(ShowHistoryActivity.class);
			}
		}).start();

		setContentView(R.layout.activity_browser);
		SysApplicationUtil.getInstance().addActivity(this);

		// runOnUiThread(new Runnable() { // ��ʱ�Ĵ����������̴߳���
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

		// ���WebViewӲ�����ٵ���ҳ����Ⱦ��˸������
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
		mWebViewClient = new WebViewClient() {// ��ֹ����ҳ��ʱ����ϵͳ�����

			public boolean shouldOverrideUrlLoading(WebView view, String url) {// ������Ϊ��ҳ�ض�����޷����ص�����
				return false;
			};

			@Override
			public void onPageStarted(WebView view, final String url,
					Bitmap map) {
				// TODO �������뷨
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(linkEditText.getWindowToken(), 0);

				loadDone = false;
				linkEditText.clearFocus();// �����ַ����ǰ�Ľ���

				refreshButton.setBackgroundResource(R.drawable.ic_stop);
				if (progressBar != null) {
					progressBar.setVisibility(ProgressBar.VISIBLE);// ��ʾҳ����ؽ�����
				}

				if (timer != null) {
					timer.cancel();
					timer.purge();
				}

				// ʹ��һ����ʱ����ʱ���Խ��г�ʱ����
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
					progressBar.setVisibility(ProgressBar.GONE);// ����ҳ����ؽ�����
				}

				// ���±���
				currentTitle = x5WebView.getTitle();
				if (linkEditText != null) {
					linkEditText.setText(currentTitle);
				}
				changeButtonInfo(view);

				super.onPageFinished(view, url);

			}

			// ����https����
			@Override
			public void onReceivedSslError(WebView view,final SslErrorHandler handler, SslError error) {
				handler.proceed();// �ȴ�֤����Ӧ
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

				if (progress >= 50) {// ���ؽ��ȳ���һ�룬��رռ�ʱ��
					timer.cancel();
					timer.purge();
				}
			}

			@Override
			public void onReceivedTitle(WebView view, final String title) {
				super.onReceivedTitle(view, title);
				// �����ʷ��¼
				String url = x5WebView.getUrl();
				if (title != null && !title.equals("") && !url.equals(HOME_PAGE)&& !url.equals(ERROR_PAGE)) {
					// ���ⲻΪ�հ�ʱ�ż�¼������ҳ������ҳ�治��¼
					ShowHistoryActivity.addNewHistoryItem(title, url);
				}
			}
		};
	}

	private void addListener() {// ��Ӽ����¼�
		// ��ַ����������¼�
		linkEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

			public void onFocusChange(View v, final boolean hasFocus) {
				if (hasFocus) {// �����ʱ������ͼ�꣬��ʾ��ַ
					refreshButton.setBackgroundResource(R.drawable.ic_go);

					String url = x5WebView.getUrl();
					if (url.equals(HOME_PAGE) || url.equals(ERROR_PAGE)) {
						// ��ǰ����ҳ�����Ǵ�����Ϣҳ������ʾ��ַ
						linkEditText.setText(null);
					} else {
						linkEditText.setText(url);
						// ���ȫѡ
						linkEditText.selectAll();
					}
				} else {// û�����ʱ����ʾ���⣬����ͼ��
					refreshButton.setBackgroundResource(R.drawable.ic_refersh);
					linkEditText.setText(x5WebView.getTitle());

					if (linkPopupWindow != null&& linkPopupWindow.isShowing()) {// ��linkPopupWindow������ʾ��������
						linkPopupWindow.dismiss();
					}
				}
			}
		});

		// ��ַ���༭�����¼�
		linkEditText.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				String msg = s.toString();
				if (linkEditText.isFocused()) {
					//�༭����ý����ʱ����ʾ�Ƽ���ѡ��
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

		// ��ַ���س����������¼�
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

		// ��ַ��ˢ�°�ť�����¼�
		refreshButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				String msg = linkEditText.getText().toString();
				if (!msg.equals(currentTitle)) {// ������ҳ��
					loadUrl(x5WebView, msg);
				} else if (loadDone) {// ˢ��ҳ��
					x5WebView.reload();
				} else {// ֹͣ����ҳ��
					x5WebView.stopLoading();
				}
			}
		});

		// WebView���ؼ����¼�
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

		// ���ذ�ť��������¼�
		backButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (x5WebView != null && x5WebView.canGoBack()) {
					x5WebView.goBack();
				}

			}
		});

		// ǰ����ť��������¼�
		forwardButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (x5WebView != null && x5WebView.canGoForward()) {
					x5WebView.goForward();
				}
			}
		});

		// ��ҳ��ť��������¼�
		homeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				loadUrl(x5WebView, HOME_PAGE);
			}
		});

		// �˵���ť��������¼�
		menuButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (menuPopupWindow == null) {
					menuPopupWindow = new MyMenuPopupWindow(BrowserActivity.this);
				}
				menuPopupWindow.showAsDropDown(menuButton);
			}
		});

		// �ര�ڰ�ť��������¼�
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
				url = HTTP + msg;// Ĭ�ϲ���ΪhttpЭ��
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

	private boolean isURL(String msg) {// �ж��û��������������Ϣ������ַ
		if (msg.indexOf(" ") == -1 && msg.indexOf(".") > 0
				&& msg.lastIndexOf(".") != msg.length() - 1
				&& !isContainsChinese(msg)) {
			// �����жϣ������ڿո��Ҵ���С������С���㲻����ĩβ���Ҳ��������ģ����϶�Ϊ��ַ
			return true;
		} else {
			return false;
		}
	}

	private boolean isContainsChinese(String string) {
		// �ж��ַ����Ƿ��������
		Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
		Matcher m = p.matcher(string);
		if (m.find()) {
			return true;
		}
		return false;
	}

	private void changeButtonInfo(WebView webView) {// �ı䰴ť�������Ϣ
		if (webView == null) {
			return;
		}

		// ���˰�ť
		if (webView.canGoBack()) {
			backButton.setAlpha(enable);
			backButton.setEnabled(true);
		} else {
			backButton.setAlpha(disable);
			backButton.setEnabled(false);
		}

		// ǰ����ť
		if (webView.canGoForward()) {
			forwardButton.setAlpha(enable);
			forwardButton.setEnabled(true);
		} else {
			forwardButton.setAlpha(disable);
			forwardButton.setEnabled(false);
		}

		// ��ҳ��ť
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
			ShowToastUtil.showToast(BrowserActivity.this, "�ٴΰ����ؼ��˳�����");
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
	// // �����ڵ�activity����ջ�����������½�
	// Intent intent = new Intent(activity, BrowserActivity.class);
	// intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
	// activity.startActivity(intent);
	// }

	@Override
	public void onBackPressed() {
		if (x5WebView != null && x5WebView.getUrl().equals(HOME_PAGE)) {// ��ǰ����ҳ��������ؼ��˳�����
			clickBackButtonExitApp();
		} else if (x5WebView != null && x5WebView.canGoBack()) {// ������ҳ���ܷ��أ��򷵻�
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

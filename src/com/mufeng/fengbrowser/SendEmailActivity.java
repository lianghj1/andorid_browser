package com.mufeng.fengbrowser;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import com.mufeng.fengbrowser.util.SysApplicationUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SendEmailActivity extends Activity {
	/**
	 * 使用org.apache.commons.mail包实现的发送邮件功能;mail.jar等包放到libs文件夹会出错，通过导入则可以成功，原因不明
	 */
	private EditText addressorText = null;
	private EditText passwordText = null;
	private EditText recipientText = null;
	private EditText themeText = null;
	private EditText contentText = null;
	private Button sendButton = null;
	private Button cancelButton = null;

	private Map<String, String> emailHostName = null;// 邮箱类型对应的服务器地址
	private final int SMTP_PORT = 25;// SMTP端口号
	private final int TIME_OUT = 30 * 1000;// 超时时间为30秒
	private final String CHARSET = "UTF-8";// 编码格式

	private Handler handler = null;

	// 邮件发送的结果
	private final String INPUT_EMPTY = "请将所有内容填写完整";
	private final String NETWORK_NOT_CONNECTED = "当前网络未连接";
	private final String ADDRESSOR_EMAIL_ERROR = "发件人邮箱格式错误";
	private final String RECIPIENT_EMAIL_ERROR = "收件人邮箱格式错误";
	private final String ADDRESSOR_EMAIL_TYPE_ERROR = "暂不支持该邮箱类型";
	private final String SEND_EMAIL_ERROR = "邮件发送时出错，请检查邮箱账号密码是否正确";
	private final String SEND_EMAIL_SUCCESS = "邮件发送成功";

	private final String SUCCESS = "成功";
	private final String ERROR = "错误";
	private final String RESULT = "result";
	private final String TYPE = "type";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_email);
		SysApplicationUtil.getInstance().addActivity(this);

		new Thread(new Runnable() {

			public void run() {
				initView();
				initEmailHostName();
				addListener();
			}
		}).start();

		initHandler();
	}

	private void initView() {
		addressorText = (EditText) findViewById(R.id.et_email_addressor);
		passwordText = (EditText) findViewById(R.id.et_email_password);
		recipientText = (EditText) findViewById(R.id.et_email_recipient);
		themeText = (EditText) findViewById(R.id.et_email_theme);
		contentText = (EditText) findViewById(R.id.et_email_content);
		sendButton = (Button) findViewById(R.id.bt_email_send);
		cancelButton = (Button) findViewById(R.id.bt_email_cancel);
	}

	private void initEmailHostName() {
		emailHostName = new HashMap<String, String>();

		emailHostName.put("163.com", "smtp.163.com");
		emailHostName.put("qiye.163.com", "smtp,qiye.163.com");
		emailHostName.put("126.com", "smtp.126.com");
		emailHostName.put("qq.com", "smtp.qq.com");
		emailHostName.put("exmail.qq.com", "smtp.exmail.qq.com");
		emailHostName.put("gmail.com", "smtp.gmail.com");
	}

	@SuppressLint("HandlerLeak")
	private void initHandler() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Bundle bundle = msg.getData();
				String result = bundle.getString(RESULT);
				String type = bundle.getString(TYPE);

				showResultMsg(result, type);
			}
		};
	}

	private void addListener() {
		sendButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				final String addressor = addressorText.getText().toString();
				final String password = passwordText.getText().toString();
				final String recipient = recipientText.getText().toString();
				final String theme = themeText.getText().toString();
				final String content = contentText.getText().toString();

				if (isNotEmpty(addressor) && isNotEmpty(password)
						&& isNotEmpty(recipient) && isNotEmpty(theme)
						&& isNotEmpty(content)) {
					// 建立发送邮件任务
					// new AsyncTask<String, Integer, Boolean>() {
					//
					// @Override
					
					new Thread(new Runnable() {

						public void run() {
							// TODO Auto-generated method stub
							String result = sendEmail(addressor, password,recipient, theme, content);

							Message msg = new Message();
							Bundle bundle = new Bundle();
							bundle.putString(RESULT, result);

							if (result.equals(SEND_EMAIL_SUCCESS)) {
								bundle.putString(TYPE, SUCCESS);
								msg.setData(bundle);
								handler.sendMessage(msg);
							} else {
								bundle.putString(TYPE, ERROR);
								msg.setData(bundle);
								handler.sendMessage(msg);
							}
						}
					}).start();
				} else {
					Message msg = new Message();
					Bundle bundle = new Bundle();
					bundle.putString(RESULT, INPUT_EMPTY);
					bundle.putString(TYPE, ERROR);
					msg.setData(bundle);
					handler.sendMessage(msg);
				}
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});
	}

	private boolean isNotEmpty(String str) {// 判断字符串是否不为空
		return str != null && !str.equals("");
	}

	private boolean isValidEmailAddress(String address) {
		// 判断输入的邮箱名是否合法
		boolean result = false;
		if (address == null || address.equals("")) {
			result = false;
		} else {
			// 匹配
			Pattern pattern = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
			Matcher matcher = pattern.matcher(address);
			result = matcher.matches();
		}

		return result;
	}

	private String getEmailType(String email) {
		// 获取邮箱后缀类型，小写字母类型
		return email.substring(email.indexOf("@") + 1).toLowerCase(Locale.US);
	}

	private boolean isNetworkConnected(Context context) {// 判断当前是否已联网
		if (context != null) {
			ConnectivityManager manager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = manager.getActiveNetworkInfo();
			if (networkInfo != null) {
				return networkInfo.isAvailable();
			}
		}

		return false;
	}

	private String sendEmail(String addressor, String password,
			String recipient, String theme, String content) {
		if (!isNetworkConnected(getApplicationContext())) {
			// 当前网络未连接
			return NETWORK_NOT_CONNECTED;
		}

		if (!isValidEmailAddress(addressor)) {
			// 发件人邮箱格式错误
			return ADDRESSOR_EMAIL_ERROR;
		}

		if (!isValidEmailAddress(recipient)) {
			// 收件人邮箱格式错误
			return RECIPIENT_EMAIL_ERROR;
		}

		SimpleEmail email = new SimpleEmail();
		email.setDebug(true);

		String emailType = getEmailType(addressor);
		String hostName = emailHostName.get(emailType);
		if (hostName == null) {
			// 暂不支持该邮箱类型
			return ADDRESSOR_EMAIL_TYPE_ERROR;
		}

		email.setHostName(hostName);
		email.setSmtpPort(SMTP_PORT);
		email.setSocketConnectionTimeout(TIME_OUT);
		email.setSocketTimeout(TIME_OUT);
		email.setCharset(CHARSET);
		email.setAuthentication(addressor, password);
		try {
			email.addTo(recipient, recipient);
			email.setFrom(addressor, addressor);
			email.setSubject(theme);

			// 设置文本编码，防止乱码
			email.buildMimeMessage();
			email.getMimeMessage().setText(content, CHARSET);
			email.sendMimeMessage();
		} catch (EmailException e) {
			e.printStackTrace();
			return SEND_EMAIL_ERROR;
		} catch (MessagingException e) {
			e.printStackTrace();
			return SEND_EMAIL_ERROR;
		}

		return SEND_EMAIL_SUCCESS;
	}

	private void showResultMsg(String result, String type) {
		// 弹窗显示结果
		Builder builder = new Builder(SendEmailActivity.this);
		builder.setTitle(type);
		builder.setMessage(result);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// 关闭窗口
				dialog.dismiss();
			}
		});
		builder.setCancelable(false);
		builder.create().show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

}

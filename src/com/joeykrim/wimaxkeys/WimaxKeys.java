package com.joeykrim.wimaxkeys;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import android.util.Log;
import java.io.File;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import android.content.Context;
import java.lang.reflect.Method;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class WimaxKeys extends Activity {



	private Button wimaxRSAButton;
	private Button wimaxVerifyButton;
	private TextView finalResults;
	private String wimaxPhone = null;
	public static final String PREFS_NAME = "PrefFile";
	private boolean disAccepted;
	static final int DIALOG_DISCLAIMER_ID = 0;
	static final int DIALOG_ABOUT_ID = 1;
	private static final int WIMAX_UNKNOWN = -1;
	private static final int WIMAX_DISABLING = 0;
	private static final int WIMAX_DISABLED = 1;
	private static final int WIMAX_ENABLING = 2;
	private static final int WIMAX_ENABLED = 3;
	private WimaxKeys me;
	static final double CURRENT_VERSION_ID = 2.4;
	private static String LOG_TAG = "WimaxKeyCheck";
	private static String PHONE_EVO = "EVO";
	private static String PHONE_SHIFT = "EVO Shift";
	private static String RESULT_SUCCESS = "Success";
	private static String RESULT_FAIL = "Fail";
	private static String RESULT_ERROR = "error";
	private static String RESULT_KEY_FOUND = "RSAKeyPresent";
	private static String RESULT_KEY_NOT_FOUND = "not found";
	private static String RESULT_CERT_NOT_FOUND = "not found";
	private static String RESULT_NO_PARTITION = "NoWiMAXPartition";
	private static String RESULT_NO_DEVICE = "RSACertNoDevice";
	private static String RESULT_NO_MATCH = "RSACertNoMatch";
	private static String RESULT_MATCH = "RSACertMatch";
	private static String RESULT_NOT_COMPATABLE = "Not Compatible";
	private static String RESULT_CERT_ERROR = "RSACertVerifyError";
	
	// Google Analytics Events
	private static String GAE_APP_VERSION = "LocalAppVersion";
	private static String GAE_SYSTEM = "SystemData";
	private static String GAE_BUTTON = "ButtonClicked";
	private static String GAE_MENU = "MenuItemSelected";
	private static String GAE_ROOT_RESULT = "RootResult";
	private static String GAE_DISCLAIMER = "DisclaimerDialog";
	private static String GAE_WIMAX_CHECK = "WiMAxCheck";
	private static String GAE_WIMAX_RESULT = "WiMAXResults";
	private static String GAE_WIMAX_VERIFY = "WiMaxVerify";
	private static String GAE_WIMAX_KEY_START = "WiMAXKeyStart";
	private static String GAE_WIMAX_KEY_COUNT = "WiMAXKeyCount";

	GoogleAnalyticsTracker tracker;

	AsyncTask mTask = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start("", this);

		me = this;

		/** Thanks AntiSocial!
		 * http://developer.android.com/reference/android/os/Build.html */ 
		tracker.trackEvent(GAE_APP_VERSION, String.valueOf(CURRENT_VERSION_ID), null, 0);

		if (Build.MANUFACTURER != null) { tracker.trackEvent(GAE_SYSTEM, Build.MANUFACTURER, null, 0); } 
		if (Build.BRAND != null) { tracker.trackEvent(GAE_SYSTEM, Build.BRAND, null, 0); }
		if (Build.PRODUCT != null) { tracker.trackEvent(GAE_SYSTEM, Build.PRODUCT, null, 0); }
		if (Build.MODEL != null) { tracker.trackEvent(GAE_SYSTEM, Build.MODEL, null, 0); }
		if (Build.VERSION.RELEASE != null) { tracker.trackEvent(GAE_SYSTEM, Build.VERSION.RELEASE, null, 0); }
		if (String.valueOf(Build.VERSION.SDK_INT) != null) { tracker.trackEvent(GAE_SYSTEM, String.valueOf(Build.VERSION.SDK_INT), null, 0); }
		tracker.dispatch();

		finalResults = (TextView) findViewById(R.id.FinalResults);

		wimaxRSAButton = (Button) findViewById(R.id.wimaxRSAButton);
		
		wimaxRSAButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tracker.trackEvent(GAE_BUTTON, GAE_WIMAX_CHECK, null, 0);
				disableButtons();
				if(canSU()) {
				tracker.trackEvent(GAE_ROOT_RESULT, RESULT_SUCCESS, null, 0);
				showToast(getString(R.string.rootSuccess));
					setWimaxPhone();
					if(!PHONE_EVO.equals(wimaxPhone) && !PHONE_SHIFT.equals(wimaxPhone)) {
						tracker.trackEvent(GAE_WIMAX_CHECK, RESULT_NOT_COMPATABLE, null, 0);
						finalResults.setTextColor(getResources().getColor(R.color.fail_text));
						wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
						finalResults.setText(getString(R.string.notCompatible));
						showToast(getString(R.string.notCompatible));
						tracker.dispatch();
						return;
					}
					mTask = new WiMaxCheckTask().execute();
				} else {
					tracker.trackEvent(GAE_ROOT_RESULT, RESULT_FAIL, null, 0);
					wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
					finalResults.setTextColor(getResources().getColor(R.color.fail_text));
					finalResults.setText(getString(R.string.rootFail));
					showToast(getString(R.string.rootFail));
				}
				tracker.dispatch();
				enableButtons();
			}
		} );

		wimaxVerifyButton = (Button) findViewById(R.id.wimaxVerifyButton);

		wimaxVerifyButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tracker.trackEvent(GAE_BUTTON, GAE_WIMAX_VERIFY, null, 0);
				disableButtons();
				setWimaxPhone();

				if(PHONE_EVO.equals(wimaxPhone)) {
					tracker.trackEvent(GAE_WIMAX_VERIFY, PHONE_EVO, null, 0);
					tracker.dispatch();
				} else if(PHONE_SHIFT.equals(wimaxPhone)) {
					tracker.trackEvent(GAE_WIMAX_VERIFY, PHONE_SHIFT, null, 0);
					tracker.dispatch();
				} else {
					tracker.trackEvent(GAE_WIMAX_VERIFY, RESULT_NOT_COMPATABLE, null, 0);
					finalResults.setTextColor(getResources().getColor(R.color.fail_text));
					wimaxVerifyButton.setTextColor(getResources().getColor(R.color.fail_button));
					finalResults.setText(getString(R.string.notCompatible));
					showToast(getString(R.string.notCompatible));
					enableButtons();
					tracker.dispatch();
					return;
				}
				mTask = new WiMaxValidateTask().execute();
			}
		} );

		Button authorButton = (Button) findViewById(R.id.authorButton);

		/** thanks AntiSociaL */
		authorButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tracker.trackEvent(GAE_BUTTON, "AuthorWebSite", null, 0);
				tracker.dispatch();
				String url = "http://www.joeyconway.com";
				/** Intent i = new Intent(Intent.ACTION_VIEW);
				 * i.setData(Uri.parse(url)); */ 
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(i);
			}
		} );
		TextView credits = (TextView) findViewById(R.id.Credits);
		credits.setMovementMethod(LinkMovementMethod.getInstance());
		/** http://developer.android.com/guide/topics/data/data-storage.html#pref */
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		disAccepted = settings.getBoolean("disclaimerAccepted", false);
		if (! disAccepted) { showDialog(DIALOG_DISCLAIMER_ID); }
	}

	private void disableButtons() {
		wimaxRSAButton.setEnabled(false);
		wimaxVerifyButton.setEnabled(false);
	}

	private void enableButtons() {
		wimaxRSAButton.setEnabled(true);
		wimaxVerifyButton.setEnabled(true);
	}

	/** http://developer.android.com/guide/topics/ui/menus.html */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.menuAbout:
				tracker.trackEvent(GAE_MENU, "AboutDialog", null, 0);
				showDialog(DIALOG_ABOUT_ID);
				tracker.dispatch();
				return true;
			case R.id.menuDisclaimer:
				tracker.trackEvent(GAE_MENU, "DisclaimerDialog", null, 0);
				showDialog(DIALOG_DISCLAIMER_ID);
				return true;
			case R.id.menuExit:
				tracker.trackEvent(GAE_MENU, "menuExit", null, 0);
				tracker.dispatch();
				tracker.stop();
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/** http://www.monkeycancode.com/android-show-a-legal-agreement-dialog-before-program-launches */
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
			case DIALOG_DISCLAIMER_ID:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.disclaimerMessage) .setCancelable(false) .setTitle(R.string.menuDisclaimer) .setPositiveButton(R.string.disclaimerAgree, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (!disAccepted) {
							tracker.trackEvent(GAE_DISCLAIMER, "DisclaimerAgree", null, 0);
							SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
							SharedPreferences.Editor editor = settings.edit();
							editor.putBoolean("disclaimerAccepted", true);
							editor.commit();
							tracker.dispatch();
						}
					}
				} ) .setNegativeButton(R.string.disclaimerDisagree, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						tracker.trackEvent(GAE_DISCLAIMER, "DisclaimerDisagree", null, 0);
						tracker.dispatch();
						tracker.stop();
						finish();
					}
				} );
				AlertDialog alert = builder.create();
				return alert;
				//break;
			case DIALOG_ABOUT_ID:
				AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
				builder1.setMessage(getString(R.string.versionInfo, CURRENT_VERSION_ID) + System.getProperty("line.separator") + getString(R.string.aboutMsg)) .setCancelable(false) .setTitle(R.string.menuAbout) .setPositiveButton(R.string.menuAboutOkay, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) { }
				} );
				AlertDialog alert1 = builder1.create();
				return alert1;
				//break;
			default:
				dialog = null;
		}
		return dialog;
	}

	/** thanks slushpupie */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString("finalResults", finalResults.getText().toString());
		super.onSaveInstanceState(savedInstanceState);
	}

	/** thanks slushpupie */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		String finalResultsText = savedInstanceState.getString("finalResults");
		if (finalResultsText != null) {
			finalResults.setText(finalResultsText);
		}
	}

	private void setWimaxPhone() {
		if(wimaxPhone != null)
			return;
		//grep supersonic /system/build.prop
		try {
			File file = new File("/system/build.prop");
			BufferedReader data = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = data.readLine();
			while(line != null) {
				if(line.contains("supersonic")) {
					tracker.trackEvent(GAE_WIMAX_CHECK, PHONE_EVO, null, 0);
					tracker.dispatch();
					wimaxPhone = PHONE_EVO;
					return;
				} else if(line.contains("speedy")) {
					tracker.trackEvent(GAE_WIMAX_CHECK, PHONE_SHIFT, null, 0);
					tracker.dispatch();
					wimaxPhone = PHONE_SHIFT;
					return;
				}
				line = data.readLine();
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Error reading build.prop", e);
			wimaxPhone = null;
		}
		wimaxPhone = null;
	}

	private boolean canSU() {
		Process process = null;
		int exitValue = -1;
		try {
			process = Runtime.getRuntime().exec("su");
			DataOutputStream toProcess = new DataOutputStream(process.getOutputStream());
			toProcess.writeBytes("exec id\n");
			toProcess.flush();
			exitValue = process.waitFor();
		} catch (Exception e) {
			exitValue = -1;
		}
		return exitValue == 0;
	}					

	private Process catRange(String device, int start, int count) {
		Process process = null;
		String cmd = String.format("exec dd if=%s bs=4096 skip=%d count=%d\n", device, start, count);
		try {
			process = Runtime.getRuntime().exec("su");
			DataOutputStream toProcess = new DataOutputStream(process.getOutputStream());
			toProcess.writeBytes(cmd);
			toProcess.flush();
		} catch(Exception e) {
			Log.e(LOG_TAG, "Exception while trying to run: '" + cmd + "' " + e.getMessage());
			process = null;
		}
		return process;
	}

	/** thanks birbeck */
	private void parseCheckResult(String result) {
		/** EditText text = (EditText)findViewById(R.id.FinalResults); */

		if(RESULT_KEY_FOUND.equals(result)) {
			tracker.trackEvent(GAE_WIMAX_RESULT, RESULT_KEY_FOUND, null, 0);
			finalResults.setTextColor(getResources().getColor(R.color.success_text));
			wimaxRSAButton.setTextColor(getResources().getColor(R.color.success_button));
			finalResults.setText(getString(R.string.WiMAXKeyPresent, wimaxPhone));
			showToast(getString(R.string.WiMAXKeyPresent, wimaxPhone));
		} else if(RESULT_KEY_NOT_FOUND.equals(result)) {
			tracker.trackEvent(GAE_WIMAX_RESULT, RESULT_KEY_NOT_FOUND, null, 0);
			finalResults.setTextColor(getResources().getColor(R.color.fail_text));
			wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
			finalResults.setText(getString(R.string.WiMAXKeyMissing, wimaxPhone));
			showToast(getString(R.string.WiMAXKeyMissing, wimaxPhone));
		} else {
			tracker.trackEvent(GAE_WIMAX_RESULT, RESULT_ERROR, null, 0);
			finalResults.setTextColor(getResources().getColor(R.color.fail_text));
			wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
			tracker.trackEvent(GAE_WIMAX_RESULT, RESULT_NO_PARTITION, null, 0);
			finalResults.setText(getString(R.string.noWiMAXPartition));
			showToast(getString(R.string.noWiMAXPartition));
		}
		tracker.dispatch();
		enableButtons();
	}

	private void parseVerifyResult(String result) {

		if(RESULT_MATCH.equals(result)) {
			tracker.trackEvent(GAE_WIMAX_RESULT, RESULT_MATCH, null, 0);
			finalResults.setTextColor(getResources().getColor(R.color.success_text));
			wimaxVerifyButton.setTextColor(getResources().getColor(R.color.success_button));
			finalResults.setText(getString(R.string.WiMAXCertMatch));
			showToast(getString(R.string.WiMAXCertMatch));
		} else if(RESULT_NO_MATCH.equals(result)) {
			tracker.trackEvent(GAE_WIMAX_RESULT, RESULT_NO_MATCH, null, 0);
			finalResults.setTextColor(getResources().getColor(R.color.warn_text));
			wimaxVerifyButton.setTextColor(getResources().getColor(R.color.warn_button));
			finalResults.setText(getString(R.string.WiMAXCertNoMatch));
			showToast(getString(R.string.WiMAXCertNoMatch));
		} else if(RESULT_NO_DEVICE.equals(result)) {
			tracker.trackEvent(GAE_WIMAX_RESULT, RESULT_NO_DEVICE, null, 0);
			finalResults.setTextColor(getResources().getColor(R.color.warn_text));
			wimaxVerifyButton.setTextColor(getResources().getColor(R.color.warn_button));
			finalResults.setText(getString(R.string.WiMAXCertNoDevice));
			showToast(getString(R.string.WiMAXCertNoDevice));
		} else {
			finalResults.setTextColor(getResources().getColor(R.color.fail_text));
			wimaxVerifyButton.setTextColor(getResources().getColor(R.color.fail_button));
			tracker.trackEvent(GAE_WIMAX_RESULT, RESULT_CERT_ERROR, null, 0);
			finalResults.setText(getString(R.string.WiMAXCertError));
			showToast(getString(R.string.WiMAXCertError));
		}
		tracker.dispatch();
		enableButtons();
	}

	/** setGravity adjustments - http://3.ly/sP6b */
	public void showToast(String toast) {
		/** Toast.makeText(WimaxKeys.this, toast, Toast.LENGTH_SHORT).show(); */

		Toast msg = Toast.makeText(WimaxKeys.this, toast, Toast.LENGTH_SHORT);
		msg.setGravity(Gravity.CENTER, msg.getXOffset() + 5, msg.getYOffset());
		msg.show();
	}

	/** thanks birbeck */
	@Override
	public void onDestroy() {
		if (mTask != null) {
			mTask.cancel(true);
		}
		tracker.dispatch();
		super.onDestroy();
	}

	class WiMaxValidateTask extends AsyncTask<Void, Void, String> {
		ProgressDialog mDialog = new ProgressDialog(WimaxKeys.this);
		boolean wimaxEnabled = false;

		@Override
		protected void onPreExecute() {
			mDialog.setMessage(getString(R.string.WiMAXKeyCheckMsg));
			mDialog.setCancelable(false);
			mDialog.show();
		}

		@Override
		protected String doInBackground(Void... params) {

			if (wimaxPhone != null) {
				String pem = null;
				String mac = null;

				Process process = null;
				String device = null;
				int count = 100;
				int start = 2100 - count; //3071 is the end of the file

				if ("supersonic".equals(wimaxPhone)) {
					device = "/dev/mtd/mtd0ro";
				} else if("speedy".equals(wimaxPhone)) {
					device = "/dev/block/mmcblk0p25";
				} else {
					return RESULT_ERROR;
				}
				
				try {
					boolean foundStart = false;
					boolean foundEnd = false;
					while (start > 0) {
						process = catRange(device, start, count);
						if(process == null) {
							return RESULT_ERROR;
						}

						StringBuilder sb = new StringBuilder();


						BufferedReader data = new BufferedReader(new InputStreamReader(process.getInputStream()));
						String line = data.readLine();
						while (line != null) {
							if(line.contains("-----BEGIN CERTIFICATE-----")) {
								foundStart = true;
								line = line.substring(line.indexOf("-----BEGIN CERTIFICATE-----"));
							} 
							//no else here- they might be on the same line.
							if(line.contains("-----END CERTIFICATE-----")) {
								foundEnd = true;
								line = line.substring(0,line.indexOf("-----END CERTIFICATE-----")+25);
							}

							if(foundStart) {
								sb.append(line).append("\n");
							}
							if(foundEnd) {
								pem = sb.toString();
								break;
							}
							line = data.readLine();
						}

						if(foundStart && foundEnd) {
							break;
						} else {
							if(foundStart) {
								count++;
							} else if(foundEnd) {
								start--;
							} else {
								start = start - count;
							}
						} 
					}

					if(pem == null || !foundStart || !foundEnd) {
						//never found the whole certificate
						return RESULT_CERT_NOT_FOUND;
					}

					// Get the MAC address of the wimax0 device

					File file = new File("/sys/class/net/wimax0/address");
					try {
						BufferedReader data = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
						mac = data.readLine();
					} catch (FileNotFoundException e) {
						return RESULT_NO_DEVICE;
					}


					if(mac == null || mac.equals("")) {
						return RESULT_NO_DEVICE;
					}



					//skip the header/footer
					//-----BEGIN CERTIFICATE----- = 27
					//-----END CERTIFICATE----- = 25
					if(pem.length() < 52) {
						//we shouldnt be able to get here
						return RESULT_CERT_NOT_FOUND;
					}
					String b64 = pem.substring(27,pem.length()-25);

					//Decode the PEM certificate to DER
					ByteArrayInputStream der = new ByteArrayInputStream(Base64.decode(b64));
					// Get the X509Certificate object from BouncyCastle
					X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509", "BC").generateCertificate(der);
					// Get the issuer's DN, and lowercase it
					String dn = cert.getSubjectX500Principal().getName("RFC2253").toLowerCase();

					// The cert does not delimit the mac, so remove the :'s
					mac = mac.toLowerCase().replaceAll(":","");

					//see if the DN has the MAC in it
					if(dn.contains(mac)) 
						return RESULT_MATCH;
					else
						return RESULT_NO_MATCH;
				} catch (Exception e) {
					Log.d(LOG_TAG,RESULT_ERROR,e);
					//TODO
				}

			}
			return RESULT_ERROR;
		}

		@Override
		protected void onPostExecute(String result) {
			
			if (isCancelled()) {
				return;
			}

			if (mDialog != null && mDialog.isShowing()) {
				mDialog.dismiss();
			}

			parseVerifyResult(result);
			mTask = null;
		}
	}

	/** thanks birbeck */
	class WiMaxCheckTask extends AsyncTask<Void, Void, String> {

		ProgressDialog mDialog = new ProgressDialog(WimaxKeys.this);

		@Override
		protected void onPreExecute() {
			mDialog.setMessage(getString(R.string.WiMAXKeyCheckMsg));
			mDialog.setCancelable(false);
			mDialog.show();
		}

		@Override
		protected String doInBackground(Void... params) {
			if (wimaxPhone != null) {
				Process process = null;
				String device = null;
				int count = 100;
				int start = 2100 - count; //3071 is the end of the file

				if (PHONE_EVO.equals(wimaxPhone)) {
					device = "/dev/mtd/mtd0ro";
				} else if(PHONE_SHIFT.equals(wimaxPhone)) {
					device = "/dev/block/mmcblk0p25";
				}
				try {
					while (start > 0) {
						process = catRange(device, start, count);
						if(process == null) {
							return RESULT_ERROR;
						}
						BufferedReader data = new BufferedReader(new InputStreamReader(process.getInputStream()));
						String line = data.readLine();
						boolean foundStart = false;
						boolean foundEnd = false;
						while (line != null) {
							if(line.contains("-----BEGIN RSA PRIVATE KEY-----")) {
								foundStart = true;
							}
							if(line.contains("-----END RSA PRIVATE KEY-----")) {
								foundEnd = true;
								break;
							}

							line = data.readLine();
						}
						if(foundStart || foundEnd) {
							//our window size cut it off?

							if(foundStart && !foundEnd) {
								//shouldnt be more than a few blocks
								count = count++;
								continue;
							} else if(!foundStart && foundEnd) {
								start = start--;
								continue;
							} else {
								if (String.valueOf(start) != null) { tracker.trackEvent(GAE_WIMAX_KEY_START, String.valueOf(start), null, 0); }
								if (String.valueOf(count) != null) { tracker.trackEvent(GAE_WIMAX_KEY_COUNT, String.valueOf(count), null, 0); }
								tracker.dispatch();
								return RESULT_KEY_FOUND;
							}
						}
						
						start = start - count;
					}
					//never found it
					return RESULT_KEY_NOT_FOUND;
				} catch (Exception e) {
					Log.d(LOG_TAG,RESULT_ERROR,e);
					//TODO
				}

			}
			return RESULT_ERROR;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isCancelled()) {
				return;
			}

			if (mDialog != null && mDialog.isShowing()) {
				mDialog.dismiss();
			}

			parseCheckResult(result);
			mTask = null;
		}
	}
}

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
import android.util.Log;
import java.io.File;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class WimaxKeys extends Activity {
 
        private Button rootButton;
        private Button wimaxRSAButton;
        private Button authorButton;
        private TextView finalResults;
        private String wimaxPhone = null;
        public static final String PREFS_NAME = "PrefFile";
        private boolean disAccepted;
        static final int DIALOG_DISCLAIMER_ID = 0;
        static final int DIALOG_ABOUT_ID = 1;

        GoogleAnalyticsTracker tracker;

        AsyncTask mTask = null;

    	/** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.main);
 
                tracker = GoogleAnalyticsTracker.getInstance();
                tracker.start("", this);
 
                /** Thanks AntiSocial!
                 * http://developer.android.com/reference/android/os/Build.html */ 
                tracker.trackEvent("LocalAppVersion", "2.3", null, 0);
 
                if (Build.MANUFACTURER != null) { tracker.trackEvent("SystemData", Build.MANUFACTURER, null, 0); } 
                if (Build.BRAND != null) { tracker.trackEvent("SystemData", Build.BRAND, null, 0); }
                if (Build.PRODUCT != null) { tracker.trackEvent("SystemData", Build.PRODUCT, null, 0); }
                if (Build.MODEL != null) { tracker.trackEvent("SystemData", Build.MODEL, null, 0); }
                if (Build.VERSION.RELEASE != null) { tracker.trackEvent("SystemData", Build.VERSION.RELEASE, null, 0); }
                if (String.valueOf(Build.VERSION.SDK_INT) != null) { tracker.trackEvent("SystemData", String.valueOf(Build.VERSION.SDK_INT), null, 0); }
                tracker.dispatch();
 
                finalResults = (TextView) findViewById(R.id.FinalResults);

                rootButton = (Button) findViewById(R.id.rootButton);
 
                rootButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                                tracker.trackEvent("ButtonClicked", "RootCheck", null, 0);
                                disableButtons();
                                boolean rootCheck = canSU();
                                if (rootCheck == true) {
                                        finalResults.setTextColor(getResources().getColor(R.color.success_text));
                                        rootButton.setTextColor(getResources().getColor(R.color.success_button));
                                        finalResults.setText(getString(R.string.rootSuccess));
                                        showToast(getString(R.string.rootSuccess));
                                        tracker.trackEvent("RootResult", "Success", null, 0);
                                } else {
                                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                                        rootButton.setTextColor(getResources().getColor(R.color.fail_button));
                                        finalResults.setText(getString(R.string.rootFail));
                                        showToast(getString(R.string.rootFail));
                                        tracker.trackEvent("RootResult", "Fail", null, 0);
                                }
                                enableButtons();
                                tracker.dispatch();
                        }
                } );
 
                wimaxRSAButton = (Button) findViewById(R.id.wimaxRSAButton);
 
                wimaxRSAButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                                tracker.trackEvent("ButtonClicked", "WiMAXCheck", null, 0);
                                disableButtons();
                                setWimaxPhone();
                                if("supersonic".equals(wimaxPhone)) {
	                                tracker.trackEvent("WiMAXCheck", "EVO", null, 0);
	                                tracker.dispatch();
                                } else if("speedy".equals(wimaxPhone)) {
                                	tracker.trackEvent("WiMAXCheck", "Shift", null, 0);
                                	tracker.dispatch();
                                } else {
                                	tracker.trackEvent("WiMAXCheck", "Not Compatible", null, 0);
                                	finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                                	wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
                                	finalResults.setText(getString(R.string.notCompatible));
                                	showToast(getString(R.string.notCompatible));
                                	enableButtons();
                                	tracker.dispatch();
                                	return;
                                }
                                mTask = new WiMaxCheckTask().execute();
                        }
                } );
 
                authorButton = (Button) findViewById(R.id.authorButton);
 
        /** thanks AntiSociaL */
                authorButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                                tracker.trackEvent("ButtonClicked", "AuthorWebSite", null, 0);
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
 		rootButton.setEnabled(false);
		wimaxRSAButton.setEnabled(false);
 	}
 	
 	private void enableButtons() {
	 	rootButton.setEnabled(true);
		wimaxRSAButton.setEnabled(true);
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
                                tracker.trackEvent("MenuItemSelected", "AboutDialog", null, 0);
                                showDialog(1);
                                tracker.dispatch();
                                return true;
                        case R.id.menuDisclaimer:
                                tracker.trackEvent("MenuItemSelected", "DisclaimerDialog", null, 0);
                                showDialog(DIALOG_DISCLAIMER_ID);
                                return true;
                        case R.id.menuExit:
                                tracker.trackEvent("MenuItemSelected", "menuExit", null, 0);
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
                                                if (! disAccepted) {
                                                        tracker.trackEvent("DisclaimerDialog", "DisclaimerAgree", null, 0);
                                                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                                                        SharedPreferences.Editor editor = settings.edit();
                                                        editor.putBoolean("disclaimerAccepted", true);
                                                        editor.commit();
                                                        tracker.dispatch();
                                                }
                                        }
                                } ) .setNegativeButton(R.string.disclaimerDisagree, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                                tracker.trackEvent("DisclaimerDialog", "DisclaimerDisagree", null, 0);
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
                                builder1.setMessage(getString(R.string.versionInfo) + System.getProperty("line.separator") + getString(R.string.aboutMsg)) .setCancelable(false) .setTitle(R.string.menuAbout) .setPositiveButton(R.string.menuAboutOkay, new DialogInterface.OnClickListener() {
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
                super.onSaveInstanceState(savedInstanceState);
                savedInstanceState.putString("finalResults", finalResults.getText().toString());
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
					wimaxPhone = "supersonic";
					return;
				} else if(line.contains("speedy")) {
					wimaxPhone = "speedy";
					return;
				}
				line = data.readLine();
			}
		} catch (Exception e) {
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
			Log.e("WimaxKeyCheck", "Exception while trying to run: '" + cmd + "' " + e.getMessage());
			process = null;
		}
		return process;
	}
 
    /** thanks birbeck */
        private void parseCheckResult(String result) {
        /** EditText text = (EditText)findViewById(R.id.FinalResults); */
 
 		if("found".equals(result)) {
 			tracker.trackEvent("WiMAXResults", "RSAKeyPresent", null, 0);
                        finalResults.setTextColor(getResources().getColor(R.color.success_text));
                        wimaxRSAButton.setTextColor(getResources().getColor(R.color.success_button));
                        finalResults.setText(getString(R.string.WiMAXKeyPresent));
                        showToast(getString(R.string.WiMAXKeyPresent));
 		} else if("not found".equals(result)) {
 			tracker.trackEvent("WiMAXResults", "RSAKeyMissing", null, 0);
                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                        wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
                        finalResults.setText(getString(R.string.WiMAXKeyMissing));
                        showToast(getString(R.string.WiMAXKeyMissing));
 		} else {
                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                        wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
                        tracker.trackEvent("WiMAXResults", "NoWiMAXPartition", null, 0);
                        finalResults.setText(getString(R.string.noWiMAXPartition));
                        showToast(getString(R.string.noWiMAXPartition));
                } 
		enableButtons();
                tracker.dispatch();
        } 
 
        
    /** setGravity adjustments - http://3.ly/sP6b */
        public void showToast(String toast) {
        /** Toast.makeText(WimaxKeys.this, toast, Toast.LENGTH_SHORT).show(); */
 
                Toast msg = Toast.makeText(WimaxKeys.this, toast, Toast.LENGTH_SHORT);
                msg.setGravity(Gravity.CENTER, msg.getXOffset() + 5, msg.getYOffset());
                msg.show();
                return;
        }
 
        
    /** thanks birbeck */
        @Override
        public void onDestroy() {
                super.onDestroy();
                if (mTask != null) {
                        mTask.cancel(true);
                }
                tracker.dispatch();
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
 				
 				if (wimaxPhone.equals("supersonic")) {
					device = "/dev/mtd/mtd0ro";
				} else if(wimaxPhone.equals("speedy")) {
					device = "/dev/block/mmcblk0p25";
				}
				try {
					while (start > 0) {
						process = catRange(device, start, count);
						if(process == null) {
							return "error";
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
								return "found";
							}
						}
						
						start = start - count;
					}
					// never found it
					return "not found";
				} catch (Exception e) {
					Log.d("WimaxKeyCheck","error",e);
					//TODO
				}
	                                
                        }
                        return "error";
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

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

import android.util.Base64;
import android.util.Base64InputStream;
import java.io.StringBufferInputStream;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import javax.security.auth.x500.X500Principal;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.joeykrim.wimaxkeys.ShellCommand.CommandResult;

public class WimaxKeys extends Activity {
 
        private Button rootButton;
        private Button busyboxButton;
        private Button wimaxButton;
        private Button wimaxVerifyButton;
        private Button authorButton;
        private TextView finalResults;
        private String wimaxPhone = null;
        public static final String PREFS_NAME = "PrefFile";
        private boolean disAccepted;
        static final int DIALOG_DISCLAIMER_ID = 0;
        static final int DIALOG_ABOUT_ID = 1;

        GoogleAnalyticsTracker tracker;


        CoreTask coretask = new CoreTask();
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
                tracker.trackEvent("LocalAppVersion", "2.2", null, 0);
 
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
                                rootButton.setEnabled(false);
                                busyboxButton.setEnabled(false);
                                wimaxButton.setEnabled(false);
                                Boolean rootCheck = coretask.hasRootPermission();
                                if (rootCheck == true) {
                                        finalResults.setTextColor(getResources().getColor(R.color.success_text));
                                        rootButton.setTextColor(getResources().getColor(R.color.success_button));
                                        finalResults.setText(getString(R.string.rootSuccess));
                                        showToast(getString(R.string.rootSuccess));
                    /** changing background color adjusts button height affecting layout
                     * rootButton.setBackgroundColor(0xff00ff00); */ 
                                        tracker.trackEvent("RootResult", "Success", null, 0);
                                } else {
                                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                                        rootButton.setTextColor(getResources().getColor(R.color.fail_button));
                                        finalResults.setText(getString(R.string.rootFail));
                                        showToast(getString(R.string.rootFail));
                                        tracker.trackEvent("RootResult", "Fail", null, 0);
                                }
                                rootButton.setEnabled(true);
                                busyboxButton.setEnabled(true);
                                wimaxButton.setEnabled(true);
                                tracker.dispatch();
                        }
                } );
 
                busyboxButton = (Button) findViewById(R.id.busyboxButton);
 
                busyboxButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                                tracker.trackEvent("ButtonClicked", "BusyboxCheck", null, 0);
                                rootButton.setEnabled(false);
                                busyboxButton.setEnabled(false);
                                wimaxButton.setEnabled(false);
                                String busyboxResults = coretask.busyboxPresent();
                                if (busyboxResults == "error") {
                                        tracker.trackEvent("BusyboxResult", "Fail", null, 0);
                                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                                        busyboxButton.setTextColor(getResources().getColor(R.color.fail_button));
                                        finalResults.setText(getString(R.string.busyboxFail));
                                        showToast(getString(R.string.busyboxFail));
                                } else {
                                        tracker.trackEvent("BusyboxResult", "Success", null, 0);
                                        finalResults.setTextColor(getResources().getColor(R.color.success_text));
                                        busyboxButton.setTextColor(getResources().getColor(R.color.success_button));
                                        finalResults.setText(getString(R.string.busyboxSuccess));
                                        showToast(getString(R.string.busyboxSuccess));
                                }
                                rootButton.setEnabled(true);
                                busyboxButton.setEnabled(true);
                                wimaxButton.setEnabled(true);
                                tracker.dispatch();
                        }
                } );
 
                wimaxButton = (Button) findViewById(R.id.wimaxButton);
 
                wimaxButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                                tracker.trackEvent("ButtonClicked", "WiMAXCheck", null, 0);
                                rootButton.setEnabled(false);
                                busyboxButton.setEnabled(false);
                                wimaxButton.setEnabled(false);
                                String busyboxResults = coretask.busyboxPresent();
                                if (busyboxResults == "error") {
                                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                                        wimaxButton.setTextColor(getResources().getColor(R.color.fail_button));
                                        finalResults.setText(getString(R.string.busyboxFail));
                                        showToast(getString(R.string.busyboxFail));
                                } else {
                                        if (coretask.runShellCommand("su", "stdout", "busybox grep supersonic /system/build.prop").indexOf("supersonic") != - 1) {
                                                wimaxPhone = "supersonic";
                                                tracker.trackEvent("WiMAXCheck", "EVO", null, 0);
                                                mTask = (MyTask) new MyTask().execute();
                                        } else {
                                                if (coretask.runShellCommand("su", "stdout", "busybox grep speedy /system/build.prop").indexOf("speedy") != - 1) {
                                                        wimaxPhone = "speedy";
                                                        tracker.trackEvent("WiMAXCheck", "Shift", null, 0);
                                                        mTask = (MyTask) new MyTask().execute();
                                                } else {
                                                        tracker.trackEvent("WiMAXCheck", "Not Compatible", null, 0);
                                                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                                                        wimaxButton.setTextColor(getResources().getColor(R.color.fail_button));
                                                        finalResults.setText(getString(R.string.notCompatible));
                                                        showToast(getString(R.string.notCompatible));
                                                } 
                                        }
                                }
                                rootButton.setEnabled(true);
                                busyboxButton.setEnabled(true);
                                wimaxButton.setEnabled(true);
                                tracker.dispatch();
                        }
                } );
                
                
                wimaxVerifyButton = (Button) findViewById(R.id.wimaxVerifyButton);
 
                wimaxVerifyButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                                tracker.trackEvent("ButtonClicked", "WiMAXVerify", null, 0);
                                disableButtons();
                                String busyboxResults = coretask.busyboxPresent();
                                if (busyboxResults == "error") {
                                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                                        wimaxButton.setTextColor(getResources().getColor(R.color.fail_button));
                                        finalResults.setText(getString(R.string.busyboxFail));
                                        showToast(getString(R.string.busyboxFail));
                                        enableButtons();
                                } else {
                                        if (coretask.runShellCommand("su", "stdout", "busybox grep supersonic /system/build.prop").indexOf("supersonic") != - 1) {
                                                wimaxPhone = "supersonic";
                                                tracker.trackEvent("WiMAXVerify", "EVO", null, 0);
                                                mTask = new WiMaxValidateTask().execute();
                                        } else {
                                                if (coretask.runShellCommand("su", "stdout", "busybox grep speedy /system/build.prop").indexOf("speedy") != - 1) {
                                                        wimaxPhone = "speedy";
                                                        tracker.trackEvent("WiMAXVerify", "Shift", null, 0);
                                                        mTask = new WiMaxValidateTask().execute();
                                                } else {
                                                        tracker.trackEvent("WiMAXVerify", "Not Compatible", null, 0);
                                                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                                                        wimaxVerifyButton.setTextColor(getResources().getColor(R.color.fail_button));
                                                        finalResults.setText(getString(R.string.notCompatible));
                                                        showToast(getString(R.string.notCompatible));
                                                        enableButtons();
                                                } 
                                        }
                                }
                                tracker.dispatch();
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
		busyboxButton.setEnabled(false);
		wimaxButton.setEnabled(false);
		wimaxVerifyButton.setEnabled(false);
 	}
 	
 	private void enableButtons() {
	 	rootButton.setEnabled(true);
		busyboxButton.setEnabled(true);
		wimaxButton.setEnabled(true);
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
 
    /** thanks birbeck */
 
        private void parseResult(String mString) {
        /** EditText text = (EditText)findViewById(R.id.FinalResults); */
 
 
                if (mString.equals("error")) {
                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                        wimaxButton.setTextColor(getResources().getColor(R.color.fail_button));
                        tracker.trackEvent("WiMAXResults", "NoWiMAXPartition", null, 0);
                        finalResults.setText(getString(R.string.noWiMAXPartition));
                        showToast(getString(R.string.noWiMAXPartition));
                }
                if (mString.lastIndexOf("RSA PRIVATE KEY") == - 1) {
                        tracker.trackEvent("WiMAXResults", "RSAKeyMissing", null, 0);
                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                        wimaxButton.setTextColor(getResources().getColor(R.color.fail_button));
                        finalResults.setText(getString(R.string.WiMAXKeyMissing));
                        showToast(getString(R.string.WiMAXKeyMissing));
                } else {
                        tracker.trackEvent("WiMAXResults", "RSAKeyPresent", null, 0);
                        finalResults.setTextColor(getResources().getColor(R.color.success_text));
                        wimaxButton.setTextColor(getResources().getColor(R.color.success_button));
                        finalResults.setText(getString(R.string.WiMAXKeyPresent));
                        showToast(getString(R.string.WiMAXKeyPresent));
                } 
                tracker.dispatch();
        } 
        
        private void parseVerifyResult(String result) {
        	
                if("match".equals(result)) {
                	tracker.trackEvent("WiMAXResults", "RSAKeyMatch", null, 0);
                        finalResults.setTextColor(getResources().getColor(R.color.success_text));
                        wimaxVerifyButton.setTextColor(getResources().getColor(R.color.success_button));
                        finalResults.setText(getString(R.string.WiMAXKeyMatch));
                        showToast(getString(R.string.WiMAXKeyMatch));
                } else if("no match".equals(result)) {
                	tracker.trackEvent("WiMAXResults", "RSAKeyNoMatch", null, 0);
                        finalResults.setTextColor(getResources().getColor(R.color.warn_text));
                        wimaxVerifyButton.setTextColor(getResources().getColor(R.color.warn_button));
                        finalResults.setText(getString(R.string.WiMAXKeyNoMatch));
                        showToast(getString(R.string.WiMAXKeyNoMatch));
                } else {
        		finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                        wimaxVerifyButton.setTextColor(getResources().getColor(R.color.fail_button));
                        tracker.trackEvent("WiMAXResults", "NoWiMAXVerify", null, 0);
                        finalResults.setText(getString(R.string.noWiMAXVerify));
                        showToast(getString(R.string.noWiMAXVerify));
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
 
        class WiMaxValidateTask extends AsyncTask<Void, Void, String> {
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
 				String result;
                                if (wimaxPhone.equals("supersonic")) {
					result = coretask.runShellCommand("su", "stdout", "busybox sed -n -e 's/^.*----BEGIN/----BEGIN/' -e '/BEGIN CERT/,/END CERT/p' /dev/mtd/mtd0");
				} else if(wimaxPhone.equals("speedy")) {
					result = coretask.runShellCommand("su", "stdout", "busybox sed -n -e 's/^.*----BEGIN/----BEGIN/' -e '/BEGIN CERT/,/END CERT/p' /dev/block/mmcblk0p25");
                                } else {
                                	result = "error";
                                }
				if("error".equals(result)) {
					return result;
				}
				
				//skip the header/footer
				//-----BEGIN CERTIFICATE-----
				//-----END CERTIFICATE-----
				result = result.substring(27,result.length()-25);
				try {
					Base64InputStream pem = new Base64InputStream(new StringBufferInputStream(result) , Base64.DEFAULT);
					X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509", "BC").generateCertificate(pem);
					String dn = cert.getSubjectX500Principal().getName("RFC2253").toLowerCase();
				       
					String mac = coretask.runShellCommand("sh", "stdout", "cat /sys/class/net/wimax0/address");
					mac = mac.toLowerCase().replaceAll(":","");
					
					
					
					if(dn.contains(mac)) 
						return "match";
					else
						return "no match";
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
 
                        parseVerifyResult(result);
                        mTask = null;
                }
        }
 
    /** thanks birbeck */
 
        class MyTask extends AsyncTask<Void, Void, String> {
 
                ProgressDialog mDialog = new ProgressDialog(WimaxKeys.this);
 
 
                @Override
                protected void onPreExecute() {
                        mDialog.setMessage(getString(R.string.WiMAXKeyCheckMsg));
                        mDialog.setCancelable(false);
                        mDialog.show();
                }
 
 
                @Override
                protected String doInBackground(Void... params) {
            /** String result = null; */
 
 
            /** try {
             * Thread.sleep(3000);
             * result = "Finished";
             * } catch (InterruptedException e) {
             * e.printStackTrace();
             * } */ 
            /** return result; */
 
            /** coretask.hasRootPermissions(); */
 
                        if (wimaxPhone != null) {
 
                                if (wimaxPhone.equals("supersonic")) {
                                        return coretask.runShellCommand("su", "stdout", "busybox grep RSA /dev/mtd/mtd0"); 
                                }
                                if (wimaxPhone.equals("speedy")) {
                                        return coretask.runShellCommand("su", "stdout", "busybox grep RSA /dev/block/mmcblk0p25");
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
 
                        parseResult(result);
                        mTask = null;
                }
        }
 
    /** thanks LouZiffer */
 
        public class CoreTask {
 
                public String busyboxPresent() {
                        return runShellCommand("sh", "stdout", "busybox");
                }
 
                public boolean hasRootPermission() {
                        boolean rooted = true;
                        try {
                                ShellCommand cmd = new ShellCommand();
                                if (! cmd.canSU()) {
                                        rooted = false;
                                }

                        } catch (Exception e) {
                /** if (debug.exists()) Log.d(MSG_TAG, "Can't obtain root - Here is what I know: "+e.getMessage());
                 * rooted = false; */ 
                        }
                        return rooted;
                }
 
        /**
         * Runs shell commands as sh or su.
         *
         * @param UserType - sh or su
         * @param OutputType - exit, stdout, or stderr
         * @param Command - properly formed shell command
         * @return Output
         * thanks LouZiffer
         */ 
                public String runShellCommand(String UserType, String OutputType, String Command) {
                        String Output = "";
 
                        if (UserType == "su") {
                                ShellCommand cmd = new ShellCommand();
                                CommandResult r = cmd.su.runWaitFor(Command);
                                if (! r.success()) {
                    /** if (debug.exists()) Log.d(MSG_TAG, "Error " + r.stderr); */
 
                                } else {
                    /** if (debug.exists()) Log.d(MSG_TAG, "Successfully executed command " + Command + " Result is: "+ r.stdout); */
 
                                        if (OutputType == "stdout") {
                                                Output = r.stdout;
                                        }
                                        if (OutputType == "stderr") {
                                                Output = r.stderr;
                                        }
                                        if (OutputType == "exit") {
                                                Output = Integer.toString(r.exit_value);
                                        }
                                }
                        } else {
                                ShellCommand cmd = new ShellCommand();
                                CommandResult r = cmd.sh.runWaitFor(Command);
                                if (! r.success()) {
                    /** if (debug.exists()) Log.d(MSG_TAG, "Error " + r.stderr); */
 
                    /** i added output string error */
 
                                        Output = "error";
                                } else {
                    /** if (debug.exists()) Log.d(MSG_TAG, "Successfully executed command " + Command + " Result is: " + r.stdout); */
 
                                        if (OutputType == "stdout") {
                                                Output = r.stdout;
                                        }
                                        if (OutputType == "stderr") {
                                                Output = r.stderr;
                                        }
                                        if (OutputType == "exit") {
                                                Output = Integer.toString(r.exit_value);
                                        }
                                }
                        }
                        return Output;
                }
        }
 
}
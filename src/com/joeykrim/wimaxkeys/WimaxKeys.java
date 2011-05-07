package com.joeykrim.wimaxkeys;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class WimaxKeys extends Activity {

    private Button wimaxRSAButton;
    private TextView finalResults;
    private String wimaxPhone = null;
    public static final String PREFS_NAME = "PrefFile";
    private boolean disAccepted;
    static final int DIALOG_DISCLAIMER_ID = 0;
    static final int DIALOG_ABOUT_ID = 1;
    static final double CURRENT_VERSION_ID = 2.9;
    private static String LOG_TAG = "WimaxKeyCheck";
    private static String PHONE_EVO = "EVO";
    private static String PHONE_SHIFT = "EVO Shift";

    // Google Analytics Events
    private static String GAE_APP_VERSION = "LocalAppVersion";
    private static String GAE_SYSTEM = "SystemData";
    private static String GAE_BUTTON = "ButtonClicked";
    private static String GAE_MENU = "MenuItemSelected";
    private static String GAE_ROOT_RESULT = "RootResult";
    private static String GAE_DISCLAIMER = "DisclaimerDialog";
    private static String GAE_WIMAX_CHECK = "WiMAXCheck";
    private static String GAE_WIMAX_RESULT = "WiMAXResults";

    GoogleAnalyticsTracker tracker;

    AsyncTask mTask = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.start("", this);

        /** Thanks AntiSocial
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
                    tracker.trackEvent(GAE_ROOT_RESULT, "Success", null, 0);
                    showToast(getString(R.string.rootSuccess));
                    setWimaxPhone();
                    if(!PHONE_EVO.equals(wimaxPhone) && !PHONE_SHIFT.equals(wimaxPhone)) {
                        tracker.trackEvent(GAE_WIMAX_CHECK, "Not Compatible", null, 0);
                        finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                        wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
                        finalResults.setText(getString(R.string.notCompatible));
                        showToast(getString(R.string.notCompatible));
                        tracker.dispatch();
                        return;
                    }
                    mTask = new WiMaxCheckTask().execute();
                } else {
                    tracker.trackEvent(GAE_ROOT_RESULT, "Fail", null, 0);
                    wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
                    finalResults.setTextColor(getResources().getColor(R.color.fail_text));
                    finalResults.setText(getString(R.string.rootFail));
                    showToast(getString(R.string.rootFail));
                }
                tracker.dispatch();
                enableButtons();
            }
        } );

        TextView author = (TextView) findViewById(R.id.author);
        author.setMovementMethod(LinkMovementMethod.getInstance());

        TextView credits = (TextView) findViewById(R.id.Credits);
        credits.setMovementMethod(LinkMovementMethod.getInstance());
        /** http://developer.android.com/guide/topics/data/data-storage.html#pref */
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        disAccepted = settings.getBoolean("disclaimerAccepted", false);
        if (! disAccepted) { showDialog(DIALOG_DISCLAIMER_ID); }
    }

    private void disableButtons() {
        wimaxRSAButton.setEnabled(false);
    }

    private void enableButtons() {
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
        //Handle item selection
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
            builder.setMessage(R.string.disclaimerMessage)
            .setCancelable(false)
            .setTitle(R.string.menuDisclaimer)
            .setPositiveButton(R.string.disclaimerAgree, new DialogInterface.OnClickListener() {
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
            } ) 
            .setNegativeButton(R.string.disclaimerDisagree, new DialogInterface.OnClickListener() {
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
            builder1.setMessage(getString(R.string.versionInfo, CURRENT_VERSION_ID) + System.getProperty("line.separator") + getString(R.string.aboutMsg))
            .setCancelable(false) .setTitle(R.string.menuAbout)
            .setPositiveButton(R.string.menuAboutOkay, new DialogInterface.OnClickListener() {
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
        if("found".equals(result)) {
            tracker.trackEvent(GAE_WIMAX_RESULT, "RSAKeyPresent", null, 0);
            finalResults.setTextColor(getResources().getColor(R.color.success_text));
            wimaxRSAButton.setTextColor(getResources().getColor(R.color.success_button));
            finalResults.setText(getString(R.string.WiMAXKeyPresent, wimaxPhone));
            showToast(getString(R.string.WiMAXKeyPresent, wimaxPhone));
        } else if("not found".equals(result)) {
            tracker.trackEvent(GAE_WIMAX_RESULT, "RSAKeyMissing", null, 0);
            finalResults.setTextColor(getResources().getColor(R.color.fail_text));
            wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
            finalResults.setText(getString(R.string.WiMAXKeyMissing, wimaxPhone));
            showToast(getString(R.string.WiMAXKeyMissing, wimaxPhone));
        } else {
            tracker.trackEvent(GAE_WIMAX_RESULT, "Error", null, 0);
            finalResults.setTextColor(getResources().getColor(R.color.fail_text));
            wimaxRSAButton.setTextColor(getResources().getColor(R.color.fail_button));
            tracker.trackEvent(GAE_WIMAX_RESULT, "NoWiMAXPartition", null, 0);
            finalResults.setText(getString(R.string.noWiMAXPartition));
            showToast(getString(R.string.noWiMAXPartition));
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
        tracker.stop();
        super.onDestroy();
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
                String device = null;
                String partitionFilename = "/proc/partitions";
                String partitionSearchValue = null;
                String partitionReturnLine = null;
                int partitionSize = 0;

                //Determine WiMAX partition size
                if (PHONE_EVO.equals(wimaxPhone)) {
                    device = "/dev/mtd/mtd0ro";
                    partitionSearchValue = "mtdblock0";
                } else if(PHONE_SHIFT.equals(wimaxPhone)) {
                    device = "/dev/block/mmcblk0p25";
                    partitionSearchValue = "mmcblk0p25";
                }
                BufferedReader data = null;
                try {
                    File file = new File(partitionFilename);
                    data = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    String line = null;
                    while((line = data.readLine()) != null) {
                        if(line.contains(partitionSearchValue)) {
                            partitionReturnLine = line;
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (data != null) {
                        try { 
                            data.close();
                        } catch (Exception e) { /* do nothing */ }
                    }
                }

                /* empty major minor blocks name */
                String[] fields = partitionReturnLine.split("  *");
                try {
                    partitionSize = Integer.parseInt(fields[3]); 
                } catch (NumberFormatException e) {
                    Log.d(LOG_TAG,"bad field",e);
                    return "error";
                }

                //Search WiMAX partition for RSA Key
                Process procWiMAX = null;
                int offset = 100;
                int overlap = 40;
                int start = 2200;
                int count = start - offset; //3071 is the end of the file
                int end = ((partitionSize / 4)-1); //3072 then -1 is 3071
                boolean readOnce = false;
                try {
                    while (count > 0) {
                        //Log.d(LOG_TAG,"count 1 is: " + String.valueOf(count));
                        procWiMAX = catRange(device, count, offset + overlap);
                        if(procWiMAX == null) {
                            return "error";
                        }
                        BufferedReader br = new BufferedReader(new InputStreamReader(procWiMAX.getInputStream()));
                        String line = br.readLine();
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

                            line = br.readLine();
                        }
                        if(foundStart || foundEnd) {
                            //our window size cut it off?

                            if(foundStart && !foundEnd) {
                                //shouldnt be more than a few blocks
                                offset++;
                                if (offset > 104) {return "not found";}
                                continue;
                            } else if(!foundStart && foundEnd) {
                                count--;
                                continue;
                            } else {
                                if (String.valueOf(count) != null) { tracker.trackEvent("WiMAXKeyStart", String.valueOf(count), null, 0); }
                                if (String.valueOf(offset) != null) { tracker.trackEvent("WiMAXKeyCount", String.valueOf(offset), null, 0); }
                                tracker.dispatch();
                                return "found";
                            }
                        }

                        count = count - offset;
                        //Log.d(LOG_TAG,"count 2 is: " + String.valueOf(count));
                        if (count < start && readOnce) {break;}
                        if (!foundStart && !foundEnd && (count <= 0) && !readOnce) {
                            readOnce = true;
                            count = end - offset;
                        }
                        //Log.d(LOG_TAG,"count 3 is: " + String.valueOf(count));
                    }
                    //never found it
                    return "not found";
                } catch (Exception e) {
                    Log.d(LOG_TAG,"error",e);
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

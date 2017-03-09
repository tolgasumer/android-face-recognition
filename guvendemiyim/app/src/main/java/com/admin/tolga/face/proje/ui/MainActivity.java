
package com.admin.tolga.face.proje.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "MyPrefsFile";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.admin.tolga.face.proje.R.layout.activity_main);


        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0); // preference dosyasi ac
        boolean firstRun = settings.getBoolean("firstRun", true); // ilk run ise true

        if (firstRun) {
            Log.w("activity", "first time");
            Intent intent2 = new Intent(this, OptionsActivity.class);
            startActivity(intent2);

            SharedPreferences.Editor editor = settings.edit(); // preference dosyasi edit
            editor.putBoolean("firstRun", false); // first run false yap
            editor.commit(); // save et kapat
        } else {
            Log.w("activity", "second time");
            Intent intent3 = new Intent(this, MainActivity.class);
            startActivity(intent3);
        }

        if (getString(com.admin.tolga.face.proje.R.string.subscription_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(com.admin.tolga.face.proje.R.string.add_subscription_key_tip_title))
                    .setMessage(getString(com.admin.tolga.face.proje.R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }
    }



    public void identification(View view) {
        Intent intent = new Intent(this, IdentificationActivity.class);
        startActivity(intent);
    }
}

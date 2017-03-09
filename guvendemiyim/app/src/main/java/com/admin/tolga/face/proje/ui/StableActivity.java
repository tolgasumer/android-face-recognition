
package com.admin.tolga.face.proje.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class StableActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.admin.tolga.face.proje.R.layout.stable);

        if (getString(com.admin.tolga.face.proje.R.string.subscription_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(com.admin.tolga.face.proje.R.string.add_subscription_key_tip_title))
                    .setMessage(getString(com.admin.tolga.face.proje.R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }
    }

    public void detection(View view) {
        Intent intent = new Intent(StableActivity.this, DetectionActivity.class);
        startActivity(intent);
    }

    public void verification(View view) {
        Intent intent = new Intent(StableActivity.this, VerificationMenuActivity.class);
        startActivity(intent);
    }

    public void grouping(View view) {
        Intent intent = new Intent(StableActivity.this, GroupingActivity.class);
        startActivity(intent);
    }

    public void findSimilarFace(View view) {
        Intent intent = new Intent(StableActivity.this, FindSimilarFaceActivity.class);
        startActivity(intent);
    }


}

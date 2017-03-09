
package com.admin.tolga.face.proje.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class AccessDeniedActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.admin.tolga.face.proje.R.layout.accessdenied);

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

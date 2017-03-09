
package com.admin.tolga.face.proje.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;


public class VerificationMenuActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.admin.tolga.face.proje.R.layout.activity_verification_menu);
    }

    public void faceFaceVerification(View view)
    {
        Intent intent = new Intent(this, FaceVerificationActivity.class);
        startActivity(intent);
    }

    //face to face verification button click
    public void facePersonVerification(View view)
    {
        Intent intent = new Intent(this, PersonVerificationActivity.class);
        startActivity(intent);
    }
}

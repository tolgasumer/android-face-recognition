
package com.admin.tolga.face.proje.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.admin.tolga.face.proje.persongroupmanagement.PersonGroupListActivity;

public class OptionsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.admin.tolga.face.proje.R.layout.options);
        String name = getIntent().getStringExtra("data");

        TextView text = (TextView) findViewById(com.admin.tolga.face.proje.R.id.textview1);
        text.setText("USER: "+name);

    }


    public void manageButtonClick(View view) {
        Intent intent = new Intent(this, PersonGroupListActivity.class);
        startActivity(intent);
    }
    public void stableButtonClick(View view) {
        Intent intent = new Intent(this, StableActivity.class);
        startActivity(intent);
    }

}

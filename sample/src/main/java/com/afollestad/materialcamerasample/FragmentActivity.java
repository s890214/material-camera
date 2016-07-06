package com.afollestad.materialcamerasample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class FragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, DemoFragment.getInstance()).commit();
        }
    }

}

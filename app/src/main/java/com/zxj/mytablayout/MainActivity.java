package com.zxj.mytablayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.zxj.mytablayout.tablayout.TabLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TabLayout mTabLayout = findViewById(R.id.tab_layout);
        for (int i = 0; i < 3; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_skill_tab, null, false);
            TextView tvName = view.findViewById(R.id.tv_name);
            tvName.setText("城市导游" + i);
            TabLayout.Tab tab = mTabLayout.newTab().setCustomView(view);
            //设置第一个默认选中Tab
            if (i == 0) {
                mTabLayout.addTab(tab, true);
            } else {
                mTabLayout.addTab(tab);
            }
        }
    }
}

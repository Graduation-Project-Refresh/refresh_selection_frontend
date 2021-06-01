package com.example.refresh_selection;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class RecommendStep extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main) ;

        //30개미만 걸음수가 없을땐 기본 8000보를 추천

        //30개이상 걸음수가 있을땐 구분해서 평균 걸음수를 추천
    }
}

package com.example.refresh_selection;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class Survey extends AppCompatActivity {

    static final String[] List_menu={"4:00-6:00","6:00-8:00","8:00-10:00","10:00-12:00","12:00-14:00","14:00-16:00","16:00-18:00","18:00-20:00","20:00-22:00","22:00-00:00"};
//활동시간대 선택할 아이템들

//장소 카테고리
    Button next;
    String act_time;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState) ;
        setContentView(R.layout.survey) ;

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, List_menu) ;

        ListView listview = (ListView) findViewById(R.id.survey_act_time) ;
        listview.setAdapter(adapter) ;
        //check 박스로 바꾸자...
//        싹다 바꾸자
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // 코드 계속 ...
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {

                // get TextView's Text.

                act_time = (String) parent.getItemAtPosition(position) ;
                Log.d("선택한 활동시간대",act_time);
                //배열일까 그냥 하나의 스트링
                //volley로 데이터베이스에 보냅니다.
                // TODO : use strText
            }
        }) ;

        next= findViewById(R.id.survey_next);

        next.setOnClickListener(new View.OnClickListener(){
            Intent login_survey = getIntent();
            String UserId= login_survey.getStringExtra("UserId");
            @Override
            public void onClick(View view) {
                if(act_time.equals("")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(Survey.this);
                    dialog = builder.setMessage("활동 시간대를 선택해 주세요.").setNegativeButton("확인", null).create();
                    dialog.show();
                    return;

                }
                Intent intent = new Intent(Survey.this,Survey_place.class);
                intent.putExtra("act_time",act_time);
                Log.d("survey_act_time",act_time);
                intent.putExtra("UserId",UserId);
                Log.d("survey_userId",UserId);
                Log.d("다음 클릭시","활동시간대 선택 완료");
                startActivity(intent);

            }
        });



    }
}

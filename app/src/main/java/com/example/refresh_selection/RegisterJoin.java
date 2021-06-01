package com.example.refresh_selection;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterJoin extends AppCompatActivity {

    private EditText join_id, join_password, join_name, join_birthday;
    private RadioButton man,woman;
    private RadioGroup radio;
    private Button join_button, check_button ,cancel_button;
    private AlertDialog dialog;
    private boolean validate = false;
    private boolean j=false;
    boolean duplicate=true;
    private String UserSex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.join);

        //아이디값 찾아주기
        join_id = findViewById( R.id.join_id );
        join_password = findViewById( R.id.join_password );
        join_name = findViewById( R.id.join_name );
        join_birthday = findViewById(R.id.join_birthday);

        man=findViewById(R.id.man);//성별
        woman=findViewById(R.id.woman);
        radio=(RadioGroup) findViewById(R.id.radio);
        //라디오 그룹 클릭 리스너
        RadioGroup.OnCheckedChangeListener radioGroupButtonChangeListener = new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup radio, @IdRes int i) {
                if(i == R.id.man){
                        UserSex=man.getText().toString();
                }
                else if(i == R.id.woman){
                    UserSex=woman.getText().toString();
                }
            }
        };
        radio.setOnCheckedChangeListener(radioGroupButtonChangeListener);





        //아이디 중복 체크
        check_button = findViewById(R.id.check_button);
        check_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String UserId = join_id.getText().toString();

                if (validate) {
                    return; //검증 완료
                }

                if (UserId.equals("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterJoin.this);
                    dialog = builder.setMessage("아이디를 입력하세요.").setPositiveButton("확인", null).create();
                    dialog.show();
                    return;
                }
                
                Pattern regex= Pattern.compile("^[a-zA-Z]{1}[a-zA-Z0-9_]{4,11}");
                Matcher idMatcher =regex.matcher(UserId);
                
                if(!idMatcher.matches()){//아이디 양식 체크
                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterJoin.this);
                    dialog = builder.setMessage("아이디는 영문자와 숫자로 구성되어야 하며 5~12자 입니다.").setPositiveButton("확인", null).create();
                    dialog.show();
                    Log.d("아이디 양식 체크","실패");
                    return;   
                }

                IdDuplication(UserId);

            }
        });


        //회원가입 버튼 클릭 시 수행
        join_button = findViewById( R.id.join_button );
        join_button.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String UserId = join_id.getText().toString();
                final String UserPwd = join_password.getText().toString();
                final String UserName = join_name.getText().toString();
                final String UserBirthday = join_birthday.getText().toString();

                  //man이 true 여자가 false니까 하나만 갖고 검사하자.
//                final String UserSex =join_sex.getText().toString();

                if (!validate) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterJoin.this);
                    dialog = builder.setMessage("중복된 아이디가 있는지 확인하세요.").setNegativeButton("확인", null).create();
                    dialog.show();
                    return;
                }
                Pattern regex= Pattern.compile("[0-9_]{6}");
                Matcher BirthdayMatcher =regex.matcher(UserBirthday);

                if(!BirthdayMatcher.matches()){//생일 양식 체크
                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterJoin.this);
                    dialog = builder.setMessage("생일은 ex)1999 01 01-> 990101 6자리 입니다.").setPositiveButton("확인", null).create();
                    dialog.show();
                    Log.d("생일 양식 체크","");
                    return;
                }


                //한 칸이라도 입력 안했을 경우 아이디,비밀번호,이름,생일,성별
//                if (UserId.equals("") || UserPwd.equals("") || UserName.equals("")||UserBirthday.equals("")||UserSex.equals("")) {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterJoin.this);
//                    dialog = builder.setMessage("모두 입력해주세요.").setNegativeButton("확인", null).create();
//                    dialog.show();
//                    return;
//                }
                if (checkNull(UserId, UserPwd, UserName, UserBirthday,UserSex)) return; //null이 하나라도있으면 true

                join(UserId, UserPwd, UserName, UserBirthday,UserSex);//사용자 정보를 서버에 전송해 회원가입함

            }
        });
        
        //취소 버튼 눌렀을대 로그인 화면으로 돌아감
        cancel_button=findViewById(R.id.cancel_button);
        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =new Intent(RegisterJoin.this,LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    protected boolean checkNull(String userId, String userPwd, String userName, String userBirthday, String userSex) {
        if (userId.equals("") || userPwd.equals("") || userName.equals("")|| userBirthday.equals("")||userSex.equals("")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(RegisterJoin.this);
            dialog = builder.setMessage("모두 입력해주세요.").setNegativeButton("확인", null).create();
            dialog.show();
            return true;
        }
        return false;
    }

//    protected boolean idDuplication(Boolean validate) {
//        if (!validate) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(RegisterJoin.this);
//            dialog = builder.setMessage("중복된 아이디가 있는지 확인하세요.").setNegativeButton("확인", null).create();
//            dialog.show();
//            return true;
//        }
//        return false;
//    }

    protected Boolean join(String userId, String userPwd, String userName, String userBirthday,String UserSex) {

        RequestQueue queue = Volley.newRequestQueue(RegisterJoin.this);
        String url = "http://3.143.147.178:3000/api/user/register";
        JSONObject testjson = new JSONObject();
        try {
            testjson.put("UserId", userId);
            testjson.put("UserPwd", userPwd);
            testjson.put("UserName", userName);
            testjson.put("UserBirthday", userBirthday);
            testjson.put("UserSex",UserSex);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, testjson, new Response.Listener() {

            @Override
            public void onResponse(Object response) {
                try {
                    JSONObject jsonObject = new JSONObject( response.toString() );
                    boolean success = jsonObject.getBoolean( "success" );

                    //회원가입 성공시
//
                        if (success) {
                            validate=false;
                            Toast.makeText(getApplicationContext(), String.format("%s님 가입을 환영합니다.", userName), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterJoin.this, LoginActivity.class);
                            startActivity(intent);
                            j=true;
                        } else {//회원가입 실패
                            j=false;
                            validate=false;
                            Toast.makeText(getApplicationContext(), "회원가입에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                            return ;
                        }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
        return j;
    }

    protected boolean IdDuplication(String userId) {

        RequestQueue queue = Volley.newRequestQueue(RegisterJoin.this);
        String url = "http://3.143.147.178:3000/api/user/"+ userId;
        JSONObject testjson = new JSONObject();
        try {
            testjson.put("UserId", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, testjson, new Response.Listener() {

            @Override
            public void onResponse(Object response) {
                try {

                            JSONObject jsonResponse = new JSONObject(response.toString());
                            boolean success = jsonResponse.getBoolean("success");
//                            Log.d("Response is: "+ response.substring(0,500),"아이디 중복 확인");
                            if (success) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterJoin.this);
                                dialog = builder.setMessage("사용할 수 있는 아이디입니다.").setPositiveButton("확인", null).create();
                                dialog.show();
                                join_id.setEnabled(false); //아이디값 고정
                                validate = true; //검증 완료
                                 duplicate = false;//아이디 중복아님
                                check_button.setBackgroundColor(getResources().getColor(R.color.colorGray));
                            }
                            else {
                                duplicate=true;
                                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterJoin.this);
                                dialog = builder.setMessage("이미 존재하는 아이디입니다.").setNegativeButton("확인", null).create();
                                dialog.show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                Log.d("ddd","ddd");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
        return duplicate;//이상해지면 제거해
    }
}
package com.example.refresh_selection;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SurveyRequest extends StringRequest {
    //서버 URL 설정(php 파일 연동)
    final static private String URL = "http://3.143.147.178:3000/api/research";//바꿔 혜빈이가 만들어 주는 survey url로
    private Map<String, String> map;

    public SurveyRequest(String user_id,String act_time , ArrayList<String> place, Response.Listener<String> listener) {
        super(Method.POST, URL, listener, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        );
        map = new HashMap<>();
//        String tm[]=act_time.split("-");
//        map.put("AcriveStartTm", tm[0]);
//        map.put("ActiveEndTm",tm[1]);
        String data= new Gson().toJson(place);
        map.put("UserId",user_id);
        Log.d("SurveyRequest",user_id);
        map.put("ActiveTime",act_time);
        Log.d("SurveyRequest",act_time);
        map.put("PreferPlace",data);
        //? 뭘까.. map 왜쓸까..
    }

    @Override
    protected Map<String, String>getParams() throws AuthFailureError {
        return map;
    }
}

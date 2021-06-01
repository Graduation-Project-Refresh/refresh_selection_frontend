package com.example.refresh_selection;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class ValidateRequest extends StringRequest {
    //서버 url 설정(php파일 연동)
    static  private String URL="http://3.143.147.178:3000/api/user/";
//            "http://gyfls7748.dothome.co.kr/UserValidation.php";
//            "http://3.143.147.178:3000/api/user/";

    private Map<String, String> map;

    public ValidateRequest(String UserId, Response.Listener<String> listener){
        super(Method.GET, URL+UserId, listener,null);
        Log.d("",URL);
        map = new HashMap<>();
        map.put("UserId", UserId);
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return map;
    }
}


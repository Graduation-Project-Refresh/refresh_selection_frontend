package com.example.refresh_selection;


import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class JoinRequest extends StringRequest{

    //서버 URL 설정
    final static private String URL = "http://3.143.147.178:3000/api/user/register";
            //"http://3.143.147.178:3000/api/user/register";
//    private Map<String, String> map;
    private Map<String,String> map;

    public JoinRequest(String UserId, String UserPwd, String UserName,String UserBirthday, String UserSex, Response.Listener<String> listener) {
        super(Method.POST, URL, listener, null);

        map = new HashMap<>();
        map.put("UserId", UserId);
        map.put("UserPwd", UserPwd);
        map.put("UserName", UserName);
        map.put("UserBirthday", UserBirthday);
        map.put("UserSex", UserSex);
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return map;
    }
//    @over
//    protected Map<String, Boolean>getParams() throws AuthFailureError {
//        return map;
//    }
}

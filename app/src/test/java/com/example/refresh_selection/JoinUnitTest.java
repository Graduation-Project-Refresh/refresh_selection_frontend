package com.example.refresh_selection;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class JoinUnitTest {
    RegisterJoin Rj =new RegisterJoin();
    LoginActivity LA= new LoginActivity();

    @Test
    public void JoinTrue() {

        assertEquals(Rj.join("test1", "test!123", "테스트1", "1999-01-01","남자"), java.util.Optional.of(true));
    }
    @Test
    public void checkNullTrue() {//null이 하나라도 있는지

        assertEquals(Rj.checkNull("test1", "", "테스트1", "","남자"),true);
    }
    @Test//중복이면 true
    public void idDuplicationTrue(){
        assertEquals(Rj.IdDuplication("test1"),true);
    }

    @Test
    public void LoginCheckTrue(){
        assertEquals(LA.Login("test1","test123!@#"),java.util.Optional.of(true));
    }

}

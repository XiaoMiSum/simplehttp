package xyz.migoo.simplehttp.test;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import xyz.migoo.simplehttp.HttpException;
import xyz.migoo.simplehttp.Request;

/**
 * @author xiaomi
 * @date 2019/9/13 11:32
 */
class SimpleHttpTest {

    @Test
    void testGet() throws HttpException {
        Assertions.assertTrue(Request.get("http://migoo.xyz").execute().body().contains("todos"));
    }

    @Test
    void testPost() throws HttpException {
        JSONObject body = new JSONObject();
        body.put("userName", "test");
        body.put("password", "123456");
        body.put("sign", "12345");
        Assertions.assertEquals(411, Request.post("http://migoo.xyz/api/login")
                .bodyJson(body.toString())
                .execute().jsonObject().getIntValue("status"));
    }

    @Test
    void testPut() throws HttpException {
        Assertions.assertEquals(200, Request.put("http://migoo.xyz/api/task/schedule/edit")
                .addHeader("token", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIxNTU4MTU4NTAyODA3IiwiZXhwIjoxNTY4NDMzMzQ2fQ.ZoxwmAnmb2zJB1Wne8bkPFpKWZKo1c-ggi6nIoT79NE")
                .bodyJson("{\"id\":\"1558159224394\",\"title\":\"哈哈\",\"locked\":0,\"isDelete\":0}")
                .execute().statusCode());
    }
}

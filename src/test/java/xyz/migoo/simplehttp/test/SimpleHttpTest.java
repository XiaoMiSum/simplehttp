package xyz.migoo.simplehttp.test;

import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import xyz.migoo.simplehttp.Client;
import xyz.migoo.simplehttp.HttpException;
import xyz.migoo.simplehttp.Request;

import java.io.IOException;

/**
 * @author xiaomi
 * @date 2019/9/13 11:32
 */
class SimpleHttpTest {

    @Test
    void testGet() throws IOException, HttpException {
        Assertions.assertTrue(Request.get("http://migoo.xyz").execute().text().contains("todos"));
    }

    @Test
    void testPost() throws IOException, HttpException {
        String body = "{\"userName\":\"test\",\"password\":\"123456\",\"sign\": \"123456\"}";
        Assertions.assertTrue(Request.post("http://migoo.xyz/api/login")
                .bodyJson(body)
                .execute().text().contains("411"));
    }

    @Test
    void testPut() throws IOException, HttpException {
        Assertions.assertEquals(200, Request.put("http://migoo.xyz/api/task/schedule/edit")
                .addHeader("token", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIxNTU4MTU4NTAyODA3IiwiZXhwIjoxNTY4NDMzMzQ2fQ.ZoxwmAnmb2zJB1Wne8bkPFpKWZKo1c-ggi6nIoT79NE")
                .bodyJson("{\"id\":\"1558159224394\",\"title\":\"哈哈\",\"locked\":0,\"isDelete\":0}")
                .execute().statusCode());
    }

    @Test()
    void testException() {
        Assertions.assertThrows(HttpException.class, () -> Request.put("http://127.0.0.1:8080").execute());
    }

    @Test
    void testDefaultClient() throws IOException, HttpException {
        Assertions.assertTrue(Client.newClient().execute(Request.get("http://migoo.xyz")).text().contains("todos"));
    }

    @Test
    void testNewClient() throws IOException, HttpException {
        Assertions.assertTrue(Client.newClient(HttpClientBuilder.create().build())
                .execute(Request.get("http://migoo.xyz")).text().contains("todos"));
    }
}

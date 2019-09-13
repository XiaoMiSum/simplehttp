package xyz.migoo.simplehttp;

/**
 * @author xiaomi
 * @date 2019/9/13 11:25
 */
public class HttpException extends Exception {
    public HttpException(String message){
        super(message);
    }

    public HttpException(String message, Throwable t){
        super(message, t);
    }
}

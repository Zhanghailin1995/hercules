package org.hercules.example.helloworld;

import java.io.Serializable;

/**
 * org.hercules.example.helloworld.HelloWorldRequest
 *
 * @author zhanghailin
 */
public class HelloWorldRequest implements Serializable {

    private static final long serialVersionUID = 9218253805003988802L;

    private String message = "hello world!";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

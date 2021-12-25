package org.hercules.example.helloworld;

import java.io.Serializable;

/**
 * org.hercules.example.helloworld.HelloWorldResponse
 *
 * @author zhanghailin
 */
public class HelloWorldResponse implements Serializable {

    private static final long serialVersionUID = -5623664785560971849L;

    private String message = "hello";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "HelloWorldResponse{" +
                "message='" + message + '\'' +
                '}';
    }
}

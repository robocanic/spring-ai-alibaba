package com.alibaba.cloud.ai.exception;

import com.alibaba.cloud.ai.common.ReturnCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RunFailedException extends RuntimeException{
    private int code;

    private String msg;

    public RunFailedException(){
        super(ReturnCode.RC500.getMsg());
        this.code = ReturnCode.RC500.getCode();
        this.msg = ReturnCode.RC500.getMsg();
    }

    public RunFailedException(String msg){
        super(msg);
        this.code = ReturnCode.RC500.getCode();
        this.msg = msg;
    }

    public RunFailedException(String msg, Throwable cause){
        super(msg, cause);
        this.code = ReturnCode.RC500.getCode();
        this.msg = msg;
    }

}

package com.zhubajie.framework.mock.enums;

/**
 * @author dreamyao
 * @version 1.0.0
 */
public enum MockEnum {

    NORMAL(1, "正常模式"),
    PRINT(2, "打印模式"),
    WRITE(3, "入库模式");

    private int code;
    private String text;

    MockEnum(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

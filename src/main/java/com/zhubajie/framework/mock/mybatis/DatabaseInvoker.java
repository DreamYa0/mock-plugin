package com.zhubajie.framework.mock.mybatis;

import com.zhubajie.framework.mock.Command;

/**
 * @author dreamyao
 * @version 1.0.0
 */
public class DatabaseInvoker {

    private Command command;

    public DatabaseInvoker(Command command) {
        this.command = command;
    }
}

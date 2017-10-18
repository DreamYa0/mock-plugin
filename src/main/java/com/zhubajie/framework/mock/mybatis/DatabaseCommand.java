package com.zhubajie.framework.mock.mybatis;

import com.zhubajie.framework.mock.Command;

/**
 * @author dreamyao
 * @version 1.0.0
 */
public class DatabaseCommand implements Command {

    private DatabaseReceiver receiver;

    public DatabaseCommand(DatabaseReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public <T> void execute(T t) {

    }

    @Override
    public <T> T execute2Result(T t) {
        return null;
    }

    @Override
    public <T> void unExecute(T t) {

    }
}

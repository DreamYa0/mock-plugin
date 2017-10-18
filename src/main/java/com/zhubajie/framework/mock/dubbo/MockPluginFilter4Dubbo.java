package com.zhubajie.framework.mock.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.zhubajie.framework.mock.data.MockContext;

/**
 * @author dreamyao
 * @version 1.0.0
 */
@Activate(group = Constants.CONSUMER)
public class MockPluginFilter4Dubbo implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String className = invocation.getInvoker().getInterface().getName();
        String methodName = invocation.getMethodName();
        MockContext context = MockContext.getContext();
        context.setTestClass(className);
        context.setTestMethod(methodName);
        context.setDbOrder(1);
        context.setRpcOrder(1);
        return null;
    }
}

package com.zhubajie.framework.mock.data;

/**
 * @author dreamyao
 * @version 1.0.0
 */
public class MockContext {

    /**
     * 测试方法
     */
    private String testMethod;

    /**
     * 测试类
     */
    private String testClass;

    /**
     * 外部调用顺序
     */
    private int dbOrder;

    /**
     * 外部调用顺序
     */
    private int rpcOrder;


    /**
     * 测试用例的顺序
     */
    private int caseIndex;

    private static final ThreadLocal<MockContext> LOCAL = ThreadLocal.withInitial(MockContext::new);

    private MockContext() {
    }

    public static MockContext getContext() {
        return LOCAL.get();
    }

    public String getTestMethod() {
        return testMethod;
    }

    public void removeContext() {
        LOCAL.remove();
    }

    public void setTestMethod(String testMethod) {
        this.testMethod = testMethod;
    }

    public int getRpcOrderAndIncrease() {
        return rpcOrder++;
    }

    public int getDbOrderAndIncrease() {
        return dbOrder++;
    }


    public int getDbOrder() {
        return dbOrder;
    }

    public void setDbOrder(int dbOrder) {
        this.dbOrder = dbOrder;
    }

    public int getRpcOrder() {
        return rpcOrder;
    }

    public void setRpcOrder(int rpcOrder) {
        this.rpcOrder = rpcOrder;
    }

    public String getTestClass() {
        return testClass;
    }

    public void setTestClass(String testClass) {
        this.testClass = testClass;
    }

    public int getCaseIndex() {
        return caseIndex;
    }

    public void setCaseIndex(int caseIndex) {
        this.caseIndex = caseIndex;
    }
}

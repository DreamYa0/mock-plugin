package com.zhubajie.framework.mock.mybatis;

import com.zbj.innovation.tool.centerconfig.CustomPropertyConfigurer;
import com.zhubajie.configcenter.client.spring.ConfigCenterConfigurer;
import com.zhubajie.framework.mock.enums.MockEnum;
import com.zhubajie.framework.mock.enums.TestMode;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.defaults.DefaultSqlSession.StrictMap;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.*;

import static com.zbj.innovation.tool.util.SpringContextUtil.getBean;
import static java.util.stream.Collectors.toList;


/**
 * Sql语句打印以及执行时间记录拦截器
 * @author dreamyao
 * @version 1.0.0
 */

@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
        @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class})
})
public class SqlInterceptor4Mybatis implements Interceptor {

    private Properties properties;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        ConfigCenterConfigurer configurer;
        try {
            configurer = getBean(ConfigCenterConfigurer.class);
        } catch (Exception e) {
            configurer = getBean(CustomPropertyConfigurer.class);
        }
        if (configurer != null && (TestMode.test.name().equals(configurer.getProfile()) || TestMode.maintest.name().equals(configurer.getProfile()))) {
            Object target = invocation.getTarget();
            long startTime = System.currentTimeMillis();
            StatementHandler statementHandler = (StatementHandler) target;
            try {
                return invocation.proceed();
            } finally {
                long endTime = System.currentTimeMillis();
                long sqlCost = endTime - startTime;
                BoundSql boundSql = statementHandler.getBoundSql();
                String sql = boundSql.getSql();
                Object parameterObject = boundSql.getParameterObject();
                List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
                // 格式化Sql语句，去除换行符，替换参数
                sql = formatSql(sql, parameterObject, parameterMappingList);
                String configMode;
                if (properties.containsKey("configMode")) {
                    configMode = properties.getProperty("configMode");
                } else {
                    configMode = MockEnum.NORMAL.name();
                }

                if (MockEnum.PRINT.name().equals(configMode)) {
                    System.out.println("SQL：[" + sql + "]执行耗时[" + sqlCost + "ms]");
                } else if (MockEnum.WRITE.name().equals(configMode)) {

                }

            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    private String formatSql(String sql, Object parameterObject, List<ParameterMapping> parameterMappingList) {
        // 输入sql字符串空判断
        if (sql == null || sql.length() == 0) {
            return "";
        }
        // 美化sql
        sql = beautifySql(sql);
        // 不传参数的场景，直接把Sql美化一下返回出去
        if (parameterObject == null || parameterMappingList == null || parameterMappingList.size() == 0) {
            return sql;
        }
        // 定义一个没有替换过占位符的sql，用于出异常时返回
        String sqlWithoutReplacePlaceholder = sql;
        try {
            Class<?> parameterObjectClass = parameterObject.getClass();
            // 如果参数是StrictMap且Value类型为Collection，获取key="list"的属性，这里主要是为了处理<foreach>循环时传入List这种参数的占位符替换
            // 例如select * from xxx where id in <foreach collection="list">...</foreach>
            if (isStrictMap(parameterObjectClass)) {
                StrictMap<Collection<?>> strictMap = (StrictMap<Collection<?>>) parameterObject;
                if (isList(strictMap.get("list").getClass())) {
                    sql = handleListParameter(sql, strictMap.get("list"), parameterMappingList, parameterObject);
                }
            } else if (isMap(parameterObjectClass)) {
                // 如果参数是Map则直接强转，通过map.get(key)方法获取真正的属性值
                // 这里主要是为了处理<insert>、<delete>、<update>、<select>时传入parameterType为map的场景
                Map<?, ?> paramMap = (Map<?, ?>) parameterObject;
                sql = handleMapParameter(sql, paramMap, parameterMappingList, parameterObject);
            } else {
                // 通用场景，比如传的是一个自定义的对象或者八种基本数据类型之一或者String
                sql = handleCommonParameter(sql, parameterMappingList, parameterObjectClass, parameterObject);
            }
        } catch (Exception e) {
            // 占位符替换过程中出现异常，则返回没有替换过占位符但是格式美化过的sql，这样至少保证sql语句比BoundSql中的sql更好看
            return sqlWithoutReplacePlaceholder;
        }
        return sql;
    }

    private String handleListParameter(String sql, Collection<?> col, List<ParameterMapping> parameterMappingList, Object parameterObject) {
        if (col != null && col.size() != 0) {
            for (Object obj : col) {
                String value = null;
                Class<?> objClass = obj.getClass();
                // 只处理基本数据类型、基本数据类型的包装类
                if (isPrimitiveOrPrimitiveWrapper(objClass)) {
                    value = obj.toString();
                } else if (objClass.isAssignableFrom(String.class)) {
                    // String 类型
                    value = "\"" + obj.toString() + "\"";
                } else {
                    // Collection<?> ? 为自定义对象
                    List<String> list = handleCommonParameter(parameterMappingList, parameterObject);
                    for (String val : list) {
                        if (val != null) {
                            sql = sql.replaceFirst("\\?", val);
                        }
                    }
                }
                if (value != null) {
                    sql = sql.replaceFirst("\\?", value);
                }
            }
        }
        return sql;
    }

    private String handleMapParameter(String sql, Map<?, ?> paramMap, List<ParameterMapping> parameterMappingList, Object parameterObject) {
        for (ParameterMapping parameterMapping : parameterMappingList) {
            Object propertyName = parameterMapping.getProperty();
            Object propertyValue = paramMap.get(propertyName);
            if (propertyValue != null) {
                // String 类型
                if (propertyValue.getClass().isAssignableFrom(String.class)) {
                    propertyValue = "\"" + propertyValue + "\"";
                }
                sql = sql.replaceFirst("\\?", propertyValue.toString());
                // Map<String, ?> value为自定义对象
                if (!isBasicType(propertyValue.getClass())) {
                    List<String> list = handleCommonParameter(parameterMappingList, parameterObject);
                    for (String val : list) {
                        if (val != null) {
                            sql = sql.replaceFirst("\\?", val);
                        }
                    }
                }
            }
        }
        return sql;
    }

    private String handleCommonParameter(String sql, List<ParameterMapping> parameterMappingList, Class<?> parameterObjectClass,
            Object parameterObject) throws Exception {
        for (ParameterMapping parameterMapping : parameterMappingList) {
            String propertyValue;
            // 基本数据类型或者基本数据类型的包装类，直接toString即可获取其真正的参数值，其余直接取parameterMapping中的property属性即可
            if (isPrimitiveOrPrimitiveWrapper(parameterObjectClass)) {
                propertyValue = parameterObject.toString();
            } else {
                String propertyName = parameterMapping.getProperty();
                Field field = parameterObjectClass.getDeclaredField(propertyName);
                // 要获取Field中的属性值，这里必须将私有属性的accessible设置为true
                field.setAccessible(true);
                propertyValue = String.valueOf(field.get(parameterObject));
                if (parameterMapping.getJavaType().isAssignableFrom(String.class)) {
                    propertyValue = "\"" + propertyValue + "\"";
                }
            }
            sql = sql.replaceFirst("\\?", propertyValue);
        }
        return sql;
    }

    private List<String> handleCommonParameter(List<ParameterMapping> parameterMappingList, Object parameterObject) {
        return parameterMappingList.stream().map(parameterMapping -> {
            String propertyValue = null;
            try {
                if (isPrimitiveOrPrimitiveWrapper(parameterObject.getClass())) {
                    propertyValue = parameterObject.toString();
                } else {
                    String propertyName = parameterMapping.getProperty();
                    Field field = parameterObject.getClass().getDeclaredField(propertyName);
                    // 要获取Field中的属性值
                    field.setAccessible(true);
                    propertyValue = String.valueOf(field.get(parameterObject));
                    if (parameterMapping.getJavaType().isAssignableFrom(String.class)) {
                        propertyValue = "\"" + propertyValue + "\"";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return propertyValue;
        }).collect(toList());
    }

    private String beautifySql(String sql) {
        sql = sql.replaceAll("[\\s\n ]+", " ");
        return sql;
    }

    private boolean isPrimitiveOrPrimitiveWrapper(Class<?> parameterObjectClass) {
        return parameterObjectClass.isPrimitive() ||
                (parameterObjectClass.isAssignableFrom(Byte.class) || parameterObjectClass.isAssignableFrom(Short.class) ||
                        parameterObjectClass.isAssignableFrom(Integer.class) || parameterObjectClass.isAssignableFrom(Long.class) ||
                        parameterObjectClass.isAssignableFrom(Double.class) || parameterObjectClass.isAssignableFrom(Float.class) ||
                        parameterObjectClass.isAssignableFrom(Character.class) || parameterObjectClass.isAssignableFrom(Boolean.class));
    }

    private boolean isStrictMap(Class<?> parameterObjectClass) {
        return parameterObjectClass.isAssignableFrom(StrictMap.class);
    }

    private boolean isList(Class<?> clazz) {
        Class<?>[] interfaceClasses = clazz.getInterfaces();
        for (Class<?> interfaceClass : interfaceClasses) {
            if (interfaceClass.isAssignableFrom(List.class)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMap(Class<?> parameterObjectClass) {
        Class<?>[] interfaceClasses = parameterObjectClass.getInterfaces();
        for (Class<?> interfaceClass : interfaceClasses) {
            if (interfaceClass.isAssignableFrom(Map.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBasicType(Class clz) {
        try {
            return clz.isPrimitive() || Arrays.asList("String", "Date").contains(clz.getSimpleName()) || ((Class) clz.getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }
}

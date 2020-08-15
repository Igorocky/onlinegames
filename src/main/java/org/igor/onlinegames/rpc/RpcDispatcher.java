package org.igor.onlinegames.rpc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igor.onlinegames.common.OnlinegamesUtils;
import org.igor.onlinegames.exceptions.OnlinegamesException;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RpcDispatcher {
    private JavaType listOfUuidsJavaType;
    private JavaType listOfObjectsJavaType;
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    @RpcMethodsCollection
    private List<Object> rpcMethodsCollections;

    private Map<String, Pair<Object, Method>> methodMap = new HashMap<>();

    @PostConstruct
    public void init() {
        listOfUuidsJavaType = objectMapper.getTypeFactory().constructType(new TypeReference<List<UUID>>() {});
        listOfObjectsJavaType = objectMapper.getTypeFactory().constructType(new TypeReference<List<Object>>() {});
        for (Object rpcMethodsCollection : rpcMethodsCollections) {
            methodMap.putAll(createMethodMap(rpcMethodsCollection));
        }
    }

    public Object dispatchRpcCall(String methodName, JsonNode passedParams, RpcAutowiredParamsLookup paramsLookup) throws IOException, InvocationTargetException, IllegalAccessException {
        return dispatchRpcCall(methodName, passedParams, methodMap, paramsLookup);
    }

    public Object dispatchRpcCall(String methodName, JsonNode passedParams, Map<String, Pair<Object, Method>> methodMap,
                                  RpcAutowiredParamsLookup paramsLookup) throws IOException, InvocationTargetException, IllegalAccessException {
        Pair<Object, Method> objectMethodPair = methodMap.get(methodName);
        if (objectMethodPair == null) {
            throw new OnlinegamesException("Could not find RPC method with name " + methodName);
        }
        Method method = objectMethodPair.getRight();
        Parameter[] methodParameters = method.getParameters();
        validatePassedParams(methodName, methodParameters, passedParams);
        return method.invoke(
                objectMethodPair.getLeft(),
                prepareArguments(methodName, methodParameters, passedParams, paramsLookup)
        );
    }

    public Map<String, Pair<Object, Method>> createMethodMap(Object rpcMethodsCollection) {
        Map<String, Pair<Object, Method>> methodMap = new HashMap<>();
        for (Method method : AopUtils.getTargetClass(rpcMethodsCollection).getMethods()) {
            if (method.getAnnotation(RpcMethod.class) != null) {
                String methodName = method.getName();
                if (methodMap.containsKey(methodName)) {
                    throw new OnlinegamesException("methodMap.containsKey(\"" + methodName + "\")");
                }
                methodMap.put(methodName, Pair.of(rpcMethodsCollection, method));
            }
        }
        return methodMap;
    }

    private void validatePassedParams(String methodName, Parameter[] declaredParams, JsonNode passedParams) {
        if (passedParams != null) {
            Set<String> allParamNames = OnlinegamesUtils.mapToSet(declaredParams, Parameter::getName);
            passedParams.fieldNames().forEachRemaining(passedParamName -> {
                if (!allParamNames.contains(passedParamName)) {
                    throw new OnlinegamesException(
                            "Unknown parameter name '" + passedParamName + "' in " + methodName + ", expected are [" + StringUtils.join(allParamNames, ",") + "]"
                    );
                }
            });
        }
    }

    private Object[] prepareArguments(String methodName,
                                      Parameter[] declaredParams,
                                      JsonNode passedParams,
                                      RpcAutowiredParamsLookup paramsLookup) throws IOException {
        Object[] arguments = new Object[declaredParams.length];
        for (int i = 0; i < declaredParams.length; i++) {
            Parameter declaredParam = declaredParams[i];
            RpcIgnore ignoreParam = declaredParam.getAnnotation(RpcIgnore.class);
            if (ignoreParam != null) {
                continue;
            }
            Class<?> paramType = declaredParam.getType();
            String paramName = declaredParam.getName();
            JsonNode passedParam = passedParams != null ? passedParams.get(paramName) : null;
            if (passedParam != null) {
                arguments[i] = parseParam(methodName, declaredParam, passedParam);
            } else {
                Default defaultAnnotation = declaredParam.getAnnotation(Default.class);
                if (defaultAnnotation != null) {
                    arguments[i] = objectMapper.readValue(defaultAnnotation.value(), (Class) declaredParam.getType());
                } else {
                    if (paramsLookup != null) {
                        Object lookedUpParam = paramsLookup.getByClass(paramType);
                        if (lookedUpParam != null) {
                            arguments[i] = lookedUpParam;
                        } else {
                            throw new OnlinegamesException("Rpc call error: required parameter '" + paramName
                                    + "' is not specified for method " + methodName + ".");
                        }
                    }
                }
            }
        }
        return arguments;
    }

    private Object parseParam(String methodName, Parameter declaredParam, JsonNode passedParam) throws IOException {
        if (passedParam.isNull()) {
            return null;
        }
        Class<?> declaredParamType = declaredParam.getType();
        if (declaredParamType.equals(List.class)) {
            Class typeArgument = getTypeArgument(declaredParam);
            if (typeArgument.equals(UUID.class)) {
                return readValue(passedParam, listOfUuidsJavaType);
            } else {
                return readValue(passedParam, listOfObjectsJavaType);
            }
        } else {
            return objectMapper.treeToValue(passedParam, declaredParamType);
        }
    }

    private Class getTypeArgument(Parameter declaredParam) {
        ParameterizedType parameterizedType = (ParameterizedType) declaredParam.getParameterizedType();
        return (Class) parameterizedType.getActualTypeArguments()[0];
    }

    private Object readValue(JsonNode passedParam, JavaType javaType) throws IOException {
        return objectMapper.readValue(objectMapper.treeAsTokens(passedParam), javaType);
    }
}

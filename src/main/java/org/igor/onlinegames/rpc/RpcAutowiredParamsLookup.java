package org.igor.onlinegames.rpc;

public interface RpcAutowiredParamsLookup {
    <T> T getByClass(Class<T> clazz);
}

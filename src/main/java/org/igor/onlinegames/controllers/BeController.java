package org.igor.onlinegames.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.igor.onlinegames.rpc.RpcDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Controller
@RequestMapping("/be")
@ResponseBody
public class BeController {
    @Autowired
    private RpcDispatcher rpcDispatcher;

    @PostMapping("/rpc/{methodName}")
    public Object rpcEntry(@PathVariable String methodName, @RequestBody JsonNode passedParams) throws IOException, InvocationTargetException, IllegalAccessException {
        return rpcDispatcher.dispatchRpcCall(methodName, passedParams);
    }
}

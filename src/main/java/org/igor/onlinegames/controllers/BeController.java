package org.igor.onlinegames.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.igor.onlinegames.model.OnlineGamesUser;
import org.igor.onlinegames.rpc.RpcDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/be")
@ResponseBody
public class BeController {
    private static final Logger FE_ERROR_LOGGER = LoggerFactory.getLogger("fe-error-logger");
    @Autowired
    private RpcDispatcher rpcDispatcher;
    @Autowired
    private OnlineGamesUser user;
    @Autowired
    private ObjectMapper mapper;

    @PostMapping("/rpc/{methodName}")
    public Object rpcEntry(@PathVariable String methodName, @RequestBody JsonNode passedParams) throws IOException, InvocationTargetException, IllegalAccessException {
        return rpcDispatcher.dispatchRpcCall(methodName, passedParams, null);
    }

    @PostMapping("/log-fe-error")
    public void logFeError(@RequestHeader MultiValueMap<String, String> headers, @RequestBody Map<String,Object> errorInfo) throws JsonProcessingException {
        Map<String, String> allHeaders = new HashMap<>();
        headers.entrySet().forEach(entry -> allHeaders.put(entry.getKey(), StringUtils.join(entry.getValue(),":::")));

        HashMap<String, Object> augmentedErrorInfo = new HashMap<>(errorInfo);
        augmentedErrorInfo.put("userId", user.getUserData().getUserId());
        augmentedErrorInfo.put("headers", allHeaders);

        FE_ERROR_LOGGER.error(mapper.writeValueAsString(augmentedErrorInfo));
    }
}

'use strict'

const CONTEXT_PATH = "/fe"

const ENTER_KEY_CODE = 13
const ESC_KEY_CODE = 27
const LEFT_KEY_CODE = 37
const UP_KEY_CODE = 38
const RIGHT_KEY_CODE = 39
const DOWN_KEY_CODE = 40

const PATH = {
    stateWebSocketUrl: "/be/websocket/state",
    gameSelector: CONTEXT_PATH + "/newgame",
}

function hasValue(variable) {
    return variable !== undefined && variable !== null
}

function doPost({url, data, onSuccess}) {
    $.ajax({
        type: "POST",
        url: url,
        data: JSON.stringify(data),
        contentType: "application/json; charset=utf-8",
        success: onSuccess
    });
}

function doGet(url, onSuccess) {
    $.ajax({
        type: "GET",
        url: url,
        success: onSuccess
    });
}

function doRpcCall(methodName, params, onSuccess) {
    doPost("/be/rpc/" + methodName, params, onSuccess)
}

function getByPath(obj, path, defaultValue) {
    if (_.size(path) == 0 && obj) {
        return obj
    } else if(!obj) {
        return defaultValue
    } else {
        return getByPath(obj[_.first(path)], _.tail(path), defaultValue)
    }
}

function disableScrollOnMouseDown(event) {
    if(event.button==1){
        event.preventDefault()
    }
}

function soundUrl(fileRelPath) {
    return "/assets/sound/" + APP_VERSION + "/" + fileRelPath
}
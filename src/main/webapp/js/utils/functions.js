'use strict'

const ENTER_KEY_CODE = 13
const ESC_KEY_CODE = 27
const LEFT_KEY_CODE = 37
const UP_KEY_CODE = 38
const RIGHT_KEY_CODE = 39
const DOWN_KEY_CODE = 40

const XO_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX = 'XoGamePlayerView.'
const WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX = 'WordsGamePlayerView.'

function hasValue(variable) {
    return variable !== undefined && variable !== null
}

function ints(start, end) {
    let i = start
    const res = [];
    while (i <= end) {
        res.push(i)
        i++
    }
    return res
}

function saveToLocalStorage(localStorageKey, value) {
    window.localStorage.setItem(localStorageKey, JSON.stringify(value))
}

function readFromLocalStorage(localStorageKey, defaultValue) {
    const item = window.localStorage.getItem(localStorageKey)
    return hasValue(item) ? JSON.parse(item) : defaultValue
}

function doPost(url, data, onSuccess) {
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

function audioUrl(fileRelPath) {
    return "/assets/sound/" + APP_VERSION + "/" + fileRelPath
}

const AUDIO_FILES_CACHE = {}

function playAudio(audioFileUrl) {
    let audioArr = AUDIO_FILES_CACHE[audioFileUrl]
    if (!audioArr) {
        audioArr = [new Audio(audioFileUrl)]
        AUDIO_FILES_CACHE[audioFileUrl] = audioArr
    }
    let audio = audioArr.find(a => a.paused)
    if (!audio) {
        audio = new Audio(audioFileUrl)
        audioArr.push(audio)
    }
    audio.play()
}
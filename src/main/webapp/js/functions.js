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
    node: CONTEXT_PATH + "/node",
    nodeWithId: CONTEXT_PATH + "/node/:id",
    createNodeWithIdPath: id => CONTEXT_PATH + "/node/" + id,
    puzzlesToRepeat: CONTEXT_PATH + "/puzzlesToRepeat",
    puzzlesToRepeatWithTab: CONTEXT_PATH + "/puzzlesToRepeat/:tab",
    createPuzzlesToRepeatPath: tab => CONTEXT_PATH + "/puzzlesToRepeat/" + tab,
    chessboard: CONTEXT_PATH + "/chessboard",
    chessboardWithPractice: CONTEXT_PATH + "/chessboard/practice/:puzzleId",
    createChessboardWithPractice: puzzleId => CONTEXT_PATH + "/chessboard/practice/" + puzzleId,
    chessboardWithFen: CONTEXT_PATH + "/chessboard/fen/:fen",
    createChessboardWithFen: fen => CONTEXT_PATH + "/chessboard/fen/" + fen,
    chessboardComponentM: CONTEXT_PATH + "/chessboardm",
    ChessManagerAudio: CONTEXT_PATH + "/chess-manager-audio",
    createChessboardComponentM: ({puzzleId,fen}) => CONTEXT_PATH + "/chessboardm?"
        + (puzzleId?("puzzleId="+puzzleId):"")
        + (fen?("fen="+urlEncodeFen(fen)):""),
    admin: CONTEXT_PATH + "/admin",
    beStates: CONTEXT_PATH + "/bestates",
    logout: "/logout",
}

function hasValue(variable) {
    return variable !== undefined && variable !== null
}

function emptyStrIfNull(str) {
    return (str===null || str==='undefined')?"":str
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

function doPatch(url, data, onSuccess) {
    $.ajax({
        type: "PATCH",
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

function doPostMocked({url, data, onSuccess, response}) {
    console.log("POST " + url + "\n" + JSON.stringify(data));
    onSuccess(response)
}

function doGetMocked({url, onSuccess, response}) {
    console.log("GET " + url);
    console.log("response: " + JSON.stringify(response));
    onSuccess(response)
}

function extractFileFromEvent(event) {
    // use event.originalEvent.clipboard for newer chrome versions
    var items = (event.clipboardData  || event.originalEvent.clipboardData).items;
    // console.log(JSON.stringify(items)); // will give you the mime types
    // find pasted image among pasted items
    var blob = null;
    for (var i = 0; i < items.length; i++) {
        if (items[i].type.indexOf("image") === 0) {
            blob = items[i].getAsFile();
        }
    }
    return blob;
}

function uploadImage({file, parentId, isNodeIcon, onSuccess}) {
    uploadFile({
        url: "/be/uploadImage",
        file,
        params:{
            parentId:parentId?parentId:null,
            isNodeIcon:isNodeIcon?true:false
        },
        onSuccess
    })
}

function uploadImportFile({file, parentId, onSuccess}) {
    uploadFile({url: "/be/importFromFile/" + parentId, file, onSuccess})
}

function exportToFile({nodeId,onSuccess}) {
    doPost({url:"/be/exportToFile/" + nodeId, data:{}, onSuccess: onSuccess})
}

function uploadFile({url, file, params, onSuccess}) {
    let fd = new FormData();
    if (file) {
        fd.append("file", file);
        if (params) {
            _.each(_.pairs(params), ([val, key]) => fd.append(val, key))
        }
        $.ajax({
            type: "POST",
            url: url,
            data: fd,
            contentType: false,
            cache: false,
            processData: false,
            success: function (data) {
                onSuccess(data);
            }
        });
    }
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

function getTagSingleValue(node, tagId, defaultValue) {
    const tag = _.find(node[NODE.tags], tag => tag[TAG.tagId] == tagId)
    if (tag) {
        return tag[TAG.value]
    } else {
        return defaultValue
    }
}

function flatMap(list, func) {
    const res = []
    _.each(list, elem=>res.push(...func(elem)))
    return res
}

function disableScrollOnMouseDown(event) {
    if(event.button==1){
        event.preventDefault()
    }
}

class RandomElemSelector {
    constructor({elems}) {
        this.origElems = elems
        this.reset()
    }

    getCurrentElem() {
        return this.state.currentElem
    }

    getIterationNumber() {
        return this.state.iterationNumber
    }

    getRemainingElements() {
        return _.size(this.state.elemsToAsk)
    }

    reset() {
        this.state = {elemsToAsk: [], iterationNumber:0}
        this.loadNextElem()
    }

    loadNextElem() {
        let elemsToAsk = this.state.elemsToAsk
        if (_.size(elemsToAsk)==0) {
            elemsToAsk = [...this.origElems]
            this.state.iterationNumber += 1
        }
        elemsToAsk = _.shuffle(elemsToAsk)
        this.state.currentElem = _.first(elemsToAsk)
        this.state.elemsToAsk = _.rest(elemsToAsk)
    }
}

function urlEncodeFen(fen) {
    return fen.replace(/ /g,"_").replace(/\//g,"!")
}

function firstDefined(attrName, newState, prevState, defVal) {
    const newValue = newState ? newState[attrName] : undefined
    if (newValue !== undefined) {
        return newValue
    }
    const oldValue = prevState ? prevState[attrName] : undefined
    if (oldValue !== undefined) {
        return oldValue
    }
    return defVal
}

function saveSettingsToLocalStorage({settings, attrsToSave, localStorageKey}) {
    const settingsToSave = attrsToSave.reduce((acc,attr) => ({...acc, [attr]:settings[attr]}), {})
    window.localStorage.setItem(localStorageKey, JSON.stringify(settingsToSave))
}

function readSettingsFromLocalStorage({localStorageKey, attrsToRead}) {
    const settingsStr = window.localStorage.getItem(localStorageKey)
    if (settingsStr) {
        const settingsFromLocalStorage = JSON.parse(settingsStr)
        return attrsToRead.reduce((acc,attr) => ({...acc, [attr]:settingsFromLocalStorage[attr]}), {})
    } else {
        return {}
    }
}

const BEEP_TYPE_SINE = "sine"
const BEEP_TYPE_SQUARE = "square"
const BEEP_TYPE_SAWTOOTH = "sawtooth"
const BEEP_TYPE_TRIANGLE = "triangle"
let AUDIO_CTX = null
function beep({durationMillis, frequencyHz, volume, type, callback}) {
    if (!AUDIO_CTX) {
        AUDIO_CTX = new (window.AudioContext || window.webkitAudioContext || window.audioContext)
    }
    const oscillator = AUDIO_CTX.createOscillator()
    const gainNode = AUDIO_CTX.createGain()

    oscillator.connect(gainNode)
    gainNode.connect(AUDIO_CTX.destination)

    oscillator.frequency.value = frequencyHz?frequencyHz:440
    oscillator.type = type?type:BEEP_TYPE_SINE
    oscillator.onended = callback?callback:undefined
    gainNode.gain.value = volume?volume:1

    oscillator.start()
    oscillator.stop(AUDIO_CTX.currentTime + ((durationMillis || 500) / 1000));
}

function set(obj, attrName, newValue) {
    return {...obj, [attrName]:newValue}
}

function soundUrl(fileRelPath) {
    return "/assets/sound/" + APP_VERSION + "/" + fileRelPath
}
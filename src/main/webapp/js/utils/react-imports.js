'use strict'

const re = React.createElement
const useState = React.useState
const useEffect = React.useEffect
const useMemo = React.useMemo
const useRef = React.useRef
const Fragment = React.Fragment

const {
    colors,
    createMuiTheme,
    CssBaseline,
    MuiThemeProvider,
    ListItemSecondaryAction,
    Grid,
    Popover,
    Popper,
    ClickAwayListener,
    CircularProgress,
    Portal,
    withStyles,
} = window['MaterialUI']

const {
    BrowserRouter,
    Redirect,
    Route,
    Switch,
    useLocation
} = window["ReactRouterDOM"]

function reFactory(elemType) {
    return (props, ...children) => re(elemType, props, ...children)
}

const MaterialUI = window['MaterialUI']
const MuiColors = MaterialUI.colors

const DIRECTION = {row: "row", column: "column",}
const JUSTIFY = {flexStart: "flex-start", center: "center", flexEnd: "flex-end", spaceBetween: "space-between", spaceAround: "space-around",}
const ALIGN_ITEMS = {flexStart: "flex-start", center: "center", flexEnd: "flex-end", stretch: "stretch", spaceAround: "baseline",}

function gridFactory(direction, justify, alignItems) {
    return (props, childProps, ...children) => re(MaterialUI.Grid, {container:true, direction:direction,
            justify:justify, alignItems:alignItems, ...props},
        React.Children.map(children, child => {
            return re(MaterialUI.Grid, {item:true, ...childProps}, child)
        })
    )
}

const RE = {
    div: reFactory('div'),
    svg: ({width, height, boundaries}, ...children) => re('svg',
        {
            width,
            height,
            viewBox: `${boundaries.minX} ${boundaries.minY} ${boundaries.maxX - boundaries.minX} ${boundaries.maxY - boundaries.minY}`
        },
        children
    ),
    img: reFactory('img'),
    span: reFactory('span'),
    table: reFactory('table'),
    tbody: reFactory('tbody'),
    th: reFactory('th'),
    tr: reFactory('tr'),
    td: reFactory('td'),
    AppBar: reFactory(MaterialUI.AppBar),
    Button: reFactory(MaterialUI.Button),
    Breadcrumbs: reFactory(MaterialUI.Breadcrumbs),
    ButtonGroup: reFactory(MaterialUI.ButtonGroup),
    CircularProgress: reFactory(MaterialUI.CircularProgress),
    Checkbox: reFactory(MaterialUI.Checkbox),
    Drawer: reFactory(MaterialUI.Drawer),
    Dialog: reFactory(MaterialUI.Dialog),
    DialogActions: reFactory(MaterialUI.DialogActions),
    DialogContent: reFactory(MaterialUI.DialogContent),
    DialogContentText: reFactory(MaterialUI.DialogContentText),
    DialogTitle: reFactory(MaterialUI.DialogTitle),
    Fragment: reFactory(React.Fragment),
    FormControl: reFactory(MaterialUI.FormControl),
    FormControlLabel: reFactory(MaterialUI.FormControlLabel),
    Grid: reFactory(MaterialUI.Grid),
    Paper: reFactory(MaterialUI.Paper),
    RadioGroup: reFactory(MaterialUI.RadioGroup),
    Radio: reFactory(MaterialUI.Radio),
    Switch: reFactory(MaterialUI.Switch),
    Slider: reFactory(MaterialUI.Slider),
    Select: reFactory(MaterialUI.Select),
    Typography: reFactory(MaterialUI.Typography),
    Toolbar: reFactory(MaterialUI.Toolbar),
    TextField: reFactory(MaterialUI.TextField),
    Table: reFactory(MaterialUI.Table),
    TableHead: reFactory(MaterialUI.TableHead),
    TableBody: reFactory(MaterialUI.TableBody),
    TableRow: reFactory(MaterialUI.TableRow),
    TableCell: reFactory(MaterialUI.TableCell),
    Tabs: reFactory(MaterialUI.Tabs),
    Tab: reFactory(MaterialUI.Tab),
    IconButton: reFactory(MaterialUI.IconButton),
    Icon: reFactory(MaterialUI.Icon),
    InputBase: reFactory(MaterialUI.InputBase),
    InputLabel: reFactory(MaterialUI.InputLabel),
    If: (condition, ...elems) => condition?re(Fragment,{},...elems):re(Fragment,{}),
    IfNot: (condition, ...elems) => !condition?re(Fragment,{},...elems):re(Fragment,{}),
    List: reFactory(MaterialUI.List),
    ListItem: reFactory(MaterialUI.ListItem),
    ListItemText: reFactory(MaterialUI.ListItemText),
    ListItemIcon: reFactory(MaterialUI.ListItemIcon),
    Link: reFactory(MaterialUI.Link),
    LinearProgress: reFactory(MaterialUI.LinearProgress),
    MenuList: reFactory(MaterialUI.MenuList),
    MenuItem: reFactory(MaterialUI.MenuItem),
    Container: {
        row: {
            left: {
                top: gridFactory(DIRECTION.row, JUSTIFY.flexStart, ALIGN_ITEMS.flexStart),
                center: gridFactory(DIRECTION.row, JUSTIFY.flexStart, ALIGN_ITEMS.center),
            },
            center: {
                top: gridFactory(DIRECTION.row, JUSTIFY.center, ALIGN_ITEMS.flexStart),
                center: gridFactory(DIRECTION.row, JUSTIFY.center, ALIGN_ITEMS.center),
            },
            right: {
                top: gridFactory(DIRECTION.row, JUSTIFY.flexEnd, ALIGN_ITEMS.flexStart),
                center: gridFactory(DIRECTION.row, JUSTIFY.flexEnd, ALIGN_ITEMS.center),
                bottom: gridFactory(DIRECTION.row, JUSTIFY.flexEnd, ALIGN_ITEMS.flexEnd),
            },
            spaceBetween: {
                top: gridFactory(DIRECTION.row, JUSTIFY.spaceBetween, ALIGN_ITEMS.flexStart),
                center: gridFactory(DIRECTION.row, JUSTIFY.spaceBetween, ALIGN_ITEMS.center),
            },
        },
        col: {
            top: {
                left: gridFactory(DIRECTION.column, JUSTIFY.flexStart, ALIGN_ITEMS.flexStart),
                center: gridFactory(DIRECTION.column, JUSTIFY.flexStart, ALIGN_ITEMS.center),
                right: gridFactory(DIRECTION.column, JUSTIFY.flexStart, ALIGN_ITEMS.flexEnd),
            },
        },
    },
}

const SVG = {
    line: reFactory('line'),
    rect: reFactory('rect'),
    circle: reFactory('circle'),
    image: reFactory('image'),
    path: reFactory('path'),
    polygon: reFactory('polygon'),
    g: reFactory('g'),
}

function useQuery() {
    return new URLSearchParams(useLocation().search);
}

const WEB_SOCKET_STATE_CONNECTING = 0
const WEB_SOCKET_STATE_OPEN = 1
const WEB_SOCKET_STATE_CLOSING = 2
const WEB_SOCKET_STATE_CLOSED = 3
const BIND_TO_BE_STATE_METHOD_NAME = "-bindToState";

function useBackend({stateType, stateId, bindParams, onBackendStateCreated, onMessageFromBackend}) {
    const [beStateId, setBeStateId] = useState(stateId)
    const [callBeStateCreated, setCallBeStateCreated] = useState(false)
    const webSocket = useRef(null)

    const backend = {isReady: beStateId != null, send: sendMessageToBeState, call: callMethodOnBeState}

    useEffect(() => {
        return () => {
            if (webSocket.current) {
                webSocket.current.close()
            }
        }
    }, [])

    useEffect(() => {
        if (!beStateId) {
            doRpcCall("createNewBackendState", {stateType:stateType}, newStateId => {
                setBeStateId(newStateId)
                setCallBeStateCreated(true)
            })
        } else if (!webSocket.current) {
            sendMessageToBeState(BIND_TO_BE_STATE_METHOD_NAME, {stateId: beStateId, bindParams})
        } else if (callBeStateCreated && onBackendStateCreated) {
            setCallBeStateCreated(false)
            onBackendStateCreated(backend)
        }
    }, [beStateId, callBeStateCreated])

    function callMethodOnBeState(methodName, params, callback) {
        doRpcCall("invokeMethodOnBackendState", {stateId:beStateId, methodName, params}, callback)
    }

    function sendMessageToBeState(methodName, params) {
        if (!webSocket.current
            || webSocket.current.readyState == WEB_SOCKET_STATE_CLOSED
            || webSocket.current.readyState == WEB_SOCKET_STATE_CLOSING) {
            webSocket.current = new WebSocket("ws://" + location.host + "/be/websocket/state")
            webSocket.current.onmessage = event => onMessageFromBackend(JSON.parse(event.data))
            webSocket.current.onopen = () => {
                callBackendStateMethodInner(webSocket.current, BIND_TO_BE_STATE_METHOD_NAME, {stateId: beStateId, bindParams})
                callBackendStateMethodInner(webSocket.current, methodName, params)
            }
        } else if (webSocket.current.readyState == WEB_SOCKET_STATE_CONNECTING) {
            window.setTimeout(() => {
                sendMessageToBeState(methodName, params)
            }, 300)
        } else {
            callBackendStateMethodInner(webSocket.current, methodName, params)
        }
    }
    
    function callBackendStateMethodInner(webSocket, methodName, params) {
        webSocket.send(JSON.stringify({methodName, params}))
    }

    return backend
}

function usePageTitle({pageTitleProvider, listenFor}) {
    const [prevTitle, setPrevTitle] = useState(null)

    useEffect(() => {
        let prevTitleVar
        const newTitle = pageTitleProvider()
        if (hasValue(newTitle)) {
            if (prevTitle == null) {
                prevTitleVar = document.title
                setPrevTitle(prevTitleVar)
            }
            document.title = newTitle
        }
        if (prevTitleVar != null) {
            return () => {document.title = prevTitleVar}
        }
        if (prevTitle != null) {
            return () => {document.title = prevTitle}
        }
    }, listenFor)
}

function usePrevious(value) {
    const ref = useRef()
    useEffect(() => {
        ref.current = value
    });
    return ref.current
}

function useConfirmActionDialog() {
    const [confirmActionDialogData, setConfirmActionDialogData] = useState(null)

    function renderConfirmActionDialog() {
        if (confirmActionDialogData) {
            return re(ConfirmActionDialog, confirmActionDialogData)
        } else {
            return null;
        }
    }

    function openConfirmActionDialog(dialogParams) {
        setConfirmActionDialogData(dialogParams)
    }

    function closeConfirmActionDialog() {
        setConfirmActionDialogData(null)
    }

    return [openConfirmActionDialog, closeConfirmActionDialog, renderConfirmActionDialog]
}

function reTabs({selectedTabKey,onTabSelected,onTabMouseUp,tabs}) {
    return RE.Container.col.top.left({}, {style:{marginBottom:"5px"}},
        RE.Paper({square:true},
            RE.Tabs({value:selectedTabKey,
                    indicatorColor:"primary",
                    textColor:"primary",
                    onChange:onTabSelected?(event,newTabKey)=>onTabSelected(newTabKey):null
                },
                _.pairs(tabs).map(([tabKey,tabData]) => RE.Tab({
                    key:tabKey,
                    label:tabData.label,
                    value:tabKey,
                    onMouseUp: onTabMouseUp?event=>onTabMouseUp(event,tabKey):null,
                    disabled:tabData.disabled
                }))
            )
        ),
        tabs[selectedTabKey].render()
    )
}

function useTabs({onTabSelected,onTabMouseUp,tabs}) {
    const [selectedTabKey, setSelectedTab] = useState(() => _.pairs(tabs)[0][0])

    return {
        renderTabs: () => reTabs({
            selectedTabKey,
            onTabSelected: selectedTabKey => {
                setSelectedTab(selectedTabKey)
                if (onTabSelected) {
                    onTabSelected(selectedTabKey)
                }
            },
            onTabMouseUp,
            tabs
        })
    }
}

/**
 * This function is used to integrate with the application router.
 * @param redirectFunction a callback from the application router which serves as a switch between application views.
 * @param url a URL the link refers to.
 * @returns {{onMouseUp: (function(...[*]=))}} an object with the only property "onMouseUp". This object should be
 * merged into properties of a react component which is expected to behave as a link.
 * @example
 *  RE.div({...link(redirectFunction, "some/url")},
 *          "Click me! (right click will open in a new tab)"
 *  )
 */
function link(redirectFunction, url) {
    return {
        onMouseUp: event => {
            if (event.nativeEvent.button == 0) {
                redirectFunction(url)
            } else if (event.nativeEvent.button == 1) {
                window.open(url)
            }
        }
    }
}

function useStateFromLocalStorage({key, validator}) {
    const [value, setValue] = useState(() => {
        return validator(readFromLocalStorage(key, undefined))
    })

    function setValueInternal(newValue) {
        newValue = validator(newValue)
        saveToLocalStorage(key, newValue)
        setValue(newValue)
    }

    return [
        value,
        newValue => {
            if (typeof newValue === 'function') {
                setValueInternal(newValue(value))
            } else {
                setValueInternal(newValue)
            }
        }
    ]
}

function useStateFromLocalStorageNumber({key, min, max, minIsDefault, maxIsDefault, defaultValue, nullable}) {
    function getDefaultValue() {
        if (typeof defaultValue === 'function') {
            return defaultValue()
        } else if (minIsDefault) {
            return min
        } else if (maxIsDefault) {
            return max
        } else if (hasValue(defaultValue) || nullable && defaultValue === null) {
            return defaultValue
        } else if (nullable) {
            return null
        } else if (hasValue(min)) {
            return min
        } else if (hasValue(max)) {
            return max
        } else {
            throw new Error('Cannot determine default value for ' + key)
        }
    }

    return useStateFromLocalStorage({
        key,
        validator: value => {
            if (value === undefined) {
                return getDefaultValue()
            } else if (value === null) {
                if (nullable) {
                    return null
                } else {
                    return getDefaultValue()
                }
            } else if (!(typeof value === 'number')) {
                return getDefaultValue()
            } else {
                if (hasValue(min) && value < min || hasValue(max) && max < value) {
                    return getDefaultValue()
                } else {
                    return value
                }
            }
        }
    })
}

function useStateFromLocalStorageString({key, defaultValue, nullable}) {
    function getDefaultValue() {
        if (typeof defaultValue === 'function') {
            return defaultValue()
        } else if (hasValue(defaultValue) || nullable && defaultValue === null) {
            return defaultValue
        } else if (nullable) {
            return null
        } else {
            throw new Error('Cannot determine default value for ' + key)
        }
    }

    return useStateFromLocalStorage({
        key,
        validator: value => {
            if (value === undefined) {
                return getDefaultValue()
            } else if (value === null) {
                if (nullable) {
                    return null
                } else {
                    return getDefaultValue()
                }
            } else if (!(typeof value === 'string')) {
                return getDefaultValue()
            } else {
                return value
            }
        }
    })
}

function useStateFromLocalStorageBoolean({key, defaultValue, nullable}) {
    function getDefaultValue() {
        if (typeof defaultValue === 'function') {
            return defaultValue()
        } else if (hasValue(defaultValue) || nullable && defaultValue === null) {
            return defaultValue
        } else if (nullable) {
            return null
        } else {
            throw new Error('Cannot determine default value for ' + key)
        }
    }

    return useStateFromLocalStorage({
        key,
        validator: value => {
            if (value === undefined) {
                return getDefaultValue()
            } else if (value === null) {
                if (nullable) {
                    return null
                } else {
                    return getDefaultValue()
                }
            } else if (!(typeof value === 'boolean')) {
                return getDefaultValue()
            } else {
                return value
            }
        }
    })
}


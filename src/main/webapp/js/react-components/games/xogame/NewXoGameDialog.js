"use strict";

const XO_GAME_FIELD_SIZE_KEY = XO_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'fieldSize'
const XO_GAME_GOAL_KEY = XO_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'goal'
const XO_GAME_PLAYER_NAME_KEY = XO_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'playerName'
const XO_GAME_TIMER_KEY = XO_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'timer'
const XO_GAME_TITLE_KEY = XO_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'title'
const XO_GAME_PASSCODE_KEY = XO_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'passcode'

const NewXoGameDialog = ({openView, onCancel}) => {

    const [fieldSize, setFieldSize] = useStateFromLocalStorageNumber({
        key: XO_GAME_FIELD_SIZE_KEY,
        min: 3,
        max: 16,
        defaultValue: 8
    })
    const [goal, setGoal] = useStateFromLocalStorageNumber({
        key: XO_GAME_GOAL_KEY,
        defaultValue: () => 4 <= fieldSize ? 4 : fieldSize
    })
    const [playerName, setPlayerName] = useStateFromLocalStorageString({
        key: XO_GAME_PLAYER_NAME_KEY,
        defaultValue: ''
    })
    const [timer, setTimer] = useStateFromLocalStorageString({
        key: XO_GAME_TIMER_KEY,
        defaultValue: ''
    })
    const [title, setTitle] = useStateFromLocalStorageString({
        key: XO_GAME_TITLE_KEY,
        defaultValue: ''
    })
    const [passcode, setPasscode] = useStateFromLocalStorageString({
        key: XO_GAME_PASSCODE_KEY,
        defaultValue: ''
    })

    function createNewXoGame() {
        doRpcCall(
            "createNewBackendState",
            {stateType: "XoGame", initParams: {fieldSize, title, playerName, passcode, goal, timer}},
            gameId => {
                openView(VIEW_URLS.xoGame({gameId}))
            }
        )
    }

    const tdStyle = {padding:'10px'}
    const inputElemsWidth = '200px';
    return RE.Dialog({open: true},
        RE.DialogTitle({}, 'New XO game'),
        RE.DialogContent({dividers:true},
            RE.table({},
                RE.tbody({},
                    RE.tr({},
                        RE.td({style: tdStyle},
                            RE.FormControl({variant:'outlined'},
                                RE.InputLabel({}, 'Field size'),
                                RE.Select(
                                    {
                                        value: fieldSize,
                                        label:'Field size',
                                        onChange: event => {
                                            const newFieldSize = event.target.value;
                                            setFieldSize(newFieldSize)
                                            if (newFieldSize<goal) {
                                                setGoal(newFieldSize)
                                            }
                                        },
                                        style: {width: inputElemsWidth}
                                    },
                                    ints(3, 16).map(i => RE.MenuItem({key: i, value: i}, i))
                                )
                            )
                        )
                    ),
                    RE.tr({},
                        RE.td({style: tdStyle},
                            RE.FormControl({variant:'outlined'},
                                RE.InputLabel({}, 'Goal'),
                                RE.Select(
                                    {
                                        value: goal,
                                        label:'Goal',
                                        onChange: event => setGoal(event.target.value),
                                        style: {width: inputElemsWidth}
                                    },
                                    ints(3, fieldSize).map(i => RE.MenuItem({key: i, value: i}, i))
                                )
                            )
                        )
                    ),
                    RE.tr({},
                        RE.td({style: tdStyle},
                            RE.TextField(
                                {
                                    variant: 'outlined', label: 'Your name (optional)',
                                    style: {width: inputElemsWidth},
                                    onChange: event => setPlayerName(event.target.value),
                                    value: playerName
                                }
                            )
                        )
                    ),
                    RE.tr({},
                        RE.td({style: tdStyle},
                            RE.FormControl({variant:'outlined'},
                                RE.InputLabel({}, 'Timer (optional)'),
                                RE.Select(
                                    {
                                        value: timer,
                                        label:'Timer (optional)',
                                        onChange: event => setTimer(event.target.value),
                                        style: {width: inputElemsWidth}
                                    },
                                    ['none', '5s', '10s', '15s', '20s', '25s', '30s', '40s', '50s', '1m', '1m30s', '2m', '3m']
                                        .map(dur => RE.MenuItem({key: dur, value: dur=='none'?'':dur}, dur))
                                )
                            )
                        )
                    ),
                    RE.tr({},
                        RE.td({style: tdStyle},
                            RE.TextField(
                                {
                                    variant: 'outlined', label: 'Title (optional)',
                                    style: {width: inputElemsWidth},
                                    onChange: event => setTitle(event.target.value),
                                    value: title
                                }
                            )
                        )
                    ),
                    RE.tr({},
                        RE.td({style: tdStyle},
                            RE.TextField(
                                {
                                    variant: 'outlined', label: 'Passcode (optional)',
                                    style: {width: inputElemsWidth},
                                    onChange: event => setPasscode(event.target.value),
                                    value: passcode
                                }
                            )
                        )
                    )
                )
            )
        ),
        RE.DialogActions({},
            RE.Button({color:'primary', onClick: onCancel}, 'Cancel'),
            RE.Button({variant:"contained", color:'primary', onClick: createNewXoGame}, 'Create'),
        ),
    )
}
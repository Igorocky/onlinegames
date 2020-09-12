"use strict";

const WORDS_GAME_PLAYER_NAME_KEY = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'playerName'
const WORDS_GAME_TIMER_KEY = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'timer'
const WORDS_GAME_TITLE_KEY = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'title'
const WORDS_GAME_PASSCODE_KEY = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'passcode'
const WORDS_GAME_WORDS_TO_LEARN_KEY = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'wordsToLearn'

const NewWordsGameDialog = ({openView, onCancel}) => {

    const [playerName, setPlayerName] = useStateFromLocalStorageString({
        key: WORDS_GAME_PLAYER_NAME_KEY,
        defaultValue: ''
    })
    const [timer, setTimer] = useStateFromLocalStorageString({
        key: WORDS_GAME_TIMER_KEY,
        defaultValue: ''
    })
    const [title, setTitle] = useStateFromLocalStorageString({
        key: WORDS_GAME_TITLE_KEY,
        defaultValue: ''
    })
    const [passcode, setPasscode] = useStateFromLocalStorageString({
        key: WORDS_GAME_PASSCODE_KEY,
        defaultValue: ''
    })
    const [wordsToLearn, setWordsToLearn] = useStateFromLocalStorageString({
        key: WORDS_GAME_WORDS_TO_LEARN_KEY,
        defaultValue: ''
    })

    function createNewWordsGame() {
        doRpcCall(
            "createNewBackendState",
            {stateType: "WordsGame", initParams: {title, playerName, passcode, timer, wordsToLearn}},
            gameId => {
                openView(VIEW_URLS.wordsGame({gameId}))
            }
        )
    }

    const tdStyle = {padding:'10px'}
    const inputElemsWidth = '400px';
    return RE.Dialog({open: true},
        RE.DialogTitle({}, 'New Words game'),
        RE.DialogContent({dividers:true},
            RE.table({},
                RE.tbody({},
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
                    ),
                    RE.tr({},
                        RE.td({style: tdStyle},
                            RE.TextField(
                                {
                                    variant: 'outlined', label: 'Text', multiline: true, rowsMax: 10,
                                    style: {width: inputElemsWidth},
                                    onChange: event => setWordsToLearn(event.target.value),
                                    value: wordsToLearn
                                }
                            )
                        )
                    )
                )
            )
        ),
        RE.DialogActions({},
            RE.Button({color:'primary', onClick: onCancel}, 'Cancel'),
            RE.Button({variant:"contained", color:'primary', onClick: createNewWordsGame}, 'Create'),
        ),
    )
}
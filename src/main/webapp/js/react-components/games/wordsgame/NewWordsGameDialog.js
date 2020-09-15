"use strict";

const WORDS_GAME_PLAYER_NAME_KEY = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'playerName'
const WORDS_GAME_TIMER_KEY = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'timer'
const WORDS_GAME_TITLE_KEY = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'title'
const WORDS_GAME_PASSCODE_KEY = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'passcode'
const WORDS_GAME_TEXT_TO_LEARN_KEY = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'textToLearn'

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
    const [textToLearn, setTextToLearn] = useStateFromLocalStorageString({
        key: WORDS_GAME_TEXT_TO_LEARN_KEY,
        defaultValue: ''
    })

    function createNewWordsGame() {
        doRpcCall(
            "createNewBackendState",
            {stateType: "WordsGame", initParams: {title, playerName, passcode, textToLearn}},
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
                                    onChange: event => setTextToLearn(event.target.value),
                                    value: textToLearn
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
"use strict";

const WORDS_GAME_SOUNDS_ENABLED = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'soundsEnabled'

const WordsGamePlayerView = ({openView}) => {
    const query = useQuery()
    const gameId = query.get("gameId")
    const [playerName, setPlayerName] = useStateFromLocalStorageString({
        key: WORDS_GAME_PLAYER_NAME_KEY,
        defaultValue: ''
    })
    const [newPlayerName, setNewPlayerName] = useState(playerName)
    const [conflictingPlayerName, setConflictingPlayerName] = useState(null)
    const [playerNameDialogOpened, setPlayerNameDialogOpened] = useState(false)

    const backend = useBackend({stateId:gameId, bindParams:{playerName}, onMessageFromBackend})
    const [passcode, setPasscode] = useStateFromLocalStorageString({
        key: WORDS_GAME_PASSCODE_KEY,
        nullable: true,
        defaultValue: null
    })
    const [incorrectPasscode, setIncorrectPasscode] = useState(false)
    const [beState, setBeState] = useState(null)
    const prevBeState = usePrevious(beState)

    const [discardDialogOpened, setDiscardDialogOpened] = useState(false)

    const [soundsEnabled, setSoundsEnabled] = useStateFromLocalStorageBoolean({
        key: WORDS_GAME_SOUNDS_ENABLED,
        defaultValue: true
    })
    const [highlightActiveWords, setHighlightActiveWords] = useState(false)

    useEffect(() => {
        if (!hasValue(prevBeState) && hasValue(beState)) {
            setConflictingPlayerName(null)
            setPlayerNameDialogOpened(false)
            setPlayerName(newPlayerName)
        }
    }, [beState])

    function onMessageFromBackend(msg) {
        if (msg.type == "state") {
            setPasscode(null)
            setBeState(old => ({...old, ...msg}))
        } else if (msg.type == "msg:PlayerNameWasSet") {
            setPlayerName(msg.newPlayerName)
            setConflictingPlayerName(null)
            setPlayerNameDialogOpened(false)
        } else if (msg.type == "error:NoAvailablePlaces") {
            openView(VIEW_URLS.gameSelector)
        } else if (msg.type == "error:PasscodeRequired") {
            setPasscode("")
        } else if (msg.type == "error:IncorrectPasscode") {
            setIncorrectPasscode(true)
        } else if (msg.type == "error:PlayerNameIsOccupied") {
            setNewPlayerName(msg.conflictingPlayerName)
            setConflictingPlayerName(msg.conflictingPlayerName)
            setPlayerNameDialogOpened(true)
        }
    }

    function goToGameSelector() {
        openView(VIEW_URLS.gameSelector)
    }

    function renderJoinedPlayersInfo() {
        const namesList = beState.namesOfWaitingPlayers.map(name => ({name, currPlayer:beState.currentPlayerName == name}))
        const unnamedPlayersNum = beState.numberOfWaitingPlayers - beState.namesOfWaitingPlayers.length
        if (unnamedPlayersNum > 0) {
            namesList.push(
                ...ints(1, unnamedPlayersNum).map(i => ({
                    name: 'incognito#' + i,
                    currPlayer:!hasValue(beState.currentPlayerName) && i == 1
                }))
            )
        }
        return RE.Typography({},
            RE.span({}, `Players joined - ${beState.numberOfWaitingPlayers}: `),
            namesList.map((nameObj, idx) => RE.span(
                {key: nameObj.name, style: nameObj.currPlayer ? {textDecoration: 'underline', fontWeight: 'bold'}:{}},
                nameObj.name + (idx == namesList.length-1 ? '' : ', ')
            )),
        )
    }

    function renderGameStatus() {
        if (beState.phase == "WAITING_FOR_PLAYERS_TO_JOIN") {
            return RE.Container.col.top.center({},{style:{marginBottom: "15px"}},
                RE.Typography({variant:"h3"},(hasValue(beState.title)?(beState.title + ': '):'') + 'Waiting for players to join...'),
                renderJoinedPlayersInfo(),
                (beState.currentUserIsGameOwner && hasValue(beState.passcode))
                    ? RE.Typography({},'Passcode: ' + beState.passcode)
                    : null,
                beState.currentUserIsGameOwner
                    ? RE.Fragment({},
                        RE.Button({variant:"contained", color:'primary', onClick: () => backend.send('startGame')}, "Start game"),
                        RE.Button(
                            {variant:"contained", color:'secondary', onClick: () => setDiscardDialogOpened(true), style:{marginLeft:'10px'}},
                            "Discard"
                        ),
                    ) : null,
                discardDialogOpened?renderDiscardDialog():null,
            )
        } else if (beState.phase == "SELECT_WORD" || beState.phase == "ENTER_WORD") {
            let title
            if (!hasValue(beState.currentPlayerId)) {
                title = RE.Fragment({}, "You are watching this game.")
            } else if (beState.playerIdToMove == beState.currentPlayerId) {
                title = RE.Fragment({}, "Your turn ")
            } else {
                title = `Waiting for your opponent${beState.players.length>2?'s':''} to respond.`
            }
            return RE.Typography({variant:"h6"}, title)
        } else if (beState.phase == "FINISHED" || beState.phase == "DISCARDED") {
            return RE.Container.col.top.center({},{},
                RE.Typography({variant:"h4"},beState.phase == "DISCARDED" ? "This game was discarded" : "Game over"),
                beState.phase == "FINISHED" ? RE.Typography({variant:"h5"}, renderScores()) : null,
                RE.Button({onClick: goToGameSelector,}, RE.Icon({fontSize:"large"}, 'home'))
            )
        }
    }

    function renderScores() {
        return "Scores:  TBD"
    }

    function getCurrentPlayer() {
        return beState.players.find(player => player.playerId == beState.currentPlayerId)
    }

    function renderPageContent() {
        if (beState) {
            return RE.Container.col.top.center({},{style:{marginBottom:"20px"}},
                renderGameStatus(),
                renderField(),
                playerNameDialogOpened?renderPlayerNameDialog():null
            )
        } else if (hasValue(passcode)) {
            return renderPasscodeDialog()
        } else if (hasValue(conflictingPlayerName)) {
            return renderPlayerNameDialog()
        } else {
            return RE.CircularProgress()
        }
    }

    function renderField() {
        return RE.Container.col.top.left({},{style:{marginBottom:"20px"}},
            renderFieldButtons(),
            renderText()
        )
    }

    function renderFieldButtons() {
        return RE.ButtonGroup({variant:"contained", size:"small"},
            RE.Button({
                    style:{},
                    onClick: () => setHighlightActiveWords(old => !old),
                },
                RE.Icon({fontSize:"large"}, 'zoom_in')
            ),
            RE.Button({
                    style:{},
                    onClick: () => setSoundsEnabled(!soundsEnabled),
                },
                soundsEnabled?RE.Icon({fontSize:"large"}, 'volume_up'):RE.Icon({fontSize:"large"}, 'volume_off')
            ),
            RE.Button({
                    style:{},
                    onClick: () => {
                        setNewPlayerName(playerName)
                        setPlayerNameDialogOpened(true)
                    },
                },
                RE.Icon({fontSize:"large"}, 'account_box')
            ),
            RE.Button({
                    style:{},
                    onClick: goToGameSelector,
                },
                RE.Icon({fontSize:"large"}, 'home')
            )
        )
    }

    function renderText() {
        if (beState.words) {
            return RE.Container.col.top.left({}, {style:{marginBottom:"20px"}},
                beState.words.map((p, pi) => RE.Typography({key:pi, variant:"h5"},
                    p.map((w, wi) => RE.span(
                        {
                            key: wi,
                            style: {backgroundColor: (highlightActiveWords && w.active) ? "yellow" : ""}
                        },
                        w.value
                    ))
                ))
            )
        } else {
            return null
        }
    }

    function sendPasscode() {
        backend.send(BIND_TO_BE_STATE_METHOD_NAME, {stateId: gameId, bindParams:{passcode, playerName}})
    }

    function sendPlayerName() {
        if (!beState) {
            backend.send(BIND_TO_BE_STATE_METHOD_NAME, {stateId: gameId, bindParams:{passcode, playerName: newPlayerName}})
        } else {
            backend.send("setPlayerName", {playerName: newPlayerName})
        }
    }

    function discardGame() {
        backend.send("discard")
        setDiscardDialogOpened(false)
    }

    function renderPasscodeDialog() {
        const tdStyle = {padding:'10px'}
        const inputElemsWidth = '200px';
        return RE.Dialog({open: true},
            RE.DialogTitle({}, 'Enter passcode'),
            RE.DialogContent({dividers:true},
                RE.table({},
                    RE.tbody({},
                        RE.tr({},
                            RE.td({style: tdStyle}, 'This game requires passcode.')
                        ),
                        incorrectPasscode?RE.tr({},
                            RE.td({style: {...tdStyle, color: 'red'}}, 'You\'ve entered incorrect passcode.')
                        ):null,
                        RE.tr({},
                            RE.td({style: tdStyle},
                                RE.TextField(
                                    {
                                        variant: 'outlined', label: 'Passcode', autoFocus:true,
                                        onKeyDown: event => event.nativeEvent.keyCode == 13 ? sendPasscode() : null,
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
                RE.Button({color:'primary', onClick: goToGameSelector }, 'Cancel'),
                RE.Button({variant:"contained", color:'primary', onClick: sendPasscode}, 'Send'),
            ),
        )
    }

    function renderPlayerNameDialog() {
        const tdStyle = {padding:'10px'}
        const inputElemsWidth = '200px';
        return RE.Dialog({open: true},
            RE.DialogTitle({}, 'Enter your name'),
            RE.DialogContent({dividers:true},
                RE.table({},
                    RE.tbody({},
                        RE.tr({},
                            RE.td({style: tdStyle}, 'Enter your name.')
                        ),
                        hasValue(conflictingPlayerName)?RE.tr({},
                            RE.td({style: {...tdStyle, color: 'red'}}, `Name '${conflictingPlayerName}' is used by another player in this game. Please choose another name.`)
                        ):null,
                        RE.tr({},
                            RE.td({style: tdStyle},
                                RE.TextField(
                                    {
                                        variant: 'outlined', label: 'Your name (Optional)', autoFocus:true,
                                        onKeyDown: event => event.nativeEvent.keyCode == 13 ? sendPlayerName() : null,
                                        style: {width: inputElemsWidth},
                                        onChange: event => setNewPlayerName(event.target.value),
                                        value: newPlayerName
                                    }
                                )
                            )
                        )
                    )
                )
            ),
            RE.DialogActions({},
                RE.Button({color:'primary', onClick: () => setPlayerNameDialogOpened(false) }, 'Cancel'),
                RE.Button({variant:"contained", color:'primary', onClick: sendPlayerName}, 'Save'),
            ),
        )
    }

    function renderDiscardDialog() {
        const tdStyle = {padding:'10px'}
        return RE.Dialog({open: true},
            RE.DialogTitle({}, 'Discard this game'),
            RE.DialogContent({dividers:true},
                RE.table({},
                    RE.tbody({},
                        RE.tr({},
                            RE.td({style: tdStyle}, 'Are you sure you want to discard this game?')
                        )
                    )
                )
            ),
            RE.DialogActions({},
                RE.Button({color:'primary', onClick: () => setDiscardDialogOpened(false) }, 'Cancel'),
                RE.Button({variant:"contained", color:'secondary', onClick: discardGame}, 'Discard'),
            ),
        )
    }

    return RE.Container.col.top.center({style:{marginTop:"20px"}},{},
        renderPageContent()
    )
}
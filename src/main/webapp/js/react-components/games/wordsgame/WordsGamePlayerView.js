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
    const [newTextToLearn, setNewTextToLearn] = useState(null)
    const [selectedWord, setSelectedWord] = useState(null)
    const [enteredWord, setEnteredWord] = useState('')

    useEffect(() => {
        if (!hasValue(prevBeState) && hasValue(beState)) {
            setConflictingPlayerName(null)
            setPlayerNameDialogOpened(false)
            setPlayerName(newPlayerName)
        }
    }, [beState])

    useEffect(() => {
        setSelectedWord(null)
        setEnteredWord('')
    }, [beState?.phase])

    function onMessageFromBackend(msg) {
        if (msg.type == "state") {
            // console.log('beState')
            // console.log(JSON.stringify(msg))
            setPasscode(null)
            setBeState(old => ({...old, ...msg}))
        } else if (msg.type == "msg:PlayerNameWasSet") {
            setPlayerName(msg.newPlayerName)
            setConflictingPlayerName(null)
            setPlayerNameDialogOpened(false)
        } else if (msg.type == "msg:NewTextWasSaved") {
            setNewTextToLearn(null)
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
        } else if (beState.phase == "SELECT_WORD") {
            let title
            if (!hasValue(beState.currentPlayerId)) {
                title = RE.Fragment({}, "You are watching this game.")
            } else if (isTurnOfCurrentUser()) {
                title = RE.Fragment({}, "Select a word")
            } else {
                title = `Waiting for ${getPlayerById(beState.playerIdToMove).name} to select a word`
            }
            return RE.Typography({variant:"h6"}, title)
        } else if (beState.phase == "ENTER_WORD") {
            let title
            if (!hasValue(beState.currentPlayerId)) {
                title = RE.Fragment({}, "You are watching this game.")
            } else if (isTurnOfCurrentUser()) {
                title = RE.Fragment({}, `Waiting for your opponent${beState.players.length>2?'s':''} to respond.`)
            } else {
                title = `Enter the hidden word`
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
        return getPlayerById(beState.currentPlayerId)
    }

    function getPlayerById(playerId) {
        return beState.players.find(player => player.playerId == playerId)
    }

    function getUserInputForCurrentPlayer() {
        if (beState.selectedWord?.userInputs) {
            return beState.selectedWord?.userInputs.find(userInput => userInput.playerId == beState.currentPlayerId)
        } else {
            return null
        }
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
            renderUserInputs(),
            renderText()
        )
    }

    function goToEditMode() {
        setNewTextToLearn(beState.textToLearn)
    }

    function renderFieldButtons() {
        return RE.ButtonGroup({variant:"contained", size:"small"},
            RE.Button({
                    style:{},
                    onClick: () => setHighlightActiveWords(old => !old),
                },
                RE.Icon({fontSize:"large"}, 'highlight')
            ),
            RE.Button({
                    style:{},
                    onClick: goToEditMode,
                },
                RE.Icon({fontSize:"large"}, 'edit')
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
            ),
            RE.Button({
                    style:{},
                    disabled: !(isCurrentUserToSelectWord() && selectedWord),
                    onClick: sendSelectedWord,
                },
                RE.Icon({fontSize:"large"}, 'my_location')
            )
        )
    }

    function saveNewTextToLearn() {
        backend.send("setTextToLearn", {newTextToLearn})
    }

    function sendSelectedWord() {
        backend.send("selectWord", {...selectedWord})
    }

    function renderUserScore(userInput) {
        const player = getPlayerById(userInput.playerId)
        return player.score.numOfCorrectWords + '/' + player.score.numOfAllWords
    }

    function renderUserInputCorrectness(userInput) {
        if (!hasValue(userInput.correct)) {
            return null
        } else if (userInput.correct) {
            return RE.span({style:{color:"green", fontWeight: "bold"}}, "\u2713")
        } else {
            return RE.span({style:{color:"red", fontWeight: "bold"}}, "\u2717")
        }
    }

    function renderUserInputCompleteness(userInput) {
        if (userInput && userInput.confirmed || !userInput && beState.phase != 'ENTER_WORD') {
            return RE.span({style:{color:"green"}}, "\u263A")
        } else {
            return null
        }
    }

    function renderUserInputs() {
        if (beState?.selectedWord?.userInputs) {
            const tableStyle = {borderCollapse: 'collapse', border: '1px solid lightgray', fontSize: '25px'};
            const cellWidth = '40px'
            const cellHeight = cellWidth
            return RE.Paper({},
                RE.table({style:{borderCollapse: 'collapse'}},
                    RE.tbody({style:{borderCollapse: 'collapse'}},
                        RE.tr({style: {height: cellHeight}},
                            RE.th({key:'playerScoreCol'}),
                            RE.th({key:'playerNameCol'}),
                            RE.th({key:'correctness'}),
                            beState.selectedWord.expectedText.map((c,ci) => RE.th(
                                {key:'char-'+ci, style: tableStyle},
                                c
                            )),
                            RE.th({key:'completeness', style: {...tableStyle, width: cellWidth, textAlign: 'center'}}, renderUserInputCompleteness()),
                        ),
                        beState.selectedWord.userInputs.map(userInput => {
                            return RE.tr({key: 'user-input' + userInput.playerId, style: {height: cellHeight}},
                                RE.td({style: {...tableStyle}}, renderUserScore(userInput)),
                                RE.td({style: {...tableStyle}}, getPlayerById(userInput.playerId).name),
                                RE.td({style: {...tableStyle, width: cellWidth, textAlign: 'center'}}, renderUserInputCorrectness(userInput)),
                                userInput.text.map((c, ci) => RE.td(
                                    {key: 'char-' + ci, style: {...tableStyle, width: cellWidth, textAlign: 'center'}},
                                    c
                                )),
                                RE.td({style: {...tableStyle, width: cellWidth, textAlign: 'center'}}, renderUserInputCompleteness(userInput)),
                            );
                        })
                    )
                )
            )
        } else {
            return null
        }
    }

    function isCurrentUserToSelectWord() {
        return beState.phase == "SELECT_WORD" && isTurnOfCurrentUser()
    }

    function isTurnOfCurrentUser() {
        return beState.playerIdToMove == beState.currentPlayerId
    }

    function getHighlightStyleForWord({paragraphIndex, wordIndex, isActive}) {
        if (highlightActiveWords) {
            return {backgroundColor: isActive ? "yellow" : ""}
        } else if (isCurrentUserToSelectWord()) {
            if (selectedWord && selectedWord.paragraphIndex == paragraphIndex && selectedWord.wordIndex == wordIndex) {
                return {backgroundColor: isActive ? "dodgerblue" : ""}
            } else {
                return {backgroundColor: isActive ? "cyan" : ""}
            }
        }
    }

    function activeWordClicked({paragraphIndex, wordIndex, text}) {
        if (isCurrentUserToSelectWord()) {
            setSelectedWord({paragraphIndex, wordIndex, text})
        }
    }

    function sendEnteredWord() {
        backend.send("enterWord", {text: enteredWord})
    }

    function renderWordText({key, paragraphIndex, wordIndex, style, isActive, text}) {
        return RE.span(
            {
                key,
                style: {
                    outline: '10px',
                    borderRadius: '10px',
                    ...getHighlightStyleForWord({paragraphIndex, wordIndex, isActive}),
                    ...style
                },
                className: isActive ? 'active-word' : '',
                onClick: () => activeWordClicked({paragraphIndex, wordIndex, text})
            },
            text
        )
    }

    function renderWord({paragraphIndex, wordIndex, word}) {
        const userInput = getUserInputForCurrentPlayer()
        const key = `${paragraphIndex}-${wordIndex}`;
        if (beState.phase == 'ENTER_WORD' && !userInput?.confirmed && !isTurnOfCurrentUser()
            && beState.selectedWord?.paragraphIndex==paragraphIndex && beState.selectedWord?.wordIndex==wordIndex) {
            return RE.Fragment({key:'fragment-'+key},
                (hasValue(userInput) && !userInput.confirmed) ? [
                    renderWordText({
                        key:'incorrect-word-'+key, paragraphIndex, wordIndex, text: userInput.text.join(''),
                        isActive: false, style:{color:'red', textDecoration: 'line-through'}
                    }),
                    renderWordText({
                        key:'correct-word-'+key, paragraphIndex, wordIndex,
                        text: beState.selectedWord.expectedText.join(''),
                        isActive: false, style:{color:'green'}
                    }),
                ] : [],
                RE.TextField(
                    {
                        key: key,
                        variant: 'outlined',
                        style: {width: '200px'},
                        onChange: event => setEnteredWord(event.target.value),
                        value: enteredWord,
                        onKeyDown: event => event.nativeEvent.keyCode == 13 ? sendEnteredWord() : null,
                        autoFocus: true,
                    }
                )
            )
        } else {
            return renderWordText({key: key, paragraphIndex, wordIndex, text: word.value, isActive: word.active})
        }
    }

    function renderText() {
        if (beState.words) {
            if (hasValue(newTextToLearn)) {
                return RE.Container.col.top.left({},{style:{marginBottom:"20px"}},
                    RE.Container.row.left.center({},{style:{marginRight:"20px"}},
                        RE.Button({color:'primary', onClick: () => setNewTextToLearn(null) }, 'Cancel'),
                        RE.Button({variant:"contained", color:'primary', onClick: saveNewTextToLearn}, 'Save'),
                    ),
                    RE.TextField(
                        {
                            variant: 'outlined', label: 'Text', multiline: true, rowsMax: 10, fullWidth:true,
                            style: {width: '600px'},
                            onChange: event => setNewTextToLearn(event.target.value),
                            value: newTextToLearn
                        }
                    )
                )
            } else {
                return RE.Container.col.top.left({}, {style:{marginBottom:"20px"}},
                    beState.words.map((p, pi) => RE.Typography({key:pi, variant:"h5"},
                        p.filter(w=>!w.meta).map((w, wi) => renderWord({paragraphIndex:pi, wordIndex:wi, word: w}))
                    ))
                )
            }
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
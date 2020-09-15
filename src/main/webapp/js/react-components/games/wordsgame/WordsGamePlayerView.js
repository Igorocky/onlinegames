"use strict";

const WORDS_GAME_SOUNDS_ENABLED = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'soundsEnabled'
const WORDS_GAME_LANG_FROM = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'langFromIdx'
const WORDS_GAME_LANG_TO = WORDS_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'langToIdx'

const WordsGamePlayerView = ({openView}) => {
    const TEXT_FONT_STYLE = {fontSize: '25px', fontFamily:'courier', fontWeight:'bold'}
    const WORDS_GAME_AVAILABLE_LANGUAGES = ['en', 'ru', 'pl']

    const query = useQuery()
    const gameId = query.get("gameId")
    const [playerName, setPlayerName] = useStateFromLocalStorageString({
        key: WORDS_GAME_PLAYER_NAME_KEY,
        defaultValue: ''
    })
    const [newPlayerName, setNewPlayerName] = useState(playerName)
    const [conflictingPlayerName, setConflictingPlayerName] = useState(null)
    const [playerNameDialogOpened, setPlayerNameDialogOpened] = useState(false)
    const [langFromIdx, setLangFromIdx] = useStateFromLocalStorageNumber({
        key: WORDS_GAME_LANG_FROM,
        min: 0,
        max: WORDS_GAME_AVAILABLE_LANGUAGES.length-1,
        defaultValue: 0
    })
    const [langToIdx, setLangToIdx] = useStateFromLocalStorageNumber({
        key: WORDS_GAME_LANG_TO,
        min: 0,
        max: WORDS_GAME_AVAILABLE_LANGUAGES.length-1,
        defaultValue: 0
    })

    const backend = useBackend({stateId:gameId, bindParams:{playerName}, onMessageFromBackend})
    const [passcode, setPasscode] = useState(null)
    const [incorrectPasscode, setIncorrectPasscode] = useState(false)
    const [beState, setBeState] = useState(null)
    const prevBeState = usePrevious(beState)

    // function printState() {
    //     console.log('beState?.selectedWord?.userInputs')
    //     console.log(beState?.selectedWord?.userInputs)
    //     console.log(JSON.stringify(beState?.selectedWord?.userInputs))
    // }
    // printState()

    const [discardDialogOpened, setDiscardDialogOpened] = useState(false)
    const [endGameDialogOpened, setEndGameDialogOpened] = useState(false)

    const [soundsEnabled, setSoundsEnabled] = useStateFromLocalStorageBoolean({
        key: WORDS_GAME_SOUNDS_ENABLED,
        defaultValue: true
    })
    const [highlightActiveWords, setHighlightActiveWords] = useState(false)
    const [newTextToLearn, setNewTextToLearn] = useState(null)
    const [highlightedWord, setHighlightedWord] = useState(null)
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
        if (soundsEnabled && beState?.phase == 'ENTER_WORD' && !isTurnOfCurrentUser()) {
            playAudio(audioUrl('on-move.mp3'))
        }
    }, [beState?.phase])

    useEffect(() => {
        setSelectedWord(null)
        setEnteredWord('')
    }, [beState?.phase])

    function onMessageFromBackend(msg) {
        if (msg.type == "state") {
            setPasscode(null)
            setIncorrectPasscode(false)
            setBeState(old => ({...old, ...msg}))
        } else if (msg.type == "msg:PlayerNameWasSet") {
            setPlayerName(msg.newPlayerName)
            setConflictingPlayerName(null)
            setPlayerNameDialogOpened(false)
        } else if (msg.type == "msg:NewTextWasSaved") {
            setNewTextToLearn(null)
            setHighlightedWord(null)
            setSelectedWord(null)
        } else if (msg.type == "error:NoAvailablePlaces") {
            openView(VIEW_URLS.gameSelector)
        } else if (msg.type == "error:PasscodeRequired") {
            setPasscode('')
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
                RE.Typography({variant:"h6"},(hasValue(beState.title)?(beState.title + ': '):'') + 'Waiting for players to join...'),
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
        } else if (beState.phase == "DISCARDED") {
            return RE.Container.col.top.center({},{},
                RE.Typography({variant:"h4"},"This game was discarded"),
                RE.Button({onClick: goToGameSelector,}, RE.Icon({fontSize:"large"}, 'home'))
            )
        } else if (beState.phase == "FINISHED") {
            return RE.Container.col.top.center({},{},
                RE.Typography({variant:"h4"},"Game over"),
                renderFinalScores(),
                RE.Button({onClick: goToGameSelector,}, RE.Icon({fontSize:"large"}, 'home'))
            )
        }
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
                playerNameDialogOpened?renderPlayerNameDialog():null,
                endGameDialogOpened?renderEndGameDialog():null
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

    function copyToClipboard(str) {
        const el = document.createElement('textarea')
        el.value = str
        document.body.appendChild(el)
        el.select()
        document.execCommand('copy')
        document.body.removeChild(el)
    }

    function renderFieldButtons() {
        return RE.Container.row.left.center({},{style:{marginRight: '15px'}},
            RE.ButtonGroup({variant:"contained", size:"small"},
                RE.Button({
                        style:{},
                        onClick: () => {
                            setHighlightActiveWords(old => !old)
                            setHighlightedWord(null)
                            setSelectedWord(null)
                        },
                    },
                    RE.Icon({fontSize:"large"}, 'highlight')
                ),
                RE.Button({
                        style:{},
                        disabled: !beState.currentUserIsGameOwner || beState.phase == 'FINISHED' || beState.phase == 'DISCARDED',
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
                        disabled: beState.phase == 'FINISHED' || beState.phase == 'DISCARDED',
                        onClick: () => {
                            setNewPlayerName(playerName)
                            setPlayerNameDialogOpened(true)
                        },
                    },
                    RE.Icon({fontSize:"large"}, 'account_box')
                ),
                RE.Button({
                        style:{},
                        disabled: !beState.currentUserIsGameOwner
                            || beState.phase == 'WAITING_FOR_PLAYERS_TO_JOIN'
                            || beState.phase == 'FINISHED'
                            || beState.phase == 'DISCARDED',
                        onClick: () => setEndGameDialogOpened(true),
                    },
                    RE.Icon({fontSize:"large"}, 'cancel')
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
            ),
            highlightedWord?RE.Container.row.left.center({},{style:{marginRight: '15px'}},
                RE.Button({variant: 'contained', onClick: () => copyToClipboard(highlightedWord.text)}, 'Copy'),
                RE.Button({
                    variant: 'contained',
                    onClick: () => window.open(
                        'https://translate.google.com/#view=home&op=translate' +
                        '&sl=' + WORDS_GAME_AVAILABLE_LANGUAGES[langFromIdx] +
                        '&tl=' + WORDS_GAME_AVAILABLE_LANGUAGES[langToIdx] +
                        '&text=' + highlightedWord.text
                    )
                }, 'Translate'),
                RE.Select(
                    {
                        value: langFromIdx,
                        onChange: event => setLangFromIdx(event.target.value),
                    },
                    WORDS_GAME_AVAILABLE_LANGUAGES
                        .map((lang,idx) => RE.MenuItem({key: lang, value: idx}, 'from ' + lang.toUpperCase()))
                ),
                RE.Select(
                    {
                        value: langToIdx,
                        onChange: event => setLangToIdx(event.target.value),
                    },
                    WORDS_GAME_AVAILABLE_LANGUAGES
                        .map((lang,idx) => RE.MenuItem({key: lang, value: idx}, 'to ' + lang.toUpperCase()))
                )
            ):null
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
            return RE.span({style:{color:'green', fontWeight: 'bold'}}, '\u2713')
        } else {
            return RE.span({style:{color:'red', fontWeight: 'bold'}}, '\u2717')
        }
    }

    function getUserInputByUserId(userId) {
        return  (beState?.selectedWord?.userInputs??[]).find(userInput => userInput.playerId==userId)
    }

    function isUserInputConfirmed(userId) {
        const userInput = getUserInputByUserId(userId)
        return userInput && userInput.confirmed
    }

    function renderUserInputCompleteness(userInput) {
        if (userInput && userInput.confirmed || !userInput && beState.phase != 'ENTER_WORD') {
            return RE.span({style:{color:"green"}}, "\u263A")
        } else {
            return null
        }
    }

    function renderUserInputs() {
        if (isUserInputConfirmed(beState?.currentPlayerId) && !(beState.phase == 'FINISHED' || beState.phase == 'DISCARDED')) {
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
                        beState.selectedWord.userInputs.map(userInput => RE.tr({key: 'user-input' + userInput.playerId, style: {height: cellHeight}},
                            RE.td({style: {...tableStyle}}, renderUserScore(userInput)),
                            RE.td(
                                {
                                    style: {
                                        ...tableStyle,
                                        ...(userInput.playerId == beState.currentPlayerId ? {textDecoration: 'underline', fontWeight: 'bold'} : {})
                                    }
                                },
                                getPlayerById(userInput.playerId).name
                            ),
                            RE.td({style: {...tableStyle, width: cellWidth, textAlign: 'center'}}, renderUserInputCorrectness(userInput)),
                            userInput.text.map((c, ci) => RE.td(
                                {key: 'char-' + ci, style: {...tableStyle, width: cellWidth, textAlign: 'center'}},
                                c
                            )),
                            RE.td({style: {...tableStyle, width: cellWidth, textAlign: 'center'}}, renderUserInputCompleteness(userInput)),
                        ))
                    )
                )
            )
        } else {
            return null
        }
    }

    function renderFinalScores() {
        if (beState.finalScores) {
            const tableStyle = {borderCollapse: 'collapse', border: '1px solid lightgray', fontSize: '25px'};
            return RE.Paper({},
                RE.table({style:{borderCollapse: 'collapse'}},
                    RE.tbody({style:{borderCollapse: 'collapse'}},
                        RE.tr({style: {}},
                            RE.th({key:'playerRank'}),
                            RE.th({key:'playerNameCol'}),
                            RE.th({key:'playerScoreCol'})
                        ),
                        beState.finalScores.map((player,rank) => RE.tr({key: 'final-score-' + player.playerId, style: {}},
                            RE.td({style: {...tableStyle}}, rank+1),
                            RE.td(
                                {
                                    style: {
                                        ...tableStyle,
                                        ...(player.playerId == beState.currentPlayerId ? {textDecoration: 'underline', fontWeight: 'bold'} : {})
                                    }
                                },
                                player.name
                            ),
                            RE.td({style: {...tableStyle}}, renderUserScore(player))
                        ))
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
        if (!isActive) {
            return {}
        } else {
            const activeWordColor = 'khaki'
            const highlightedWordColor = 'coral'
            const availableForSelectionWordColor = 'mediumaquamarine'
            const selectedWordColor = 'dodgerblue'
            let backgroundColor
            if (highlightActiveWords) {
                if (highlightedWord?.paragraphIndex == paragraphIndex && highlightedWord?.wordIndex == wordIndex) {
                    backgroundColor = highlightedWordColor
                } else {
                    backgroundColor = activeWordColor
                }
            } else if (beState.phase == 'SELECT_WORD') {
                if (isCurrentUserToSelectWord()) {
                    if (selectedWord?.paragraphIndex == paragraphIndex && selectedWord?.wordIndex == wordIndex) {
                        backgroundColor = selectedWordColor
                    } else {
                        backgroundColor = availableForSelectionWordColor
                    }
                } else {
                    backgroundColor = ''
                }
            } else if (beState.phase == 'ENTER_WORD') {
                if (beState.selectedWord?.userInputs
                    && beState.selectedWord?.paragraphIndex == paragraphIndex
                    && beState.selectedWord?.wordIndex == wordIndex) {
                    backgroundColor = selectedWordColor
                } else {
                    backgroundColor = ''
                }
            }
            return {backgroundColor}
        }

        if (highlightActiveWords) {
            return {backgroundColor: isActive ? "yellow" : ""}
        } else if (selectedWord?.paragraphIndex == paragraphIndex && selectedWord?.wordIndex == wordIndex
            || beState.selectedWord?.paragraphIndex == paragraphIndex && beState.selectedWord?.wordIndex == wordIndex) {
            return {backgroundColor: isActive ? "dodgerblue" : ""}
        } else {
            return {backgroundColor: isActive ? (beState.pahase == 'SELECT_WORD' ? "cyan" : '') : ""}
        }
    }

    function activeWordClicked({paragraphIndex, wordIndex, text}) {
        if (highlightActiveWords) {
            setHighlightedWord({paragraphIndex, wordIndex, text})
        } else if (!highlightActiveWords && isCurrentUserToSelectWord()) {
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
        if (hasValue(beState.currentPlayerId) && beState.phase == 'ENTER_WORD' && !userInput?.confirmed && !isTurnOfCurrentUser()
            && beState.selectedWord?.paragraphIndex==paragraphIndex && beState.selectedWord?.wordIndex==wordIndex) {
            let textFieldBorder = (getUserInputByUserId(beState?.currentPlayerId)?.correct===false)
                ? 'red 2px solid' : 'black 1px solid'
            return RE.Fragment({key:'fragment-'+key},
                (hasValue(userInput) && !userInput.confirmed) ? [
                    renderWordText({
                        key:'incorrect-word-'+key, paragraphIndex:-1, wordIndex:-1, text: userInput.text.join(''),
                        isActive: false, style:{color:'red', textDecoration: 'line-through'}
                    }),
                    renderWordText({
                        key:'incorrect-word-space'+key, paragraphIndex:-1, wordIndex:-1, text: '\u00A0',
                        isActive: false, style:{}
                    }),
                    renderWordText({
                        key:'correct-word-'+key, paragraphIndex:-1, wordIndex:-1,
                        text: beState.selectedWord.expectedText.join(''),
                        isActive: false, style:{color:'green'}
                    }),
                    renderWordText({
                        key:'correct-word-space'+key, paragraphIndex:-1, wordIndex:-1, text: '\u00A0',
                        isActive: false, style:{}
                    }),
                ] : [],
                RE.InputBase({
                    key: key,
                    onKeyDown: event => event.nativeEvent.keyCode == 13 ? sendEnteredWord() : null,
                    autoFocus: true,
                    value: enteredWord,
                    variant: "outlined",
                    onChange: event => setEnteredWord(event.target.value),
                    style: {
                        background: "moccasin",
                        padding:"0px 5px",
                        borderRadius: "5px",
                        border: textFieldBorder,
                        marginLeft: "5px",
                        width: "250px",
                        ...TEXT_FONT_STYLE
                    }
                })
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
                    beState.words.map((p, pi) => RE.div({key:pi, style: {...TEXT_FONT_STYLE, lineHeight:'35px'}},
                        p.map((w, wi) => [w,wi]).filter(([w,wi])=>!w.meta).map(([w,wi]) => renderWord({paragraphIndex:pi, wordIndex:wi, word: w}))
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

    function endGame() {
        backend.send("end")
        setEndGameDialogOpened(false)
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

    function renderEndGameDialog() {
        const tdStyle = {padding:'10px'}
        return RE.Dialog({open: true},
            RE.DialogTitle({}, 'End this game'),
            RE.DialogContent({dividers:true},
                RE.table({},
                    RE.tbody({},
                        RE.tr({},
                            RE.td({style: tdStyle}, 'Are you sure you want to end this game?')
                        )
                    )
                )
            ),
            RE.DialogActions({},
                RE.Button({color:'primary', onClick: () => setEndGameDialogOpened(false) }, 'Cancel'),
                RE.Button({variant:"contained", color:'secondary', onClick: endGame}, 'End'),
            ),
        )
    }

    return RE.Container.col.top.center({style:{marginTop:"20px"}},{},
        renderPageContent()
    )
}
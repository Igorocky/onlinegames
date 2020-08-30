"use strict";

const XO_GAME_PLAY_FIELD_SIZE_PER_CELL_KEY = XO_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'playFieldSizePerCell'
const XO_GAME_SOUNDS_ENABLED = XO_GAME_PLAYER_VIEW_LOC_STORAGE_KEY_PREFIX + 'soundsEnabled'

const XoGamePlayerView = ({openView}) => {
    const query = useQuery()
    const gameId = query.get("gameId")
    const [playerName, setPlayerName] = useStateFromLocalStorageString({
        key: XO_GAME_PLAYER_NAME_KEY,
        defaultValue: ''
    })
    const [newPlayerName, setNewPlayerName] = useState(playerName)
    const [conflictingPlayerName, setConflictingPlayerName] = useState(null)
    const [playerNameDialogOpened, setPlayerNameDialogOpened] = useState(false)

    const backend = useBackend({stateId:gameId, bindParams:{playerName}, onMessageFromBackend})
    const [passcode, setPasscode] = useStateFromLocalStorageString({
        key: XO_GAME_PASSCODE_KEY,
        nullable: true,
        defaultValue: null
    })
    const [incorrectPasscode, setIncorrectPasscode] = useState(false)
    const [beState, setBeState] = useState(null)
    const prevBeState = usePrevious(beState)

    const [discardDialogOpened, setDiscardDialogOpened] = useState(false)

    const PLAY_FIELD_SIZE_PER_CELL_MIN = 20
    const PLAY_FIELD_SIZE_PER_CELL_MAX = 300
    const [playFieldSizePerCell, setPlayFieldSizePerCell] = useStateFromLocalStorageNumber({
        key: XO_GAME_PLAY_FIELD_SIZE_PER_CELL_KEY,
        min: PLAY_FIELD_SIZE_PER_CELL_MIN,
        max: PLAY_FIELD_SIZE_PER_CELL_MAX,
        defaultValue: 100
    })
    const [soundsEnabled, setSoundsEnabled] = useStateFromLocalStorageBoolean({
        key: XO_GAME_SOUNDS_ENABLED,
        defaultValue: true
    })

    useEffect(() => {
        if (!hasValue(prevBeState) && hasValue(beState)) {
            setConflictingPlayerName(null)
            setPlayerNameDialogOpened(false)
            setPlayerName(newPlayerName)
        }
    }, [beState])

    const prevLastCellStr = usePrevious(getLastCellStr())
    const currLastCellStr = getLastCellStr()
    useEffect(() => {
        if (soundsEnabled && prevLastCellStr != currLastCellStr) {
            playAudio(audioUrl('on-move.mp3'))
        }
    }, [currLastCellStr])

    function onMessageFromBackend(msg) {
        if (msg.type && msg.type == "state") {
            setPasscode(null)
            setBeState(msg)
        } else if (msg.type && msg.type == "msg:PlayerNameWasSet") {
            setPlayerName(msg.newPlayerName)
            setConflictingPlayerName(null)
            setPlayerNameDialogOpened(false)
        } else if (msg.type && msg.type == "error:NoAvailablePlaces") {
            openView(VIEW_URLS.gameSelector)
        } else if (msg.type && msg.type == "error:PasscodeRequired") {
            setPasscode("")
        } else if (msg.type && msg.type == "error:IncorrectPasscode") {
            setIncorrectPasscode(true)
        } else if (msg.type && msg.type == "error:PlayerNameIsOccupied") {
            setNewPlayerName(msg.conflictingPlayerName)
            setConflictingPlayerName(msg.conflictingPlayerName)
            setPlayerNameDialogOpened(true)
        }
    }

    function getLastCellStr() {
        const lastCell = beState?.lastCell
        return lastCell?(lastCell[0]+'-'+lastCell[1]):undefined
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
        } else if (beState.phase == "IN_PROGRESS") {
            return RE.Typography({variant:"h6"},
                beState.playerIdToMove == beState.currentPlayerId
                    ? RE.Fragment({}, "Your turn ", symbolToImg(getCurrentPlayer().symbol))
                    : (`Waiting for your opponent${beState.players.length>2?'s':''} to respond.`)
            )
        } else if (beState.phase == "FINISHED" || beState.phase == "DISCARDED") {
            return RE.Container.col.top.center({},{},
                RE.Typography({variant:"h4"},beState.phase == "DISCARDED" ? "This game was discarded" : "Game over"),
                beState.phase == "FINISHED" ? RE.Typography({variant:"h5"}, renderWinnerInfo()) : null,
                RE.Button({onClick: goToGameSelector,}, RE.Icon({fontSize:"large"}, 'home'))
            )
        }
    }

    function symbolToImg(symbol) {
        return RE.img({src:`/img/xogame/${symbol}-symbol.svg`, style: {width:'20px', height:'20px'}})
    }

    function renderWinnerInfo() {
        if (hasValue(beState.winnerId)) {
            const winner = getWinner();
            return beState.currentPlayerId==beState.winnerId
                ? RE.span({style:{fontWeight:'bold', color:'forestgreen'}}, "You are the winner!")
                : RE.Fragment({}, (hasValue(winner.name) ? winner.name : symbolToImg(winner.symbol)), " won.")
        } else {
            return "It's a draw."
        }
    }

    function getWinner() {
        return beState.players.find(player => player.playerId == beState.winnerId)
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

    function cellClicked({x,y}) {
        backend.send("clickCell", {x,y})
    }

    function zoomField(factor) {
        setPlayFieldSizePerCell(oldValue => oldValue * factor)
    }

    function renderField() {
        if (!beState.field) {
            return null
        }
        const tableData = []
        for (let x = 0; x < beState.fieldSize; x++) {
            tableData.push([])
            for (let y = 0; y < beState.fieldSize; y++) {
                tableData[x].push({x,y})
            }
        }
        beState.field.forEach(cellDto => {
            tableData[cellDto.x][cellDto.y] = {...tableData[cellDto.x][cellDto.y], ...cellDto}
        })
        if (beState.winnerPath) {
            beState.winnerPath.forEach(([x,y]) => tableData[x][y].isWinnerCell=true)
        }
        if (beState.lastCell) {
            const lastCellX = beState.lastCell[0];
            const lastCellY = beState.lastCell[1];
            tableData[lastCellX][lastCellY] = {...tableData[lastCellX][lastCellY], lastCell:{x:lastCellX, y:lastCellY}}
        }

        return RE.Container.row.left.top({},{},
            RE.Container.col.top.left({style:{marginRight:'10px'}},{style:{marginBottom:'10px'}},
                RE.span({style:{fontSize:'30px'}}, "Goal: " + beState.goal),
                (beState.phase == "WAITING_FOR_PLAYERS_TO_JOIN" && beState.timerSeconds)
                    ?RE.span({style:{fontSize:'20px'}}, "Time: " + beState.timerSeconds):null,
                beState.players?re(XoGameTableOfPlayersComponent, beState):null,
            ),
            re(XoGamePlayfieldComponent, {
                size: playFieldSizePerCell*beState.fieldSize,
                fieldSize: beState.fieldSize,
                tableData,
                onCellClicked: cellClicked,
                frameSymbol: (hasValue(beState.currentPlayerId) && beState.currentPlayerId==beState.playerIdToMove)?getCurrentPlayer().symbol:null
            }),
            RE.ButtonGroup({variant:"contained", size:"small", orientation:"vertical"},
                RE.Button({
                        style:{},
                        disabled: playFieldSizePerCell == PLAY_FIELD_SIZE_PER_CELL_MAX,
                        onClick: () => zoomField(1.1),
                    },
                    RE.Icon({fontSize:"large"}, 'zoom_in')
                ),
                RE.Button({
                        style:{},
                        disabled: playFieldSizePerCell == PLAY_FIELD_SIZE_PER_CELL_MIN,
                        onClick: () => zoomField(1/1.1),
                    },
                    RE.Icon({fontSize:"large"}, 'zoom_out')
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
                )
            ),
        )
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
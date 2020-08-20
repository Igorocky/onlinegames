"use strict";

const XoGamePlayerView = ({openView}) => {
    const query = useQuery()
    const gameId = query.get("gameId")

    const backend = useBackend({stateId:gameId, onMessageFromBackend})
    const [passcode, setPasscode] = useState(null)
    const [incorrectPasscode, setIncorrectPasscode] = useState(false)
    const [beState, setBeState] = useState(null)

    const playFieldSizePerCellKey = 'XoGamePlayerView.playFieldSizePerCell'
    const PLAY_FIELD_SIZE_PER_CELL_MIN = 20
    const PLAY_FIELD_SIZE_PER_CELL_MAX = 300
    const [playFieldSizePerCell, setPlayFieldSizePerCell] = useState(() =>
        validatePlayFieldSizePerCell(readFromLocalStorage(playFieldSizePerCellKey, 100))
    )

    function validatePlayFieldSizePerCell(newValue) {
        if (newValue < PLAY_FIELD_SIZE_PER_CELL_MIN) {
            return PLAY_FIELD_SIZE_PER_CELL_MIN
        } else if (newValue > PLAY_FIELD_SIZE_PER_CELL_MAX) {
            return PLAY_FIELD_SIZE_PER_CELL_MAX
        } else {
            return newValue
        }
    }

    function onMessageFromBackend(msg) {
        if (msg.type && msg.type == "state") {
            setPasscode(null)
            setBeState(msg)
        } else if (msg.type && msg.type == "error:NoAvailablePlaces") {
            openView(VIEW_URLS.gameSelector)
        } else if (msg.type && msg.type == "error:PasscodeRequired") {
            setPasscode("")
        } else if (msg.type && msg.type == "error:IncorrectPasscode") {
            setIncorrectPasscode(true)
        }
    }

    function goToGameSelector() {
        openView(VIEW_URLS.gameSelector)
    }

    function renderGameStatus() {
        if (beState.phase == "WAITING_FOR_PLAYERS_TO_JOIN") {
            return RE.Container.col.top.center({},{style:{marginBottom: "15px"}},
                RE.Typography({variant:"h3"},(hasValue(beState.title)?(beState.title + ': '):'') + 'Waiting for players to join...'),
                RE.Typography({},'Number of joined players: ' + beState.numberOfWaitingPlayers),
                (beState.currentUserIsGameOwner && hasValue(beState.passcode))
                    ? RE.Typography({},'Passcode: ' + beState.passcode)
                    : null,
                beState.currentUserIsGameOwner
                    ? RE.Button({variant:"contained", onClick: () => backend.send('startGame')}, "Start game")
                    : null,
            )
        } else if (beState.phase == "IN_PROGRESS") {
            return RE.Typography({variant:"h6"},
                beState.playerIdToMove == beState.currentPlayerId
                    ? RE.Fragment({}, "Your turn ", symbolToImg(getCurrentPlayer().symbol))
                    : (`Waiting for your opponent${beState.players.length>2?'s':''} to respond.`)
            )
        } else if (beState.phase == "FINISHED") {
            return RE.Container.col.top.center({},{},
                RE.Typography({variant:"h4"},"Game over"),
                RE.Typography({variant:"h5"}, renderWinnerInfo()),
                RE.Button({onClick: goToGameSelector}, "New game"),
            )
        }
    }

    function symbolToImg(symbol) {
        return RE.img({src:`/img/xogame/${symbol}-symbol.svg`, style: {width:'20px', height:'20px'}})
    }

    function renderWinnerInfo() {
        if (hasValue(beState.winnerId)) {
            return beState.currentPlayerId==beState.winnerId
                ? RE.span({style:{fontWeight:'bold', color:'forestgreen'}}, "You are the winner!")
                : RE.Fragment({}, symbolToImg(getWinner().symbol), " won.")
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
            )
        } else if (hasValue(passcode)) {
            return renderPasscodeDialog()
        } else {
            return RE.CircularProgress()
        }
    }

    function cellClicked({x,y}) {
        backend.send("clickCell", {x,y})
    }

    function zoomField(factor) {
        setPlayFieldSizePerCell(oldValue => {
            const newValue = validatePlayFieldSizePerCell(oldValue * factor)
            saveToLocalStorage(playFieldSizePerCellKey, newValue)
            return newValue
        })
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
                beState.players?re(XoGameTableOfPlayersComponent, beState):null,
            ),
            re(XoGamePlayfieldComponent, {
                size: playFieldSizePerCell*beState.fieldSize,
                fieldSize: beState.fieldSize,
                tableData,
                onCellClicked: cellClicked
            }),
            RE.ButtonGroup({variant:"contained", size:"small", orientation:"vertical"},
                RE.Button({
                        style:{},
                        disabled: playFieldSizePerCell == PLAY_FIELD_SIZE_PER_CELL_MAX,
                        onClick: () => zoomField(1.1),
                    },
                    RE.Icon({}, 'zoom_in')
                ),
                RE.Button({
                        style:{},
                        disabled: playFieldSizePerCell == PLAY_FIELD_SIZE_PER_CELL_MIN,
                        onClick: () => zoomField(1/1.1),
                    },
                    RE.Icon({}, 'zoom_out')
                )
            ),
        )
    }

    function sendPasscode() {
        backend.send(BIND_TO_BE_STATE_METHOD_NAME, {stateId: gameId, bindParams:{passcode}})
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
                                    },
                                    passcode
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

    return RE.Container.col.top.center({style:{marginTop:"20px"}},{},
        renderPageContent()
    )
}
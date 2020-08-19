"use strict";

const XoGamePlayerView = ({openView}) => {
    const query = useQuery()
    const gameId = query.get("gameId")

    // const backend = useBackend({stateId:gameId, onMessageFromBackend})
    const [beState, setBeState] = useState(
        {
            "type": "state",
            "phase": "FINISHED",
            "currentUserIsGameOwner": true,
            "field": [
                {"symbol": "o", "x": 0, "y": 1},
                {"symbol": "x", "x": 1, "y": 1},
                {"symbol": "s", "x": 2, "y": 1},
                {"symbol": "t", "x": 2, "y": 2},
                {"symbol": "a", "x": 2, "y": 0},
                ],
            "players": [
                {"playerId": 0, "gameOwner": false, "symbol": "x"},
                {"playerId": 1, "gameOwner": true, "symbol": "o"},
                {"playerId": 2, "gameOwner": true, "symbol": "s"},
                {"playerId": 3, "gameOwner": true, "symbol": "t"},
                {"playerId": 4, "gameOwner": true, "symbol": "a"},
                ],
            "currentPlayerId": 1,
            "playerIdToMove": 4,
            "winnerId": 1
        }
    )

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
            setBeState(msg)
        } else if (msg.type && msg.type == "error:NoAvailablePlaces") {
            openView(VIEW_URLS.gameSelector)
        }
    }

    function renderGameStatus() {
        if (beState.phase == "WAITING_FOR_PLAYERS_TO_JOIN") {
            return RE.Container.col.top.center({},{style:{marginBottom: "30px"}},
                RE.Typography({variant:"h3"},"Waiting for players to join..."),
                RE.Typography({},'Number of joined players: ' + beState.numberOfWaitingPlayers),
                beState.currentUserIsGameOwner
                    ? RE.Button({variant:"contained", onClick: () => backend.send('startGame')}, "Start game")
                    : null
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
                RE.Button({onClick: () => openView(VIEW_URLS.gameSelector)}, "New game"),
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
                : RE.Fragment({}, symbolToImg(getWinner().symbol), " wins.")
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

    function getOpponents() {
        return beState.players.filter(player => player.playerId != beState.currentPlayerId)
    }

    function renderPageContent() {
        if (beState) {
            return RE.Container.col.top.center({},{style:{marginBottom:"20px"}},
                renderGameStatus(),
                renderField(),
            )
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
        for (let x = 0; x < 3; x++) {
            tableData.push([])
            for (let y = 0; y < 3; y++) {
                tableData[x].push({x,y})
            }
        }
        beState.field.forEach(cellDto => {
            tableData[cellDto.x][cellDto.y] = {...tableData[cellDto.x][cellDto.y], ...cellDto}
        })

        return RE.Container.row.left.top({},{},
            re(XoGamePlayfieldComponent, {
                size: playFieldSizePerCell*3,
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
            re(XoGameTableOfPlayersComponent, beState)
        )
    }

    return RE.Container.col.top.center({style:{marginTop:"20px"}},{},
        renderPageContent()
    )
}
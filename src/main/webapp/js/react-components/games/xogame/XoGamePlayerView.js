"use strict";

const XoGamePlayerView = ({joinId}) => {
    const backend = useBackend({stateId:joinId, onMessageFromBackend})
    const [beState, setBeState] = useState(null)

    // const [beState, setBeState] = useState(BE_STATE)

    function onMessageFromBackend(msg) {
        if (msg.type && msg.type == "state") {
            setBeState(msg)
        }
    }

    function renderGameStatus() {
        if (beState.phase == "WAITING_FOR_PLAYERS_TO_JOIN") {
            return RE.Container.col.top.center({},{style:{marginTop: "30px"}},
                RE.Typography({variant:"h3"},"Waiting for your opponent to join..."),
                RE.Typography({},"Send this URL to your opponent to join"),
                renderJoinUrls(getOpponents())
            )
        } else if (beState.phase == "IN_PROGRESS") {
            return RE.Typography({variant:"h6"},
                beState.playerIdToMove == beState.currentPlayerId
                    ? ("Your turn " + getCurrentPlayer().symbol)
                    : ("Waiting for your opponent to respond " + getOpponents()[0].symbol)
            )
        } else if (beState.phase == "FINISHED") {
            return RE.Container.col.top.center({},{style:{marginTop: "30px"}},
                RE.Typography({variant:"h3"},"Game over"),
                RE.Typography({variant:"h4"},
                    beState.winnerId ? (getWinner().symbol + " wins.") : "It's a draw."
                ),
            )
        }
    }

    function getWinner() {
        return beState.players.filter(player => player.playerId == beState.winnerId)[0]
    }

    function getCurrentPlayer() {
        return beState.players.filter(player => player.playerId == beState.currentPlayerId)[0]
    }

    function getOpponents() {
        return beState.players.filter(player => player.playerId != beState.currentPlayerId)
    }

    function renderJoinUrls(players) {
        return RE.Fragment({},
            players.map(player => RE.Container.row.left.center({key:player.playerId},{style:{marginLeft:"50px"}},
                RE.span({style:{fontSize:"50px"}},player.symbol),
                getViewAbsoluteUrl(VIEW_URLS.xoGame({joinId:player.joinId})),
                player.connected ? "CONNECTED" : "WAITING..."
            ))
        )
    }

    function renderPageContent() {
        if (beState) {
            return RE.Container.col.top.center({},{style:{marginTop:"20px"}},
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

    function renderField() {
        const tableData = []
        for (let x = 0; x < 3; x++) {
            tableData.push([])
            for (let y = 0; y < 3; y++) {
                tableData[x].push(null)
            }
        }
        beState.field.forEach(cellDto => tableData[cellDto.x][cellDto.y] = cellDto)
        return RE.table({style:{borderCollapse: "collapse"}},
            RE.tbody({},
                tableData.map((row, x) => RE.tr({key:x},
                    row.map((cell, y) => RE.td(
                        {
                            key:y,
                            style:{border: "1px solid black", width:"100px", height:"100px"},
                            onClick: () => cellClicked({x, y})
                        },
                        RE.span({style:{fontSize: "80px", marginLeft:"25px"}},
                            cell ? cell.symbol : ""
                        )
                    ))
                ))
            )
        )
    }

    return RE.Container.col.top.center({style:{marginTop:"100px"}},{},
        renderPageContent()
    )
}
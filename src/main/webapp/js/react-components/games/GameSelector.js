"use strict";

const GameSelector = ({openView}) => {

    const [availableNewGames, setAvailableNewGames] = useState(null)
    const timerHandle = useRef(null)
    const [history, setHistory] = useState(null)

    const {renderTabs} = useTabs({tabs: createTabs(), onTabSelected})

    const [openedDialog, setOpenedDialog] = useState(null)

    useEffect(() => {
        loadListOfAvailableNewGames()
        return () => {
            if (timerHandle.current) {
                window.clearTimeout(timerHandle.current)
            }
        }
    }, [])

    function loadListOfAvailableNewGames() {
        doRpcCall('listNewGames', {}, listOfGames => {
            setAvailableNewGames(listOfGames)
            timerHandle.current = window.setTimeout(loadListOfAvailableNewGames, 5000)
        })
    }

    function loadHistory() {
        doRpcCall('getHistory', {}, history => {
            setHistory(history)
        })
    }

    function createTabs() {
        return {
            "play": {
                label: "Play",
                render: renderAllGames
            },
            "archive": {
                label: "Archive",
                render: renderHistory
            }
        }
    }

    function onTabSelected(newTabKey) {
        if ("archive" === newTabKey) {
            setHistory(null)
            loadHistory()
        }
    }

    function startNewXoGame() {
        doRpcCall("createNewBackendState", {stateType:"XoGame"}, gameId => {
            openView(VIEW_URLS.xoGame({gameId}))
        })
    }

    function joinGame(gameType, gameId) {
        if (gameType === 'XoGame') {
            openView(VIEW_URLS.xoGame({gameId}))
        }
    }

    function renderTableWithAvailableNewGames() {
        if (availableNewGames) {
            if (availableNewGames.length == 0) {
                return 'No new games were created.'
            } else {
                return RE.Paper({},
                    RE.Table({},
                        RE.TableHead({},
                            RE.TableRow({},
                                RE.TableCell({}, 'Type'),
                                RE.TableCell({}, 'Title'),
                                RE.TableCell({}, 'Description'),
                                RE.TableCell({}, ''),
                                RE.TableCell({}, ''),
                            )
                        ),
                        RE.TableBody({},
                            availableNewGames.map(gameDto => RE.TableRow({key:gameDto.gameId},
                                RE.TableCell({}, gameDto.gameDisplayType),
                                RE.TableCell({}, gameDto.title),
                                RE.TableCell({}, gameDto.shortDescription),
                                RE.TableCell({}, gameDto.hasPasscode?RE.Icon({fontSize:'small'}, 'lock'):null),
                                RE.TableCell({}, RE.Button(
                                    {
                                        ...(gameDto.currUserIsOwner?({variant:"contained", color:"primary"}):{}),
                                        onClick: () => joinGame(gameDto.gameType, gameDto.gameId)
                                    },
                                    "Join"
                                )),
                            ))
                        )
                    )
                )
            }
        } else {
            return RE.CircularProgress()
        }
    }

    function renderTableWithHistory() {
        if (history) {
            if (history.length == 0) {
                return 'Nothing to show.'
            } else {
                return RE.Paper({},
                    RE.Table({},
                        RE.TableHead({},
                            RE.TableRow({},
                                RE.TableCell({}, ''),
                                RE.TableCell({}, 'Started at'),
                                RE.TableCell({}, 'Configuration'),
                                RE.TableCell({}, 'Players'),
                            )
                        ),
                        RE.TableBody({},
                            history.map(gameRecord => RE.TableRow({key:gameRecord.gameId},
                                RE.TableCell({},
                                    gameRecord.currUserIsWinner
                                        ?RE.Icon({fontSize:'small', style:{color:'gold'}}, 'emoji_events')
                                        :gameRecord.draw
                                        ?RE.Icon({fontSize:'small', style:{color:'grey', transform: 'rotate(90deg)'}}, 'pause')
                                        :null
                                ),
                                RE.TableCell({}, new Date(gameRecord.startedAt).toLocaleString()),
                                RE.TableCell({},
                                    `F ${gameRecord.fieldSize} / G ${gameRecord.goal}`
                                    + (gameRecord.secondsPerMove == null ? '' : ' / T ' + gameRecord.secondsPerMove)
                                ),
                                RE.TableCell({}, renderListOfPlayersFromGameRecord(gameRecord)),
                            ))
                        )
                    )
                )
            }
        } else {
            return RE.CircularProgress()
        }
    }

    function renderListOfPlayersFromGameRecord(gameRecord) {
        return RE.Fragment({},
            gameRecord.playerNames.map(playerName => RE.span(
                {key: playerName, style:{fontWeight: playerName == gameRecord.winnerName ? 'bold' : ''}},
                playerName + " "
            ))
        )
    }

    function renderButtonList() {
        return RE.Container.row.left.center({},{style:{marginBottom: '10px'}},
            RE.Button({variant:"contained", onClick: () => setOpenedDialog(() => NewXoGameDialog)}, 'New XO game')
        )
    }

    function renderAllGames() {
        return RE.Container.col.top.left({},{},
            renderButtonList(),
            renderTableWithAvailableNewGames(),
            openedDialog ? re(openedDialog, {openView, onCancel: () => setOpenedDialog(null)}) : null,
        )
    }

    function renderHistory() {
        return RE.Container.col.top.left({},{},
            history?.length?RE.span({style:{color:'red'}}, 'This history of games may be erased at any moment...'):null,
            renderTableWithHistory()
        )
    }

    return RE.Container.col.top.center({style:{marginTop:'100px'}},{},
        renderTabs()
    )
}
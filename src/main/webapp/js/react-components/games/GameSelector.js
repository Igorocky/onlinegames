"use strict";

const GameSelector = ({openView}) => {

    const LIST_NEW_GAMES = 'listNewGames';
    const LIST_IN_PROGRESS_GAMES = 'listInProgressGames';
    const loadGamesMethodName = useRef(LIST_NEW_GAMES)
    const [availableGames, setAvailableGames] = useState(null)
    const timerHandle = useRef(null)
    const [history, setHistory] = useState(null)

    const {renderTabs} = useTabs({tabs: createTabs(), onTabSelected})

    const [openedDialog, setOpenedDialog] = useState(null)

    useEffect(() => {
        loadListOfAvailableGames()
        return () => {
            stopLoadingAvailableGames()
        }
    }, [])

    function stopLoadingAvailableGames() {
        if (timerHandle.current) {
            window.clearTimeout(timerHandle.current)
        }
    }

    function loadListOfAvailableGames(dontSetTimer) {
        doRpcCall(loadGamesMethodName.current, {}, listOfGames => {
            setAvailableGames(listOfGames)
            if (!dontSetTimer) {
                timerHandle.current = window.setTimeout(loadListOfAvailableGames, 5000)
            }
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
                render: renderNewGames
            },
            "view": {
                label: "Watch",
                render: renderInProgressGames
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
        } else if ("play" === newTabKey) {
            setAvailableGames(null)
            loadGamesMethodName.current = LIST_NEW_GAMES
            loadListOfAvailableGames(true)
        } else if ("view" === newTabKey) {
            setAvailableGames(null)
            loadGamesMethodName.current = LIST_IN_PROGRESS_GAMES
            loadListOfAvailableGames(true)
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
        } else if (gameType === 'WordsGame') {
            openView(VIEW_URLS.wordsGame({gameId}))
        }
    }

    function renderTableWithAvailableGames() {
        if (availableGames) {
            if (availableGames.length == 0) {
                return 'No games to show.'
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
                            availableGames.map(gameDto => RE.TableRow({key:gameDto.gameId},
                                RE.TableCell({}, gameDto.gameDisplayType),
                                RE.TableCell({}, gameDto.title),
                                RE.TableCell({}, gameDto.shortDescription),
                                RE.TableCell({}, gameDto.hasPasscode?RE.Icon({fontSize:'small'}, 'lock'):null),
                                RE.TableCell({}, RE.Button(
                                    {
                                        ...(gameDto.currUserIsOwner?({variant:"contained", color:"primary"}):{}),
                                        onClick: () => joinGame(gameDto.gameType, gameDto.gameId)
                                    },
                                    gameDto.inProgress?"Watch":"Join"
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
                return 'No previous games found.'
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
        return RE.Container.row.left.center({},{style:{marginBottom: '10px', marginRight: '10px'}},
            RE.Button({variant:"contained", onClick: () => setOpenedDialog(() => NewXoGameDialog)}, 'New XO game'),
            RE.Button({variant:"contained", onClick: () => setOpenedDialog(() => NewWordsGameDialog)}, 'New WORDS game'),
        )
    }

    function renderNewGames() {
        return RE.Container.col.top.left({},{},
            renderButtonList(),
            renderTableWithAvailableGames(),
            openedDialog ? re(openedDialog, {openView, onCancel: () => setOpenedDialog(null)}) : null,
        )
    }

    function renderInProgressGames() {
        return RE.Container.col.top.left({},{},
            renderTableWithAvailableGames()
        )
    }

    function renderHistory() {
        return RE.Container.col.top.left({},{},
            history?.length?RE.span({style:{color:'red'}}, 'This history of games may have been erased at any moment...'):null,
            renderTableWithHistory()
        )
    }

    return RE.Container.col.top.center({style:{marginTop:'100px'}},{},
        renderTabs()
    )
}
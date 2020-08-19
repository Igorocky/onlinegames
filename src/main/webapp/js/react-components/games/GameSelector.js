"use strict";

const GameSelector = ({openView}) => {

    const [availableNewGames, setAvailableNewGames] = useState(null)
    const timerHandle = useRef(null)

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
                                RE.TableCell({}, ''),
                            )
                        ),
                        RE.TableBody({},
                            availableNewGames.map(gameDto => RE.TableRow({key:gameDto.gameId},
                                RE.TableCell({}, gameDto.gameDisplayType),
                                RE.TableCell({}, RE.Button({onClick: () => joinGame(gameDto.gameType, gameDto.gameId)}, "Join")),
                            ))
                        )
                    )
                )
            }
        } else {
            return RE.CircularProgress()
        }
    }

    function renderButtonList() {
        return RE.Container.row.left.center({},{style:{marginBottom: '10px'}},
            RE.Button({variant:"contained", onClick: () => setOpenedDialog(() => NewXoGameDialog)}, 'New XO game')
        )
    }

    return RE.Container.col.top.center({style:{marginTop:'100px'}},{},
        RE.Container.col.top.left({},{},
            renderButtonList(),
            renderTableWithAvailableNewGames(),
            openedDialog ? re(openedDialog, {openView, onCancel: () => setOpenedDialog(null)}) : null,
        )
    )
}
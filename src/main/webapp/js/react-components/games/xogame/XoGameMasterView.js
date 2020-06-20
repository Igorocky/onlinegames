"use strict";

const XoGameMasterView = ({openView, gameId}) => {
    const backend = useBackend({stateId:gameId, onMessageFromBackend})
    const [beState, setBeState] = useState(null)

    useEffect(() => backend.send("getCurrentState"), [])

    function onMessageFromBackend(msg) {
        if (msg.type && msg.type == "state") {
            setBeState(msg)
        }
    }

    function joinGame(joinId) {
        openView(VIEW_URLS.xoGame({joinId}))
    }

    function renderJoinUrlElems(player) {
        return RE.Fragment({},
            player.symbol,
            RE.Button({style:{marginLeft:"20px"}, onClick: () => joinGame(player.joinId)}, "Join")
        )
    }

    function renderJoinUrls() {
        return RE.Container.col.top.center({style:{marginTop:"100px"}},{},
            RE.Container.col.top.left({},{},
                beState.players.map(player => RE.Container.row.center.center({},{},
                    renderJoinUrlElems(player)
                ))
            )
        )
    }

    if (beState) {
        return renderJoinUrls()
    } else {
        return RE.CircularProgress({})
    }
}
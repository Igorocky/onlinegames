"use strict";

const XoGameView = ({openView}) => {
    const query = useQuery()
    const gameId = query.get("gameId")
    const joinId = query.get("joinId")

    if (gameId) {
        return re(XoGameMasterView, {openView, gameId})
    } else if (joinId) {
        return re(XoGamePlayerView, {openView, joinId})
    } else {
        return "Incorrect URL"
    }
}
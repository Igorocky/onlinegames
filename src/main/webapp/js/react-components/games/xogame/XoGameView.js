"use strict";

const XoGameView = ({}) => {
    const query = useQuery()
    const gameId = query.get("gameId")
    const joinId = query.get("joinId")

    if (gameId) {
        return re(XoGameMasterView, {gameId})
    } else if (joinId) {
        return re(XoGamePlayerView, {joinId})
    } else {
        return "Incorrect URL"
    }
}
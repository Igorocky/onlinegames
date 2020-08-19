"use strict";

const XoGameTableOfPlayersComponent = ({players, currentPlayerId, playerIdToMove}) => {
    const symbolSize = "20px"

    return RE.table({style:{marginRight:'15px'}},
        RE.tbody({},
            players.map(player => RE.tr({key:player.playerId},
                RE.td({},
                    player.playerId==playerIdToMove
                    ?RE.img({src:`/img/xogame/player-to-move-arrow.svg`, style: {maxWidth:symbolSize, maxHeight:symbolSize}})
                    :null
                ),
                RE.td({}, RE.img({src:`/img/xogame/${player.symbol}-symbol.svg`, style: {width:symbolSize, height:symbolSize}})),
                RE.td({}, player.playerId==currentPlayerId?'You':''),
            ))
        )
    )
}
"use strict";

const XoGameTableOfPlayersComponent = ({players, currentPlayerId, playerIdToMove, timerSeconds}) => {
    const symbolSize = "20px"

    return RE.table({style:{marginRight:'15px'}},
        RE.tbody({},
            players.map(player => RE.tr({key:player.playerId},
                RE.td({style:{width: '20px'}},
                    (timerSeconds && player.playerId==playerIdToMove)
                    ?re(XoGameTimerComponent, {timerSeconds})
                    :null
                ),
                RE.td({},
                    player.playerId==playerIdToMove
                    ?RE.img({src:`/img/xogame/player-to-move-arrow.svg`, style: {maxWidth:symbolSize, maxHeight:symbolSize}})
                    :null
                ),
                RE.td({}, RE.img({src:`/img/xogame/${player.symbol}-symbol.svg`, style: {width:symbolSize, height:symbolSize}})),
                RE.td(
                    {style: player.playerId==currentPlayerId ? {textDecoration: 'underline', fontWeight: 'bold'}:{}},
                    hasValue(player.name)?player.name:'Incognito'
                ),
            ))
        )
    )
}
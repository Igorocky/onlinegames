"use strict";

const GameSelector = ({}) => {

    function startNewGame(nameOfGame) {
        doRpcCall("createNewBackendState", {stateType:nameOfGame}, gameId => {
            console.log("gameId = " + JSON.stringify(gameId))
        })
    }

    return RE.Container.col.top.center({style:{marginTop:"100px"}},{},
        RE.Button({onClick: () => startNewGame("XoGame")}, "Start new XO game")
    )
}
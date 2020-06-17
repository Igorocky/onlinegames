"use strict";

const GameSelector = ({}) => {

    function startNewGame(gameId) {

    }

    return RE.Container.col.top.center({style:{marginTop:"100px"}},{},
        RE.Button({onClick: () => startNewGame("XoGame")}, "Start new XO game")
    )
}
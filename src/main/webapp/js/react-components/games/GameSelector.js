"use strict";

const GameSelector = ({redirect}) => {

    function startNewXoGame() {
        doRpcCall("createNewBackendState", {stateType:"XoGame"}, gameId => {
            redirect(PATH.xoGame({gameId}))
        })
    }

    return RE.Container.col.top.center({style:{marginTop:"100px"}},{},
        RE.Button({onClick: startNewXoGame}, "Start new XO game")
    )
}
"use strict";

const GameSelector = ({openView}) => {

    function startNewXoGame() {
        doRpcCall("createNewBackendState", {stateType:"XoGame"}, gameId => {
            openView(VIEW_URLS.xoGame({gameId}))
        })
    }

    return RE.Container.col.top.center({style:{marginTop:"100px"}},{},
        RE.Button({onClick: startNewXoGame}, "Start new XO game")
    )
}
"use strict";

const NewXoGameDialog = ({openView, onCancel}) => {

    const [fieldSize, setFieldSize] = useState(8)

    function createNewXoGame() {
        doRpcCall("createNewBackendState", {stateType:"XoGame", initParams:{fieldSize}}, gameId => {
            openView(VIEW_URLS.xoGame({gameId}))
        })
    }

    return RE.Dialog({open: true},
        RE.DialogTitle({}, 'New XO game'),
        RE.DialogContent({dividers:true},
            RE.table({},
                RE.tbody({},
                    RE.tr({},
                        RE.td({},
                            RE.FormControl({variant:'outlined'},
                                RE.InputLabel({}, 'Field size'),
                                RE.Select(
                                    {
                                        value: fieldSize,
                                        label:'Field size',
                                        onChange: event => setFieldSize(event.target.value),
                                        style: {width:'200px'}
                                    },
                                    ints(3, 16).map(i => RE.MenuItem({key: i, value: i}, i))
                                )
                            )
                        )
                    )
                )
            )
        ),
        RE.DialogActions({},
            RE.Button({color:'primary', onClick: onCancel}, 'Cancel'),
            RE.Button({variant:"contained", color:'primary', onClick: createNewXoGame}, 'Create'),
        ),
    )
}
"use strict";

const NewXoGameDialog = ({openView, onCancel}) => {

    const [fieldSize, setFieldSize] = useState(8)
    const [title, setTitle] = useState(null)
    const [passcode, setPasscode] = useState(null)

    function createNewXoGame() {
        doRpcCall(
            "createNewBackendState",
            {stateType: "XoGame", initParams: {fieldSize, title, passcode}},
            gameId => {
                openView(VIEW_URLS.xoGame({gameId}))
            }
        )
    }

    const tdStyle = {padding:'10px'}
    const inputElemsWidth = '200px';
    return RE.Dialog({open: true},
        RE.DialogTitle({}, 'New XO game'),
        RE.DialogContent({dividers:true},
            RE.table({},
                RE.tbody({},
                    RE.tr({},
                        RE.td({style: tdStyle},
                            RE.FormControl({variant:'outlined'},
                                RE.InputLabel({}, 'Field size'),
                                RE.Select(
                                    {
                                        value: fieldSize,
                                        label:'Field size',
                                        onChange: event => setFieldSize(event.target.value),
                                        style: {width: inputElemsWidth}
                                    },
                                    ints(3, 16).map(i => RE.MenuItem({key: i, value: i}, i))
                                )
                            )
                        )
                    ),
                    RE.tr({},
                        RE.td({style: tdStyle},
                            RE.TextField(
                                {
                                    variant: 'outlined', label: 'Title (optional)',
                                    style: {width: inputElemsWidth},
                                    onChange: event => setTitle(event.target.value),
                                },
                                title
                            )
                        )
                    ),
                    RE.tr({},
                        RE.td({style: tdStyle},
                            RE.TextField(
                                {
                                    variant: 'outlined', label: 'Passcode (optional)',
                                    style: {width: inputElemsWidth},
                                    onChange: event => setPasscode(event.target.value),
                                },
                                passcode
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
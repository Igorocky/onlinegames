"use strict";

const STATE_INFO_DTO = {
    stateId: "stateId",
    stateType: "stateType",
    createdAt: "createdAt",
    lastInMsgAt: "lastInMsgAt",
    lastOutMsgAt: "lastOutMsgAt",
    viewRepresentation: "viewRepresentation",
}

const AdminView = ({}) => {

    const TAB_BE_STATES = "TAB_BE_STATES"
    const TAB_OTHER = "TAB_OTHER"
    const [beStates, setBeStates] = useState(null)

    const tabs = {
        [TAB_BE_STATES]: {label: "BE States", render: renderBeStates},
        [TAB_OTHER]: {label: "Other", render: () => "Other"},
    }

    const {renderTabs} = useTabs({onTabSelected, tabs})

    useEffect(() => {
        loadBeStates()
    }, [])

    function onTabSelected(selectedTabKey) {
        if (selectedTabKey == TAB_BE_STATES) {
            loadBeStates()
        } else {
            setBeStates(null)
        }
    }

    function loadBeStates() {
        doRpcCall("listBeStates", {}, beStatesResponse => {
            setBeStates(beStatesResponse)
        })
    }

    function renderBeStates() {
        if (!beStates) {
            return RE.CircularProgress({})
        } else {
            return RE.Paper({}, RE.Table({size:"small"},
                RE.TableHead({},
                    RE.TableRow({},
                        RE.TableCell({}, "ID"),
                        RE.TableCell({}, "TYPE"),
                        RE.TableCell({}, "CREATED AT"),
                        RE.TableCell({}, "IN MSG"),
                        RE.TableCell({}, "OUT MSG"),
                        RE.TableCell({}, "STATE"),
                    )
                ),
                RE.TableBody({},
                    beStates.map((beState,idx) => RE.TableRow({key:beState[STATE_INFO_DTO.stateId], className:"grey-background-on-hover"},
                        RE.TableCell({}, beState[STATE_INFO_DTO.stateId]),
                        RE.TableCell({}, beState[STATE_INFO_DTO.stateType]),
                        RE.TableCell({}, beState[STATE_INFO_DTO.createdAt]),
                        RE.TableCell({}, beState[STATE_INFO_DTO.lastInMsgAt]),
                        RE.TableCell({}, beState[STATE_INFO_DTO.lastOutMsgAt]),
                        RE.TableCell({}, RE.div({style:{maxWidth:"300px", maxHeight:"100px", overflow:"scroll"}},
                            JSON.stringify(beState[STATE_INFO_DTO.viewRepresentation])
                        )),
                    ))
                )
            ))
        }
    }

    return renderTabs()
}
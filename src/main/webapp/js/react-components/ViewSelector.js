const VIEWS = [
    {name:"Nodes",
        component: NodeCommonView, path: [PATH.node, PATH.nodeWithId]},
    {name:"Puzzles To Repeat",
        component: PuzzlesToRepeatReport, path: [PATH.puzzlesToRepeat, PATH.puzzlesToRepeatWithTab]},
    {name:"Chessboard",
        component: ChessComponent, props:{showPracticeTab:true, setPageTitle: true},
        path: [PATH.chessboard, PATH.chessboardWithPractice, PATH.chessboardWithFen]},
    {name:"Chessboard-M",
        component: ChessComponentM,
        path: [PATH.chessboardComponentM]},
    {name:"ChessPuzzlesAudio",
        component: ChessManagerAudioView,
        path: [PATH.ChessManagerAudio]},
    {name:"Admin",
        component: AdminView, path: [PATH.admin]},
    {name:"BE States",
        component: BeStatesView, path: [PATH.beStates]},
    {name:"ChessComponentM", showOnDrawer: false,
        component: ChessComponentM, path: [PATH.chessboardComponentM]},
]

const ViewSelector = ({}) => {
    const [sideMenuIsOpen, setSideMenuIsOpen] = useState(false)
    const [redirect, setRedirect] = useState(null)
    const actionsContainerRef = React.useRef(null)

    function isTabOrShift(event) {
        return event.type === 'keydown' && (event.key === 'Tab' || event.key === 'Shift')
    }

    function openSideMenu(event) {
        if (isTabOrShift(event)) {
            return
        }
        setSideMenuIsOpen(true)
    }

    function closeSideMenu(event) {
        if (isTabOrShift(event)) {
            return
        }
        setSideMenuIsOpen(false)
    }

    function renderAppBar() {
        return RE.AppBar({position: "static"},
            RE.Toolbar({variant: "dense", ref:actionsContainerRef},
                RE.IconButton({edge: "start", color: "inherit", onClick: openSideMenu},
                    RE.Icon({style: {fontSize: "24px"}}, "menu")
                )
            )
        )
    }

    function renderDrawer() {
        return RE.Drawer({
                open: sideMenuIsOpen,
                onClick: closeSideMenu,
                onKeyDown: closeSideMenu
            },
            RE.List({},
                VIEWS.filter(v => !hasValue(v.showOnDrawer) || v.showOnDrawer).map((view,idx) =>
                    RE.ListItem({
                            button:true,
                            key:idx,
                            onMouseUp: event => {
                                setSideMenuIsOpen(false)
                                link(setRedirect, VIEWS[idx].path[0]).onMouseUp(event)
                            }
                        },
                        RE.ListItemText({},view.name)
                    )
                ),
                RE.ListItem({
                        button:true,
                        key:"Logout-button",
                        onClick: () => window.location.href = PATH.logout
                    },
                    RE.ListItemText({},"Logout")
                )
            )
        )
    }

    function getViewRoutes() {
        return VIEWS.map(view => re(Route, {
            key: view.path[0],
            path: view.path,
            exact: true,
            render: props => re(view.component, {
                ...props,
                ...(view.props?view.props:{}),
                actionsContainerRef: actionsContainerRef,
                redirect: path => setRedirect(path),
                createLink: url => link(setRedirect, url)
            })
        }))
    }

    function redirectTo(to) {
        return to ? re(Redirect,{key: to, to: to}) : null
    }

    if (!redirect) {
        const newRedirect = window.location.pathname + window.location.search;
        setRedirect(newRedirect)
        return redirectTo(newRedirect)
    } else {
        return re(BrowserRouter, {},
            renderAppBar(),
            renderDrawer(),
            re(Switch, {}, ...getViewRoutes()),
            redirectTo(redirect)
        )
    }
}
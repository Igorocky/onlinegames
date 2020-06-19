"use strict";

const FE_CONTEXT_PATH = "/fe"
const XO_GAME_BASE_PATH = FE_CONTEXT_PATH + "/xogame"

const PATH = {
    stateWebSocketUrl: "/be/websocket/state",
    admin: FE_CONTEXT_PATH + "/admin",
    gameSelector: FE_CONTEXT_PATH + "/newgame",
    xoGame: ({gameId,joinId}) => XO_GAME_BASE_PATH + "?"
        + (gameId?("gameId="+gameId):"")
        + (joinId?("joinId="+joinId):""),
}

const VIEWS = [
    {name:"AdminPage", component: AdminView, path: PATH.admin},
    {name:"GameSelector", component: GameSelector, path: PATH.gameSelector},
    {name:"XoGame", component: XoGameView, path: XO_GAME_BASE_PATH},
]

const ViewSelector = ({}) => {
    const [redirect, setRedirect] = useState(null)

    function getViewRoutes() {
        return VIEWS.map(view => re(Route, {
            key: view.path,
            path: view.path,
            exact: true,
            render: props => re(view.component, {
                ...props,
                ...(view.props?view.props:{}),
                redirect: path => setRedirect(path),
                createLink: url => link(setRedirect, url)
            })
        }))
    }

    function redirectTo(to) {
        return to ? re(Redirect,{key: to, to: to}) : null
    }

    if (redirect) {
        return re(BrowserRouter, {},
            re(Switch, {}, getViewRoutes()),
            redirectTo(redirect)
        )
    } else {
        const newRedirect = window.location.pathname + window.location.search
        setRedirect(newRedirect)
        return redirectTo(newRedirect)
    }
}
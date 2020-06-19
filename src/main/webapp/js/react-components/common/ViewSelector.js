"use strict";

const FE_CONTEXT_PATH = "/fe"
const XO_GAME_BASE_PATH = FE_CONTEXT_PATH + "/xogame"

const VIEW_URLS = {
    admin: FE_CONTEXT_PATH + "/admin",
    gameSelector: FE_CONTEXT_PATH + "/newgame",
    xoGame: ({gameId,joinId}) => XO_GAME_BASE_PATH + "?"
        + (gameId?("gameId="+gameId):"")
        + (joinId?("joinId="+joinId):""),
}

const VIEWS = [
    {name:"AdminPage", component: AdminView, path: VIEW_URLS.admin},
    {name:"GameSelector", component: GameSelector, path: VIEW_URLS.gameSelector},
    {name:"XoGame", component: XoGameView, path: XO_GAME_BASE_PATH},
]

const ViewSelector = ({}) => {
    const [currentViewUrl, setCurrentViewUrl] = useState(null)

    function getViewRoutes() {
        return VIEWS.map(view => re(Route, {
            key: view.path,
            path: view.path,
            exact: true,
            render: props => re(view.component, {
                ...props,
                ...(view.props?view.props:{}),
                openView: url => setCurrentViewUrl(url),
                createLink: url => link(setCurrentViewUrl, url)
            })
        }))
    }

    function renderRedirectElem(url) {
        return url ? re(Redirect,{key: url, to: url}) : null
    }

    if (currentViewUrl) {
        return re(BrowserRouter, {},
            re(Switch, {}, getViewRoutes()),
            renderRedirectElem(currentViewUrl)
        )
    } else {
        const newViewUrl = window.location.pathname + window.location.search
        setCurrentViewUrl(newViewUrl)
        return renderRedirectElem(newViewUrl)
    }
}
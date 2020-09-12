"use strict";

const FE_CONTEXT_PATH = "/fe"
const XO_GAME_BASE_PATH = FE_CONTEXT_PATH + "/xogame"
const WORDS_GAME_BASE_PATH = FE_CONTEXT_PATH + "/wordsgame"

const VIEW_URLS = {
    admin: FE_CONTEXT_PATH + "/admin",
    gameSelector: FE_CONTEXT_PATH + "/newgame",
    xoGame: ({gameId}) => XO_GAME_BASE_PATH + "?"
        + (gameId?("gameId="+gameId):""),
    wordsGame: ({gameId}) => WORDS_GAME_BASE_PATH + "?"
        + (gameId?("gameId="+gameId):""),
}

const VIEWS = [
    {name:"AdminPage", component: AdminView, path: VIEW_URLS.admin},
    {name:"GameSelector", component: GameSelector, path: VIEW_URLS.gameSelector},
    {name:"XoGame", component: XoGamePlayerView, path: XO_GAME_BASE_PATH},
    {name:"WordsGame", component: WordsGamePlayerView, path: WORDS_GAME_BASE_PATH},
]

function getViewAbsoluteUrl(relUrl) {
    const location = window.location
    return location.protocol + "//" + location.host + relUrl
}

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
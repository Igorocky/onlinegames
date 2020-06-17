const VIEWS = [
    {name:"GameSelector", component: GameSelector, props:{}, path: PATH.gameSelector},
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
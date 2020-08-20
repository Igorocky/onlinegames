"use strict";

const XoGameTimerComponent = ({timerSeconds}) => {
    const startedAt = useMemo(() => new Date().getTime())
    const [seconds, setSeconds] = useState(timerSeconds)
    const timeoutHandle = useRef(null)

    useEffect(() => {
        updateSeconds()
        return () => {
            if (timeoutHandle.current != null) {
                window.clearTimeout(timeoutHandle.current)
            }
        }
    }, [])

    function updateSeconds() {
        const newSeconds = timerSeconds - (new Date().getTime() - startedAt)/1000
        setSeconds(newSeconds > 0 ? Math.floor(newSeconds) : 0)
        if (newSeconds > 0) {
            timeoutHandle.current = window.setTimeout(
                updateSeconds,
                1000
            )
        }
    }

    return RE.span({}, seconds)
}
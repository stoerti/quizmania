import React, {useState} from 'react';

import {useSnackbar} from 'material-ui-snackbar-provider'
import Cookies from 'js-cookie';

import LoginPage from "./Login";
import GameSelectionPage from "./GameSelectionPage";
import GamePage from "./game/GamePage";

enum MainPageState {
    LOGIN,
    GAME_SELECTION,
    IN_GAME
}

const QuizmaniaMainUI = () => {
    const [username, setUsername] = useState<string | undefined>(undefined);
    const [gameId, setGameId] = useState<string | undefined>(undefined);
    const [mainPageState, setMainPageState] = useState(MainPageState.LOGIN)

    const snackbar = useSnackbar()

    if (mainPageState === MainPageState.LOGIN
        && Cookies.get('username') && username === undefined) {
        setUsername(Cookies.get('username'))

        if (Cookies.get('gameId') && gameId === undefined) {
            setGameId(Cookies.get('gameId'))
            setMainPageState(MainPageState.IN_GAME)
        } else {
            setMainPageState(MainPageState.GAME_SELECTION)
        }
    }

    const onLoginSuccess = (username: string) => {
        Cookies.set('username', username, {expires: 7});
        setUsername(username)
        setMainPageState(MainPageState.GAME_SELECTION)
        snackbar.showMessage(
            'Username: ' + username
        )
    }

    const onGameSelected = (gameId: string) => {
        setGameId(gameId)
        setMainPageState(MainPageState.IN_GAME)
        Cookies.set('gameId', gameId, {expires: 7});
    }

    const onGameEnded = () => {
        setGameId(undefined)
        setMainPageState(MainPageState.GAME_SELECTION)
        Cookies.remove('gameId');
    }

    let page;
    if (mainPageState === MainPageState.LOGIN) {
        page = <LoginPage loginSuccessAction={onLoginSuccess}/>
    } else if (mainPageState === MainPageState.GAME_SELECTION) {
        page = <GameSelectionPage onGameSelected={onGameSelected}/>
    } else if (mainPageState === MainPageState.IN_GAME) {
        page = <GamePage gameId={gameId!} onGameEnded={onGameEnded}/>
    } else {
        page = <div>Unknown state {mainPageState}</div>
    }

    return (
        <div>
            {page}
        </div>
    )
}

export default QuizmaniaMainUI;

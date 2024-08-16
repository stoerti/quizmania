import React, {useCallback, useState} from 'react';

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

  const onLoginSuccess = useCallback((username: string) => {
    Cookies.set('username', username, {expires: 1});
    setUsername(username)
    setMainPageState(MainPageState.GAME_SELECTION)
    snackbar.showMessage(
      'Username: ' + username
    )
  }, [snackbar])

  const onLogout = useCallback(() => {
    Cookies.remove('username');
    setUsername(undefined)
    setMainPageState(MainPageState.LOGIN)
  }, [])

  const onGameSelected = useCallback((gameId: string) => {
    setGameId(gameId)
    setMainPageState(MainPageState.IN_GAME)
    Cookies.set('gameId', gameId, {expires: 1});
  }, [])

  const onGameEnded = useCallback(() => {
    setGameId(undefined)
    setMainPageState(MainPageState.GAME_SELECTION)
    Cookies.remove('gameId');
  }, [])

  if (mainPageState === MainPageState.LOGIN) {
    return <LoginPage loginSuccessAction={onLoginSuccess}/>
  }
  if (mainPageState === MainPageState.GAME_SELECTION) {
    return <GameSelectionPage onGameSelected={onGameSelected} onLogout={onLogout}/>
  }
  if (mainPageState === MainPageState.IN_GAME) {
    return <GamePage gameId={gameId!} onGameEnded={onGameEnded}/>
  }
  return <div>Unknown state {mainPageState}</div>
}

export default QuizmaniaMainUI;

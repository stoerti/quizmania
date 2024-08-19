import React, {useEffect, useMemo} from "react";
import {GameEvent, PlayerLeftGameEvent} from "../../services/GameEventTypes";
import {useSnackbar} from "material-ui-snackbar-provider";
import Cookies from "js-cookie";
import {GameLobbyPage} from "./GameLobby";
import {GameRoomPage} from "./GameRoom";
import {GameFinishedPage} from "./GameFinished";
import {GameEventType, GameRepository} from "../../services/GameRepository";
import {Game, GameStatus} from "../../domain/GameModel";

type GamePageProps = {
  gameId: string,
  onGameEnded(): void
}

const GamePage = ({gameId, onGameEnded}: GamePageProps) => {

  const [game, setGame] = React.useState<Game | undefined>(undefined)

  const gameRepository = useMemo(() =>
      new GameRepository()
    , []);
  const snackbar = useSnackbar()

  useEffect(() => {
    gameRepository.findGame(gameId, setGame, () => {
      gameRepository.unsubscribeFromGame()
      onGameEnded()
    })
    gameRepository.subscribeToGame(gameId, {
      onGameEvent(event: GameEvent, eventType: GameEventType, game: Game) {
        if (eventType === 'GameCanceledEvent') {
          gameRepository.unsubscribeFromGame()
          onGameEnded()
          snackbar.showMessage("Game was canceled by creator or system")
        } else if (eventType === 'PlayerLeftGameEvent') {
          if ((event as PlayerLeftGameEvent).username === Cookies.get('username')) {
            // I left the game - return to game selection page
            gameRepository.unsubscribeFromGame()
            onGameEnded()
          }
        }
        setGame(game)
      },
    })
  }, [gameId, gameRepository, onGameEnded, snackbar]);

  if (game === undefined) {
    return <div>Loading...</div>
  }
  if (game.status === GameStatus.CREATED) {
    return <GameLobbyPage game={game}/>
  }
  if (game.status === GameStatus.STARTED) {
    return <GameRoomPage game={game} onLeaveGame={() => {
      gameRepository.unsubscribeFromGame();
      onGameEnded();
    }}/>
  }
  if (game.status === GameStatus.ENDED) {
    return <GameFinishedPage game={game} onClickLeaveGame={() => {
      gameRepository.unsubscribeFromGame();
      onGameEnded();
    }}/>
  }
  if (game.status === GameStatus.CANCELED) {
    gameRepository.unsubscribeFromGame()
    onGameEnded()
    return <div>Game was cancelled</div>
  }

  return <div>Unknown game state {game.status}</div>
}

export default GamePage

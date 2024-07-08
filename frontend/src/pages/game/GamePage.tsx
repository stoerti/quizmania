import React, {useEffect} from "react";
import {UserRemovedEvent} from "../../services/GameEventTypes";
import {useSnackbar} from "material-ui-snackbar-provider";
import Cookies from "js-cookie";
import {GameLobbyPage} from "./GameLobby";
import {GameRoomPage} from "./GameRoom";
import {GameCommandService} from "../../services/GameCommandService";
import {GameFinishedPage} from "./GameFinished";
import {GameEventType, GameRepository} from "../../services/GameRepository";
import {Game, GameStatus} from "../../domain/GameModel";
import {GameEvent} from "../../services/GameEventTypes";

type GamePageProps = {
  gameId: string,
  onGameEnded(): void
}

const GamePage = (props: GamePageProps) => {

  const [game, setGame] = React.useState<Game | undefined>(undefined)

  const gameRepository = new GameRepository()
  const gameCommandService = new GameCommandService()
  const snackbar = useSnackbar()

  useEffect(() => {
    gameRepository.findGame(props.gameId, setGame, () => {
      gameRepository.unsubscribeFromGame()
      props.onGameEnded()
    })
    gameRepository.subscribeToGame(props.gameId, {
      onGameEvent(event: GameEvent, eventType: GameEventType, game: Game) {
        if (eventType == 'GameCanceledEvent') {
          gameRepository.unsubscribeFromGame()
          props.onGameEnded()
          snackbar.showMessage("Game was canceled by creator or system")
        } else if (eventType == 'UserRemovedEvent') {
          if ((event as UserRemovedEvent).username === Cookies.get('username')) {
            // I left the game - return to game selection page
            gameRepository.unsubscribeFromGame()
            props.onGameEnded()
          }
        }
        setGame(game)
      },
    })
  }, []);

  let page
  if (game === undefined) {
    page = <div>Loading...</div>
  } else if (game.status === GameStatus.CREATED) {
    page = <GameLobbyPage game={game} gameSCommandervice={gameCommandService}/>
  } else if (game.status === GameStatus.STARTED) {
    page = <GameRoomPage game={game} gameCommandService={gameCommandService}/>
  } else if (game.status === GameStatus.ENDED) {
    page = <GameFinishedPage game={game} gameCommandService={gameCommandService} onClickLeaveGame={() => {
      gameRepository.unsubscribeFromGame();
      props.onGameEnded();
    }}/>
  } else if (game.status === GameStatus.CANCELED) {
    gameRepository.unsubscribeFromGame()
    props.onGameEnded()
    page = <div>Game was cancelled</div>
  } else {
    page = <div>Unknown game state {game.status}</div>
  }

  return (
    <div>
      {page}
    </div>
  )
}

export default GamePage

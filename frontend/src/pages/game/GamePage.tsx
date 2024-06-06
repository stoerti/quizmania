import React, {useEffect} from "react";
import {GameDto, GameStartedEvent, GameStatus, UserRemovedEvent} from "../../services/GameServiceTypes";
import {useSnackbar} from "material-ui-snackbar-provider";
import Cookies from "js-cookie";
import {GameLobbyPage} from "./GameLobby";
import {GameRoomPage} from "./GameRoom";
import {GameService} from "../../services/GameService";
import {GameFinishedPage} from "./GameFinished";

type GamePageProps = {
  gameId: string,
  onGameEnded(): void
}

const GamePage = (props: GamePageProps) => {

  const [game, setGame] = React.useState<GameDto | undefined>(undefined)

  const gameService = new GameService()
  const snackbar = useSnackbar()

  useEffect(() => {
    gameService.findGame(props.gameId, setGame, () => {
      gameService.unsubscribeFromGame()
      props.onGameEnded()
    })
    gameService.subscribeToGame(props.gameId, {
      onGameStarted(event: GameStartedEvent) {
        setGame(event.game)
      },
      onGameEnded(event, game) {
        setGame(game)
      },
      onGameCanceled(event, game) {
        gameService.unsubscribeFromGame()
        props.onGameEnded()
        snackbar.showMessage("Game was canceled by creator or system")
      },
      onUserAdded(event, game) {
        setGame(game)
      },
      onUserRemoved(event: UserRemovedEvent) {
        if (event.username === Cookies.get('username')) {
          // I left the game - return to game selection page
          gameService.unsubscribeFromGame()
          props.onGameEnded()
        } else {
          // someone else left the game - remove user
          setGame({
            ...game!,
            users: game!.users
              .filter((user, index) => {
                return user.id !== event.gameUserId
              })
              .map((user) => {
                return {...user}
              })
          })
        }
      },
      onQuestionAsked(event, game) {
        setGame(game)
      },
      onQuestionAnswered(event, game) {
        setGame(game)
      },
      onQuestionAnswerOverriddenEvent(event, game) {
        setGame(game)
      },
      onQuestionClosed(event, game) {
        setGame(game)
      },
      onQuestionRated(event, game) {
        setGame(game)
      }
    })
  }, []);

  let page
  if (game === undefined) {
    page = <div>Loading...</div>
  } else if (game.status === GameStatus.CREATED) {
    page = <GameLobbyPage game={game} gameService={gameService}/>
  } else if (game.status === GameStatus.STARTED) {
    page = <GameRoomPage game={game} gameService={gameService}/>
  } else if (game.status === GameStatus.ENDED) {
    page = <GameFinishedPage game={game} gameService={gameService} onClickLeaveGame={() => {
      gameService.unsubscribeFromGame();
      props.onGameEnded();
    }}/>
  } else if (game.status === GameStatus.CANCELED) {
    gameService.unsubscribeFromGame()
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

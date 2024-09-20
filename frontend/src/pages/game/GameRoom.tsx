import {Game} from "../../domain/GameModel";
import React from "react";
import {AppBar, Box, IconButton, Toolbar, Tooltip, Typography} from "@mui/material";
import {ModeratorGameRoomPanel} from "./gameroom/ModeratorGameRoomPanel";
import {PlayerGameRoomPanel} from "./gameroom/PlayerGameRoomPanel";
import {SpectatorGameRoomPanel} from "./gameroom/SpectatorGameRoomPanel.tsx";
import Logout from "@mui/icons-material/Logout";
import {gameCommandService, GameException} from "../../services/GameCommandService.tsx";
import {useSnackbar} from "material-ui-snackbar-provider";
import {useUsername} from "../../hooks/useUsername.ts";


export type GameRoomPageProps = {
  game: Game
  onLeaveGame: () => void
}

export const GameRoomPage = ({game, onLeaveGame}: GameRoomPageProps) => {
  const snackbar = useSnackbar()
  const {username} = useUsername()

  const currentPlayer = game.players.find(player => player.name === username)

  let container
  let onClickLeaveGame
  if (game.moderator == username) {
    container = <ModeratorGameRoomPanel game={game}/>
    onClickLeaveGame = async () => {
      try {
        await gameCommandService.leaveGame(game.id)
      } catch (error) {
        if (error instanceof GameException) {
          snackbar.showMessage(error.message)
        }
      }
    }
  } else if (!currentPlayer) {
    container = <SpectatorGameRoomPanel game={game}/>
    onClickLeaveGame = onLeaveGame
  } else {
    container = <PlayerGameRoomPanel game={game} player={currentPlayer}/>
    onClickLeaveGame = async () => {
      try {
        await gameCommandService.leaveGame(game.id)
      } catch (error) {
        if (error instanceof GameException) {
          snackbar.showMessage(error.message)
        }
      }
    }
  }

  return (
    <div>
      <AppBar position="static">
        <Toolbar>
          <Typography sx={{flex: '1 1 100%'}} variant="h6" component="div">
            {game.name}{game.currentQuestion !== undefined ? "  -  Question " + game.currentQuestion.gameQuestionNumber + "/" + game.config.numQuestions : null}
          </Typography>
          <Tooltip title="Leave game">
            <IconButton color="inherit" onClick={onClickLeaveGame}>
              <Logout/>
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Box sx={{
        display: "flex",
        justifyContent: "center",
        margin: 2
      }}>{container}</Box>
      {currentPlayer !== undefined ?
      <AppBar position="fixed" sx={{top: 'auto', bottom: 0}}>
        <Toolbar variant={"dense"}>
          <Typography sx={{flex: '1 1 100%'}} variant="body1" component="div">
            {currentPlayer.name}
          </Typography>
          <Typography sx={{flex: '1 1 100%', textAlign: 'right'}} variant="body1" component="div">
            {'Points: ' + currentPlayer.points}
          </Typography>
        </Toolbar>
      </AppBar> : null }
    </div>
  )
}

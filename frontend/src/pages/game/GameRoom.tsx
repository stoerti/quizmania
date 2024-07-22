import {Game, GameQuestion} from "../../domain/GameModel";
import React from "react";
import Cookies from "js-cookie";
import {AppBar, Box, Toolbar, Typography} from "@mui/material";
import {ModeratorGameRoomPanel} from "./gameroom/ModeratorGameRoomPanel";
import {PlayerGameRoomPanel} from "./gameroom/PlayerGameRoomPanel";


export type GameRoomPageProps = {
  game: Game
}

const findCurrentQuestion = (game: Game): GameQuestion | undefined => {
  return [...game.questions].sort((q1, q2) => q2.gameQuestionNumber - q1.gameQuestionNumber)[0]
}

export const GameRoomPage = ({game}: GameRoomPageProps) => {
  const username = Cookies.get("username")
  const currentQuestion = findCurrentQuestion(game)!

  let container
  if (game.moderator == username) {
    container = <ModeratorGameRoomPanel game={game} currentQuestion={currentQuestion}/>
  } else {
    const currentUser = game.players.find(user => user.name === username)!
    container = <PlayerGameRoomPanel game={game} user={currentUser} currentQuestion={currentQuestion}/>
  }

  return (
    <div>
      <AppBar position="static">
        <Toolbar>
          <Typography sx={{flex: '1 1 100%'}} variant="h6" component="div">
            {game.name}
          </Typography>
        </Toolbar>
      </AppBar>
      <Box sx={{
        display: "flex",
        justifyContent: "center",
        margin: 2
      }}>{container}</Box>
    </div>
  )
}

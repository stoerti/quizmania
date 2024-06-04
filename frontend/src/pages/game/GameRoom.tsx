import {GameService} from "../../services/GameService";
import {GameDto, GameQuestionDto} from "../../services/GameServiceTypes";
import React from "react";
import Cookies from "js-cookie";
import {AppBar, Box, Toolbar, Typography} from "@mui/material";
import {ModeratorGameRoomPanel} from "./gameroom/ModeratorGameRoomPanel";
import {PlayerGameRoomPanel} from "./gameroom/PlayerGameRoomPanel";


export type GameRoomPageProps = {
  game: GameDto,
  gameService: GameService
}

const findCurrentQuestion = (game: GameDto): GameQuestionDto | undefined => {
  return [...game.questions].sort((q1, q2) => q2.questionNumber - q1.questionNumber)[0]
}

export const GameRoomPage = (props: GameRoomPageProps) => {
  const game = props.game

  let username = Cookies.get("username")
  let currentQuestion = findCurrentQuestion(game)!

  let container
  if (props.game.moderator == username) {
    container = <ModeratorGameRoomPanel game={props.game} currentQuestion={currentQuestion} gameService={props.gameService}/>
  } else {
    let currentUser = props.game.users.find(user => user.name === username)!
    container = <PlayerGameRoomPanel game={props.game} user={currentUser} currentQuestion={currentQuestion} gameService={props.gameService}/>
  }

  return (
    <div>
      <AppBar position="static">
        <Toolbar>
          <Typography sx={{flex: '1 1 100%'}} variant="h6" component="div">
            {props.game.name}
          </Typography>
        </Toolbar>
      </AppBar>
      <Box sx={{
        display: "flex",
        justifyContent: "center",
        marginTop: 2
      }}>{container}</Box>
    </div>
  )
}

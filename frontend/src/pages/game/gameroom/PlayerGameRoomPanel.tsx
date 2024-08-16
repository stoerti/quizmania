import {Game, Player, QuestionStatus} from "../../../domain/GameModel";
import {gameCommandService, GameException} from "../../../services/GameCommandService";
import {Box, Button, CircularProgress, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography} from "@mui/material";
import {QuestionContainer} from "../question/QuestionContainer";
import Cookies from "js-cookie";
import PlayArrow from "@mui/icons-material/PlayArrow";
import React from "react";
import {QuestionPhrasePanel} from "../question/QuestionPhrasePanel";
import {GameQuestionMode} from "../../../services/GameEventTypes";
import {BuzzerQuestionContainer} from "../question/BuzzerQuestionContainer";
import {useSnackbar} from "material-ui-snackbar-provider";
import {CorrectAnswerContainer} from "../question/CorrectAnswerContainer";
import {Scoreboard} from "./Scoreboard.tsx";

export type PlayerGameRoomPanelProps = {
  game: Game,
  player: Player,
}


export const PlayerGameRoomPanel = ({game, player}: PlayerGameRoomPanelProps) => {
  const snackbar = useSnackbar()

  const question = game.currentQuestion

  let container
  if (question === undefined) {
    container = <div>Waiting for first question</div>
  } else if (question.status === QuestionStatus.OPEN) {
    if (question.hasPlayerAlreadyAnswered(player.id)) {
      return <Stack spacing={2} alignItems={"center"}>
        <QuestionPhrasePanel gameQuestion={question}/>
        <Paper sx={{padding: 2}}>
          <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
            <Typography sx={{flex: '1 1 100%', textAlign: 'center'}} variant="h4" component="div">
              {question.answers.length} of {game.players.length} players answered
            </Typography>
            <Box sx={{display: 'flex', justifyContent: 'center'}}>
              <CircularProgress/>
            </Box>
          </Box>
        </Paper>
      </Stack>
    } else {
      if (question.questionMode === GameQuestionMode.COLLECTIVE) {
        const onAnswerQuestion = async (answer: string) => {
          try {
            await gameCommandService.answerQuestion({
              gameId: game.id,
              gameQuestionId: question.gameQuestionId,
              answer: answer
            })
          } catch (error) {
            if (error instanceof GameException) {
              snackbar.showMessage(error.message)
            }
          }
        }

        return <QuestionContainer gameQuestion={question} onAnswerQuestion={onAnswerQuestion}/>
      } else if (question.questionMode === GameQuestionMode.BUZZER) {
        const onBuzzQuestion = async () => {
          try {
            await gameCommandService.buzzQuestion({
              gameId: game.id,
              gameQuestionId: question.gameQuestionId
            })
          } catch (error) {
            if (error instanceof GameException) {
              snackbar.showMessage(error.message)
            }
          }
        }
        return <BuzzerQuestionContainer gameQuestion={question} player={player} onBuzzQuestion={onBuzzQuestion}/>
      } else {
        return <div>Unknown gameQuestionMode {question.questionMode}</div>
      }
    }
  } else if (question.status === QuestionStatus.CLOSED) {
    return <Stack spacing={2} alignItems={"center"}>
        <QuestionPhrasePanel gameQuestion={question}/>
        <CorrectAnswerContainer correctAnswer={question.question.correctAnswer} />
        <Table aria-label="simple table">
          <TableHead>
            <TableRow>
              <TableCell>Username</TableCell>
              <TableCell>Answer</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {[...question.answers].sort((a1, a2) => game.findPlayerName(a1.gamePlayerId).localeCompare(game.findPlayerName(a2.gamePlayerId))).map(answer => {
              return (
                <TableRow key={answer.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                  <TableCell component="th" scope="row">
                    <Typography variant="body1" component="div">
                      {game.findPlayerName(answer.gamePlayerId)}
                    </Typography>
                  </TableCell>
                  <TableCell component="th" scope="row">
                    <Typography variant="body1" component="div">
                      {answer.answer}
                    </Typography>
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </Stack>

  } else if (question.status === QuestionStatus.SCORED) {
    let nextButton;
    const onNextQuestion = async () => {
      try {
        await gameCommandService.askNextQuestion(game.id)
      } catch (error) {
        if (error instanceof GameException) {
          snackbar.showMessage(error.message)
        }
      }
    }


    if (question.status === QuestionStatus.SCORED && game.creator === Cookies.get('username')) {
      nextButton = <div style={{display: "flex", alignItems: "center"}}>
        <Button id="nextQuestion" sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
                onClick={onNextQuestion}>Next question</Button>
      </div>
    }
    return <Stack spacing={2} alignItems={"center"}>
        <QuestionPhrasePanel gameQuestion={question}/>
        <CorrectAnswerContainer correctAnswer={question.question.correctAnswer} />
        {nextButton}
        <Scoreboard game={game} />
      </Stack>

  } else {
    return <div>Unknown state</div>
  }

  return <Box sx={{ width: '100%', alignItems: 'center'}}>
    {container}
  </Box>
}

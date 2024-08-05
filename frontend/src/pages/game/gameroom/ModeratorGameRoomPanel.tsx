import {Game, GameQuestion, QuestionStatus} from "../../../domain/GameModel";
import {gameCommandService, GameException} from "../../../services/GameCommandService";
import {Box, Button, CircularProgress, IconButton, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography} from "@mui/material";
import CheckCircle from "@mui/icons-material/CheckCircle";
import PlayArrow from "@mui/icons-material/PlayArrow";
import Cancel from "@mui/icons-material/Cancel";
import React from "react";
import {Build, MarkEmailRead, QuestionMark, StopCircle} from "@mui/icons-material";
import {QuestionPhrasePanel} from "../question/QuestionPhrasePanel";
import Countdown from "react-countdown";
import {QuestionCountdownBar} from "../question/QuestionCountdownBar";
import {GameQuestionMode} from "../../../services/GameEventTypes";
import {useSnackbar} from "material-ui-snackbar-provider";
import {CorrectAnswerContainer} from "../question/CorrectAnswerContainer";

export type ModeratorGameRoomPanelProps = {
  game: Game,
  currentQuestion: GameQuestion,
}


export const ModeratorGameRoomPanel = (props: ModeratorGameRoomPanelProps) => {

  const game = props.game
  const question = props.currentQuestion
  const snackbar = useSnackbar()

  if (question === undefined) {
    return <div>Waiting for first question</div>
  } else if (question.status === QuestionStatus.OPEN) {
    if (question.questionMode === GameQuestionMode.COLLECTIVE) {
      return <Box>
        <QuestionPhrasePanel gameQuestion={question}/>
        <Countdown
          date={question.questionAsked.getTime() + question.questionTimeout}
          intervalDelay={0}
          precision={3}
          renderer={p => <QuestionCountdownBar timeLeft={p.total} totalTime={question.questionTimeout}/>}
        />
        <Paper sx={{padding: 2}}>
          <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
            <Box sx={{display: 'flex', justifyContent: 'center'}}>
              <Button id="closeQuestion" sx={{margin: 'auto'}} startIcon={<StopCircle/>} variant="contained"
                      onClick={() => gameCommandService.closeQuestion(props.game.id, question.gameQuestionId)}
              >Close question</Button>
            </Box>
            <Table aria-label="simple table">
              <TableHead>
                <TableRow>
                  <TableCell></TableCell>
                  <TableCell>Username</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {props.game.players.sort((a1, a2) => a1.name.localeCompare(a2.name)).map(player => {
                  let icon;
                  if (question.hasPlayerAlreadyAnswered(player.id)) {
                    icon = <MarkEmailRead color='success'/>
                  } else {
                    icon = <QuestionMark color='info'/>
                  }
                  return (
                    <TableRow key={player.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                      <TableCell align="left">{icon}</TableCell>
                      <TableCell component="th" scope="row">
                        <Typography variant="body1" component="div">
                          {player.name}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </Box>
        </Paper>
      </Box>
    } else if (question.questionMode === GameQuestionMode.BUZZER) {
      let answerContainer;
      if (question.currentBuzzWinnerId == null) {
        answerContainer =
          <Box sx={{maxWidth: '650px', width: '100%'}}>
            <Stack spacing={2}>
              <Button id="closeQuestion" sx={{margin: 'auto'}} startIcon={<StopCircle/>} variant="contained"
                      onClick={() => gameCommandService.closeQuestion(props.game.id, question.gameQuestionId)}
              >Close question</Button>
              <Paper sx={{padding: 2}}>
                <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
                  <Typography sx={{flex: '1 1 100%', textAlign: 'center'}} variant="h4" component="div">
                    Waiting on players to hit the buzzer
                  </Typography>
                  <Box sx={{display: 'flex', justifyContent: 'center'}}>
                    <CircularProgress/>
                  </Box>
                </Box>
              </Paper>
            </Stack>
          </Box>

      } else {
        answerContainer =
          <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
            <Typography sx={{flex: '1 1 100%', textAlign: 'center'}} variant="h4" component="div" id="buzzWinner">
              {game.findPlayerName(question.currentBuzzWinnerId)}
            </Typography>
            <Box sx={{mt: 3, display: 'flex', justifyContent: 'center'}}>
              <Button id="acceptAnswer" sx={{margin: 'auto'}} startIcon={<CheckCircle/>} variant="contained" color="success"
                      onClick={() => gameCommandService.answerBuzzerQuestion({
                        gameId: props.game.id,
                        gameQuestionId: question.gameQuestionId,
                        answerCorrect: true
                      })}
              >Accept answer</Button>
              <Button id="rejectAnswer" sx={{margin: 'auto'}} startIcon={<StopCircle/>} variant="contained" color="error"
                      onClick={() => gameCommandService.answerBuzzerQuestion({
                        gameId: props.game.id,
                        gameQuestionId: question.gameQuestionId,
                        answerCorrect: false
                      })}
              >Reject answer</Button>
            </Box>
          </Box>
      }
      return (
        <Stack spacing={2}>
          <QuestionPhrasePanel gameQuestion={question}/>
          {answerContainer}
        </Stack>
      )
    } else {
      return <div>Unknown GameQuestionMode {question.questionMode}</div>
    }
  } else if (question.status === QuestionStatus.CLOSED) {
    return (
      <Stack spacing={2}>
        <QuestionPhrasePanel gameQuestion={question}/>
        <CorrectAnswerContainer correctAnswer={question.question.correctAnswer} />
        <Button id="rateQuestion" sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
                onClick={() => gameCommandService.scoreQuestion(props.game.id, question.gameQuestionId)}
        >Rate question</Button>
        <Table aria-label="simple table">
          <TableHead>
            <TableRow>
              <TableCell></TableCell>
              <TableCell>Username</TableCell>
              <TableCell>Answer</TableCell>
              <TableCell align="right"></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {[...question.answers].sort((a1, a2) => game.findPlayerName(a1.gamePlayerId).localeCompare(game.findPlayerName(a2.gamePlayerId))).map(answer => {
              const answerCorrect = answer.answer.toLowerCase() === question.question.correctAnswer.toLowerCase() // TODO better check if answer is correct
              const icon = answerCorrect ? <CheckCircle color='success'/> : <Cancel color='error'/>
              const action = !answerCorrect ?
                <IconButton
                  onClick={
                    async () => {
                      try {
                        await gameCommandService.overrideAnswer({
                          gameId: game.id,
                          gameQuestionId: question.gameQuestionId,
                          gamePlayerId: answer.gamePlayerId,
                          playerAnswerId: answer.id,
                          answer: question.question.correctAnswer
                        })
                      } catch (error) {
                        if (error instanceof GameException) {
                          snackbar.showMessage(error.message)
                        }
                      }
                    }
                  }
                ><Build color='success'></Build></IconButton> : undefined
              return (
                <TableRow key={answer.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                  <TableCell align="left">{icon}</TableCell>
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
                  <TableCell align="right">{action}</TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </Stack>
    )
  } else if (question.status === QuestionStatus.SCORED) {
    return (
      <Stack spacing={2}>
        <QuestionPhrasePanel gameQuestion={question}/>
        <CorrectAnswerContainer correctAnswer={question.question.correctAnswer} />
        <div style={{display: "flex", alignItems: "center"}}>
          <Button id="nextQuestion" sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
                  onClick={
                    async () => {
                      try {
                        await gameCommandService.askNextQuestion(props.game.id)
                      } catch (error) {
                        if (error instanceof GameException) {
                          snackbar.showMessage(error.message)
                        }
                      }
                    }
                  }
          >Next question</Button>
        </div>
        <Table aria-label="simple table">
          <TableHead>
            <TableRow>
              <TableCell></TableCell>
              <TableCell>Username</TableCell>
              <TableCell>Answer</TableCell>
              <TableCell align="right">Points</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {[...question.answers].sort((a1, a2) => game.findPlayerName(a1.gamePlayerId).localeCompare(game.findPlayerName(a2.gamePlayerId))).map(answer => {
              const icon = answer.points > 0 ? <CheckCircle color='success'/> : <Cancel color='error'/>
              return (
                <TableRow key={answer.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                  <TableCell align="left">{icon}</TableCell>
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
                  <TableCell align="right">{answer.points}</TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </Stack>
    )
  } else {
    return <div>Unknown state</div>
  }


}

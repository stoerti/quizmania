import {Game, GameQuestion, QuestionStatus} from "../../../domain/GameModel";
import {Box, Button, CircularProgress, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography} from "@mui/material";
import CheckCircle from "@mui/icons-material/CheckCircle";
import Cancel from "@mui/icons-material/Cancel";
import React from "react";
import {MarkEmailRead, QuestionMark} from "@mui/icons-material";
import {QuestionPhrasePanel} from "../question/QuestionPhrasePanel";
import Countdown from "react-countdown";
import {QuestionCountdownBar} from "../question/QuestionCountdownBar";
import {GameQuestionMode, QuestionType} from "../../../services/GameEventTypes";
import {CorrectAnswerContainer} from "../question/CorrectAnswerContainer";
import {Scoreboard} from "./Scoreboard.tsx";
import {PlayerAnswerLog} from "./PlayerAnswerLog.tsx";

export type SpectatorGameRoomPanelProps = {
  game: Game,
}


export const SpectatorGameRoomPanel = ({game}: SpectatorGameRoomPanelProps) => {

  const question = game.findLastQuestion()

  if (question === undefined) {
    return <div>Waiting for first question</div>
  } else if (question.status === QuestionStatus.OPEN) {
    if (question.questionMode === GameQuestionMode.COLLECTIVE) {
      let answerContainer
      if (question.question.type === QuestionType.CHOICE) {
        answerContainer = <Stack spacing={2} direction="column" justifyContent="center" alignItems="center" useFlexGap
                                 flexWrap="wrap">
          {question.question.answerOptions.map((answer, index) =>
            <Button key={index} variant="contained" sx={{width: '80%', maxWidth: '300px'}}>{answer}</Button>)}
        </Stack>
      }
      return <Box>
        <QuestionPhrasePanel gameQuestion={question}/>
        <Countdown
          date={question.questionAsked.getTime() + question.questionTimeout}
          intervalDelay={0}
          precision={3}
          renderer={p => <QuestionCountdownBar timeLeft={p.total} totalTime={question.questionTimeout}/>}
        />
        <br/>

        <Stack spacing={2}>
          {answerContainer}
          <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
            <PlayerAnswerLog game={game}/>
          </Box>
        </Stack>
      </Box>
    } else if (question.questionMode === GameQuestionMode.BUZZER) {
      let answerContainer;
      if (question.currentBuzzWinnerId == null) {
        answerContainer =
          <Box sx={{maxWidth: '650px', width: '100%'}}>
            <Stack spacing={2}>
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
        <CorrectAnswerContainer correctAnswer={question.question.correctAnswer}/>
        <Table aria-label="simple table">
          <TableHead>
            <TableRow>
              <TableCell></TableCell>
              <TableCell>Username</TableCell>
              <TableCell>Answer</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {[...question.answers].sort((a1, a2) => game.findPlayerName(a1.gamePlayerId).localeCompare(game.findPlayerName(a2.gamePlayerId))).map(answer => {
              const answerCorrect = answer.answer.toLowerCase() === question.question.correctAnswer.toLowerCase() // TODO better check if answer is correct
              const icon = answerCorrect ? <CheckCircle color='success'/> : <Cancel color='error'/>
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
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </Stack>
    )
  } else if (question.status === QuestionStatus.SCORED) {
    return (
      <Stack spacing={2} sx={{width: '100%'}} alignItems={"center"}>
        <QuestionPhrasePanel gameQuestion={question}/>
        <CorrectAnswerContainer correctAnswer={question.question.correctAnswer}/>
        <Scoreboard game={game}/>
      </Stack>
    )
  } else {
    return <div>Unknown state</div>
  }


}

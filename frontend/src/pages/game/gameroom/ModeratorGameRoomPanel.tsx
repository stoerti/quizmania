import {Game, QuestionStatus, RoundStatus} from "../../../domain/GameModel";
import {gameCommandService, GameException} from "../../../services/GameCommandService";
import {Box, Button, CircularProgress, IconButton, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography} from "@mui/material";
import CheckCircle from "@mui/icons-material/CheckCircle";
import PlayArrow from "@mui/icons-material/PlayArrow";
import Cancel from "@mui/icons-material/Cancel";
import React from "react";
import {Build, StopCircle} from "@mui/icons-material";
import {QuestionPhrasePanel} from "../question/QuestionPhrasePanel";
import Countdown from "react-countdown";
import {QuestionCountdownBar} from "../question/QuestionCountdownBar";
import {GameQuestionMode, QuestionType} from "../../../services/GameEventTypes";
import {useSnackbar} from "material-ui-snackbar-provider";
import {CorrectAnswerContainer} from "../question/CorrectAnswerContainer";
import {Scoreboard, ScoreboardMode} from "./Scoreboard.tsx";
import {PlayerAnswerLog} from "./PlayerAnswerLog.tsx";
import {StartRoundPanel} from "./StartRoundPanel.tsx";
import {ScoredQuestionResult} from "./ScoredQuestionResult.tsx";
import {formatAnswerForDisplay} from "../../../utils/answerFormatter.ts";

export type ModeratorGameRoomPanelProps = {
  game: Game,
}


export const ModeratorGameRoomPanel = ({game}: ModeratorGameRoomPanelProps) => {
  const snackbar = useSnackbar()
  const question = game.currentQuestion
  const round = game.currentRound

  if (round === undefined) {
    return <div>Waiting for first round</div>
  } else if (round.status === RoundStatus.OPEN) {
    if (question === undefined) {
      return <StartRoundPanel game={game} isModerator={true}/>
    } else if (question.status === QuestionStatus.OPEN) {
      let answerContainer;
      if (question.question.type === QuestionType.CHOICE || question.question.type === QuestionType.MULTIPLE_CHOICE) {
        answerContainer = <Stack spacing={2} direction="column" justifyContent="center" alignItems="center" useFlexGap
                                 flexWrap="wrap">
          {question.question.answerOptions.map((answer, index) =>
            <Button key={index} variant="contained" sx={{width: '80%', maxWidth: '300px'}}>{answer}</Button>)}
        </Stack>
      }

      if (question.questionMode === GameQuestionMode.COLLECTIVE) {
        return <Box>
          <QuestionPhrasePanel gameQuestion={question}/>
          <Countdown
            date={question.questionAsked.getTime() + question.questionTimeout}
            intervalDelay={0}
            precision={3}
            renderer={p => <QuestionCountdownBar timeLeft={p.total} totalTime={question.questionTimeout}/>}
          />
          <br/>
          <Stack spacing={2} sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
            {answerContainer}
            <Box sx={{display: 'flex', justifyContent: 'center'}}>
              <Button id="closeQuestion" sx={{margin: 'auto'}} startIcon={<StopCircle/>} variant="contained"
                      onClick={() => gameCommandService.closeQuestion(game.id, question.gameQuestionId)}
              >Close question</Button>
            </Box>
            <PlayerAnswerLog game={game}/>
          </Stack>
        </Box>
      } else if (question.questionMode === GameQuestionMode.BUZZER) {
        let container;
        if (question.currentBuzzWinnerId == null) {
          container =
            <Box sx={{maxWidth: '650px', width: '100%'}}>
              <Stack spacing={2}>
                <Button id="closeQuestion" sx={{margin: 'auto'}} startIcon={<StopCircle/>} variant="contained"
                        onClick={() => gameCommandService.closeQuestion(game.id, question.gameQuestionId)}
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
          container =
            <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
              <CorrectAnswerContainer correctAnswer={question.question.correctAnswer}/>
              <Box sx={{marginTop: 5, marginBottom: 10}}>
                <Typography sx={{flex: '1 1 100%', textAlign: 'center'}} variant="body1" component="div">
                  Player on the clock:
                </Typography>
                <Typography sx={{flex: '1 1 100%', textAlign: 'center'}} variant="h2" component="div" id="buzzWinner">
                  {game.findPlayerName(question.currentBuzzWinnerId)}
                </Typography>
              </Box>
              <Box sx={{mt: 3, display: 'flex', justifyContent: 'center'}}>
                <Button id="acceptAnswer" sx={{margin: 'auto'}} startIcon={<CheckCircle/>} variant="contained" color="success"
                        onClick={() => gameCommandService.answerBuzzerQuestion({
                          gameId: game.id,
                          gameQuestionId: question.gameQuestionId,
                          answerCorrect: true
                        })}
                >Accept answer</Button>
                <Button id="rejectAnswer" sx={{margin: 'auto'}} startIcon={<StopCircle/>} variant="contained" color="error"
                        onClick={() => gameCommandService.answerBuzzerQuestion({
                          gameId: game.id,
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
            {container}
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
          <Button id="rateQuestion" sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
                  onClick={() => gameCommandService.scoreQuestion(game.id, question.gameQuestionId)}
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
                        {formatAnswerForDisplay(answer.answer, question.question.type)}
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
        <Stack spacing={2} alignItems={"center"}>
          <QuestionPhrasePanel gameQuestion={question}/>
          <CorrectAnswerContainer correctAnswer={question.question.correctAnswer}/>
          <div style={{display: "flex", alignItems: "center"}}>
            <Button id="nextQuestion" sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
                    onClick={
                      async () => {
                        try {
                          await gameCommandService.askNextQuestion(game.id)
                        } catch (error) {
                          if (error instanceof GameException) {
                            snackbar.showMessage(error.message)
                          }
                        }
                      }
                    }
            >Next question</Button>
          </div>
          <Scoreboard game={game} mode={ScoreboardMode.QUESTION}/>
        </Stack>
      )
    }
  } else if (round.status === RoundStatus.SCORED) {
    const onCloseRound = async () => {
      try {
        await gameCommandService.closeRound(game.id)
      } catch (error) {
        if (error instanceof GameException) {
          snackbar.showMessage(error.message)
        }
      }
    }
    return <Stack spacing={2} alignItems={"center"}>
      <div style={{display: "flex", alignItems: "center"}}>
        <Button id="closeRound" sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
                onClick={onCloseRound}>Close Round</Button>
      </div>
      <Typography variant="h4" component="div">
        Round {round.roundNumber} Scoreboard
      </Typography>
      <Scoreboard game={game} mode={ScoreboardMode.ROUND}/>
    </Stack>
  } else {
    return <div>Unknown state</div>
  }
}

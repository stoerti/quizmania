import {Game, GameQuestion, Player, QuestionStatus} from "../../../domain/GameModel";
import {GameCommandService} from "../../../services/GameCommandService";
import {
  Box,
  Button,
  CircularProgress,
  List,
  ListItem,
  ListItemText,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
  useTheme
} from "@mui/material";
import CheckCircle from "@mui/icons-material/CheckCircle";
import {QuestionContainer} from "../question/QuestionContainer";
import Cookies from "js-cookie";
import PlayArrow from "@mui/icons-material/PlayArrow";
import Cancel from "@mui/icons-material/Cancel";
import React from "react";
import {QuestionMark} from "@mui/icons-material";
import {QuestionPhrasePanel} from "../question/QuestionPhrasePanel";

export type PlayerGameRoomPanelProps = {
  game: Game,
  user: Player,
  currentQuestion: GameQuestion,
  gameService: GameCommandService
}


export const PlayerGameRoomPanel = (props: PlayerGameRoomPanelProps) => {
  const question = props.currentQuestion
  const user = props.user

  let container = undefined
  if (question == undefined) {
    container = <div>Waiting for first question</div>
  } else if (question.status == QuestionStatus.OPEN) {
    if (question.hasPlayerAlreadyAnswered(user.id)) {
      container = <Stack spacing={2}>
        <QuestionPhrasePanel gameQuestion={question}/>
        <Paper sx={{padding: 2}}>
          <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
            <Typography sx={{flex: '1 1 100%', textAlign: 'center'}} variant="h4" component="div">
              {question.answers.length} of {props.game.players.length} players answered
            </Typography>
            <Box sx={{display: 'flex', justifyContent: 'center'}}>
              <CircularProgress/>
            </Box>
          </Box>
        </Paper>
      </Stack>
    } else {
      const onAnswerQuestion = (answer: string) =>
        props.gameService.answerQuestion({
          gameId: props.game.id,
          gameQuestionId: question.gameQuestionId,
          answer: answer
        }, () => {
        })
      container = <QuestionContainer gameQuestion={question} onAnswerQuestion={onAnswerQuestion}/>
    }
  } else if (question.status == QuestionStatus.CLOSED || question.status == QuestionStatus.RATED) {
    let nextButton;
    if (question.status == QuestionStatus.RATED && props.game.creator === Cookies.get('username')) {
      nextButton = <div style={{display: "flex", alignItems: "center"}}>
        <Button id="nextQuestion" sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
                onClick={() => props.gameService.askNextQuestion(props.game.id, () => {
                })}>Next question</Button>
      </div>
    }
    container =
      <Stack spacing={2}>
        <QuestionPhrasePanel gameQuestion={question}/>
        {nextButton}
        <List dense>
          <ListItem key="correctAnswer">
            <ListItemText primary={question.question.correctAnswer} secondary='Answer'/>
          </ListItem>
        </List>
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
            {[...question.answers].sort((a1, a2) => a1.points - a2.points || props.game.findPlayerName(a1.gamePlayerId).localeCompare(props.game.findPlayerName(a2.gamePlayerId))).map(answer => {
              let icon;
              if (question.status == QuestionStatus.RATED) {
                if (answer.points > 0) {
                  icon = <CheckCircle color='success'/>
                } else {
                  icon = <Cancel color='error'/>
                }
              } else {
                icon = <QuestionMark color='info'/>
              }
              return (
                <TableRow key={answer.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                  <TableCell align="left">{icon}</TableCell>
                  <TableCell component="th" scope="row">
                    <Typography variant="body1" component="div">
                      {props.game.findPlayerName(answer.gamePlayerId)}
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

  } else {
    container = <div>Unknown state</div>
  }

  return <Box sx={{maxWidth: '650px', width: '100%'}}>
    {container}
  </Box>
}

import {GameDto, GameQuestionDto, QuestionStatus} from "../../../services/GameServiceTypes";
import {GameService} from "../../../services/GameService";
import {
  Box,
  Button,
  CircularProgress,
  IconButton,
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
import PlayArrow from "@mui/icons-material/PlayArrow";
import Cancel from "@mui/icons-material/Cancel";
import React from "react";
import {Build, QuestionMark} from "@mui/icons-material";
import {QuestionPhrasePanel} from "../question/QuestionPhrasePanel";

export type ModeratorGameRoomPanelProps = {
  game: GameDto,
  currentQuestion: GameQuestionDto,
  gameService: GameService
}


export const ModeratorGameRoomPanel = (props: ModeratorGameRoomPanelProps) => {
  const getUsername = (gameUserId: string): string => {
    return props.game.users.find(user => user.id === gameUserId)?.name ?? ''
  }

  const alreadyAnswered = (gameUserId: string): boolean => {
    return question.userAnswers.find(answer => answer.gameUserId === gameUserId) !== undefined
  }

  const game = props.game
  const question = props.currentQuestion

  const theme = useTheme();
  if (question.status == QuestionStatus.OPEN) {
    return <Box>
      <Paper sx={{padding: 2, mb: 2, backgroundColor: theme.palette.primary.light}}>
        <Typography sx={{flex: '1 1 100%'}} variant="overline" component="div">
          Question {question.questionNumber}
        </Typography>
        <Typography sx={{flex: '1 1 100%'}} variant="h5" component="div">
          {question.phrase}
        </Typography>
      </Paper>
      <Paper sx={{padding: 2}}>
        <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
          <Box sx={{display: 'flex', justifyContent: 'center'}}>
            <CircularProgress/>
          </Box>
          <Table aria-label="simple table">
            <TableHead>
              <TableRow>
                <TableCell></TableCell>
                <TableCell>Username</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {props.game.users.sort((a1, a2) => a1.name.localeCompare(a2.name)).map(user => {
                let icon;
                if (alreadyAnswered(user.id)) {
                  icon = <CheckCircle color='success'/>
                } else {
                  icon = <QuestionMark color='error'/>
                }
                return (
                  <TableRow key={user.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                    <TableCell align="left">{icon}</TableCell>
                    <TableCell component="th" scope="row">
                      <Typography variant="body1" component="div">
                        {user.name}
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
  } else if (question.status == QuestionStatus.CLOSED) {
    return (
      <Stack spacing={2}>
        <QuestionPhrasePanel question={question}/>
        <Button sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
                onClick={() => props.gameService.rateQuestion(props.game.id, question.id)}
        >Rate question</Button>
        <List dense>
          <ListItem key="correctAnswer">
            <ListItemText primary={question.correctAnswer} secondary='Answer'/>
          </ListItem>
        </List>
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
            {[...question.userAnswers].sort((a1, a2) => a1.points - a2.points || getUsername(a1.gameUserId).localeCompare(getUsername(a2.gameUserId))).map(answer => {
              let answerCorrect = answer.answer.toLowerCase() == question.correctAnswer.toLowerCase() // TODO better check if answer is correct
              let icon = answerCorrect ? <CheckCircle color='success'/> : <Cancel color='error'/>
              let action = !answerCorrect ?
                <IconButton
                  onClick={() => props.gameService.overrideAnswer({
                    gameId: game.id,
                    gameQuestionId: question.id,
                    gameUserId: answer.gameUserId,
                    userAnswerId: answer.id,
                    answer: question.correctAnswer
                  }, () => {
                  })}
                ><Build color='success'></Build></IconButton> : undefined
              return (
                <TableRow key={answer.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                  <TableCell align="left">{icon}</TableCell>
                  <TableCell component="th" scope="row">
                    <Typography variant="body1" component="div">
                      {getUsername(answer.gameUserId)}
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
  } else if (question.status == QuestionStatus.RATED) {
    return (
      <Stack spacing={2}>
        <QuestionPhrasePanel question={question}/>
        <div style={{display: "flex", alignItems: "center"}}>
          <Button sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
                  onClick={() => props.gameService.askNextQuestion(props.game.id, () => {
                  })}
          >Next question</Button>
        </div>
        <List dense>
          <ListItem key="correctAnswer">
            <ListItemText primary={question.correctAnswer} secondary='Answer'/>
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
            {[...question.userAnswers].sort((a1, a2) => a1.points - a2.points || getUsername(a1.gameUserId).localeCompare(getUsername(a2.gameUserId))).map(answer => {
              let icon = answer.points > 0 ? <CheckCircle color='success'/> : <Cancel color='error'/> // TODO better check if answer is correct
              return (
                <TableRow key={answer.id} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                  <TableCell align="left">{icon}</TableCell>
                  <TableCell component="th" scope="row">
                    <Typography variant="body1" component="div">
                      {getUsername(answer.gameUserId)}
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

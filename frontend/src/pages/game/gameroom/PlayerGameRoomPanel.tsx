import {GameDto, GameQuestionDto, GameUserDto, QuestionStatus} from "../../../services/GameServiceTypes";
import {GameService} from "../../../services/GameService";
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
    game: GameDto,
    user: GameUserDto,
    currentQuestion: GameQuestionDto,
    gameService: GameService
}


export const PlayerGameRoomPanel = (props: PlayerGameRoomPanelProps) => {
    const getUsername = (gameUserId: string): string => {
        return props.game.users.find(user => user.id === gameUserId)?.name ?? ''
    }

    const alreadyAnswered = (gameUserId: string): boolean => {
        return question.userAnswers.find(answer => answer.gameUserId === gameUserId) !== undefined
    }

    const question = props.currentQuestion
    const user = props.user

    let container = undefined

    if (question.status == QuestionStatus.OPEN) {
        if (alreadyAnswered(user.id)) {
          container = <Stack spacing={2}>
                <QuestionPhrasePanel question={question} />
                <Paper sx={{padding: 2}}>
                    <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
                        <Typography sx={{flex: '1 1 100%', textAlign: 'center'}} variant="h4" component="div">
                            {question.userAnswers.length} of {props.game.users.length} players answered
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
                    gameQuestionId: question.id,
                    answer: answer
                }, () => {
                })
          container = <QuestionContainer question={question} onAnswerQuestion={onAnswerQuestion}/>
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
                <QuestionPhrasePanel question={question} />
                {nextButton}
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

    } else {
      container = <div>Unknown state</div>
    }

    return <Box sx={{maxWidth: '650px', width: '100%'}}>
      {container}
    </Box>
}

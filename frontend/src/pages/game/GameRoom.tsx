import {GameService} from "../../services/GameService";
import {GameDto, GameQuestionDto, QuestionType} from "../../services/GameServiceTypes";
import React from "react";
import Cookies from "js-cookie";
import {
    AppBar,
    Box,
    Button,
    CircularProgress,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    Paper,
    Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField,
    Toolbar,
    Typography,
    useTheme
} from "@mui/material";
import CheckCircle from "@mui/icons-material/CheckCircle";
import Cancel from "@mui/icons-material/Cancel";
import PlayArrow from "@mui/icons-material/PlayArrow";
import EmojiEvents from "@mui/icons-material/EmojiEvents";
import {amber, brown, grey} from "@mui/material/colors";


export type QuestionContainerProps = {
    question: GameQuestionDto
    onAnswerQuestion: (answer: string) => void
}

const QuestionContainer = (props: QuestionContainerProps) => {
    const theme = useTheme();

    const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        let answerString: string = new FormData(event.currentTarget).get('answer')!.toString();

        props.onAnswerQuestion(answerString)
    };


    let answerContainer = null
    if (props.question.type === QuestionType.CHOICE) {
        answerContainer = <Stack spacing={2} direction="column" justifyContent="center" alignItems="center" useFlexGap
                                 flexWrap="wrap">
            {props.question.answerOptions.map((answer, index) =>
                <Button key={index} variant="contained" sx={{width: '80%', maxWidth: '300px'}} onClick={() => props.onAnswerQuestion(answer)}>{answer}</Button>)}
        </Stack>
    } else if (props.question.type === QuestionType.FREE_INPUT) {
        answerContainer = <Box component="form" noValidate onSubmit={handleSubmit} sx={{
            my: 4, mx: 4, display: 'flex', flexDirection: 'column', alignItems: 'center',
        }}>
            <TextField margin="normal" required fullWidth id="answer" label="Answer" name="answer" autoFocus sx={{margin: 2}}
            />
            <Button type="submit" fullWidth variant="contained" sx={{ml: 2, mr: 2, mb: 2}}>Submit answer</Button>
        </Box>
    } else if (props.question.type === QuestionType.ESTIMATE) {
        answerContainer =
            <Box component="form" noValidate onSubmit={handleSubmit} sx={{my: 4, mx: 4, display: 'flex', flexDirection: 'column', alignItems: 'center'}}>
                <TextField margin="normal" required fullWidth id="answer" label="Answer" name="answer" autoFocus type="number" sx={{margin: 2}}/>
                <Button type="submit" fullWidth variant="contained" sx={{ml: 2, mr: 2, mb: 2}}>Submit answer</Button>
            </Box>
    }

    return (
        <div>
            <Paper sx={{padding: 2, backgroundColor: theme.palette.primary.light}}>
                <Typography sx={{flex: '1 1 100%'}} variant="overline" component="div">
                    Question {props.question.questionNumber}
                </Typography>
                <Typography sx={{flex: '1 1 100%'}} variant="h5" component="div">
                    {props.question.phrase}
                </Typography>
            </Paper>
            <br/>
            {answerContainer}
        </div>
    )
}

export type GameRoomPageProps = {
    game: GameDto,
    gameService: GameService
}

const hasOpenQuestion = (game: GameDto): boolean => {
    return findOpenQuestion(game) !== undefined
}

const findOpenQuestion = (game: GameDto): GameQuestionDto | undefined => {
    return game.questions.find(question => question.open)
}

const alreadyAnswered = (question: GameQuestionDto, gameUserId: string): boolean => {
    return question.userAnswers.find(answer => answer.gameUserId === gameUserId) !== undefined
}


export const GameRoomPage = (props: GameRoomPageProps) => {
    const getUsername = (gameUserId: string): string => {
        return props.game.users.find(user => user.id === gameUserId)?.name ?? ''
    }

    const theme = useTheme();

    let username = Cookies.get("username")
    let gameUserId = props.game.users.find(user => user.name === username)!.id

    let container;
    if (hasOpenQuestion(props.game)) {
        let question = findOpenQuestion(props.game)!

        if (alreadyAnswered(question, gameUserId)) {
            container = <Box>
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
                        <Typography sx={{flex: '1 1 100%', textAlign: 'center'}} variant="h4" component="div">
                            {question.userAnswers.length} of {props.game.users.length} players answered
                        </Typography>
                        <Box sx={{display: 'flex', justifyContent: 'center'}}>
                            <CircularProgress/>
                        </Box>
                    </Box>
                </Paper>


            </Box>
        } else {
            const onAnswerQuestion = (answer: string) =>
                props.gameService.answerQuestion({
                    gameId: props.game.id,
                    gameQuestionId: question.id,
                    answer: answer
                }, () => {
                })
            container =
                <QuestionContainer question={findOpenQuestion(props.game)!} onAnswerQuestion={onAnswerQuestion}/>
        }

    } else if (!hasOpenQuestion(props.game)) {
        let nextButton;
        if (props.game.moderator === Cookies.get('username') || (!props.game.moderator && props.game.creator === Cookies.get('username'))) {
            nextButton = <div style={{display: "flex", alignItems: "center"}}>
                <Button sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained"
                        onClick={() => props.gameService.askNextQuestion(props.game.id, () => {
                        })}>Next question</Button>
            </div>
        }

        if (props.game.questions.length > 0) {
            let lastQuestion = [...props.game.questions].sort((q1, q2) => q2.questionNumber - q1.questionNumber)[0]
            container = (
                <Stack spacing={2}>
                    <Paper sx={{padding: 2, backgroundColor: theme.palette.primary.light}}>
                        <Typography sx={{flex: '1 1 100%'}} variant="overline" component="div">
                            Question {lastQuestion.questionNumber}
                        </Typography>
                        <Typography sx={{flex: '1 1 100%'}} variant="h5" component="div">
                            {lastQuestion.phrase}
                        </Typography>
                    </Paper>
                    {nextButton}
                    <List dense>
                        <ListItem key="numberOfQuestions">
                            <ListItemText primary={lastQuestion.correctAnswer} secondary='Answer'/>
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
                            {[...lastQuestion.userAnswers].sort((a1, a2) => a1.points - a2.points || getUsername(a1.gameUserId).localeCompare(getUsername(a2.gameUserId))).map(answer => {
                                let icon;
                                if (answer.points > 0) {
                                    icon = <CheckCircle color='success'/>
                                } else {
                                    icon = <Cancel color='error'/>
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
            )
        }
    } else {
        container = <div>Unknown state</div>
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
            <Box sx={{padding: 2}}>{container}</Box>
        </div>
    )
}

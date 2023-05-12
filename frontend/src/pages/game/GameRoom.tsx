import {GameService} from "../../services/GameService";
import {GameDto, GameQuestionDto} from "../../services/GameServiceTypes";
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
    Stack,
    Toolbar,
    Typography,
    useTheme
} from "@mui/material";
import CheckCircle from "@mui/icons-material/CheckCircle";
import Cancel from "@mui/icons-material/Cancel";
import PlayArrow from "@mui/icons-material/PlayArrow";


export type QuestionContainerProps = {
    question: GameQuestionDto
    onAnswerQuestion: (answer: string) => void
}

const QuestionContainer = (props: QuestionContainerProps) => {
    const theme = useTheme();

    return (
        <div>
            <Paper sx={{padding: 2, backgroundColor: theme.palette.primary.light}}>
                <Typography
                    sx={{flex: '1 1 100%'}}
                    variant="overline"
                    component="div"
                >
                    Question {props.question.questionNumber}
                </Typography>
                <Typography
                    sx={{flex: '1 1 100%'}}
                    variant="h5"
                    component="div"
                >
                    {props.question.phrase}
                </Typography>
            </Paper>
            <br/>
            <Stack spacing={2} direction="column" justifyContent="center" alignItems="center" useFlexGap flexWrap="wrap">
                {props.question.answerOptions.map(answer =>
                    <Button variant="contained" sx={{width: '80%', maxWidth: '300px'}}
                            onClick={() => props.onAnswerQuestion(answer)}>{answer}</Button>)}
            </Stack>

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
                    <Typography
                        sx={{flex: '1 1 100%'}}
                        variant="overline"
                        component="div"
                    >
                        Question {question.questionNumber}
                    </Typography>
                    <Typography
                        sx={{flex: '1 1 100%'}}
                        variant="h5"
                        component="div"
                    >
                        {question.phrase}
                    </Typography>
                </Paper>
                <Paper sx={{padding: 2}}>
                    <Box sx={{display: 'block', m: 'auto', alignContent: 'center'}}>
                        <Typography
                            sx={{flex: '1 1 100%', textAlign: 'center'}}
                            variant="h4"
                            component="div"
                        >
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
            nextButton = <div
                style={{
                    display: "flex",
                    alignItems: "center"
                }}
            >
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
                        <Typography
                            sx={{flex: '1 1 100%'}}
                            variant="overline"
                            component="div"
                        >
                            Question {lastQuestion.questionNumber}
                        </Typography>
                        <Typography
                            sx={{flex: '1 1 100%'}}
                            variant="h5"
                            component="div"
                        >
                            {lastQuestion.phrase}
                        </Typography>
                    </Paper>
                    {nextButton}
                    <List dense>
                        <ListItem key="numberOfQuestions">
                            <ListItemText
                                primary={lastQuestion.correctAnswer}
                                secondary='Answer'
                            />
                        </ListItem>
                        {[...lastQuestion.userAnswers].sort((a1, a2) => getUsername(a1.gameUserId).localeCompare(getUsername(a2.gameUserId))).map(answer =>
                            <ListItem key={answer.gameUserId}>
                                {answer.answer === lastQuestion.correctAnswer
                                    ? <ListItemIcon><CheckCircle color='success'/></ListItemIcon>
                                    : <ListItemIcon><Cancel color='error'/></ListItemIcon>}
                                <ListItemText
                                    primary={getUsername(answer.gameUserId)}
                                    secondary={getUsername(answer.answer)}
                                />
                            </ListItem>
                        )}
                    </List>
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
                    <Typography
                        sx={{flex: '1 1 100%'}}
                        variant="h6"
                        component="div"
                    >
                        {props.game.name}
                    </Typography>
                </Toolbar>
            </AppBar>
            <Box sx={{padding: 2}}>
                {container}
            </Box>
        </div>
    )
}

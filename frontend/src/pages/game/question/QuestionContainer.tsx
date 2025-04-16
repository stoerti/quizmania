import {GameQuestion} from "../../../domain/GameModel";
import {Box, Button, Stack, TextField, useTheme} from "@mui/material";
import React, {useState} from "react";
import {QuestionPhrasePanel} from "./QuestionPhrasePanel";
import Countdown from "react-countdown";
import {QuestionCountdownBar} from "./QuestionCountdownBar";
import {QuestionType} from "../../../services/GameEventTypes";

export type MultipleChoiceAnswerContainerProps = {
  gameQuestion: GameQuestion
  onAnswerQuestion: (answer: string) => void
}

export const MultipleChoiceAnswerContainer = ({gameQuestion, onAnswerQuestion}: MultipleChoiceAnswerContainerProps) => {
  const [selected, setSelected] = useState([] as string[])
  const theme = useTheme()

  const selectAnswer = (answer: string) => {
    setSelected([answer, ...selected])
  }

  const deselectAnswer = (answer: string) => {
    setSelected(selected.filter((a) => a !== answer))
  }

  const submitAnswer = () => {
    const answerString = selected.join(", ")
    onAnswerQuestion(answerString)
  }

  return <Stack spacing={2} direction="column" justifyContent="center" alignItems="center" useFlexGap
                flexWrap="wrap">
    {gameQuestion.question.answerOptions.map((answer, index) => {
        const isSelected = selected.includes(answer)
        if (isSelected) {
          return <Button key={index} variant="contained" sx={{width: '80%', maxWidth: '300px'}} onClick={() => deselectAnswer(answer)}>{answer}</Button>
        } else {
          return <Button key={index} variant="outlined" sx={{width: '80%', maxWidth: '300px', backgroundColor: theme.palette.error.main}} onClick={() => selectAnswer(answer)}>{answer}</Button>
        }
      }
    )}

    <Button key='send' variant="contained" sx={{width: '80%', maxWidth: '300px', top: '20px'}} onClick={() => submitAnswer()}>Confirm</Button>
  </Stack>
}

export type QuestionContainerProps = {
  gameQuestion: GameQuestion
  onAnswerQuestion: (answer: string) => void
}

export const QuestionContainer = ({gameQuestion, onAnswerQuestion}: QuestionContainerProps) => {
  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const answerString: string = new FormData(event.currentTarget).get('answer')!.toString();

    onAnswerQuestion(answerString)
  };


  let answerContainer = null
  if (gameQuestion.question.type === QuestionType.CHOICE) {
    answerContainer = <Stack spacing={2} direction="column" justifyContent="center" alignItems="center" useFlexGap
                             flexWrap="wrap">
      {gameQuestion.question.answerOptions.map((answer, index) =>
        <Button key={index} variant="contained" sx={{width: '80%', maxWidth: '300px'}} onClick={() => onAnswerQuestion(answer)}>{answer}</Button>)}
    </Stack>
  } else if (gameQuestion.question.type === QuestionType.MULTIPLE_CHOICE) {
    answerContainer = <MultipleChoiceAnswerContainer gameQuestion={gameQuestion} onAnswerQuestion={onAnswerQuestion}/>
  } else if (gameQuestion.question.type === QuestionType.FREE_INPUT) {
    answerContainer = <Box component="form" noValidate onSubmit={handleSubmit} sx={{
      my: 4, mx: 4, display: 'flex', flexDirection: 'column', alignItems: 'center',
    }}>
      <TextField margin="normal" required fullWidth id="freeChoiceAnswer" label="Answer" name="answer" autoFocus sx={{margin: 2}}
      />
      <Button id="submitAnswer" type="submit" fullWidth variant="contained" sx={{ml: 2, mr: 2, mb: 2}}>Submit answer</Button>
    </Box>
  } else if (gameQuestion.question.type === QuestionType.ESTIMATE) {
    answerContainer =
      <Box component="form" noValidate onSubmit={handleSubmit} sx={{my: 4, mx: 4, display: 'flex', flexDirection: 'column', alignItems: 'center'}}>
        <TextField margin="normal" required fullWidth id="estimationAnswer" label="Answer" name="answer" autoFocus type="number" sx={{margin: 2}}/>
        <Button id="submitAnswer" type="submit" fullWidth variant="contained" sx={{ml: 2, mr: 2, mb: 2}}>Submit answer</Button>
      </Box>
  }

  return (
    <div style={{width: '100%', maxWidth: 650}}>
      <QuestionPhrasePanel gameQuestion={gameQuestion}/>
      <Countdown
        date={gameQuestion.questionAsked.getTime() + gameQuestion.questionTimeout}
        intervalDelay={0}
        precision={3}
        renderer={p => <QuestionCountdownBar timeLeft={p.total} totalTime={gameQuestion.questionTimeout}/>}
      />
      <br/>
      {answerContainer}
    </div>
  )
}


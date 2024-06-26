import {GameQuestionDto, QuestionType} from "../../../services/GameServiceTypes";
import {Box, Button, Paper, Stack, TextField, Typography, useTheme} from "@mui/material";
import React from "react";
import {QuestionPhrasePanel} from "./QuestionPhrasePanel";
import Countdown from "react-countdown";
import {QuestionCountdownBar} from "./QuestionCountdownBar";

export type QuestionContainerProps = {
  question: GameQuestionDto
  onAnswerQuestion: (answer: string) => void
}

export const QuestionContainer = (props: QuestionContainerProps) => {
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
      <TextField margin="normal" required fullWidth id="freeChoiceAnswer" label="Answer" name="answer" autoFocus sx={{margin: 2}}
      />
      <Button id="submitAnswer" type="submit" fullWidth variant="contained" sx={{ml: 2, mr: 2, mb: 2}}>Submit answer</Button>
    </Box>
  } else if (props.question.type === QuestionType.ESTIMATE) {
    answerContainer =
      <Box component="form" noValidate onSubmit={handleSubmit} sx={{my: 4, mx: 4, display: 'flex', flexDirection: 'column', alignItems: 'center'}}>
        <TextField margin="normal" required fullWidth id="estimationAnswer" label="Answer" name="answer" autoFocus type="number" sx={{margin: 2}}/>
        <Button id="submitAnswer" type="submit" fullWidth variant="contained" sx={{ml: 2, mr: 2, mb: 2}}>Submit answer</Button>
      </Box>
  }

  return (
    <div style={{width: '100%'}}>
      <QuestionPhrasePanel question={props.question}/>
      <Countdown
        date={Date.parse(props.question.questionAsked) + props.question.questionTimeout * 1000}
        intervalDelay={0}
        precision={3}
        renderer={p => <QuestionCountdownBar timeLeft={p.total} totalTime={props.question.questionTimeout * 1000} />}
      />
      <br/>
      {answerContainer}
    </div>
  )
}


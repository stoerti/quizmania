import {GameQuestion, Player} from "../../../domain/GameModel";
import {Button, Stack, useTheme} from "@mui/material";
import React from "react";
import {QuestionPhrasePanel} from "./QuestionPhrasePanel";
import {QuestionType} from "../../../services/GameEventTypes";

enum BuzzerStatus {
  OPEN,
  WAITING,
  SUCCESS,
  FAIL
}

type BuzzerProps = {
  status: BuzzerStatus,
  onClick: () => void
}

const Buzzer = (props: BuzzerProps) => {
  const theme = useTheme();

  let text;
  let color;
  switch (props.status) {
    case BuzzerStatus.OPEN: text = "HIT THE BUZZER"; color = theme.palette.warning.main; break;
    case BuzzerStatus.WAITING: text = "WAITING..."; color = theme.palette.warning.main; break;
    case BuzzerStatus.SUCCESS: text = "ANSWER QUESTION"; color = theme.palette.success.main; break;
    case BuzzerStatus.FAIL: text = "SOMEONE ELSE WAS FASTER"; color = theme.palette.error.main; break;
  }

  return <div id="buzzer" style={{
    backgroundColor: color,
    width: '200px',
    height: '200px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    textAlign: 'center',
    cursor: 'pointer',
    margin: 'auto',
    boxShadow: '#222 0px 0px 20px, inset #DDD 0px 0px 20px',
  }} onClick={props.onClick}>
    {text}
  </div>
}


export type BuzzerQuestionContainerProps = {
  gameQuestion: GameQuestion
  gameUser: Player
  onBuzzQuestion: () => void
}

export const BuzzerQuestionContainer = (props: BuzzerQuestionContainerProps) => {
  let answerContainer = null
  if (props.gameQuestion.question.type === QuestionType.CHOICE) {
    answerContainer = <Stack spacing={2} direction="column" justifyContent="center" alignItems="center" useFlexGap
                             flexWrap="wrap">
      {props.gameQuestion.question.answerOptions.map((answer, index) =>
        <Button key={index} variant="contained" sx={{width: '80%', maxWidth: '300px'}}>{answer}</Button>)}
    </Stack>
  }

  let buzzerContainer
  if (props.gameQuestion.buzzedPlayerIds.includes(props.gameUser.id)) {
    if (props.gameQuestion.currentBuzzWinnerId === props.gameUser.id) {
      buzzerContainer = <Buzzer status={BuzzerStatus.SUCCESS} onClick={() => {}}/>
    } else if(props.gameQuestion.currentBuzzWinnerId === null) {
      buzzerContainer = <Buzzer status={BuzzerStatus.WAITING} onClick={() => {}}/>
    } else {
      buzzerContainer = <Buzzer status={BuzzerStatus.FAIL} onClick={() => {}}/>
    }
  } else {
    buzzerContainer = <Buzzer status={BuzzerStatus.OPEN} onClick={props.onBuzzQuestion}/>
  }

  return (
    <div style={{width: '100%'}}>
      <QuestionPhrasePanel gameQuestion={props.gameQuestion}/>
      <br/>
      {answerContainer}
      <Stack sx={{my: 4, mx: 4}} spacing={2} direction="column" justifyContent="center" alignItems="center" useFlexGap
             flexWrap="wrap">
        {buzzerContainer}
      </Stack>
    </div>
  )
}


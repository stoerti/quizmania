import {GameQuestion, Player} from "../../../domain/GameModel";
import {Button, Stack, useTheme} from "@mui/material";
import React from "react";
import {QuestionPhrasePanel} from "./QuestionPhrasePanel";
import {QuestionType} from "../../../services/GameEventTypes";
import {Simulate} from "react-dom/test-utils";


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
    case BuzzerStatus.OPEN: text = "HIT THE BUZZER"; color = theme.palette.secondary.main; break;
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
    boxShadow: '#111 5px 5px 15px, inset #666 -5px -5px 10px',
  }} onClick={props.onClick}>
    {text}
  </div>
}


export type BuzzerQuestionContainerProps = {
  gameQuestion: GameQuestion
  player: Player
  onBuzzQuestion: () => void
}

export const BuzzerQuestionContainer = ({gameQuestion, player, onBuzzQuestion}: BuzzerQuestionContainerProps) => {
  let answerContainer = null
  if (gameQuestion.question.type === QuestionType.CHOICE) {
    answerContainer = <Stack spacing={2} direction="column" justifyContent="center" alignItems="center" useFlexGap
                             flexWrap="wrap">
      {gameQuestion.question.answerOptions.map((answer, index) =>
        <Button key={index} variant="contained" sx={{width: '80%', maxWidth: '300px'}}>{answer}</Button>)}
    </Stack>
  }

  let buzzerContainer
  if (gameQuestion.buzzedPlayerIds.includes(player.id)) {
    if (gameQuestion.currentBuzzWinnerId === player.id) {
      buzzerContainer = <Buzzer status={BuzzerStatus.SUCCESS} onClick={() => {}}/>
    } else if(gameQuestion.currentBuzzWinnerId == null) {
      buzzerContainer = <Buzzer status={BuzzerStatus.WAITING} onClick={() => {}}/>
    } else {
      buzzerContainer = <Buzzer status={BuzzerStatus.FAIL} onClick={() => {}}/>
    }
  } else {
    buzzerContainer = <Buzzer status={BuzzerStatus.OPEN} onClick={onBuzzQuestion}/>
  }

  return (
    <div style={{width: '100%'}}>
      <QuestionPhrasePanel gameQuestion={gameQuestion}/>
      <br/>
      {answerContainer}
      <Stack sx={{my: 4, mx: 4}} spacing={2} direction="column" justifyContent="center" alignItems="center" useFlexGap
             flexWrap="wrap">
        {buzzerContainer}
      </Stack>
    </div>
  )
}


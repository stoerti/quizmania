import {GameQuestionDto} from "../../../services/GameServiceTypes";
import {Paper, Typography, useTheme} from "@mui/material";
import React from "react";

export type QuestionPhrasePanelProps = {
    question: GameQuestionDto,
}


export const QuestionPhrasePanel = (props: QuestionPhrasePanelProps) => {
    const theme = useTheme()

    return (<Paper sx={{padding: 2, backgroundColor: theme.palette.primary.light}}>
        <Typography sx={{flex: '1 1 100%'}} variant="overline" component="div">
            Question {props.question.questionNumber}
        </Typography>
        <Typography sx={{flex: '1 1 100%'}} variant="h5" component="div">
            {props.question.phrase}
        </Typography>
    </Paper>)
}
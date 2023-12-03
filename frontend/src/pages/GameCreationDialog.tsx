import {
    Box,
    Button,
    Dialog,
    DialogTitle,
    FormControlLabel,
    FormGroup,
    MenuItem,
    Select,
    Switch,
    TextField
} from "@mui/material";
import React, {useEffect} from "react";
import {NewGameDto} from "../services/GameServiceTypes";
import {QuestionSetDto} from "../services/QuestionSetServiceTypes";
import {QuestionSetService} from "../services/QuestionSetService";

type GameCreationDialogProps = {
    open: boolean
    onClose: () => void
    onCreateGame: (newGame: NewGameDto) => void
}


export const GameCreationDialog = (props: GameCreationDialogProps) => {
    const {open, onClose, onCreateGame} = props
    const [questionSets, setQuestionSets] = React.useState<QuestionSetDto[]>([])

    const questionSetService = new QuestionSetService()

    useEffect(() => {
        questionSetService.searchQuestionSets(setQuestionSets)
    }, []);

    const handleClose = () => {
        onClose()
    }

    const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault()

        const data = new FormData(event.currentTarget);

        let gameName: string = data.get('gameName')!.toString()
        let maxPlayers: number = parseInt(data.get('maxPlayers')!.toString())
        let numQuestions: number = parseInt(data.get('numQuestions')!.toString())
        let questionSetId: string = data.get('questionSet')!.toString()
        let withModerator: boolean = data.get('moderator') === 'on'

        onCreateGame({
            name: gameName,
            config: {
                maxPlayers: maxPlayers,
                numQuestions: numQuestions,
                questionSetId: questionSetId
            },
            withModerator: withModerator
        })
    }

    return (
        <Dialog onClose={handleClose} open={open}>
            <DialogTitle>Create new game</DialogTitle>
            <Box component="form" noValidate onSubmit={handleSubmit} sx={{
                my: 4,
                mx: 4,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
            }}>
                <FormGroup>
                    <TextField margin="normal"
                               required
                               fullWidth
                               id="gameName"
                               label="Game name"
                               name="gameName"
                               autoFocus
                    />
                    <TextField margin="normal"
                               required
                               fullWidth
                               id="maxPlayers"
                               label="Number of players"
                               name="maxPlayers"
                               type="number"
                               defaultValue="5"
                    />
                    <TextField margin="normal"
                               required
                               fullWidth
                               id="numQuestions"
                               label="Number of questions"
                               name="numQuestions"
                               type="number"
                               defaultValue="10"
                    />
                    <Select margin="dense"
                        required
                        fullWidth
                        id="questionSet"
                        name="questionSet"
                        label="Question set"
                            value={questionSets != undefined && questionSets.length > 0 ? questionSets[0].id : undefined}
                    >
                        {questionSets.map((row) => (
                            <MenuItem key={row.id} value={row.id}>{row.name}</MenuItem>
                        ))}
                    </Select>
                    <FormControlLabel control={<Switch id="moderator" name="moderator" defaultValue="false"/>}
                                      label="Moderator"/>
                    <Button
                        id="createGameSubmit"
                        type="submit"
                        fullWidth
                        variant="contained"
                        sx={{ml: 2, mr: 2, mb: 2}}
                    >Create new game</Button>
                </FormGroup>
            </Box>
        </Dialog>
    )
}
export default GameCreationDialog
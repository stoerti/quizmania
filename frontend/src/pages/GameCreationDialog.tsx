import {Box, Button, Dialog, DialogTitle, FormControlLabel, FormGroup, Switch, TextField} from "@mui/material";
import React from "react";
import {NewGameDto} from "../services/GameServiceTypes";

type GameCreationDialogProps = {
    open: boolean
    onClose: () => void
    onCreateGame: (newGame: NewGameDto) => void
}


export const GameCreationDialog = (props: GameCreationDialogProps) => {
    const {open, onClose, onCreateGame} = props

    const handleClose = () => {
        onClose()
    }

    const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault()

        const data = new FormData(event.currentTarget);

        let gameName: string = data.get('gameName')!.toString()
        let maxPlayers: number = parseInt(data.get('maxPlayers')!.toString())
        let numQuestions: number = parseInt(data.get('numQuestions')!.toString())
        let withModerator: boolean = data.get('moderator') === 'on'

        onCreateGame({
            name: gameName,
            config: {
                maxPlayers: maxPlayers,
                numQuestions: numQuestions
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
                    <FormControlLabel control={<Switch id="moderator" name="moderator" defaultValue="false"/>}
                                      label="Moderator"/>
                    <Button
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
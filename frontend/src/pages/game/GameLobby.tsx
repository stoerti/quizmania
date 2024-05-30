import {GameService} from "../../services/GameService";
import Cookies from "js-cookie";
import {
    AppBar,
    Button,
    IconButton,
    List,
    ListItem,
    ListItemIcon,
    ListItemText, Stack,
    Toolbar,
    Tooltip,
    Typography
} from "@mui/material";
import PlayArrow from "@mui/icons-material/PlayArrow";
import Logout from "@mui/icons-material/Logout";
import Numbers from "@mui/icons-material/Numbers";
import RecordVoiceOver from "@mui/icons-material/RecordVoiceOver";
import Person from "@mui/icons-material/Person";
import React from "react";
import {GameDto} from "../../services/GameServiceTypes";

export type GameLobbyPageProps = {
    game: GameDto,
    gameService: GameService
}

export const GameLobbyPage = (props: GameLobbyPageProps) => {

    const onClickLeaveGame = () => {
        props.gameService.leaveGame(props.game.id, () => {
        })
    }

    const onClickStartGame = () => {
        props.gameService.startGame(props.game.id, () => {
        })
    }

    let startButton;
    if (props.game.moderator === Cookies.get('username') || props.game.creator === Cookies.get('username')) {
        startButton = <div
            style={{
                display: "flex",
                alignItems: "center"
            }}
        >
            <Button id="startGame" sx={{margin: 'auto'}} startIcon={<PlayArrow/>} variant="contained" onClick={onClickStartGame}>Start
                game</Button>
        </div>
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
                    <Tooltip title="Leave game">
                        <IconButton color="inherit" onClick={onClickLeaveGame}>
                            <Logout/>
                        </IconButton>
                    </Tooltip>
                </Toolbar>
            </AppBar>
            <Stack spacing={2}>
                <List dense={false}>
                    <ListItem key="numberOfQuestions">
                        <ListItemIcon>
                            <Numbers/>
                        </ListItemIcon>
                        <ListItemText
                            primary={props.game.numQuestions}
                            secondary='number of questions'
                        />
                    </ListItem>
                    <ListItem key="gameMaster">
                        <ListItemIcon>
                            <RecordVoiceOver/>
                        </ListItemIcon>
                        <ListItemText
                            primary={props.game.moderator !== undefined && props.game.moderator != null ? props.game.moderator : 'Computer'}
                            secondary='game master'
                        />
                    </ListItem>
                </List>
                {startButton}
                <Typography variant="h6" component="div" sx={{pl: 2}}>
                    Participants
                </Typography>
                <List dense={false}>
                    {props.game.users.map((row) => (
                        <ListItem key={row.id}>
                            <ListItemIcon>
                                <Person/>
                            </ListItemIcon>
                            <ListItemText
                                primary={row.name}
                            />
                        </ListItem>
                    ))}
                </List>
            </Stack>


        </div>
    )
}

import {GameDto} from "../../services/GameServiceTypes";
import {GameService} from "../../services/GameService";
import React from "react";
import {
    AppBar,
    Box,
    Button,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Toolbar,
    Typography
} from "@mui/material";
import Logout from "@mui/icons-material/Logout";
import EmojiEvents from "@mui/icons-material/EmojiEvents";
import {amber, brown, grey} from "@mui/material/colors";

export type GameFinishedPageProps = {
    game: GameDto,
    gameService: GameService,
    onClickLeaveGame: () => void
}

export const GameFinishedPage = (props: GameFinishedPageProps) => {

    return (
        <div>
            <AppBar position="static">
                <Toolbar>
                    <Typography
                        sx={{flex: '1 1 100%'}}
                        variant="h6"
                        component="div"
                    >
                        {props.game.name} - Results
                    </Typography>
                </Toolbar>
            </AppBar>
            <Table aria-label="simple table">
                <TableHead>
                    <TableRow>
                        <TableCell></TableCell>
                        <TableCell>Name</TableCell>
                        <TableCell align="right">Points</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {[...props.game.users].sort((u1, u2) => u2.points - u1.points).map((row, index) => {
                        let cup;
                        switch (index) {
                            case 0: cup = <EmojiEvents sx={{color: amber[500]}} />; break;
                            case 1: cup = <EmojiEvents sx={{color: grey[500]}} />; break;
                            case 2: cup = <EmojiEvents sx={{color: brown[700]}} />; break;
                        }
                        return (
                            <TableRow
                                key={row.id}
                                sx={{'&:last-child td, &:last-child th': {border: 0}}}
                            >
                                <TableCell align="left">{cup}</TableCell>
                                <TableCell component="th" scope="row">
                                    <Typography
                                        variant="body1"
                                        component="div"
                                    >
                                        {row.name}
                                    </Typography>
                                </TableCell>
                                <TableCell align="right">{row.points}</TableCell>
                            </TableRow>
                        )
                    })}
                </TableBody>
            </Table>
            <Box
                sx={{
                    display: "flex",
                    alignItems: "center",
                    marginTop: 2
                }}
            >
                <Button sx={{margin: 'auto'}} startIcon={<Logout/>} variant="contained"
                        onClick={props.onClickLeaveGame}>Leave game</Button>
            </Box>
        </div>
    )
}

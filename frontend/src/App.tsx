import React from 'react';

import {SnackbarProvider} from 'material-ui-snackbar-provider'

import QuizmaniaMainUI from "./pages/QuizmaniaMainUI";
import {createTheme, ThemeProvider} from "@mui/material";

const SOCKET_URL = 'ws://localhost:8080/ws-message';

interface MessageDto {
    message: string;
}

const App = () => {
    const theme = createTheme();

    return (
        <div>
            <ThemeProvider theme={theme}>
                <SnackbarProvider SnackbarProps={{autoHideDuration: 4000}}>
                    <QuizmaniaMainUI/>
                </SnackbarProvider>
            </ThemeProvider>
        </div>
    )

    //   let onMessageReceived = (msg: MessageDto) => {
    //       setMessage(msg.message);
    //   }
    //
    //   const client = new Client({
    //       brokerURL: SOCKET_URL,
    //       onConnect: () => {
    //           client.subscribe('/chat/message', message =>
    //               onMessageReceived(JSON.parse(message.body))
    //           );
    //       },
    //       onWebSocketError: (e: Event) => {
    //           setMessage(e.type)
    //       }
    //   });
    //
    //   client.activate();
    //
    // return (
    //     <div>
    //       <div>{message}</div>
    //       <Button variant="contained">Hello World</Button>
    //     </div>
    // );
}

export default App;
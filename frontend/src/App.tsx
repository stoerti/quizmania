import React from 'react';

import {SnackbarProvider} from 'material-ui-snackbar-provider'

import QuizmaniaMainUI from "./pages/QuizmaniaMainUI";
import {createTheme, CssBaseline, ThemeProvider} from "@mui/material";

const App = () => {
    const theme = createTheme();

    return (
        <div style={{backgroundColor: theme.palette.background.default, height: '100%'}}>
            <ThemeProvider theme={theme}>
              <CssBaseline />
                <SnackbarProvider SnackbarProps={{autoHideDuration: 4000}}>
                    <QuizmaniaMainUI/>
                </SnackbarProvider>
            </ThemeProvider>
        </div>
    )
}

export default App;

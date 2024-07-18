import React from 'react';

import {SnackbarProvider} from 'material-ui-snackbar-provider'

import QuizmaniaMainUI from "./pages/QuizmaniaMainUI";
import {createTheme, CssBaseline, ThemeProvider} from "@mui/material";

const App = () => {
  const theme = createTheme({
    palette: {
      mode: 'light',
      primary: {
        main: '#102924',
        contrastText: '#c8ffa5',
      },
      secondary: {
        main: '#c7cfeb',
      },
      text: {
        primary: '#102924',
      },
      error: {
        main: '#EB4B2D',
      },
      info: {
        main: '#C7CFEB',
      },
      success: {
        main: '#1EC85A',
      },
      warning: {
        main: '#ff9800',
      },
      divider: '#102924',
    },
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: '20px'
          }
        },
      }
    }
  })

  return (
    <div style={{backgroundColor: theme.palette.background.default, height: '100%'}}>
      <ThemeProvider theme={theme}>
        <CssBaseline/>
        <SnackbarProvider SnackbarProps={{autoHideDuration: 4000}}>
          <QuizmaniaMainUI/>
        </SnackbarProvider>
      </ThemeProvider>
    </div>
  )
}

export default App;

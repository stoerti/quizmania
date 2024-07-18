import { setHeadlessWhen, setCommonPlugins, setWindowSize } from '@codeceptjs/configure';
// turn on headless mode when running with HEADLESS=true environment variable
// export HEADLESS=true && npx codeceptjs run
setHeadlessWhen(process.env.HEADLESS);
setWindowSize(580, 960);

// enable all common plugins https://github.com/codeceptjs/configure#setcommonplugins
setCommonPlugins();


export const config: CodeceptJS.MainConfig = {
  tests: './test/*.tsx',
  output: './output',
  helpers: {
    Playwright: {
      browser: 'chromium',
      url: 'http://localhost:3000',
      show: true
    }
  },
  include: {
    I: './steps_file',
    loginPage: './pages/login.ts',
    lobbyPage: './pages/lobby.ts',
    gameRoomPage: './pages/gameRoom.ts',

  },
  name: 'e2e'
}

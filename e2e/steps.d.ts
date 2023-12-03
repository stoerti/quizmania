/// <reference types='codeceptjs' />
type steps_file = typeof import('./steps_file');
type loginPage = typeof import('./pages/login');
type lobbyPage = typeof import('./pages/lobby');

declare namespace CodeceptJS {
    interface SupportObject {
        I: I,
        current: any,
        loginPage: loginPage,
        lobbyPage: lobbyPage
    }

    interface Methods extends Playwright {
    }

    interface I extends ReturnType<steps_file> {
    }

    namespace Translation {
        interface Actions {
        }
    }
}

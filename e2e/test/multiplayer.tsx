Feature('create_game');

Scenario('multiplayer',  ({ I , loginPage, lobbyPage}) => {
    let gameName = "Game " + Math.floor(Math.random() * 100000)
    let username1 = "User 1" + Math.floor(Math.random() * 100000)
    let username2 = "User 2" + Math.floor(Math.random() * 100000)
    let username3 = "User 3" + Math.floor(Math.random() * 100000)
    let username4 = "User 4" + Math.floor(Math.random() * 100000)

    loginPage.logInWithUsername(username1)
    I.wait(1)

    lobbyPage.createGame(gameName)
    I.wait(1)

    session('player2', () => {
        loginPage.logInWithUsername(username2)
        I.wait(1)
        lobbyPage.joinGame(gameName)
    });

    session('player3', () => {
        loginPage.logInWithUsername(username3)
        I.wait(1)
        lobbyPage.joinGame(gameName)
    });

    I.wait(50000)

    I.waitForText(username2)
    I.waitForText(username3)

});

Scenario('multiplayser_moderated',  ({ I , loginPage, lobbyPage}) => {
    let gameName = "Game " + Math.floor(Math.random() * 100000)
    let username1 = "User 1" + Math.floor(Math.random() * 100000)
    let username2 = "User 2" + Math.floor(Math.random() * 100000)
    let username3 = "User 3" + Math.floor(Math.random() * 100000)
    let username4 = "User 4" + Math.floor(Math.random() * 100000)

    loginPage.logInWithUsername(username1)
    I.wait(1)

    lobbyPage.createGame(gameName, true)
    I.wait(1)

    session('player2', () => {
        loginPage.logInWithUsername(username2)
        I.wait(1)
        lobbyPage.joinGame(gameName)
    });

    session('player3', () => {
        loginPage.logInWithUsername(username3)
        I.wait(1)
        lobbyPage.joinGame(gameName)
    });

    I.wait(50000)

    I.waitForText(username2)
    I.waitForText(username3)

});

Feature('create_game');

Scenario('multiplayer_buzzer', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  let gameName = "Middle earth quiz"
  let moderator = "Elrond"
  let username1 = "Frodo"
  let username2 = "Gimli"
  let username3 = "Legolas"

  loginPage.logInWithUsername(moderator)
  I.wait(1)

  lobbyPage.createGame(gameName, "werkstatt_finale", true, true)

  session('player1', () => {
    loginPage.logInWithUsername(username1)
    lobbyPage.joinGame(gameName)
  });

  session('player2', () => {
    loginPage.logInWithUsername(username2)
    lobbyPage.joinGame(gameName)
  });

  session('player3', () => {
    loginPage.logInWithUsername(username3)
    lobbyPage.joinGame(gameName)
  });

  I.waitForText(username1)
  I.waitForText(username2)
  I.waitForText(username3)

  I.wait(10)

  gameRoomPage.startGame()

  I.wait(1000)

  // -----------------------------
  // QUESTION 1
  // -----------------------------


  session('player1', () => {
    gameRoomPage.buzz()
  })
  session('player2', () => {
    gameRoomPage.buzz()
  })
  session('player1', () => {
    gameRoomPage.wonBuzz()
  })
  session('player2', () => {
    gameRoomPage.lostBuzz()
  })

  I.waitForText(username1, 5, {id: 'buzzWinner'})

  I.wait(1000)

  gameRoomPage.acceptBuzzerAnswer()
  gameRoomPage.nextQuestion()

  // -----------------------------
  // QUESTION 2
  // -----------------------------

  session('player2', () => {
    gameRoomPage.buzz()
  })
  session('player1', () => {
    gameRoomPage.buzz()
  })
  session('player3', () => {
    gameRoomPage.buzz()
  })

  I.waitForText(username2, 5, {id: 'buzzWinner'})
  gameRoomPage.rejectBuzzerAnswer()
  I.waitForText(username1, 5, {id: 'buzzWinner'})
  gameRoomPage.rejectBuzzerAnswer()
  I.waitForText(username3, 5, {id: 'buzzWinner'})
  gameRoomPage.acceptBuzzerAnswer()
  gameRoomPage.nextQuestion()

  // -----------------------------
  // QUESTION 3
  // -----------------------------

  gameRoomPage.closeQuestion()
  gameRoomPage.nextQuestion()

  I.waitForText("Results")
  I.wait(1)
});

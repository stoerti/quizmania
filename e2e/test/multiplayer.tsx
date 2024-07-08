Feature('create_game');

Scenario('multiplayer', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  let gameName = "Game " + Math.floor(Math.random() * 100000)
  let username1 = "User 1" + Math.floor(Math.random() * 100000)
  let username2 = "User 2" + Math.floor(Math.random() * 100000)
  let username3 = "User 3" + Math.floor(Math.random() * 100000)
  let username4 = "User 4" + Math.floor(Math.random() * 100000)

  loginPage.logInWithUsername(username1)
  I.wait(1)

  lobbyPage.createGame(gameName, "test01")
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

Scenario('multiplayer_moderated', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  let gameName = "Game " + Math.floor(Math.random() * 100000)
  let moderator = "Moderator " + Math.floor(Math.random() * 100000)
  let username1 = "User " + Math.floor(Math.random() * 100000)
  let username2 = "User " + Math.floor(Math.random() * 100000)
  let username3 = "User " + Math.floor(Math.random() * 100000)

  loginPage.logInWithUsername(moderator)
  I.wait(1)

  lobbyPage.createGame(gameName, "test01", true)
  I.wait(1)

  session('player1', () => {
    loginPage.logInWithUsername(username1)
    I.wait(1)
    lobbyPage.joinGame(gameName)
  });

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

  I.waitForText(username1)
  I.waitForText(username2)
  I.waitForText(username3)

  gameRoomPage.startGame()

  session('player1', () => {
    gameRoomPage.answerChoiceQuestion("Banone")
  })

  I.wait(2)
  session('player2', () => {
    gameRoomPage.answerChoiceQuestion("Banone")
  })
  I.wait(2)
  session('player3', () => {
    gameRoomPage.answerChoiceQuestion("Gürkin")
  })

  I.waitForText("Banone", 5, "tr:has-text('"+username1+"')")
  I.waitForText("Banone", 5, "tr:has-text('"+username2+"')")
  I.waitForText("Gürkin", 5, "tr:has-text('"+username3+"')")

  gameRoomPage.nextQuestion()

  session('player1', () => {
    gameRoomPage.answerEstimateQuestion(800)
  })
  session('player2', () => {
    gameRoomPage.answerEstimateQuestion(900)
  })
  session('player3', () => {
    gameRoomPage.answerEstimateQuestion(1000)
  })

  I.waitForText("800", 5, "tr:has-text('"+username1+"')")
  I.waitForText("900", 5, "tr:has-text('"+username2+"')")
  I.waitForText("1000", 5, "tr:has-text('"+username3+"')")

  I.wait(1)
  gameRoomPage.nextQuestion()

  session('player1', () => {
    gameRoomPage.answerFreeQuestion("Neil armstrong")
  })
  session('player2', () => {
    gameRoomPage.answerFreeQuestion("NielArmstrong")
  })
  I.wait(6)

  I.waitForText("Neil armstrong", 5, "tr:has-text('"+username1+"')")
  I.waitForText("NielArmstrong", 5, "tr:has-text('"+username2+"')")
  I.dontSeeElement("tr:has-text('"+username3+"')")


  gameRoomPage.rateQuestion()
  gameRoomPage.nextQuestion()

  I.waitForText("Results")

  I.wait(2)
});

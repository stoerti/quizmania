Feature('create_game');

Scenario('multiplayer', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  let gameName = "Game " + Math.floor(Math.random() * 100000)
  let username1 = "User 1" + Math.floor(Math.random() * 100000)
  let username2 = "User 2" + Math.floor(Math.random() * 100000)
  let username3 = "User 3" + Math.floor(Math.random() * 100000)

  loginPage.logInWithUsername(username1)
  I.wait(1)

  lobbyPage.createGame(gameName, "quizmas2025")
  I.wait(1)

  session('player2', () => {
    loginPage.logInWithUsername(username2)
    lobbyPage.joinGame(gameName)
  });

  session('player3', () => {
    loginPage.logInWithUsername(username3)
    lobbyPage.joinGame(gameName)
  });

  I.waitForText(username2)
  I.waitForText(username3)

  I.wait(10000)

});

Scenario('multiplayer_moderated', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  let gameName = "Game " + Math.floor(Math.random() * 100000)
//  let moderator = "Moderator " + Math.floor(Math.random() * 100000)
//  let username1 = "User " + Math.floor(Math.random() * 100000)
//  let username2 = "User " + Math.floor(Math.random() * 100000)
//  let username3 = "User " + Math.floor(Math.random() * 100000)
  let moderator = "Moderator"
  let username1 = "Alice"
  let username2 = "Bob"
  let username3 = "John Doe"

  loginPage.logInWithUsername(moderator)
  I.wait(1)

  lobbyPage.createGame(gameName, "ali2025_test", true)
  I.wait(1)

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
//    lobbyPage.joinGame(gameName)
  });

  I.waitForText(username1)
  I.waitForText(username2)
//  I.waitForText(username3)

  I.wait(4)

  gameRoomPage.startGame()

  I.wait(4000)
  session('player1', () => {
    gameRoomPage.answerChoiceQuestion("3")
  })

  I.wait(1)
  session('player2', () => {
    gameRoomPage.answerChoiceQuestion("9")
  })
  I.wait(1)
  session('player3', () => {
    gameRoomPage.answerChoiceQuestion("7")
  })

  I.waitForText("3", 5, "tr:has-text('"+username1+"')")
  I.waitForText("9", 5, "tr:has-text('"+username2+"')")
  I.waitForText("7", 5, "tr:has-text('"+username3+"')")

  I.wait(3)

  gameRoomPage.nextQuestion()

  I.wait(1)
  session('player1', () => {
    gameRoomPage.answerEstimateQuestion(800)
  })
  I.wait(1)
  session('player2', () => {
    gameRoomPage.answerEstimateQuestion(900)
  })
  I.wait(1)
  session('player3', () => {
    gameRoomPage.answerEstimateQuestion(1000)
  })

  I.waitForText("800", 5, "tr:has-text('"+username1+"')")
  I.waitForText("900", 5, "tr:has-text('"+username2+"')")
  I.waitForText("1000", 5, "tr:has-text('"+username3+"')")

  I.wait(4)
  gameRoomPage.nextQuestion()

  session('player1', () => {
    gameRoomPage.answerFreeQuestion("Neil armstrong")
  })
  session('player2', () => {
    gameRoomPage.answerFreeQuestion("NielArmstrong")
  })
  I.wait(10)

  I.waitForText("Neil armstrong", 5, "tr:has-text('"+username1+"')")
  I.waitForText("NielArmstrong", 5, "tr:has-text('"+username2+"')")
  I.dontSeeElement("tr:has-text('"+username3+"')")


  gameRoomPage.rateQuestion()
  gameRoomPage.nextQuestion()

  I.waitForText("Results")

  I.wait(10)
});

Scenario('multiplayer_sort_questions', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  let gameName = "Sort Game " + Math.floor(Math.random() * 100000)
  let moderator = "Moderator " + Math.floor(Math.random() * 100000)
  let username1 = "Alice " + Math.floor(Math.random() * 100000)
  let username2 = "Bob " + Math.floor(Math.random() * 100000)
  let username3 = "Charlie " + Math.floor(Math.random() * 100000)

  // Moderator logs in and creates the game
  loginPage.logInWithUsername(moderator)
  I.wait(1)

  lobbyPage.createGame(gameName, "test_sort_e2e", true)
  I.wait(1)

  // Three players join the game
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
    lobbyPage.joinGameAsSpectator(gameName)
  });

  I.waitForText(username1)
  I.waitForText(username2)

  I.wait(10)

  // Moderator starts the game
  gameRoomPage.startGame()

  I.wait(2)

  // Question 1: Sort planets by distance from Sun
  // Correct answer: Mercury, Venus, Earth, Mars
  // Initial order in UI: Earth, Mars, Mercury, Venus (indices 0,1,2,3)

  // Player 1 (Alice): Perfect answer - Mercury, Venus, Earth, Mars
  session('player1', () => {
    gameRoomPage.answerSortQuestion([
      {index: 1, direction: 'down'}, // Mars: 1->2 (Earth, Mercury, Mars, Venus)
      {index: 2, direction: 'down'}, // Mars: 2->3 (Earth, Mercury, Venus, Mars)
      {index: 0, direction: 'down'}, // Mars: 0->1 (Mercury, Earth, Venus, Mars)
      {index: 1, direction: 'down'}, // Mars: 0->1 (Mercury, Venus, Earth, Mars)
    ])
  })

  I.wait(1)

  // Player 2 (Bob): One swap wrong - Mercury, Earth, Venus, Mars
  session('player2', () => {
    gameRoomPage.answerSortQuestion([
      {index: 1, direction: 'down'}, // Mars: 1->2 (Earth, Mercury, Mars, Venus)
      {index: 2, direction: 'down'}, // Mars: 2->3 (Earth, Mercury, Venus, Mars)
      {index: 0, direction: 'down'}, // Mars: 0->1 (Mercury, Earth, Venus, Mars)
    ])
  })

  I.wait(8)

  // Wait for answers to be submitted
  I.waitForText("Mercu...", 10)

  I.wait(2)

  // Verify scores after first question
  // Alice should have 25 points (perfect), Bob should have 16 points (1 swap with 4 items)
  I.see("25", "tr:has-text('"+username1+"')")
  I.see("16", "tr:has-text('"+username2+"')")

  I.wait(2)
  gameRoomPage.nextQuestion()
  I.wait(2)

  // Question 2: Sort countries by population (smallest to largest)
  // Correct answer: Iceland, Switzerland, Belgium, Netherlands
  // Initial order in UI: Belgium, Iceland, Netherlands, Switzerland (indices 0,1,2,3)

  // Player 1 (Alice): Answer - Belgium, Iceland, Switzerland, Netherlands
  // Correct order: Iceland, Switzerland, Belgium, Netherlands
  // Using Kendall tau distance to calculate pairwise inversions:
  // - Belgium before Iceland: wrong (should be after) = 1 inversion
  // - Belgium before Switzerland: wrong (should be after) = 1 inversion
  // - Belgium before Netherlands: wrong (should be after) = 1 inversion
  // - Iceland before Switzerland: correct = 0
  // - Iceland before Belgium: wrong (should be after) = already counted
  // - Switzerland before Netherlands: correct = 0
  // Total distance: 3 inversions
  // Score: (1 - 3/6) * 20 = (0.5) * 20 = 10 points
  session('player1', () => {
    gameRoomPage.answerSortQuestion([
      {index: 3, direction: 'up'}, // Switzerland: 3->2 (Belgium, Iceland, Switzerland, Netherlands)
    ])
  })

  I.wait(1)

  // Player 2 (Bob): Perfect answer - Iceland, Switzerland, Belgium, Netherlands
  session('player2', () => {
    gameRoomPage.answerSortQuestion([
      {index: 1, direction: 'up'},   // Iceland: 1->0 (Iceland, Belgium, Netherlands, Switzerland)
      {index: 3, direction: 'up'},   // Switzerland: 3->2 (Iceland, Belgium, Switzerland, Netherlands)
      {index: 2, direction: 'up'},   // Switzerland: 2->1 (Iceland, Switzerland, Belgium, Netherlands)
    ])
  })

  I.wait(8)

  // Wait for answers to be submitted
  I.waitForText("Icela...", 10)

  I.wait(2)

  // Verify final scores
  // Alice: 25 (Q1 perfect) + 10 (Q2, distance 3 with max 6) = 35 total
  // Bob: 16 (Q1, distance 1 with max 6) + 25 (Q2 perfect) = 41 total
  I.see("35", "tr:has-text('"+username1+"')") // Alice's total
  I.see("41", "tr:has-text('"+username2+"')") // Bob's total

  I.wait(2)
});



Scenario('many_multiplayer_moderated', ({I, loginPage, lobbyPage, gameRoomPage}) => {
  const gameName = "Game " + Math.floor(Math.random() * 100000)
  const numPlayers = 30

  const moderator = "Moderator " + Math.floor(Math.random() * 100000)
  const players = [...Array(numPlayers)].map((_, index) => { return "User "+index + Math.floor(Math.random() * 100000)})


  loginPage.logInWithUsername(moderator)
  I.wait(1)

  lobbyPage.createGame(gameName, "test01", true)
  I.wait(1)

  players.forEach((player, index) => {
    session('player'+index, () => {
      loginPage.logInWithUsername(player)
      lobbyPage.joinGame(gameName)
    });
  })

  players.forEach((player) => {
    I.waitForText(player)
  })

  I.wait(5)

  gameRoomPage.startGame()

  I.wait(1)

  players.forEach((player, index) => {
    session('player'+index, () => {
      gameRoomPage.answerChoiceQuestion(Math.random() < 0.5 ? "9" : "7")
    })
  })

});

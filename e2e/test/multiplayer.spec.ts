import {expect, test, type Browser, type BrowserContext} from '@playwright/test';
import {GameClient} from './game-client';

type MultiplayerGame = {
  moderator: GameClient;
  alice: GameClient;
  bob: GameClient;
  contexts: BrowserContext[];
};

async function createMultiplayerGame(
  browser: Browser,
  questionSetId: string,
  testName: string,
): Promise<MultiplayerGame> {
  const contexts = await Promise.all([
    browser.newContext(),
    browser.newContext(),
    browser.newContext(),
  ]);
  const [moderatorPage, alicePage, bobPage] = await Promise.all(contexts.map(context => context.newPage()));

  const moderator = new GameClient(moderatorPage);
  const alice = new GameClient(alicePage);
  const bob = new GameClient(bobPage);
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 10_000)}`;
  const gameName = `${testName} ${suffix}`;

  await moderator.login(`Moderator ${suffix}`);
  await moderator.createModeratedGame(gameName, questionSetId);

  await alice.login(`Alice ${suffix}`);
  await alice.joinGame(gameName);
  await bob.login(`Bob ${suffix}`);
  await bob.joinGame(gameName);

  await expect(moderatorPage.getByText(`Alice ${suffix}`, {exact: true})).toBeVisible();
  await expect(moderatorPage.getByText(`Bob ${suffix}`, {exact: true})).toBeVisible();

  return {moderator, alice, bob, contexts};
}

async function closeContexts(contexts: BrowserContext[]) {
  await Promise.all(contexts.map(context => context.close()));
}

test.describe('moderated multiplayer games', () => {
  test('collective questions close after everyone answers and after the deadline', async ({browser}) => {
    const game = await createMultiplayerGame(browser, 'e2e_collective', 'Collective E2E');

    try {
      await game.moderator.page.locator('#startGame').click();

      await expect(game.alice.page.getByText('How many Nazgul are there?', {exact: true})).toBeVisible();
      await game.alice.answerChoice('9');
      await expect(game.alice.page.getByText('1 of 2 players answered', {exact: true})).toBeVisible();

      await game.bob.answerChoice('7');
      await expect(game.moderator.page.locator('#nextQuestion')).toBeVisible({timeout: 2_000});

      await game.moderator.page.locator('#nextQuestion').click();
      await expect(game.moderator.page.getByText('Wie heißt die Hauptstadt von Deutschland?', {exact: true})).toBeVisible();
      await expect(game.moderator.page.locator('#closeQuestion')).toBeVisible();

      // Nobody answers question two. Its three-second aggregate deadline must close and score it.
      await expect(game.moderator.page.locator('#nextQuestion')).toBeVisible({timeout: 6_000});
      await game.moderator.page.locator('#nextQuestion').click();

      await expect(game.moderator.page.getByText('Results', {exact: false})).toBeVisible();
    } finally {
      await closeContexts(game.contexts);
    }
  });

  test('buzzer questions cover a sole winner, reopening, and a queued second winner', async ({browser}) => {
    const game = await createMultiplayerGame(browser, 'e2e_buzzer', 'Buzzer E2E');

    try {
      await game.moderator.page.locator('#startGame').click();

      // Question 1: Alice is the only player to buzz and wins.
      await expect(game.alice.page.getByText('Was ist der größte Planet des Sonnensystems?', {exact: true})).toBeVisible();
      await game.alice.buzz();
      await expect(game.moderator.page.locator('#buzzWinner')).toContainText('Alice', {timeout: 2_000});
      await expect(game.alice.page.locator('#buzzer')).toHaveText('ANSWER QUESTION');
      await game.moderator.page.locator('#acceptAnswer').click();
      await expect(game.moderator.page.locator('#nextQuestion')).toBeVisible();
      await game.moderator.page.locator('#nextQuestion').click();

      // Question 2: Alice is wrong, the buzzer reopens, and Bob then wins.
      await expect(game.bob.page.getByText('Wo wurde die Currywurst erfunden?', {exact: true})).toBeVisible();
      await game.alice.buzz();
      await expect(game.moderator.page.locator('#buzzWinner')).toContainText('Alice', {timeout: 2_000});
      await game.moderator.page.locator('#rejectAnswer').click();
      await expect(game.bob.page.locator('#buzzer')).toHaveText('HIT THE BUZZER');
      await game.bob.buzz();
      await expect(game.moderator.page.locator('#buzzWinner')).toContainText('Bob', {timeout: 2_000});
      await game.moderator.page.locator('#acceptAnswer').click();
      await expect(game.moderator.page.locator('#nextQuestion')).toBeVisible();
      await game.moderator.page.locator('#nextQuestion').click();

      // Question 3: both buzz inside the 500 ms window. Alice is first; after rejection Bob is promoted.
      await expect(game.alice.page.getByText('An welchem Fluss liegt Hamburg?', {exact: true})).toBeVisible();
      await game.alice.buzz();
      await game.alice.page.waitForTimeout(100);
      await game.bob.buzz();
      await expect(game.moderator.page.locator('#buzzWinner')).toContainText('Alice', {timeout: 2_000});
      await game.moderator.page.locator('#rejectAnswer').click();
      await expect(game.moderator.page.locator('#buzzWinner')).toContainText('Bob');
      await expect(game.bob.page.locator('#buzzer')).toHaveText('ANSWER QUESTION');
      await game.moderator.page.locator('#acceptAnswer').click();
      await expect(game.moderator.page.locator('#nextQuestion')).toBeVisible();
      await game.moderator.page.locator('#nextQuestion').click();

      await expect(game.moderator.page.getByText('Results', {exact: false})).toBeVisible();
    } finally {
      await closeContexts(game.contexts);
    }
  });
});

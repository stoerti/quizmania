import {expect, type Page} from '@playwright/test';

export class GameClient {
  constructor(readonly page: Page) {}

  async login(username: string) {
    await this.page.goto('/');
    await this.page.locator('#username').fill(username);
    await this.page.locator('#submitLogin').click();
    await expect(this.page.locator('#createGame')).toBeVisible();
  }

  async createModeratedGame(gameName: string, questionSetId: string) {
    await this.page.locator('#createGame').click();
    await this.page.locator('#gameName').fill(gameName);
    await this.page.locator('#questionSet').click();
    await this.page.locator(`li[data-value="${questionSetId}"]`).click();
    await this.page.locator('#moderator').check();
    await this.page.locator('#createGameSubmit').click();
    await expect(this.page.getByText('Participants', {exact: false})).toBeVisible();
  }

  async joinGame(gameName: string) {
    const gameRow = this.page.getByRole('row').filter({hasText: gameName});
    await expect(gameRow).toBeVisible();
    await gameRow.locator('button[name="join"]').click();
    await expect(this.page.getByText('Participants', {exact: false})).toBeVisible();
  }

  async answerChoice(answer: string) {
    await this.page.getByRole('button', {name: answer, exact: true}).click();
  }

  async buzz() {
    await this.page.locator('#buzzer').click();
  }
}

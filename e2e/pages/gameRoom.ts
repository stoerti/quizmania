import {expect} from "playwright/test";

const { I } = inject();

export function startGame() {
  I.click({id: 'startGame'});
}

export function nextQuestion() {
  I.click({id: 'nextQuestion'});
}

export function rateQuestion() {
  I.click({id: 'rateQuestion'});
}

export function closeQuestion() {
  I.click({id: 'closeQuestion'});
}

export function acceptBuzzerAnswer() {
  I.click({id: 'acceptAnswer'});
}

export function rejectBuzzerAnswer() {
  I.click({id: 'rejectAnswer'});
}

export function buzz() {
  I.click({id: 'buzzer'});
}

export function answerChoiceQuestion(answer: string) {
  I.click('button:has-text("'+answer+'")');
}

export function answerFreeQuestion(answer: string) {
  I.fillField({id: 'freeChoiceAnswer'}, answer);
  I.click({id: 'submitAnswer'});
}

export function answerEstimateQuestion(answer: number) {
  I.fillField({id: 'estimationAnswer'}, answer);
  I.click({id: 'submitAnswer'});
}

// then

export function wonBuzz() {
  I.waitForText("ANSWER QUESTION", 2, {id: 'buzzer'})
}

export function lostBuzz() {
  I.waitForText("SOMEONE ELSE WAS FASTER", 2, {id: 'buzzer'})
}




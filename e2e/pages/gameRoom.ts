import {expect} from "playwright/test";

const { I } = inject();

export function startGame() {
  I.click({id: 'startGame'});
}

export function startRound() {
  I.click({id: 'startRound'});
}

export function closeRound() {
  I.click({id: 'closeRound'});
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

export function answerSortQuestion(moves: {index: number, direction: 'up' | 'down'}[]) {
  // Perform the sort moves
  moves.forEach(move => {
    const buttonId = move.direction === 'up' ? `sort-up-${move.index}` : `sort-down-${move.index}`;
    I.click({id: buttonId});
    I.wait(0.1); // Small wait between moves for stability
  });
  
  // Submit the answer
  I.click({id: 'submitSortAnswer'});
}

// then

export function wonBuzz() {
  I.waitForText("ANSWER QUESTION", 2, {id: 'buzzer'})
}

export function lostBuzz() {
  I.waitForText("SOMEONE ELSE WAS FASTER", 2, {id: 'buzzer'})
}




package org.quizmania

import org.springframework.messaging.simp.SimpMessagingTemplate

class UserController(
    var template: SimpMessagingTemplate
) {
}
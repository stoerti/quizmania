package org.quizmania

import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping(value = ["/api/chat"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Slf4j
class ChatController(
    var template: SimpMessagingTemplate
) {
    private val log: Logger = LoggerFactory.getLogger(ChatController::class.java)

    @PutMapping("/")
    fun put(@RequestParam("message") message: String) {
        log.info(message)
        template.convertAndSend("/chat/message", TextMessageDTO(message))
    }

    @PostMapping("/send")
    fun sendMessage(@RequestBody textMessageDTO: TextMessageDTO): ResponseEntity<Void> {
        template.convertAndSend("/chat/message", textMessageDTO)
        return ResponseEntity(HttpStatus.OK)
    }

    @MessageMapping("/sendMessage")
    fun receiveMessage(@Payload textMessageDTO: TextMessageDTO) {
        log.info("Socket: $textMessageDTO")
    }

    @SendTo("/chat/message")
    fun broadcastMessage(@Payload textMessageDTO: TextMessageDTO): TextMessageDTO {
        return textMessageDTO
    }
}

class TextMessageDTO(
    var message: String
) {}
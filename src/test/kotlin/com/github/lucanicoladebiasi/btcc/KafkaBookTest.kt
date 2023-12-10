package com.github.lucanicoladebiasi.btcc

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9093", "port=9093"])
class KafkaBookTest {

    val blockingQueue: BlockingQueue<String> = LinkedBlockingDeque(1)

    @Autowired
    private val controller: Controller? = null

    @LocalServerPort
    private val port = 0

    @Autowired
    private val restTemplate: TestRestTemplate? = null

    @KafkaListener(topics = ["book"], groupId = "btcc")
    fun listen(data: String) {
        blockingQueue.put(data)
    }

    @Test
    fun call() {
        val mobile = controller?.mobiles?.get(0)
        Assertions.assertNotNull(mobile)
        val requester = "me"
        val now = LocalDateTime.now()
        val due = now.plusDays(1).toString()
        val request = Book.Request(mobile!!, requester, due)
        restTemplate?.postForObject("http://localhost:$port/book", request, Book.Response::class.java)
        val message = blockingQueue.take()
        assertTrue(message.contains(mobile))
    }

}
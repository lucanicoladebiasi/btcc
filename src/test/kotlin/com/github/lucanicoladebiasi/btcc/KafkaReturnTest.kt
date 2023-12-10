package com.github.lucanicoladebiasi.btcc

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
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
class KafkaReturnTest {

    val blockingQueue: BlockingQueue<String> = LinkedBlockingDeque(1)

    @Autowired
    private val controller: Controller? = null

    @LocalServerPort
    private val port = 0

    @Autowired
    private val restTemplate: TestRestTemplate? = null

    @KafkaListener(topics = ["return"], groupId = "btcc")
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
        restTemplate?.postForObject(
            "http://localhost:$port/book",
            Book.Request(mobile!!, requester, due),
            Book.Response::class.java
        )
        val request = Return.Request(mobile!!, requester)
        restTemplate?.exchange(
            "http://localhost:$port/return",
            HttpMethod.POST,
            HttpEntity(request),
            Any::class.java
        )
        val message = blockingQueue.take()
        assertTrue(message.contains(mobile))
    }

}
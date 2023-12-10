package com.github.lucanicoladebiasi.btcc

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDateTime
import java.time.ZoneOffset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9093", "port=9093"])
class ControllerTest {

    companion object {

        private val mapper = ObjectMapper()

    }

    @Autowired
    private val controller: Controller? = null

    @LocalServerPort
    private val port = 0

    @Autowired
    private val restTemplate: TestRestTemplate? = null

    @Test
    fun contextLoads() {
        assertNotNull(controller)
        assertNotNull(controller?.mobiles)
    }

    @Test
    fun book_available() {
        val mobile = controller?.mobiles?.get(0)
        assertNotNull(mobile)
        val requester = "me"
        val now = LocalDateTime.now()
        val due = now.plusDays(1).toString()
        val request = Book.Request(mobile!!, requester, due)
        val response = restTemplate?.postForObject("http://localhost:$port/book", request, Book.Response::class.java)
        assertNotNull(response)
        assertEquals(mobile, response?.mobile)
        assertEquals(requester, response?.requester)
        assertEquals(due, request.due)
    }

    @Test
    fun book_before_now() {
        val mobile = controller?.mobiles?.get(1)
        assertNotNull(mobile)
        val requester = "me"
        val now = LocalDateTime.now()
        val due = now.minusDays(1).toString()
        val request = Book.Request(mobile!!, requester, due)
        val response = restTemplate?.exchange(
            "http://localhost:$port/book",
            HttpMethod.POST,
            HttpEntity(request),
            Any::class.java
        )
        assertNotNull(response)
        assertEquals(HttpStatus.BAD_REQUEST, response?.statusCode)
    }

    @Test
    fun book_due_parse_exception() {
        val mobile = controller?.mobiles?.get(2)
        assertNotNull(mobile)
        val requester = "me"
        val due = "nonsense"
        val request = Book.Request(mobile!!, requester, due)
        val response = restTemplate?.exchange(
            "http://localhost:$port/book",
            HttpMethod.POST,
            HttpEntity(request),
            Any::class.java
        )
        assertNotNull(response)
        assertEquals(HttpStatus.BAD_REQUEST, response?.statusCode)
    }

    @Test
    fun book_unavailable() {
        val mobile = controller?.mobiles?.get(3)
        assertNotNull(mobile)
        val requester = "me"
        val now = LocalDateTime.now()
        val due = now.plusDays(1).toString()
        val request = Book.Request(mobile!!, requester, due)
        restTemplate?.postForObject("http://localhost:$port/book", request, Book.Response::class.java)
        val response = restTemplate?.exchange(
            "http://localhost:$port/book",
            HttpMethod.POST,
            HttpEntity(request),
            Any::class.java
        )
        assertNotNull(response)
        assertEquals(HttpStatus.LOCKED, response?.statusCode)
    }

    @Test
    fun list() {
        val possiblyInUseMobiles = setOf(
            controller?.mobiles?.get(0),
            controller?.mobiles?.get(1),
            controller?.mobiles?.get(2),
            controller?.mobiles?.get(3),
            controller?.mobiles?.get(5),
            controller?.mobiles?.get(6)
        )
        val overdueMobile = controller?.mobiles?.get(4)
        assertNotNull(overdueMobile)
        val requester = "me"
        val now = LocalDateTime.now()
        val due = now.plusSeconds(1).toString()
        val request = Book.Request(overdueMobile!!, requester, due)
        restTemplate?.postForObject("http://localhost:$port/book", request, Book.Response::class.java)
        Thread.sleep(1000)
        val response = restTemplate?.getForObject("http://localhost:$port/list", List::class.java)
        assertNotNull(response)
        response?.forEach {
            val booking = mapper.readValue(mapper.writeValueAsString(it), Booking.Response::class.java)
            val status = Controller.Status.valueOf(booking.status)
            when (status) {
                Controller.Status.AVAILABLE -> {
                    assertNull(booking.requester)
                    assertNull(booking.made)
                    assertNull(booking.due)
                }

                Controller.Status.DUE -> {
                    assertEquals(overdueMobile, booking.mobile)
                    assertEquals(requester, booking.requester)
                    assertNotNull(booking.made)
                    assertNotNull(booking.due)
                }

                Controller.Status.IN_USE -> {
                    assertTrue(possiblyInUseMobiles.contains(booking.mobile))
                    assertEquals(requester, booking.requester)
                    assertNotNull(booking.made)
                    assertNotNull(booking.due)
                }

                else -> fail()
            }
        }
    }

    @Test
    fun ping() {
        val now = System.currentTimeMillis()
        val response = restTemplate?.getForObject("http://localhost:$port/ping", String::class.java)
        assertNotNull(response)
        val pong = LocalDateTime.parse(response).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertTrue(pong > now)
    }

    @Test
    fun return_available() {
        val mobile = controller?.mobiles?.get(5)
        assertNotNull(mobile)
        val requester = "me"
        val now = LocalDateTime.now()
        val due = now.plusDays(1).toString()
        restTemplate?.postForObject(
            "http://localhost:$port/book",
            Book.Request(mobile!!, requester, due),
            Book.Response::class.java
        )
        val request = Return.Request(mobile!!, requester)
        val response = restTemplate?.exchange(
            "http://localhost:$port/return",
            HttpMethod.POST,
            HttpEntity(request),
            Any::class.java
        )
        assertNotNull(response)
        assertEquals(HttpStatus.OK, response?.statusCode)
    }

    @Test
    fun return_forbidden() {
        val mobile = controller?.mobiles?.get(6)
        assertNotNull(mobile)
        val requester = "me"
        val now = LocalDateTime.now()
        val due = now.plusDays(1).toString()
        restTemplate?.postForObject(
            "http://localhost:$port/book",
            Book.Request(mobile!!, requester, due),
            Book.Response::class.java
        )
        val request = Return.Request(mobile!!, "not-$requester")
        val response = restTemplate?.exchange(
            "http://localhost:$port/return",
            HttpMethod.POST,
            HttpEntity(request),
            Any::class.java
        )
        assertNotNull(response)
        assertEquals(HttpStatus.FORBIDDEN, response?.statusCode)
    }

    @Test
    fun return_not_found() {
        val mobile = "preposterous"
        val requester = "me"
        val request = Return.Request(mobile, requester)
        val response = restTemplate?.exchange(
            "http://localhost:$port/return",
            HttpMethod.POST,
            HttpEntity(request),
            Any::class.java
        )
        assertNotNull(response)
        assertEquals(HttpStatus.NOT_FOUND, response?.statusCode)
    }

}
package com.github.lucanicoladebiasi.btcc

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.concurrent.ConcurrentHashMap


/**
 * Exposes the REST API end-points to [book], [return] and [list] [mobiles] available or in use for tests.
 */
@RestController
class Controller {

    companion object {

        /**
         * Logger.
         */
        private val log: Logger = LoggerFactory.getLogger(Controller::class.java)

        /**
         * Serializer and deserializer mapper between JSON and objects, used to log objects and produce Kafka messages.
         */
        private val mapper = ObjectMapper()

        /**
         * Reference to the [mapper] writer, used to log objects and produce Kafka messages.
         */
        private val writer = mapper.writerWithDefaultPrettyPrinter()

    } //~ companion

    /**
     * Represents the [Booking.Response.status].
     */
    enum class Status {
        AVAILABLE,
        DUE,
        IN_USE
    }

    @Suppress("EnumEntryName")
    enum class Topic {
        book,
        `return`
    }

    /**
     * Mimic the DB relating [Booking] objects with [Book.Request.requester] and [mobiles].
     */
    private val bookings = ConcurrentHashMap<String, Booking>()

    @Autowired
    private val kafkaTemplate: KafkaTemplate<String, String>? = null


    /**
     * Represents the mobiles defined in the `mobiles` key in the `application.properties` file.
     */
    @Value("#{'\${mobiles}'.split(',')}")
    internal var mobiles: List<String>? = null

    /**
     * Book the mobile device set for tests.
     *
     * @param request defines who collects the mobile and when it will be returned.
     * @return the [Book.Response] to confirm the mobile is collected for tests.
     * @throws ResponseStatusException if [Book.Request.due] can't be parsed as ISO 8601,
     * if [Book.Request.due] is due before now,
     * if [Book.Request.mobile] is already in use.
     */
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Mobile collected.",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Book.Response::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "ISO 8601 parse error",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = Void::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Return due before now.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = Void::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "In use.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = Void::class))]
            ),
        ]
    )
    @Operation(summary = "Book/collect the mobile if available.")
    @PostMapping("/book")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Throws(ResponseStatusException::class)
    fun book(@RequestBody request: Book.Request): Book.Response {
        log.debug("Book: ${writer.writeValueAsString(request)}...")
        val made = System.currentTimeMillis()
        try {
            val due = LocalDateTime.parse(request.due).toInstant(ZoneOffset.UTC).toEpochMilli()
            when (due > made) {
                true -> {
                    val booking = Booking(request.requester, made, due)
                    val locked = bookings.putIfAbsent(request.mobile, booking)
                    if (locked != null) {
                        val message = "Book: ${request.mobile} in use by ${locked.requester}!"
                        log.warn(message)
                        throw ResponseStatusException(HttpStatus.LOCKED, message)
                    }
                    val response = Book.Response(
                        request.mobile,
                        request.requester,
                        Instant.ofEpochMilli(made).atZone(ZoneId.systemDefault()).toLocalDateTime().toString(),
                        Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDateTime().toString()

                    )
                    val data = writer.writeValueAsString(response)
                    val send = kafkaTemplate?.send(Topic.book.name, data)
                    send?.whenComplete { result, ex ->
                        when (ex) {
                            null -> log.info("Book: Kafka offset ${result.recordMetadata.offset()}.")
                            else -> log.warn("Book: Kafka error ${ex.message}!")
                        }
                    }
                    log.info("Book: $data.")
                    return response
                }

                else -> {
                    val message = "Book: due before now!"
                    log.warn(message)
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
                }
            }
        } catch (e: DateTimeParseException) {
            val message = "Book: ${e.message}!"
            log.warn(message)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, message, e)
        }
    }

    /**
     * List the mobiles available, in use and overdue.
     *
     * @return the list of [Booking.Response] objects representing the state of each mobile.
     */
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "List of mobiles and their status.",
                content = [Content(
                    mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = Booking.Response::class))
                )]
            )
        ]
    )
    @GetMapping("/list")
    @Operation(summary = "List the mobiles and their status.")
    @ResponseStatus(HttpStatus.OK)
    fun list(): List<Booking.Response> {
        log.debug("List...")
        val response = mutableListOf<Booking.Response>()
        if (mobiles != null) {
            val now = System.currentTimeMillis()
            mobiles!!.forEach {
                when (val booking: Booking? = bookings[it]) {
                    null -> response.add(Booking.Response(it, Status.AVAILABLE.name))
                    else -> {
                        val made = Instant.ofEpochMilli(booking.made)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            .toString()
                        val due = Instant.ofEpochMilli(booking.due)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            .toString()
                        when (now > booking.due) {
                            true -> response.add(Booking.Response(it, Status.DUE.name, booking.requester, made, due))
                            else -> response.add(Booking.Response(it, Status.IN_USE.name, booking.requester, made, due))
                        }
                    }
                }
            }
        }
        log.info("List: ${writer.writeValueAsString(response)}.")
        return response
    }

    /**
     * Ping the REST API service.
     *
     * @return an ISO 8601 string representing the instant the server received the request,
     * it can be used as template for the [Book.Request.due] property calling [book].
     */
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "ISO 8601 time stamp.",
                content = [Content(mediaType = "text/plain", schema = Schema(implementation = String::class))]
            )
        ]
    )
    @GetMapping("/ping")
    @Operation(summary = "Ping the service.")
    fun ping(): String {
        log.debug("Ping...")
        val response = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString()
        log.info("Pong: $response.")
        return response
    }

    /**
     * Return the device.
     *
     * @param request defines the mobile to return and who returns it.
     * @throws ResponseStatusException if [Return.Request.mobile] of the [request] isn't in use
     * or [Return.Request.requester] is not the same using the mobile.
     */
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Mobile returned.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = Void::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "In use by a different requester!",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = Void::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Not found!",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = Void::class))]
            )
        ]
    )
    @Operation(summary = "Return the mobile to make it available to book.")
    @PostMapping("/return")
    @ResponseStatus(HttpStatus.OK)
    @Throws(ResponseStatusException::class)
    fun `return`(@RequestBody request: Return.Request) {
        log.debug("Return: ${writer.writeValueAsString(request)}...")
        when (val booking = bookings[request.mobile]) {
            null -> {
                val message = "Return: ${request.mobile} not found!"
                log.warn(message)
                throw ResponseStatusException(HttpStatus.NOT_FOUND, message)
            }

            else -> when (booking.requester == request.requester) {
                true -> {
                    bookings.remove(request.mobile)
                    val data = writer.writeValueAsString(request)
                    val send = kafkaTemplate?.send(Topic.`return`.name, data)
                    send?.whenComplete { result, ex ->
                        when (ex) {
                            null -> log.info("Return: Kafka offset ${result.recordMetadata.offset()}.")
                            else -> log.warn("Return: Kafka error ${ex.message}!")
                        }
                    }
                    log.info("Return: $data.")
                }

                else -> {
                    val message = "Return: ${request.mobile}: in use by ${booking.requester}!"
                    log.warn(message)
                    throw ResponseStatusException(HttpStatus.FORBIDDEN, message)
                }
            }
        }
    }

} //~ Controller
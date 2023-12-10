package com.github.lucanicoladebiasi.btcc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * REST API service, see [Controller].
 */
@SpringBootApplication
class BtccApplication

/**
 * Application server entry point.
 *
 * @param args passed to the application server.
 */
fun main(args: Array<String>) {
    runApplication<BtccApplication>(*args)
}

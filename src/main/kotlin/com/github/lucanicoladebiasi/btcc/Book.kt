package com.github.lucanicoladebiasi.btcc


/**
 * Class wrapper for the [Controller.book] end-point.
 */
class Book {

    /**
     * Request body for [Controller.book] end-point.
     */
    data class Request(

        /**
         * The device to collect for tests.
         */
        val mobile: String,

        /**
         * Who collects the device.
         */
        val requester: String,

        /**
         * When the device should be returned, expressed as ISO 8601 local date time.
         */
        val due: String
    )

    /**
     * Response body for the [Controller.book] end-point.
     */
    data class Response(

        /**
         * The device to collect for tests.
         */
        val mobile: String,

        /**
         * Who collects the device.
         */
        val requester: String,

        /**
         * When the [mobile] was collected, expressed as ISO 8601 local date time.
         */
        val made: String,

        /**
         * When the device should be returned, expressed as ISO 8601 local date time.
         */
        val due: String
    )

} //~ Book
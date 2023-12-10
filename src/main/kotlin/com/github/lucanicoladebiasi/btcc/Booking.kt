package com.github.lucanicoladebiasi.btcc

/**
 * The class represents the booking made to test [Controller.mobiles] in the [Controller.bookings] map.
 */
data class Booking(

    /**
     * Who collected the mobile.
     */
    val requester: String,

    /**
     * When the mobile was collected, milliseconds of the Epoch.
     */
    val made: Long,

    /**
     * When the mobile is due for return, milliseconds of the Epoch.
     */
    val due: Long
) {

    /**
     * Element of the list returned as response body for the [Controller.list] end-point.
     */
    data class Response(
        /**
         * Device label.
         */
        val mobile: String = "",

        /**
         * "AVAILABLE", "IN_USE" or "DUE".
         */
        val status: String = "",

        /**
         * Who collected the mobile if [status] is "IN_USE" or "DUE".
         */
        val requester: String? = null,

        /**
         * When the mobile was collected, expressed in ISO 8601 local date time.
         */
        val made: String? = null,

        /**
         * When the mobile should be returned, expressed in ISO 8601 local date time.
         */
        val due: String? = null,
    )

} //~ Bookings
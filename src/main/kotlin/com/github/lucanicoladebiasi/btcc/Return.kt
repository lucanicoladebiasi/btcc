package com.github.lucanicoladebiasi.btcc

/**
 * Class wrapper for the [Controller.return] end-point.
 */
class Return {

    /**
     * Request body for [Controller.return] end point.
     */
    data class Request(
        /**
         * Device label.
         */
        val mobile: String,

        /**
         * User returning the device.
         */
        val requester: String
    )

} //~ Return

package com.exanthiax.ecominions.api.upgrade

/** What an attempt to buy an upgrade tier did. */
enum class PurchaseResult {
    PURCHASED,
    CANNOT_AFFORD,

    /** The minion's type may not buy this upgrade, or the tier has no way to pay for it. */
    NOT_ALLOWED,

    /** The tier's buy conditions are not met. */
    CONDITIONS_NOT_MET,

    /** The tier is not configured, or is not the next one up. */
    INVALID_TIER,
    CANCELLED
}

package com.example.qwant.server
object UrgencyRules {
    const val ORANGE_AFTER_MINUTES = 10L

    const val RED_AFTER_MINUTES = 15L

    /**
     * @return цвет «светофора» для заявки, либо null, если индикация не применяется
     *         (заявка ещё не принята, уже завершена или это не заявка на подсыпку по МРГ).
     */
    fun of(type: String, status: String, acceptedAtMillis: Long?, nowMillis: Long): String? {
        if (status != OrderStatus.ACCEPTED) return null
        if (type != OrderType.PODSYPKA) return null
        if (acceptedAtMillis == null) return null
        val minutes = (nowMillis - acceptedAtMillis) / 60_000L
        return when {
            minutes < ORANGE_AFTER_MINUTES -> Urgency.GREEN
            minutes < RED_AFTER_MINUTES -> Urgency.ORANGE
            else -> Urgency.RED
        }
    }

    /** Сколько минут прошло с момента принятия (или null). */
    fun elapsedMinutes(acceptedAtMillis: Long?, nowMillis: Long): Long? =
        acceptedAtMillis?.let { (nowMillis - it) / 60_000L }
}

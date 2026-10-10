package com.exanthiax.ecominions.testsupport

import io.mockk.every
import io.mockk.mockk
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

/** A map-backed [PersistentDataContainer], so persistence can be tested without a server. */
object FakePersistentDataContainer {
    fun create(): PersistentDataContainer {
        val values = HashMap<NamespacedKey, Any>()
        return mockk<PersistentDataContainer>().also { container ->
            every { container.get(any<NamespacedKey>(), any<PersistentDataType<Any, Any>>()) } answers { values[firstArg()] }
            every { container.set(any<NamespacedKey>(), any<PersistentDataType<Any, Any>>(), any()) } answers {
                values[firstArg()] = thirdArg()
            }
            every { container.remove(any()) } answers { values.remove(firstArg()) }
            every { container.has(any<NamespacedKey>()) } answers { firstArg<NamespacedKey>() in values }
        }
    }
}

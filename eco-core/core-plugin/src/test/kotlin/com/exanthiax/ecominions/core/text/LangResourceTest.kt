package com.exanthiax.ecominions.core.text

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

/**
 * eco validates the shipped `lang.yml` against the keys in its own `LangYml`
 * class and warns in console for every plugin missing one, so the defaults have
 * to carry them even where EcoMinions never reads them itself.
 */
class LangResourceTest {

    @Test
    fun `lang yml carries the keys eco requires`() {
        val lang = checkNotNull(javaClass.classLoader.getResourceAsStream("lang.yml")).use {
            Yaml().load<Map<String, Any?>>(it)
        }

        val messages = lang["messages"] as? Map<*, *>
        assertTrue(messages != null, "lang.yml is missing the messages section")

        for (key in listOf("prefix", "no-permission", "not-player")) {
            assertTrue(messages!!.containsKey(key), "lang.yml is missing messages.$key")
        }
    }
}

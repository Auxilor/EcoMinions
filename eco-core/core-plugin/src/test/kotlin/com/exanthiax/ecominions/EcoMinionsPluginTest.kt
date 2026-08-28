package com.exanthiax.ecominions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Guards the one rule the composition root has to keep: it is built before
 * libreforge exists.
 *
 * libreforge-loader only puts libreforge on the classpath during `onLoad`, so
 * every field initialiser in the plugin runs against eco and the loader alone.
 * A Kotlin lambda is the easy way to break that without noticing: it compiles to
 * an `invokedynamic` whose bootstrap resolves every class named in its
 * descriptor, so a callback typed against a libreforge class takes the whole
 * plugin down with NoClassDefFoundError before it ever loads.
 */
class EcoMinionsPluginTest {

    @Test
    fun `plugin class names no libreforge type outside the loader`() {
        val bytecode = checkNotNull(
            javaClass.classLoader.getResourceAsStream("com/exanthiax/ecominions/EcoMinionsPlugin.class")
        ).use { it.readBytes() }.toString(Charsets.ISO_8859_1)

        val referenced = Regex("""com/willfp/libreforge/[A-Za-z0-9/$]+""")
            .findAll(bytecode)
            .map { it.value }
            .filterNot { it.startsWith("com/willfp/libreforge/loader/") }
            .distinct()
            .sorted()
            .toList()

        assertEquals(
            emptyList<String>(),
            referenced,
            "EcoMinionsPlugin must not name a libreforge type: libreforge is not " +
                "on the classpath while the plugin is being constructed."
        )
    }
}

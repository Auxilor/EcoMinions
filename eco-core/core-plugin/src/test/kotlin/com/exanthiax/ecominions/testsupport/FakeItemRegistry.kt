package com.exanthiax.ecominions.testsupport

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.Tag
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.stream.Stream

/**
 * Minimal fake [RegistryAccess] so plain-JVM unit tests can construct real
 * [ItemStack]s.
 *
 * This reaches into `ItemStack`'s specific constructor-delegation behaviour on
 * Paper API `1.21.11-R0.1-SNAPSHOT`; re-verify (or replace) this fake if the
 * project's pinned `paper-api` version is ever bumped.
 *
 * A real Bukkit/Paper server supplies `RegistryAccess` via `ServiceLoader`;
 * nothing on a unit test classpath does, so touching `Material.asItemType()`
 * (which any `ItemStack` construction does, transitively) throws
 * `IllegalStateException: No RegistryAccess implementation found`. This class
 * is registered via `META-INF/services` so `ServiceLoader` finds it instead.
 *
 * Only wired deep enough for `Material.asItemType()` -> `ItemStack.of(...)`:
 * `ItemType.createItemStack(int)` is the only member with real behaviour,
 * returning a [FakeItemStack] that fakes type/amount/equals/hashCode/clone
 * without touching any further registry machinery. Every other registry or
 * `ItemType` member a real server would answer is unimplemented on purpose -
 * nothing in this codebase's tests needs it, and a loud failure here is
 * better than a silently wrong one.
 */
class FakeRegistryAccess : RegistryAccess {
    @Deprecated("Overrides a deprecated member; unused by anything this fake needs to support.")
    override fun <T : Keyed> getRegistry(type: Class<T>): Registry<T> {
        @Suppress("UNCHECKED_CAST")
        return EmptyRegistry as Registry<T>
    }

    override fun <T : Keyed> getRegistry(registryKey: RegistryKey<T>): Registry<T> {
        @Suppress("UNCHECKED_CAST")
        return if (registryKey == RegistryKey.ITEM) ItemTypeRegistry as Registry<T> else EmptyRegistry as Registry<T>
    }
}

private object EmptyRegistry : Registry<Keyed> {
    override fun get(key: NamespacedKey): Keyed? = null
    override fun getKey(value: Keyed): NamespacedKey? = null
    override fun iterator(): MutableIterator<Keyed> = mutableListOf<Keyed>().iterator()
    override fun hasTag(key: TagKey<Keyed>): Boolean = false
    override fun getTag(key: TagKey<Keyed>): Tag<Keyed> =
        throw UnsupportedOperationException("Tags are not faked for tests")
    override fun getTags(): MutableCollection<Tag<Keyed>> = mutableListOf()
    override fun stream(): Stream<Keyed> = Stream.empty()
    override fun keyStream(): Stream<NamespacedKey> = Stream.empty()
    override fun size(): Int = 0
}

/** Resolves any item key to an [ItemType] proxy backed by the matching [Material]. */
private object ItemTypeRegistry : Registry<ItemType> {
    private val cache = HashMap<NamespacedKey, ItemType>()

    override fun get(key: NamespacedKey): ItemType = cache.getOrPut(key) { proxyFor(key) }
    override fun getKey(value: ItemType): NamespacedKey? = null

    override fun iterator(): MutableIterator<ItemType> = mutableListOf<ItemType>().iterator()
    override fun hasTag(key: TagKey<ItemType>): Boolean = false
    override fun getTag(key: TagKey<ItemType>): Tag<ItemType> =
        throw UnsupportedOperationException("Tags are not faked for tests")
    override fun getTags(): MutableCollection<Tag<ItemType>> = mutableListOf()
    override fun stream(): Stream<ItemType> = Stream.empty()
    override fun keyStream(): Stream<NamespacedKey> = Stream.empty()
    override fun size(): Int = 0

    /** Every material the tests use stacks to 64; the real server answers this properly. */
    private const val DEFAULT_MAX_STACK_SIZE = 64

    private fun proxyFor(key: NamespacedKey): ItemType {
        val material = Material.matchMaterial(key.key) ?: Material.STONE
        val handler = InvocationHandler { proxy, method, args ->
            when (method.name) {
                "createItemStack" -> FakeItemStack(material, (args?.getOrNull(0) as? Int) ?: 1)
                // Material.getMaxStackSize() delegates here; storage needs a stack limit.
                "getMaxStackSize" -> DEFAULT_MAX_STACK_SIZE
                "equals" -> proxy === args?.get(0)
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "FakeItemType($key)"
                else -> throw UnsupportedOperationException("ItemType.${method.name} is not faked for tests")
            }
        }
        return Proxy.newProxyInstance(
            ItemType::class.java.classLoader,
            arrayOf(ItemType::class.java),
            handler
        ) as ItemType
    }
}

/**
 * A real [ItemStack] subclass that never touches `craftDelegate`.
 *
 * The public `ItemStack(Material, int)` constructor always routes through the
 * registry via `ItemStack.of(...)`; the protected no-arg constructor doesn't,
 * so subclassing it and overriding the handful of members every caller in
 * this codebase actually uses is what makes real `ItemStack`s buildable here.
 */
private class FakeItemStack(private val material: Material, private var count: Int) : ItemStack() {
    override fun getType(): Material = material
    override fun getAmount(): Int = count
    override fun setAmount(amount: Int) {
        count = amount
    }

    override fun clone(): ItemStack = FakeItemStack(material, count)

    override fun equals(other: Any?): Boolean =
        other is FakeItemStack && other.material == material && other.count == count

    override fun hashCode(): Int = material.hashCode() * 31 + count

    override fun toString(): String = "FakeItemStack{type=$material, amount=$count}"
}

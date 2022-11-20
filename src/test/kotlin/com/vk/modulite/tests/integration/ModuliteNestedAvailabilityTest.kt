package com.vk.modulite.tests.integration

import com.vk.modulite.dsl.shouldBe
import com.vk.modulite.infrastructure.IntegrationTestBase

class ModuliteNestedAvailabilityTest : IntegrationTestBase() {
    fun `test all internal`() = integrationTest {
        step("create modulite") {
            // Когда все модули internal, любой модуль может использовать только
            // его прямых потомков.

            createModuliteFromSource("Messages")
            createModuliteFromSource("Messages/Core")
            createModuliteFromSource("Messages/Core/Impl")

            modulite("messages/core")      canUse modulite("messages/core/impl") shouldBe true
            // Нельзя использовать, так как messages/core не экспортирует messages/core/impl.
            modulite("messages")           canUse modulite("messages/core/impl") shouldBe false
            modulite("messages")           canUse modulite("messages/core") shouldBe true
        }
    }

    fun `test only last is exported`() = integrationTest {
        step("create modulite") {
            createModuliteFromSource("Messages")
            createModuliteFromSource("Messages/Core")
            createModuliteFromSource("Messages/Core/Impl") {
                exportFromParent()
            }

            modulite("messages/core/impl") canUse modulite("messages/core/impl") shouldBe true
            modulite("messages/core")      canUse modulite("messages/core/impl") shouldBe true
            modulite("messages")           canUse modulite("messages/core/impl") shouldBe true
        }
    }

    fun `test first is internal`() = integrationTest {
        step("create modulite") {
            createModuliteFromSource("Messages")
            createModuliteFromSource("Messages/Core") {
                exportFromParent()
            }
            createModuliteFromSource("Messages/Core/Impl") {
                exportFromParent()
            }

            modulite("messages/core/impl") canUse modulite("messages/core/impl") shouldBe true
            modulite("messages/core")      canUse modulite("messages/core/impl") shouldBe true
            modulite("messages")           canUse modulite("messages/core/impl") shouldBe true
        }
    }

    fun `test four only last is exported`() = integrationTest {
        step("create modulite") {
            createModuliteFromSource("Messages")
            createModuliteFromSource("Messages/Core")
            createModuliteFromSource("Messages/Core/Impl")
            createModuliteFromSource("Messages/Core/Impl/Deep") /* Exported */ {
                exportFromParent()
            }

            modulite("messages/core/impl/deep") canUse modulite("messages/core/impl/deep") shouldBe true
            modulite("messages/core/impl")      canUse modulite("messages/core/impl/deep") shouldBe true
            modulite("messages/core")           canUse modulite("messages/core/impl/deep") shouldBe true
            // Нельзя использовать, так как messages/core не экспортирует messages/core/impl.
            modulite("messages")                canUse modulite("messages/core/impl/deep") shouldBe false


            modulite("messages/core/impl")      canUse modulite("messages/core/impl") shouldBe true
            modulite("messages/core")           canUse modulite("messages/core/impl") shouldBe true
            // Нельзя использовать, так как messages/core не экспортирует messages/core/impl.
            modulite("messages")                canUse modulite("messages/core/impl") shouldBe false
        }
    }

    fun `test four two last is exported`() = integrationTest {
        step("create modulite") {
            createModuliteFromSource("Messages")
            createModuliteFromSource("Messages/Core")
            createModuliteFromSource("Messages/Core/Impl") /* Exported */ {
                exportFromParent()
            }
            createModuliteFromSource("Messages/Core/Impl/Deep") /* Exported */ {
                exportFromParent()
            }

            modulite("messages/core/impl/deep") canUse modulite("messages/core/impl/deep") shouldBe true
            modulite("messages/core/impl")      canUse modulite("messages/core/impl/deep") shouldBe true
            modulite("messages/core")           canUse modulite("messages/core/impl/deep") shouldBe true
            // Можно использовать, так как messages/core прямой потомок и messages/core
            // экспортирует messages/core/impl который в свою очередь экспортирует messages/core/impl/deep.
            modulite("messages")                canUse modulite("messages/core/impl/deep") shouldBe true


            modulite("messages/core/impl")      canUse modulite("messages/core/impl") shouldBe true
            modulite("messages/core")           canUse modulite("messages/core/impl") shouldBe true
            // Можно использовать, так как messages/core прямой потомок и messages/core
            // экспортирует messages/core/impl который в свою очередь экспортирует messages/core/impl/deep.
            modulite("messages")                canUse modulite("messages/core/impl") shouldBe true
        }
    }

    fun `test last is allowed for root`() = integrationTest {
        step("create modulite") {
            createModuliteFromSource("Messages")
            createModuliteFromSource("Messages/Core")
            createModuliteFromSource("Messages/Core/Impl")

            modulite("messages/core/impl") {
                allowInForModulite("messages/core", "messages")
            }

            modulite("messages/core/impl") canUse modulite("messages/core/impl") shouldBe true
            modulite("messages/core")      canUse modulite("messages/core/impl") shouldBe true

            // Можно использовать несмотря на то что messages/core/impl не экспортируется из
            // messages/core, так как есть allow-internal-access и messages/core можно использовать.
            modulite("messages")           canUse modulite("messages/core/impl") shouldBe true
        }
    }

    fun `test last but one is export and last is allowed for root`() = integrationTest {
        step("create modulite") {
            createModuliteFromSource("Messages")
            createModuliteFromSource("Messages/Core")
            createModuliteFromSource("Messages/Core/Impl") {
                exportFromParent()
            }
            createModuliteFromSource("Messages/Core/Impl/Deep")

            modulite("messages/core/impl/deep") {
                allowInForModulite("messages/core/impl", "messages")
            }

            modulite("messages/core/impl") canUse modulite("messages/core/impl/deep") shouldBe true
            modulite("messages/core")      canUse modulite("messages/core/impl") shouldBe true

            modulite("messages")           canUse modulite("messages/core") shouldBe true
            modulite("messages")           canUse modulite("messages/core/impl") shouldBe true
            // Можно использовать несмотря на то что messages/core/impl/deep не экспортируется из
            // messages/core/impl, так как есть allow-internal-access и messages/core/impl можно использовать
            // так как он экспортируется из messages/core, который является прямым потомком.
            modulite("messages")           canUse modulite("messages/core/impl/deep") shouldBe true
        }
    }

    fun `test access to direct child modulites of parent modulite`() = integrationTest {
        step("create modulite") {
            createModuliteFromSource("Messages")
            createModuliteFromSource("Messages/Core")
            createModuliteFromSource("Messages/Channels")

            // Можно использовать, так как messages/core прямой потомок messages
            // который является прямым родителем messages/channels.
            modulite("messages/channels")  canUse modulite("messages/core") shouldBe true
            // То же самое.
            modulite("messages/core")      canUse modulite("messages/channels") shouldBe true
        }
    }

    fun `test access to nested child modulites of parent modulite`() = integrationTest {
        step("create modulite") {
            createModuliteFromSource("Messages")
            createModuliteFromSource("Messages/Core")
            createModuliteFromSource("Messages/Core/Impl")
            createModuliteFromSource("Messages/Core/Utils") /* Exported */ {
                exportFromParent()
            }
            createModuliteFromSource("Messages/Channels")

            // Нельзя использовать, так как messages/core/impl не прямой потомок messages
            // и messages/core не экспортирует messages/core/impl.
            modulite("messages/channels")  canUse modulite("messages/core/impl") shouldBe true // TODO: should be false?

            // Можно использовать, хоть messages/core/impl не прямой потомок messages,
            // но messages/core который является прямым потомком экспортирует messages/core/utils.
            modulite("messages/channels")  canUse modulite("messages/core/utils") shouldBe true
        }
    }

    override fun getTestDataPath() = "src/test/fixtures/integration/ModuliteNestedAvailability"
}

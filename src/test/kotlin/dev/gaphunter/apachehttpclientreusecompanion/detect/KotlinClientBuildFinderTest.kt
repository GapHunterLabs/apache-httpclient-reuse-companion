package dev.gaphunter.apachehttpclientreusecompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KotlinClientBuildFinderTest : BasePlatformTestCase() {

    fun `test HttpClients-createDefault inside a regular method is flagged`() {
        val file = myFixture.configureByText(
            "PaymentGateway.kt",
            """
            class PaymentGateway {
                fun charge(payload: String): Any {
                    val client = HttpClients.createDefault()
                    return client.execute(request)
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, KotlinClientBuildFinder.findAll(file).size)
    }

    fun `test HttpClients-custom-build inside a regular method is flagged`() {
        val file = myFixture.configureByText(
            "PaymentGateway.kt",
            """
            class PaymentGateway {
                fun charge(payload: String): Any {
                    val client = HttpClients.custom().setMaxConnTotal(50).build()
                    return client.execute(request)
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, KotlinClientBuildFinder.findAll(file).size)
    }

    fun `test construction inside a class initializer is not flagged`() {
        val file = myFixture.configureByText(
            "PaymentGateway.kt",
            """
            class PaymentGateway {
                private val client = HttpClients.createDefault()
            }
            """.trimIndent(),
        )
        assertTrue(KotlinClientBuildFinder.findAll(file).isEmpty())
    }

    fun `test unrelated factory call is not flagged`() {
        val file = myFixture.configureByText(
            "PaymentGateway.kt",
            """
            class PaymentGateway {
                fun build(): Any {
                    return Executors.createDefault()
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinClientBuildFinder.findAll(file).isEmpty())
    }
}

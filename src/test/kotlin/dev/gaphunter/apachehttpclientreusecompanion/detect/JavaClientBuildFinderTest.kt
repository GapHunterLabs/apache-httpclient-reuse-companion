package dev.gaphunter.apachehttpclientreusecompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaClientBuildFinderTest : BasePlatformTestCase() {

    fun `test HttpClients-createDefault inside a regular method is flagged`() {
        val file = myFixture.configureByText(
            "PaymentGateway.java",
            """
            class PaymentGateway {
                Object charge(String payload) {
                    CloseableHttpClient client = HttpClients.createDefault();
                    return client.execute(request);
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, JavaClientBuildFinder.findAll(file).size)
    }

    fun `test HttpClients-custom-build inside a regular method is flagged`() {
        val file = myFixture.configureByText(
            "PaymentGateway.java",
            """
            class PaymentGateway {
                Object charge(String payload) {
                    CloseableHttpClient client = HttpClients.custom().setMaxConnTotal(50).build();
                    return client.execute(request);
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, JavaClientBuildFinder.findAll(file).size)
    }

    fun `test construction inside a constructor is not flagged`() {
        val file = myFixture.configureByText(
            "PaymentGateway.java",
            """
            class PaymentGateway {
                private final CloseableHttpClient client;

                PaymentGateway() {
                    this.client = HttpClients.createDefault();
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaClientBuildFinder.findAll(file).isEmpty())
    }

    fun `test unrelated factory call is not flagged`() {
        val file = myFixture.configureByText(
            "PaymentGateway.java",
            """
            class PaymentGateway {
                Object build() {
                    return Executors.createDefault();
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaClientBuildFinder.findAll(file).isEmpty())
    }
}

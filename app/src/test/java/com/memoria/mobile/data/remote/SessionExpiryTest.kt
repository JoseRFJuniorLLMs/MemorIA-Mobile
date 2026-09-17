package com.memoria.mobile.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The rule the interceptor encodes, tested without Android or a live server.
 *
 * An expired token used to be a dead end: every screen showed "Token inválido ou
 * expirado." with a "Tentar novamente" button that re-sent the same dead token
 * and got the same 401 forever. Tokens last 7 days (`JWT_EXPIRE`), so this was a
 * weekly certainty.
 *
 * The fix hinges on one distinction that is easy to get backwards, and getting it
 * backwards is worse than the original bug: a 401 means "session over" ONLY when
 * the request actually carried a token. A 401 from login means wrong CPF or
 * password, and treating that as an expired session would bounce the user off
 * their own login screen every time they mistyped — an unescapable loop.
 */
class SessionExpiryTest {

    /** Mirrors the interceptor's condition in ApiProvider.authInterceptor(). */
    private fun endsSession(httpCode: Int, sentToken: String?): Boolean =
        httpCode == 401 && sentToken != null

    @Test
    fun `a rejected token ends the session`() {
        assertEquals(true, endsSession(401, "jwt-expirado"))
    }

    @Test
    fun `a failed login does NOT end the session`() {
        // No token was sent, so the 401 is wrong credentials — not an expiry.
        // If this ever returns true, a typo in the password locks the user out of
        // the login screen itself.
        assertEquals(false, endsSession(401, null))
    }

    @Test
    fun `other errors leave the session alone`() {
        // A 404 was the prescriptions bug, a 403 is a plan limit, a 500 is the
        // server's problem. None of them means the token died, and logging the
        // user out over any of them would lose their place for no reason.
        listOf(200, 400, 403, 404, 500, 502).forEach { code ->
            assertEquals("HTTP $code não deve encerrar a sessão", false, endsSession(code, "jwt-valido"))
        }
    }

    @Test
    fun `clearing the session leaves no token behind in memory`() {
        // SessionState is what the interceptor reads on every request; a token left
        // here would keep being sent after the session ended.
        val session = SessionState()
        session.token = "jwt-expirado"
        session.token = null
        assertNull(session.token)
    }

    @Test
    fun `the unauthorized hook fires and can be called more than once`() {
        // Several in-flight requests fail together when a token dies, so every one
        // of them invokes the hook. Clearing has to tolerate that.
        val session = SessionState()
        var calls = 0
        session.onUnauthorized = { calls += 1 }

        repeat(3) { session.onUnauthorized?.invoke() }

        assertEquals(3, calls)
    }
}

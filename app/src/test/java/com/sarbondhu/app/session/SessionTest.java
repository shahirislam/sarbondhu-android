package com.sarbondhu.app.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sarbondhu.app.data.repository.AuthRepository.Role;

import org.junit.After;
import org.junit.Test;

public class SessionTest {

    @After
    public void tearDown() {
        Session.get().logout();
    }

    @Test
    public void startsLoggedOut() {
        assertFalse(Session.get().isLoggedIn());
        assertFalse(Session.get().isAdmin());
    }

    @Test
    public void adminLoginAndLogout() {
        Session.get().login(Role.ADMIN, 1, "অ্যাডমিন");

        assertTrue(Session.get().isAdmin());
        assertEquals(1, Session.get().getUserId());
        assertEquals("অ্যাডমিন", Session.get().getDisplayName());

        Session.get().logout();
        assertFalse(Session.get().isLoggedIn());
    }

    @Test
    public void dealerIsNotAdmin() {
        Session.get().login(Role.DEALER, 10, "দোকান");

        assertTrue(Session.get().isLoggedIn());
        assertTrue(Session.get().isDealer());
        assertFalse(Session.get().isAdmin());
    }
}

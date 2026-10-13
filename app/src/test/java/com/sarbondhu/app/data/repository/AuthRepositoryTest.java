package com.sarbondhu.app.data.repository;

import static org.junit.Assert.assertEquals;

import com.sarbondhu.app.data.repository.AuthRepository.LoginOutcome;
import com.sarbondhu.app.data.repository.AuthRepository.LoginResult;
import com.sarbondhu.app.data.repository.AuthRepository.Role;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class AuthRepositoryTest {

    private AuthRepository auth;

    @Before
    public void setUp() throws IOException {
        auth = new AuthRepository(TestStores.store());
    }

    @Test
    public void adminLogsInWithTrimmedCaseInsensitiveUsername() throws IOException {
        LoginOutcome outcome = auth.login(Role.ADMIN, "  Admin ", "admin123");

        assertEquals(LoginResult.SUCCESS, outcome.getResult());
        assertEquals(1, outcome.getUserId());
        assertEquals("অ্যাডমিন", outcome.getDisplayName());
    }

    @Test
    public void wrongPasswordIsRejected() throws IOException {
        assertEquals(LoginResult.WRONG_CREDENTIALS,
                auth.login(Role.ADMIN, "admin", "nope").getResult());
    }

    @Test
    public void dealerLogsInAsDealer() throws IOException {
        LoginOutcome outcome = auth.login(Role.DEALER, "dealer10", "dealer123");

        assertEquals(LoginResult.SUCCESS, outcome.getResult());
        assertEquals(10, outcome.getUserId());
        assertEquals("দোকান 10", outcome.getDisplayName());
    }

    @Test
    public void inactiveDealerCannotLogIn() throws IOException {
        assertEquals(LoginResult.INACTIVE,
                auth.login(Role.DEALER, "dealer20", "dealer123").getResult());
    }

    @Test
    public void roleMustMatchAccount() throws IOException {
        assertEquals(LoginResult.WRONG_CREDENTIALS,
                auth.login(Role.DEALER, "admin", "admin123").getResult());
        assertEquals(LoginResult.WRONG_CREDENTIALS,
                auth.login(Role.ADMIN, "dealer10", "dealer123").getResult());
    }
}

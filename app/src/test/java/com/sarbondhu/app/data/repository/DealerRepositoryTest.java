package com.sarbondhu.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.repository.AuthRepository.LoginResult;
import com.sarbondhu.app.data.repository.AuthRepository.Role;
import com.sarbondhu.app.data.store.MockDataStore;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class DealerRepositoryTest {

    private MockDataStore store;
    private DealerRepository dealers;

    @Before
    public void setUp() throws IOException {
        store = TestStores.store();
        dealers = new DealerRepository(store);
    }

    @Test
    public void addAssignsNextIdAndIsActive() throws Exception {
        Shop created = dealers.addDealer(TestStores.dealer(0, "newdealer", true));

        assertEquals(21, created.getId());
        assertTrue(dealers.getDealer(21).isActive());
        assertEquals(3, dealers.getAllDealers().size());
    }

    @Test
    public void duplicateUsernameIsRejectedCaseInsensitively() throws IOException {
        assertReason(DataException.Reason.USERNAME_TAKEN,
                () -> dealers.addDealer(TestStores.dealer(0, "DEALER10", true)));
    }

    @Test
    public void adminUsernameCannotBeUsedByDealer() throws IOException {
        assertReason(DataException.Reason.USERNAME_TAKEN,
                () -> dealers.addDealer(TestStores.dealer(0, "admin", true)));
    }

    @Test
    public void updateKeepsPasswordAndAllowsOwnUsername() throws Exception {
        dealers.resetPassword(10, "secret");
        Shop edited = new Shop(10, "নতুন নাম", "মালিক", "0171", "জেলা", "উপজেলা", "ঠিকানা",
                "dealer10", "", true);

        dealers.updateDealer(edited);

        assertEquals("নতুন নাম", dealers.getDealer(10).getShopName());
        assertEquals("secret", dealers.getDealer(10).getPassword());
    }

    @Test
    public void deactivatedDealerCannotLogInAndReactivatedCan() throws Exception {
        AuthRepository auth = new AuthRepository(store);

        dealers.setActive(10, false);
        assertFalse(dealers.getDealer(10).isActive());
        assertEquals(LoginResult.INACTIVE, auth.login(Role.DEALER, "dealer10", "dealer123").getResult());

        dealers.setActive(10, true);
        assertEquals(LoginResult.SUCCESS, auth.login(Role.DEALER, "dealer10", "dealer123").getResult());
    }

    @Test
    public void resetPasswordChangesLogin() throws Exception {
        AuthRepository auth = new AuthRepository(store);

        dealers.resetPassword(10, "newpass");

        assertEquals(LoginResult.WRONG_CREDENTIALS, auth.login(Role.DEALER, "dealer10", "dealer123").getResult());
        assertEquals(LoginResult.SUCCESS, auth.login(Role.DEALER, "dealer10", "newpass").getResult());
    }

    @Test
    public void changePasswordNeedsCurrentPassword() throws Exception {
        AuthRepository auth = new AuthRepository(store);

        assertReason(DataException.Reason.WRONG_PASSWORD,
                () -> dealers.changePassword(10, "nope", "newpass1"));
        dealers.changePassword(10, "dealer123", "newpass1");

        assertEquals(LoginResult.SUCCESS, auth.login(Role.DEALER, "dealer10", "newpass1").getResult());
    }

    @Test
    public void unknownDealerIsNotFound() throws IOException {
        assertReason(DataException.Reason.NOT_FOUND, () -> dealers.setActive(99, false));
    }

    private interface Action {
        void run() throws Exception;
    }

    private static void assertReason(DataException.Reason reason, Action action) {
        try {
            action.run();
            fail("expected DataException " + reason);
        } catch (DataException e) {
            assertEquals(reason, e.getReason());
        } catch (Exception e) {
            fail("unexpected " + e);
        }
    }
}

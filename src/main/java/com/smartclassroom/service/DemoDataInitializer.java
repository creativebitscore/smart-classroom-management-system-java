package com.smartclassroom.service;

import com.smartclassroom.dao.UserDao;
import com.smartclassroom.util.PasswordUtil;

import java.util.List;

/**
 * The SQL script cannot create secure PBKDF2 hashes, so it inserts the demo accounts with the
 * placeholder 'PENDING'. On start-up this class replaces the placeholder with a real salted hash
 * of the demo password for that role. Real accounts created in the app are never touched.
 * DEMO PASSWORDS ONLY: change or delete the demo accounts before any real use.
 */
public class DemoDataInitializer {
    private final UserDao userDao;

    public DemoDataInitializer() {
        this(new UserDao());
    }

    public DemoDataInitializer(UserDao userDao) {
        this.userDao = userDao;
    }

    /** Returns how many accounts were initialised. */
    public int initialise() {
        List<UserDao.PendingAccount> pending = userDao.findAccountsNeedingPassword();
        for (UserDao.PendingAccount account : pending) {
            String demoPassword = switch (account.role()) {
                case ADMIN -> "Admin@123";
                case FACULTY -> "Faculty@123";
                case STUDENT -> "Student@123";
            };
            userDao.updatePasswordHash(account.userId(), PasswordUtil.hash(demoPassword));
        }
        return pending.size();
    }
}

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.accounts;

import android.Manifest;
import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.AccountManagerInternal;
import android.accounts.AccountManagerResponse;
import android.accounts.AuthenticatorDescription;
import android.accounts.CantAddAccountActivity;
import android.accounts.ChooseAccountActivity;
import android.accounts.GrantCredentialsPermissionActivity;
import android.accounts.IAccountAuthenticator;
import android.accounts.IAccountAuthenticatorResponse;
import android.accounts.IAccountManager;
import android.accounts.IAccountManagerResponse;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.compat.CompatChanges;
import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledAfter;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.RegisteredServicesCache;
import android.content.pm.RegisteredServicesCacheListener;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.SigningDetails.CertCapabilities;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteStatement;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.stats.devicepolicy.DevicePolicyEnums;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.internal.messages.nano.SystemMessageProto.SystemMessage;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemService;

import com.google.android.collect.Lists;
import com.google.android.collect.Sets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A system service that provides  account, password, and authtoken management for all
 * accounts on the device. Some of these calls are implemented with the help of the corresponding
 * {@link IAccountAuthenticator} services. This service is not accessed by users directly,
 * instead one uses an instance of {@link AccountManager}, which can be accessed as follows:
 *    AccountManager accountManager = AccountManager.get(context);
 * @hide
 */
public class AccountManagerService
        extends IAccountManager.Stub
        implements RegisteredServicesCacheListener<AuthenticatorDescription> {
    private static final String TAG = "AccountManagerService";

    public static class Lifecycle extends SystemService {
        private AccountManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            mService = new AccountManagerService(new Injector(getContext()));
            publishBinderService(Context.ACCOUNT_SERVICE, mService);
        }

        @Override
        public void onUserUnlocking(@NonNull TargetUser user) {
            mService.onUnlockUser(user.getUserIdentifier());
        }

        @Override
        public void onUserStopped(@NonNull TargetUser user) {
            Slog.i(TAG, "onUserStopped " + user);
            mService.purgeUserData(user.getUserIdentifier());
        }
    }

    final Context mContext;

    private final PackageManager mPackageManager;
    private final AppOpsManager mAppOpsManager;
    private UserManager mUserManager;
    private final Injector mInjector;

    final MessageHandler mHandler;

    // Messages that can be sent on mHandler
    private static final int MESSAGE_TIMED_OUT = 3;
    private static final int MESSAGE_COPY_SHARED_ACCOUNT = 4;

    private final IAccountAuthenticatorCache mAuthenticatorCache;
    private static final String PRE_N_DATABASE_NAME = "accounts.db";
    private static final Intent ACCOUNTS_CHANGED_INTENT;
    private static final Bundle ACCOUNTS_CHANGED_OPTIONS = new BroadcastOptions()
            .setDeliveryGroupPolicy(BroadcastOptions.DELIVERY_GROUP_POLICY_MOST_RECENT)
            .toBundle();

    private static final int SIGNATURE_CHECK_MISMATCH = 0;
    private static final int SIGNATURE_CHECK_MATCH = 1;
    private static final int SIGNATURE_CHECK_UID_MATCH = 2;

    /**
     * Apps targeting Android U and above need to declare the package visibility needs in the
     * manifest to access the AccountManager APIs.
     */
    @ChangeId
    @EnabledAfter(targetSdkVersion = Build.VERSION_CODES.TIRAMISU)
    private static final long ENFORCE_PACKAGE_VISIBILITY_FILTERING = 154726397;

    static {
        ACCOUNTS_CHANGED_INTENT = new Intent(AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION);
        ACCOUNTS_CHANGED_INTENT.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
    }

    private final LinkedHashMap<String, Session> mSessions = new LinkedHashMap<String, Session>();

    static class UserAccounts {
        private final int userId;
        final AccountsDb accountsDb;
        private final HashMap<Pair<Pair<Account, String>, Integer>, NotificationId>
                credentialsPermissionNotificationIds = new HashMap<>();
        private final HashMap<Account, NotificationId> signinRequiredNotificationIds
                = new HashMap<>();
        final Object cacheLock = new Object();
        final Object dbLock = new Object(); // if needed, dbLock must be obtained before cacheLock
        /** protected by the {@link #cacheLock} */
        final HashMap<String, Account[]> accountCache = new LinkedHashMap<>();
        /** protected by the {@link #cacheLock} */
        private final Map<Account, Map<String, String>> userDataCache = new HashMap<>();
        /** protected by the {@link #cacheLock} */
        private final Map<Account, Map<String, String>> authTokenCache = new HashMap<>();
        /** protected by the {@link #cacheLock} */
        private final TokenCache accountTokenCaches = new TokenCache();
        /** protected by the {@link #cacheLock} */
        private final Map<Account, Map<String, Integer>> visibilityCache = new HashMap<>();

        /** protected by the {@link #mReceiversForType},
         *  type -> (packageName -> number of active receivers)
         *  type == null is used to get notifications about all account types
         */
        private final Map<String, Map<String, Integer>> mReceiversForType = new HashMap<>();

        /**
         * protected by the {@link #cacheLock}
         *
         * Caches the previous names associated with an account. Previous names
         * should be cached because we expect that when an Account is renamed,
         * many clients will receive a LOGIN_ACCOUNTS_CHANGED broadcast and
         * want to know if the accounts they care about have been renamed.
         *
         * The previous names are wrapped in an {@link AtomicReference} so that
         * we can distinguish between those accounts with no previous names and
         * those whose previous names haven't been cached (yet).
         */
        private final HashMap<Account, AtomicReference<String>> previousNameCache =
                new HashMap<Account, AtomicReference<String>>();

        UserAccounts(Context context, int userId, File preNDbFile, File deDbFile) {
            this.userId = userId;
            synchronized (dbLock) {
                synchronized (cacheLock) {
                    accountsDb = AccountsDb.create(context, userId, preNDbFile, deDbFile);
                }
            }
        }
    }

    private final SparseArray<UserAccounts> mUsers = new SparseArray<>();
    private final SparseBooleanArray mLocalUnlockedUsers = new SparseBooleanArray();
    // Not thread-safe. Only use in synchronized context
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private CopyOnWriteArrayList<AccountManagerInternal.OnAppPermissionChangeListener>
            mAppPermissionChangeListeners = new CopyOnWriteArrayList<>();

    private static AtomicReference<AccountManagerService> sThis = new AtomicReference<>();
    private static final Account[] EMPTY_ACCOUNT_ARRAY = new Account[]{};

    /**
     * This should only be called by system code. One should only call this after the service
     * has started.
     * @return a reference to the AccountManagerService instance
     * @hide
     */
    public static AccountManagerService getSingleton() {
        return sThis.get();
    }

    public AccountManagerService(Injector injector) {
        mInjector = injector;
        mContext = injector.getContext();
        mPackageManager = mContext.getPackageManager();
        mAppOpsManager = mContext.getSystemService(AppOpsManager.class);
        mHandler = new MessageHandler(injector.getMessageHandlerLooper());
        mAuthenticatorCache = mInjector.getAccountAuthenticatorCache();
        mAuthenticatorCache.setListener(this, mHandler);

        sThis.set(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context1, Intent intent) {
                // Don't delete accounts when updating a authenticator's
                // package.
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    /* Purging data requires file io, don't block the main thread. This is probably
                     * less than ideal because we are introducing a race condition where old grants
                     * could be exercised until they are purged. But that race condition existed
                     * anyway with the broadcast receiver.
                     *
                     * Ideally, we would completely clear the cache, purge data from the database,
                     * and then rebuild the cache. All under the cache lock. But that change is too
                     * large at this point.
                     */
                    final String removedPackageName = intent.getData().getSchemeSpecificPart();
                    Runnable purgingRunnable = new Runnable() {
                        @Override
                        public void run() {
                            purgeOldGrantsAll();
                            // Notify authenticator about removed app?
                            removeVisibilityValuesForPackage(removedPackageName);
                        }
                    };
                    mHandler.post(purgingRunnable);
                }
            }
        }, intentFilter);

        injector.addLocalService(new AccountManagerInternalImpl());

        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction(Intent.ACTION_USER_REMOVED);
        mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_USER_REMOVED.equals(action)) {
                    int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                    if (userId < 1) return;
                    Slog.i(TAG, "User " + userId + " removed");
                    purgeUserData(userId);
                }
            }
        }, UserHandle.ALL, userFilter, null, null);

        // Need to cancel account request notifications if the update/install can access the account
        new PackageMonitor() {
            @Override
            public void onPackageAdded(String packageName, int uid) {
                // Called on a handler, and running as the system
                cancelAccountAccessRequestNotificationIfNeeded(uid, true);
            }

            @Override
            public void onPackageUpdateFinished(String packageName, int uid) {
                // Called on a handler, and running as the system
                cancelAccountAccessRequestNotificationIfNeeded(uid, true);
            }
        }.register(mContext, mHandler.getLooper(), UserHandle.ALL, true);

        // Cancel account request notification if an app op was preventing the account access
        mAppOpsManager.startWatchingMode(AppOpsManager.OP_GET_ACCOUNTS, null,
                new AppOpsManager.OnOpChangedInternalListener() {
            @Override
            public void onOpChanged(int op, String packageName) {
                try {
                    final int userId = ActivityManager.getCurrentUser();
                    final int uid = mPackageManager.getPackageUidAsUser(packageName, userId);
                    final int mode = mAppOpsManager.checkOpNoThrow(
                            AppOpsManager.OP_GET_ACCOUNTS, uid, packageName);
                    if (mode == AppOpsManager.MODE_ALLOWED) {
                        final long identity = Binder.clearCallingIdentity();
                        try {
                            cancelAccountAccessRequestNotificationIfNeeded(packageName, uid, true);
                        } finally {
                            Binder.restoreCallingIdentity(identity);
                        }
                    }
                } catch (NameNotFoundException e) {
                    /* ignore */
                }
            }
        });

        // Cancel account request notification if a permission was preventing the account access
        mPackageManager.addOnPermissionsChangeListener(
                (int uid) -> {
            // Permission changes cause requires updating accounts cache.
            AccountManager.invalidateLocalAccountsDataCaches();

            Account[] accounts = null;
            String[] packageNames = mPackageManager.getPackagesForUid(uid);
            if (packageNames != null) {
                final int userId = UserHandle.getUserId(uid);
                final long identity = Binder.clearCallingIdentity();
                try {
                    for (String packageName : packageNames) {
                                // if app asked for permission we need to cancel notification even
                                // for O+ applications.
                                if (mPackageManager.checkPermission(
                                        Manifest.permission.GET_ACCOUNTS,
                                        packageName) != PackageManager.PERMISSION_GRANTED) {
                                    continue;
                                }

                        if (accounts == null) {
                            accounts = getAccountsAsUser(null, userId, "android");
                            if (ArrayUtils.isEmpty(accounts)) {
                                return;
                            }
                        }

                        for (Account account : accounts) {
                            cancelAccountAccessRequestNotificationIfNeeded(
                                    account, uid, packageName, true);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        });
    }


    boolean getBindInstantServiceAllowed(int userId) {
        return  mAuthenticatorCache.getBindInstantServiceAllowed(userId);
    }

    void setBindInstantServiceAllowed(int userId, boolean allowed) {
        mAuthenticatorCache.setBindInstantServiceAllowed(userId, allowed);
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(int uid,
            boolean checkAccess) {
        Account[] accounts = getAccountsAsUser(null, UserHandle.getUserId(uid), "android");
        for (Account account : accounts) {
            cancelAccountAccessRequestNotificationIfNeeded(account, uid, checkAccess);
        }
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(String packageName, int uid,
            boolean checkAccess) {
        Account[] accounts = getAccountsAsUser(null, UserHandle.getUserId(uid), "android");
        for (Account account : accounts) {
            cancelAccountAccessRequestNotificationIfNeeded(account, uid, packageName, checkAccess);
        }
    }

    private void cancelAccountAccessRequestNotificationIfNeeded(Account account, int uid,
            boolean checkAccess) {
        String[] packageNames = mPackageManager.getPackagesForUid(uid);
    

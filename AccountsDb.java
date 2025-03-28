/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.server.accounts;

import android.accounts.Account;
import android.annotation.Nullable;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistence layer abstraction for accessing accounts_ce/accounts_de databases.
 *
 * <p>At first, CE database needs to be {@link #attachCeDatabase(File) attached to DE},
 * in order for the tables to be available. All operations with CE database are done through the
 * connection to the DE database, to which it is attached. This approach allows atomic
 * transactions across two databases</p>
 */
class AccountsDb implements AutoCloseable {
    private static final String TAG = "AccountsDb";

    private static final String DATABASE_NAME = "accounts.db";
    private static final int PRE_N_DATABASE_VERSION = 9;
    private static final int CE_DATABASE_VERSION = 10;
    private static final int DE_DATABASE_VERSION = 3; // Added visibility support in O

    static final String TABLE_ACCOUNTS = "accounts";
    private static final String ACCOUNTS_ID = "_id";
    private static final String ACCOUNTS_NAME = "name";
    private static final String ACCOUNTS_TYPE = "type";
    private static final String ACCOUNTS_TYPE_COUNT = "count(type)";
    private static final String ACCOUNTS_PASSWORD = "password";
    private static final String ACCOUNTS_PREVIOUS_NAME = "previous_name";
    private static final String ACCOUNTS_LAST_AUTHENTICATE_TIME_EPOCH_MILLIS =
            "last_password_entry_time_millis_epoch";

    private static final String TABLE_AUTHTOKENS = "authtokens";
    private static final String AUTHTOKENS_ID = "_id";
    private static final String AUTHTOKENS_ACCOUNTS_ID = "accounts_id";
    private static final String AUTHTOKENS_TYPE = "type";
    private static final String AUTHTOKENS_AUTHTOKEN = "authtoken";

    private static final String TABLE_VISIBILITY = "visibility";
    private static final String VISIBILITY_ACCOUNTS_ID = "accounts_id";
    private static final String VISIBILITY_PACKAGE = "_package";
    private static final String VISIBILITY_VALUE = "value";

    private static final String TABLE_GRANTS = "grants";
    private static final String GRANTS_ACCOUNTS_ID = "accounts_id";
    private static final String GRANTS_AUTH_TOKEN_TYPE = "auth_token_type";
    private static final String GRANTS_GRANTEE_UID = "uid";

    private static final String TABLE_EXTRAS = "extras";
    private static final String EXTRAS_ID = "_id";
    private static final String EXTRAS_ACCOUNTS_ID = "accounts_id";
    private static final String EXTRAS_KEY = "key";
    private static final String EXTRAS_VALUE = "value";

    private static final String TABLE_META = "meta";
    private static final String META_KEY = "key";
    private static final String META_VALUE = "value";

    static final String TABLE_SHARED_ACCOUNTS = "shared_accounts";
    private static final String SHARED_ACCOUNTS_ID = "_id";

    private static String TABLE_DEBUG = "debug_table";

    // Columns for debug_table table
    private static String DEBUG_TABLE_ACTION_TYPE = "action_type";
    private static String DEBUG_TABLE_TIMESTAMP = "time";
    private static String DEBUG_TABLE_CALLER_UID = "caller_uid";
    private static String DEBUG_TABLE_TABLE_NAME = "table_name";
    private static String DEBUG_TABLE_KEY = "primary_key";

    // These actions correspond to the occurrence of real actions. Since
    // these are called by the authenticators, the uid associated will be
    // of the authenticator.
    static String DEBUG_ACTION_SET_PASSWORD = "action_set_password";
    static String DEBUG_ACTION_CLEAR_PASSWORD = "action_clear_password";
    static String DEBUG_ACTION_ACCOUNT_ADD = "action_account_add";
    static String DEBUG_ACTION_ACCOUNT_REMOVE = "action_account_remove";
    static String DEBUG_ACTION_ACCOUNT_REMOVE_DE = "action_account_remove_de";
    static String DEBUG_ACTION_AUTHENTICATOR_REMOVE = "action_authenticator_remove";
    static String DEBUG_ACTION_ACCOUNT_RENAME = "action_account_rename";

    // These actions don't necessarily correspond to any action on
    // accountDb taking place. As an example, there might be a request for
    // addingAccount, which might not lead to addition of account on grounds
    // of bad authentication. We will still be logging it to keep track of
    // who called.
    static String DEBUG_ACTION_CALLED_ACCOUNT_ADD = "action_called_account_add";
    static String DEBUG_ACTION_CALLED_ACCOUNT_REMOVE = "action_called_account_remove";
    static String DEBUG_ACTION_SYNC_DE_CE_ACCOUNTS = "action_sync_de_ce_accounts";

    //This action doesn't add account to accountdb. Account is only
    // added in finishSession which may be in a different user profile.
    static String DEBUG_ACTION_CALLED_START_ACCOUNT_ADD = "action_called_start_account_add";
    static String DEBUG_ACTION_CALLED_ACCOUNT_SESSION_FINISH =
            "action_called_account_session_finish";

    static final String CE_DATABASE_NAME = "accounts_ce.db";
    static final String DE_DATABASE_NAME = "accounts_de.db";
    private static final String CE_DB_PREFIX = "ceDb.";
    private static final String CE_TABLE_ACCOUNTS = CE_DB_PREFIX + TABLE_ACCOUNTS;
    private static final String CE_TABLE_AUTHTOKENS = CE_DB_PREFIX + TABLE_AUTHTOKENS;
    private static final String CE_TABLE_EXTRAS = CE_DB_PREFIX + TABLE_EXTRAS;

    static final int MAX_DEBUG_DB_SIZE = 64;

    private static final String[] ACCOUNT_TYPE_COUNT_PROJECTION =
            new String[] { ACCOUNTS_TYPE, ACCOUNTS_TYPE_COUNT};

    private static final String COUNT_OF_MATCHING_GRANTS = ""
            + "SELECT COUNT(*) FROM " + TABLE_GRANTS + ", " + TABLE_ACCOUNTS
            + " WHERE " + GRANTS_ACCOUNTS_ID + "=" + ACCOUNTS_ID
            + " AND " + GRANTS_GRANTEE_UID + "=?"
            + " AND " + GRANTS_AUTH_TOKEN_TYPE + "=?"
            + " AND " + ACCOUNTS_NAME + "=?"
            + " AND " + ACCOUNTS_TYPE + "=?";

    private static final String COUNT_OF_MATCHING_GRANTS_ANY_TOKEN = ""
            + "SELECT COUNT(*) FROM " + TABLE_GRANTS + ", " + TABLE_ACCOUNTS
            + " WHERE " + GRANTS_ACCOUNTS_ID + "=" + ACCOUNTS_ID
            + " AND " + GRANTS_GRANTEE_UID + "=?"
            + " AND " + ACCOUNTS_NAME + "=?"
            + " AND " + ACCOUNTS_TYPE + "=?";

    private static final String SELECTION_ACCOUNTS_ID_BY_ACCOUNT =
        "accounts_id=(select _id FROM accounts WHERE name=? AND type=?)";

    private static final String[] COLUMNS_AUTHTOKENS_TYPE_AND_AUTHTOKEN =
            {AUTHTOKENS_TYPE, AUTHTOKENS_AUTHTOKEN};

    private static final String[] COLUMNS_EXTRAS_KEY_AND_VALUE = {EXTRAS_KEY, EXTRAS_VALUE};

    private static final String ACCOUNT_ACCESS_GRANTS = ""
            + "SELECT " + AccountsDb.ACCOUNTS_NAME + ", "
            + AccountsDb.GRANTS_GRANTEE_UID
            + " FROM " + AccountsDb.TABLE_ACCOUNTS
            + ", " + AccountsDb.TABLE_GRANTS
            + " WHERE " + AccountsDb.GRANTS_ACCOUNTS_ID
            + "=" + AccountsDb.ACCOUNTS_ID;

    private static final String META_KEY_FOR_AUTHENTICATOR_UID_FOR_TYPE_PREFIX =
            "auth_uid_for_type:";
    private static final String META_KEY_DELIMITER = ":";
    private static final String SELECTION_META_BY_AUTHENTICATOR_TYPE = META_KEY + " LIKE ?";

    private final DeDatabaseHelper mDeDatabase;
    private final Context mContext;
    private final File mPreNDatabaseFile;

    final Object mDebugStatementLock = new Object();
    private volatile long mDebugDbInsertionPoint = -1;
    private volatile SQLiteStatement mDebugStatementForLogging; // not thread safe.

    AccountsDb(DeDatabaseHelper deDatabase, Context context, File preNDatabaseFile) {
        mDeDatabase = deDatabase;
        mContext = context;
        mPreNDatabaseFile = preNDatabaseFile;
    }

    private static class CeDatabaseHelper extends SQLiteOpenHelper {

        CeDatabaseHelper(Context context, String ceDatabaseName) {
            super(context, ceDatabaseName, null, CE_DATABASE_VERSION);
        }

        /**
         * This call needs to be made while the mCacheLock is held.
         * @param db The database.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "Creating CE database " + getDatabaseName());
            db.execSQL("CREATE TABLE " + TABLE_ACCOUNTS + " ( "
                    + ACCOUNTS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ACCOUNTS_NAME + " TEXT NOT NULL, "
                    + ACCOUNTS_TYPE + " TEXT NOT NULL, "
                    + ACCOUNTS_PASSWORD + " TEXT, "
                    + "UNIQUE(" + ACCOUNTS_NAME + "," + ACCOUNTS_TYPE + "))");

            db.execSQL("CREATE TABLE " + TABLE_AUTHTOKENS + " (  "
                    + AUTHTOKENS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,  "
                    + AUTHTOKENS_ACCOUNTS_ID + " INTEGER NOT NULL, "
                    + AUTHTOKENS_TYPE + " TEXT NOT NULL,  "
                    + AUTHTOKENS_AUTHTOKEN + " TEXT,  "
                    + "UNIQUE (" + AUTHTOKENS_ACCOUNTS_ID + "," + AUTHTOKENS_TYPE + "))");

            db.execSQL("CREATE TABLE " + TABLE_EXTRAS + " ( "
                    + EXTRAS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + EXTRAS_ACCOUNTS_ID + " INTEGER, "
                    + EXTRAS_KEY + " TEXT NOT NULL, "
                    + EXTRAS_VALUE + " TEXT, "
                    + "UNIQUE(" + EXTRAS_ACCOUNTS_ID + "," + EXTRAS_KEY + "))");

            createAccountsDeletionTrigger(db);
        }

        private void createAccountsDeletionTrigger(SQLiteDatabase db) {
            db.execSQL(""
                    + " CREATE TRIGGER " + TABLE_ACCOUNTS + "Delete DELETE ON " + TABLE_ACCOUNTS
                    + " BEGIN"
                    + "   DELETE FROM " + TABLE_AUTHTOKENS
                    + "     WHERE " + AUTHTOKENS_ACCOUNTS_ID + "=OLD." + ACCOUNTS_ID + " ;"
                    + "   DELETE FROM " + TABLE_EXTRAS
                    + "     WHERE " + EXTRAS_ACCOUNTS_ID + "=OLD." + ACCOUNTS_ID + " ;"
                    + " END");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(TAG, "Upgrade CE from version " + oldVersion + " to version " + newVersion);

            if (oldVersion == 9) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "onUpgrade upgrading to v10");
                }
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_META);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHARED_ACCOUNTS);
                // Recreate the trigger, since the old one references the table to be removed
                db.execSQL("DROP TRIGGER IF EXISTS " + TABLE_ACCOUNTS + "Delete");
                createAccountsDeletionTrigger(db);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_GRANTS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEBUG);
                oldVersion++;
            }

            if (oldVersion != newVersion) {
                Log.e(TAG, "failed to upgrade version " + oldVersion + " to version " + newVersion);
            }
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e(TAG, "onDowngrade: recreate accounts CE table");
            resetDatabase(db);
            onCreate(db);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "opened database " + CE_DATABASE_NAME);
        }


        /**
         * Creates a new {@code CeDatabaseHelper}. If pre-N db file is present at the old location,
         * it also performs migration to the new CE database.
         */
        static CeDatabaseHelper create(
                Context context,
                File preNDatabaseFile,
                File ceDatabaseFile) {
            boolean newDbExists = ceDatabaseFile.exists();
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "CeDatabaseHelper.create ceDatabaseFile=" + ceDatabaseFile
                        + " oldDbExists=" + preNDatabaseFile.exists()
                        + " newDbExists=" + newDbExists);
            }
            boolean removeOldDb = false;
            if (!newDbExists && preNDatabaseFile.exists()) {
                removeOldDb = migratePreNDbToCe(preNDatabaseFile, ceDatabaseFile);
            }
            // Try to open and upgrade if necessary
            CeDatabaseHelper ceHelper = new CeDatabaseHelper(context, ceDatabaseFile.getPath());
            ceHelper.getWritableDatabase();
            ceHelper.close();
            if (removeOldDb) {
                Slog.i(TAG, "Migration complete - removing pre-N db " + preNDatabaseFile);
                if (!SQLiteDatabase.deleteDatabase(preNDatabaseFile)) {
                    Slog.e(TAG, "Cannot remove pre-N db " + preNDatabaseFile);
                }
            }
            return ceHelper;
        }

        private static boolean migratePreNDbToCe(File oldDbFile, File ceDbFile) {
            Slog.i(TAG, "Moving pre-N DB " + oldDbFile + " to CE " + ceDbFile);
            try {
                FileUtils.copyFileOrThrow(oldDbFile, ceDbFile);
            } catch (IOException e) {
                Slog.e(TAG, "Cannot copy file to " + ceDbFile + " from " + oldDbFile, e);
                // Try to remove potentially damaged file if I/O error occurred
                deleteDbFileWarnIfFailed(ceDbFile);
                return false;
            }
            return true;
        }
    }

    /**
     * Returns information about auth tokens and their account for the specified query
     * parameters.
     * Output is in the format:
     * <pre><code> | AUTHTOKEN_ID |  ACCOUNT_NAME | AUTH_TOKEN_TYPE |</code></pre>
     */
    Cursor findAuthtokenForAllAccounts(String accountType, String authToken) {
        SQLiteDatabase db = mDeDatabase.getReadableDatabaseUserIsUnlocked();
        return db.rawQuery(
                "SELECT " + CE_TABLE_AUTHTOKENS + "." + AUTHTOKENS_ID
                        + ", " + CE_TABLE_ACCOUNTS + "." + ACCOUNTS_NAME
                        + ", " + CE_TABLE_AUTHTOKENS + "." + AUTHTOKENS_TYPE
                        + " FROM " + CE_TABLE_ACCOUNTS
                        + " JOIN " + CE_TABLE_AUTHTOKENS
                        + " ON " + CE_TABLE_ACCOUNTS + "." + ACCOUNTS_ID
                        + " = " + CE_TABLE_AUTHTOKENS + "." + AUTHTOKENS_ACCOUNTS_ID
                        + " WHERE " + CE_TABLE_AUTHTOKENS + "." + AUTHTOKENS_AUTHTOKEN
                        + " = ? AND " + CE_TABLE_ACCOUNTS + "." + ACCOUNTS_TYPE + " = ?",
                new String[]{authToken, accountType});
    }

    Map<String, String> findAuthTokensByAccount(Account account) {
        SQLiteDatabase db = mDeDatabase.getReadableDatabaseUserIsUnlocked();
        HashMap<String, String> authTokensForAccount = new HashMap<>();
        Cursor cursor = db.query(CE_TABLE_AUTHTOKENS,
                COLUMNS_AUTHTOKENS_TYPE_AND_AUTHTOKEN,
                SELECTION_ACCOUNTS_ID_BY_ACCOUNT,
                new String[] {account.name, account.type},
                null, null, null);
        try {
            while (cursor.moveToNext()) {
                final String type = cursor.getString(0);
                final String authToken = cursor.getString(1);
                authTokensForAccount.put(type, authToken);
            }
        } finally {
            cursor.close();
        }
        return authTokensForAccount;
    }

    boolean deleteAuthtokensByAccountIdAndType(long accountId, String authtokenType) {
        SQLiteDatabase db = mDeDatabase.getWritableDatabaseUserIsUnlocked();
        return db.delete(CE_TABLE_AUTHTOKENS,
                AUTHTOKENS_ACCOUNTS_ID + "=?" + " AND " + AUTHTOKENS_TYPE + "=?",
                new String[]{String.valueOf(accountId), authtokenType}) > 0;
    }

    boolean deleteAuthToken(String authTokenId) {
        SQLiteDatabase db = mDeDatabase.getWritableDatabaseUserIsUnlocked();
        return db.delete(
                CE_TABLE_AUTHTOKENS, AUTHTOKENS_ID + "= ?",
                new String[]{authTokenId}) > 0;
    }

    long insertAuthToken(long accountId, String authTokenType, String authToken) {
        SQLiteDatabase db = mDeDatabase.getWritableDatabaseUserIsUnlocked();
        ContentValues values = new ContentValues();
        values.put(AUTHTOKENS_ACCOUNTS_ID, accountId);
        values.put(AUTHTOKENS_TYPE, authTokenType);
        values.put(AUTHTOKENS_AUTHTOKEN, authToken);
        return db.insert(
                CE_TABLE_AUTHTOKENS, AUTHTOKENS_AUTHTOKEN, values);
    }

    int updateCeAccountPassword(long accountId, String password) {
        SQLiteDatabase db = mDeDatabase.getWritableDatabaseUserIsUnlocked();
        final ContentValues values = new ContentValues();
        values.put(ACCOUNTS_PASSWORD, password);
        return db.update(
                CE_TABLE_ACCOUNTS, values, ACCOUNTS_ID + "=?",
                new String[] {String.valueOf(accountId)});
    }

    boolean renameCeAccount(long accountId, String newName) {
        SQLiteDatabase db = mDeDatabase.getWritableDatabaseUserIsUnlocked();
        final ContentValues values = new ContentValues();
        values.put(ACCOUNTS_NAME, newName);
        final String[] argsAccountId = {String.valueOf(accountId)};
        return db.update(
                CE_TABLE_ACCOUNTS, values, ACCOUNTS_ID + "=?", argsAccountId) > 0;
    }

    boolean deleteAuthTokensByAccountId(long accountId) {
        SQLiteDatabase db = mDeDatabase.getWritableDatabaseUserIsUnlocked();
        return db.delete(CE_TABLE_AUTHTOKENS, AUTHTOKENS_ACCOUNTS_ID + "=?",
                new String[] {String.valueOf(accountId)}) > 0;
    }

    long findExtrasIdByAccountId(long accountId, String key) {
        SQLiteDatabase db = mDeDatabase.getReadableDatabaseUserIsUnlocked();
        Cursor cursor = db.query(
                CE_TABLE_EXTRAS, new String[]{EXTRAS_ID},
                EXTRAS_ACCOUNTS_ID + "=" + accountId + " AND " + EXTRAS_KEY + "=?",
                new String[]{key}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
            return -1;
        } finally {
            cursor.close();
        }
    }

    boolean updateExtra(long extrasId, String value) {
        SQLiteDatabase db = mDeDatabase.getWritableDatabaseUserIsUnlocked();
        ContentValues values = new ContentValues();
        values.put(EXTRAS_VALUE, value);
        int rows = db.update(
                TABLE_EXTRAS, values, EXTRAS_ID + "=?",
                new String[]{String

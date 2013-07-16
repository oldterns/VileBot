/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.db;

import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.oldterns.vilebot.util.HMAC;

public class PasswordDB
    extends RedisDB
{
    private static final String keyOfPassHash = "password";

    private static final String keyOfPassSaltsHash = "password-salts";

    // Note, there is some overlap in terms between the crypto term "Secure Hash" and the Redis structure "Hash". Redis
    // hashes are maps, though called hashes because of a common method of implementing them via a hash function. Except
    // for keyOfPassHash and keyOfPassSaltsHash, every use of "hash" in this file refers to the cryptography term.

    /**
     * @return A long random string
     */
    private static String generateSalt()
    {
        return UUID.randomUUID().toString();
    }

    private static String getSalt( String username )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return jedis.hget( keyOfPassSaltsHash, username );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    private static String hash( String salt, String input )
    {
        return HMAC.generateHMAC( input, salt );
    }

    /**
     * Checks the validity of a password for a user
     * 
     * @param username user to check the password of
     * @param password the password to check
     * @return true iff the given password is valid
     */
    public static boolean isValidPassword( String username, String password )
    {
        String storedHash;

        Jedis jedis = pool.getResource();
        try
        {
            storedHash = jedis.hget( keyOfPassHash, username );
        }
        finally
        {
            pool.returnResource( jedis );
        }

        String hash = hash( password, getSalt( username ) );

        return hash.equals( storedHash );
    }

    /**
     * Remove a user and their password information, if they exist.
     * 
     * @param username unique user name
     */
    public static void remUserPassword( String username )
    {
        Jedis jedis = pool.getResource();
        try
        {
            Transaction trans = jedis.multi();
            trans.hdel( keyOfPassHash, username );
            trans.hdel( keyOfPassSaltsHash, username );
            trans.exec();
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Add or modify a user's password
     * 
     * @param username unique user name
     * @param password the user's password (will be hashed)
     * @return true iff a new element was inserted
     */
    public static boolean setUserPassword( String username, String password )
    {
        Jedis jedis = pool.getResource();
        try
        {
            boolean newUser;

            // Create a salt if the user is new
            Transaction trans;
            do
            {
                // Can't use intermediate results of a Redis transaction in that transaction, so watch the keys and do
                // the query before opening the transaction. The transaction will fail on exec() call if the keys
                // changed.
                jedis.watch( keyOfPassHash, keyOfPassSaltsHash );
                newUser = jedis.hexists( keyOfPassHash, username );

                trans = jedis.multi();
                if ( newUser )
                {
                    String salt = generateSalt();
                    trans.hset( keyOfPassSaltsHash, username, generateSalt() );

                    String hash = hash( password, salt );
                    jedis.hset( keyOfPassHash, username, hash );
                }
            }
            while ( trans.exec() == null );

            return newUser;
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }
}

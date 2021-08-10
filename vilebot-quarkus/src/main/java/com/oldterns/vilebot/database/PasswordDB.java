/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.database;

import com.oldterns.vilebot.util.HMAC;
import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PasswordDB
{
    private static final String keyOfPassHash = "password";

    private static final String keyOfPassSaltsHash = "password-salts";

    @Inject
    RedisClient redisClient;

    // Note, there is some overlap in terms between the crypto term "Secure Hash" and the Redis structure "Hash". Redis
    // hashes are maps, though called hashes because of a common method of implementing them via a hash function. Except
    // for keyOfPassHash and keyOfPassSaltsHash, every use of "hash" in this file refers to the cryptography term.

    /**
     * @return A long random string
     */
    private String generateSalt()
    {
        return UUID.randomUUID().toString();
    }

    private String getSalt( String username )
    {
        return redisClient.hget( keyOfPassSaltsHash, username ).toString();
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
    public boolean isValidPassword( String username, String password )
    {
        Response storedHash = redisClient.hget( keyOfPassHash, username );
        if ( storedHash == null )
        {
            return false;
        }
        String hash = hash( password, getSalt( username ) );
        return hash.equals( storedHash.toString() );
    }

    /**
     * Remove a user and their password information, if they exist.
     * 
     * @param username unique user name
     */
    public void removeUserPassword( String username )
    {
        redisClient.multi();
        redisClient.hdel( List.of( keyOfPassHash, username ) );
        redisClient.hdel( List.of( keyOfPassSaltsHash, username ) );
        redisClient.exec();
    }

    /**
     * Add or modify a user's password
     * 
     * @param username unique user name
     * @param password the user's password (will be hashed)
     * @return true iff a new element was inserted
     */
    public boolean setUserPassword( String username, String password )
    {
        boolean newUser;
        do
        {
            // Can't use intermediate results of a Redis transaction in that transaction, so watch the keys and do
            // the query before opening the transaction. The transaction will fail on exec() call if the keys
            // changed.
            redisClient.watch( List.of( keyOfPassHash, keyOfPassSaltsHash ) );
            boolean exists = redisClient.hexists( keyOfPassHash, username ).toBoolean();

            redisClient.multi();
            // Create a salt as well as the new password entry if the user is new
            if ( !exists )
            {
                newUser = true;

                String salt = generateSalt();
                redisClient.hset( List.of( keyOfPassSaltsHash, username, salt ) );

                String hash = hash( password, salt );
                redisClient.hset( List.of( keyOfPassHash, username, hash ) );
            }
            else
            {
                newUser = false;
            }
        }
        while ( redisClient.exec() == null );
        return newUser;
    }
}
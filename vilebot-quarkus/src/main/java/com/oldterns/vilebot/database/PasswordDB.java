/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.database;

import io.quarkus.redis.client.RedisClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.MaskedPassword;
import org.wildfly.security.password.spec.EncryptablePasswordSpec;
import org.wildfly.security.password.spec.MaskedPasswordAlgorithmSpec;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PasswordDB
{
    private static final String keyOfPassHash = "password";

    private static final String keyOfPassSaltsHash = "password-salts";

    private static final String PASSWORD_ALGORITHM = MaskedPassword.ALGORITHM_MASKED_HMAC_SHA512_AES_256;

    private static final int ITERATION_COUNT = 10;

    @Inject
    RedisClient redisClient;

    @ConfigProperty( name = "vilebot.security.secret-key" )
    String secretKey;

    WildFlyElytronPasswordProvider passwordProvider;

    PasswordFactory passwordFactory;

    @PostConstruct
    void setupPasswordFactory()
        throws NoSuchAlgorithmException
    {
        passwordProvider = new WildFlyElytronPasswordProvider();
        passwordFactory = PasswordFactory.getInstance( PASSWORD_ALGORITHM, passwordProvider );
    }

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

    private MaskedPassword getMaskedPassword( String salt, String input )
    {
        try
        {
            MaskedPasswordAlgorithmSpec maskedPasswordAlgorithmSpec =
                new MaskedPasswordAlgorithmSpec( secretKey.toCharArray(), ITERATION_COUNT, salt.getBytes() );
            EncryptablePasswordSpec encryptableSpec =
                new EncryptablePasswordSpec( input.toCharArray(), maskedPasswordAlgorithmSpec );
            passwordFactory.generatePassword( encryptableSpec );
            return (MaskedPassword) passwordFactory.generatePassword( encryptableSpec );
        }
        catch ( InvalidKeySpecException e )
        {
            throw new IllegalStateException( "Unable to hash password", e );
        }
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
        String storedBase64Hash = redisClient.hget( keyOfPassHash, username ).toString();
        byte[] storedHash = Base64.getDecoder().decode( storedBase64Hash );
        String salt = getSalt( username );

        try
        {
            MaskedPassword rawPassword = MaskedPassword.createRaw( PASSWORD_ALGORITHM, secretKey.toCharArray(),
                                                                   ITERATION_COUNT, salt.getBytes(), storedHash );
            MaskedPassword userPassword = (MaskedPassword) passwordFactory.translate( rawPassword );
            return passwordFactory.verify( userPassword, password.toCharArray() );
        }
        catch ( InvalidKeyException e )
        {
            return false;
        }
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
                MaskedPassword hash = getMaskedPassword( password, salt );
                String hashBase64String = Base64.getEncoder().encodeToString( hash.getMaskedPasswordBytes() );
                redisClient.hset( List.of( keyOfPassHash, username, hashBase64String ) );
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
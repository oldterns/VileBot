/*
  Copyright (C) 2013 Oldterns

  This file may be modified and distributed under the terms
  of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.util;

import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HMAC
{

    private static Logger logger = Logger.getLogger( HMAC.class );

    /**
     * Generate an HMAC signature from message and key, using the SHA2 512 bit hash algo.
     * 
     * @param message The message to be signed
     * @param key The key
     * @return HMAC signature of message using key if no error, else null
     */
    public static String generateHMAC( String message, String key )
    {
        try
        {
            return generateHMAC( "HmacSHA512", message, key );
        }
        catch ( NoSuchAlgorithmException e )
        {
            logger.error( e.getMessage() );
            logger.error( "Hash algorithim unsupported" );
        }
        return null;
    }

    /**
     * Generate an HMAC signature from message and key, using the given hash algo.
     * 
     * @param algo An Hmac Algorithm to use, see {@link Mac#getInstance(String)}
     * @param message The message to be signed
     * @param key The key
     * @return HMAC signature of message using key if no error, else null
     */
    public static String generateHMAC( String algo, String message, String key )
        throws NoSuchAlgorithmException
    {
        Mac mac = Mac.getInstance( algo );
        SecretKeySpec secret = new SecretKeySpec( key.getBytes(), algo );
        try
        {
            mac.init( secret );

            byte[] digest = mac.doFinal( message.getBytes() );
            return bytesToHex( digest );
        }
        catch ( InvalidKeyException e )
        {
            logger.error( e.getMessage() );
            logger.error( "Can't init Mac instance with key" );
        }
        return null;
    }

    /**
     * http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
     * 
     * @param bytes byte array
     * @return String of byte array converted to hex characters. Lower case alphabet.
     */
    private static String bytesToHex( byte[] bytes )
    {
        final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ )
        {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String( hexChars );
    }
}

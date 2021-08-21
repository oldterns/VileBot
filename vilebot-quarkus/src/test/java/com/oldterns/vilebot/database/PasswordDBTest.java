package com.oldterns.vilebot.database;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.oldterns.vilebot.util.HMAC;
import com.oldterns.vilebot.util.RandomProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.vertx.redis.client.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PasswordDBTest
    extends AbstractDatabaseTest
{

    @Inject
    PasswordDB passwordDB;

    @InjectMock
    RandomProvider randomProvider;

    @Test
    public void testValidPassword()
    {
        String salt = "my-salt";
        String passwordHash = HMAC.generateHMAC( salt, "password" );
        Response hgetHashResponse = mockResponse( Response::toString, passwordHash );
        Response hgetSaltResponse = mockResponse( Response::toString, salt );
        when( redisClient.hget( "password", "bob" ) ).thenReturn( hgetHashResponse );
        when( redisClient.hget( "password-salts", "bob" ) ).thenReturn( hgetSaltResponse );

        assertThat( passwordDB.isValidPassword( "bob", "password" ) ).isTrue();

        verify( redisClient ).hget( "password", "bob" );
        verify( redisClient ).hget( "password-salts", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testInvalidPassword()
    {
        String salt = "my-salt";
        String passwordHash = HMAC.generateHMAC( salt, "password" );
        Response hgetHashResponse = mockResponse( Response::toString, passwordHash );
        Response hgetSaltResponse = mockResponse( Response::toString, salt );
        when( redisClient.hget( "password", "bob" ) ).thenReturn( hgetHashResponse );
        when( redisClient.hget( "password-salts", "bob" ) ).thenReturn( hgetSaltResponse );

        assertThat( passwordDB.isValidPassword( "bob", "incorrect-password" ) ).isFalse();

        verify( redisClient ).hget( "password", "bob" );
        verify( redisClient ).hget( "password-salts", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testNoPassword()
    {
        when( redisClient.hget( "password", "bob" ) ).thenReturn( null );

        assertThat( passwordDB.isValidPassword( "bob", "password" ) ).isFalse();

        verify( redisClient ).hget( "password", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testDeletePassword()
    {
        passwordDB.removeUserPassword( "bob" );

        verify( redisClient ).multi();
        verify( redisClient ).hdel( List.of( "password", "bob" ) );
        verify( redisClient ).hdel( List.of( "password-salts", "bob" ) );
        verify( redisClient ).exec();
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testAddNewUser()
    {
        Random usedRandom = new Random( 0 );
        byte[] expectedSaltBytes = new byte[16];
        usedRandom.nextBytes( expectedSaltBytes );
        String expectedSalt = UUID.nameUUIDFromBytes( expectedSaltBytes ).toString();
        String passwordHash = HMAC.generateHMAC( expectedSalt, "password" );

        Response hexistsResponse = mockResponse( Response::toBoolean, false );
        // Result not used; checked for null
        Response execResponse = mock( Response.class );
        when( redisClient.hexists( "password", "bob" ) ).thenReturn( hexistsResponse );
        when( redisClient.exec() ).thenReturn( execResponse );
        when( randomProvider.getRandom() ).thenAnswer( invocation -> new Random( 0 ) );

        assertThat( passwordDB.setUserPassword( "bob", "password" ) ).isTrue();

        verify( redisClient ).watch( List.of( "password", "password-salts" ) );
        verify( redisClient ).hexists( "password", "bob" );
        verify( redisClient ).multi();
        verify( redisClient ).hset( List.of( "password", "bob", passwordHash ) );
        verify( redisClient ).hset( List.of( "password-salts", "bob", expectedSalt ) );
        verify( redisClient ).exec();
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testModifyExistingUser()
    {
        Response hexistsResponse = mockResponse( Response::toBoolean, true );
        // Result not used; checked for null
        Response execResponse = mock( Response.class );
        when( redisClient.hexists( "password", "bob" ) ).thenReturn( hexistsResponse );
        when( redisClient.exec() ).thenReturn( execResponse );

        assertThat( passwordDB.setUserPassword( "bob", "password" ) ).isFalse();

        verify( redisClient ).watch( List.of( "password", "password-salts" ) );
        verify( redisClient ).hexists( "password", "bob" );
        verify( redisClient ).multi();
        verify( redisClient ).exec();
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testConcurrentAddNewUserAfterTheUserWasAdded()
    {
        Random usedRandom = new Random( 0 );
        byte[] expectedSaltBytes = new byte[16];
        usedRandom.nextBytes( expectedSaltBytes );
        String expectedSalt = UUID.nameUUIDFromBytes( expectedSaltBytes ).toString();
        String passwordHash = HMAC.generateHMAC( expectedSalt, "password" );

        Response hexistsResponse1 = mockResponse( Response::toBoolean, false );
        Response hexistsResponse2 = mockResponse( Response::toBoolean, true );
        // Result not used; checked for null
        Response execResponse = mock( Response.class );
        when( redisClient.hexists( "password", "bob" ) ).thenReturn( hexistsResponse1 ).thenReturn( hexistsResponse2 );
        when( redisClient.exec() ).thenReturn( null ).thenReturn( execResponse );
        when( randomProvider.getRandom() ).thenAnswer( invocation -> new Random( 0 ) );

        assertThat( passwordDB.setUserPassword( "bob", "password" ) ).isFalse();

        verify( redisClient, times( 2 ) ).watch( List.of( "password", "password-salts" ) );
        verify( redisClient, times( 2 ) ).hexists( "password", "bob" );
        verify( redisClient, times( 2 ) ).multi();
        verify( redisClient ).hset( List.of( "password", "bob", passwordHash ) );
        verify( redisClient ).hset( List.of( "password-salts", "bob", expectedSalt ) );
        verify( redisClient, times( 2 ) ).exec();
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testConcurrentAddNewUserAfterTheUserWasRemoved()
    {
        Random usedRandom = new Random( 0 );
        byte[] expectedSaltBytes = new byte[16];
        usedRandom.nextBytes( expectedSaltBytes );
        String expectedSalt = UUID.nameUUIDFromBytes( expectedSaltBytes ).toString();
        String passwordHash = HMAC.generateHMAC( expectedSalt, "password" );

        Response hexistsResponse1 = mockResponse( Response::toBoolean, true );
        Response hexistsResponse2 = mockResponse( Response::toBoolean, false );
        // Result not used; checked for null
        Response execResponse = mock( Response.class );
        when( redisClient.hexists( "password", "bob" ) ).thenReturn( hexistsResponse1 ).thenReturn( hexistsResponse2 );
        when( redisClient.exec() ).thenReturn( null ).thenReturn( execResponse );
        when( randomProvider.getRandom() ).thenAnswer( invocation -> new Random( 0 ) );

        assertThat( passwordDB.setUserPassword( "bob", "password" ) ).isTrue();

        verify( redisClient, times( 2 ) ).watch( List.of( "password", "password-salts" ) );
        verify( redisClient, times( 2 ) ).hexists( "password", "bob" );
        verify( redisClient, times( 2 ) ).multi();
        verify( redisClient ).hset( List.of( "password", "bob", passwordHash ) );
        verify( redisClient ).hset( List.of( "password-salts", "bob", expectedSalt ) );
        verify( redisClient, times( 2 ) ).exec();
        verifyNoMoreInteractions( redisClient );
    }
}

package com.oldterns.vilebot.services;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;

import javax.inject.Inject;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserPingServiceTest
{
    @Test
    public void testPingUser()
    {
        UserPingService userPingService = new UserPingService();
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "user" );
        assertThat( userPingService.ping( user ) ).isEqualTo( "user: pong" );
        when( user.getNick() ).thenReturn( "user|lunch" );
        assertThat( userPingService.ping( user ) ).isEqualTo( "user|lunch: pong" );
    }
}

package com.oldterns.vilebot.services.admin;

import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.database.LogDB;
import com.oldterns.vilebot.util.SessionService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class GetLogServiceTest
{

    @Inject
    GetLogService getLogService;

    @InjectMock
    GroupDB groupDB;

    @InjectMock
    LogDB logDB;

    @InjectMock
    SessionService sessionService;

    @Test
    public void testCannotGetLogAsNonAdmin()
    {
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( null );
        when( groupDB.isAdmin( null ) ).thenReturn( false );

        assertThat( getLogService.showLog( user ) ).isNull();
        verifyNoInteractions( logDB );
    }

    @Test
    public void testGetLogAsAdmin()
    {
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( logDB.getLog() ).thenReturn( "The log" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );

        assertThat( getLogService.showLog( user ) ).isEqualTo( "Getting log...\nThe log" );
        verify( logDB ).getLog();
        verifyNoMoreInteractions( logDB );
    }

    @Test
    public void testCannotDeleteLogAsNonAdmin()
    {
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( null );
        when( groupDB.isAdmin( null ) ).thenReturn( false );

        assertThat( getLogService.deleteLog( user ) ).isNull();
        verifyNoInteractions( logDB );
    }

    @Test
    public void testDeleteLogAsAdmin()
    {
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( sessionService.getSession( Nick.valueOf( "bob" ) ) ).thenReturn( "bob" );
        when( groupDB.isAdmin( "bob" ) ).thenReturn( true );

        assertThat( getLogService.deleteLog( user ) ).isEqualTo( "Log deleted." );
        verify( logDB ).deleteLog();
        verifyNoMoreInteractions( logDB );
    }

}

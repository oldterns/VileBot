package com.oldterns.vilebot.services;

import javax.inject.Inject;

import com.oldterns.vilebot.database.LogDB;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class ChatLoggerServiceTest
{

    @Inject
    ChatLoggerService chatLoggerService;

    @InjectMock
    LogDB logDB;

    @Test
    public void testLogMessages()
    {
        chatLoggerService.logMessage( "Hi there!" );
        Mockito.verify( logDB ).addItem( "Hi there!\n" );

        chatLoggerService.logMessage( "How are you?" );
        Mockito.verify( logDB ).addItem( "How are you?\n" );

        chatLoggerService.logMessage( "    Goodish; thanks?" );
        Mockito.verify( logDB ).addItem( "    Goodish; thanks?\n" );

        Mockito.verifyNoMoreInteractions( logDB );
    }
}

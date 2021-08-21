package com.oldterns.vilebot.services;

import javax.inject.Inject;
import javax.naming.LimitExceededException;

import com.oldterns.vilebot.util.LimitService;
import com.oldterns.vilebot.util.TestUrlStreamHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AsciiServiceTest
{

    @Inject
    AsciiService asciiService;

    @Inject
    TestUrlStreamHandler urlMocker;

    @InjectMock
    LimitService limitService;

    @BeforeEach
    public void setupFontUrl()
    {
        urlMocker.mockConnection( "http://www.figlet.org/fonts/graffiti.flf",
                                  AsciiServiceTest.class.getResourceAsStream( "graffiti.flf" ) );
    }

    @Test
    public void singleLineDefaultFontTest()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        String expectedReply = "    _  _     _     _               __                   _                      \n"
            + "  _| || |_  | |_  | |__     ___   / _|   ___     ___   | |__     __ _   _ __   \n"
            + " |_  ..  _| | __| | '_ \\   / _ \\ | |_   / _ \\   / _ \\  | '_ \\   / _` | | '__|  \n"
            + " |_      _| | |_  | | | | |  __/ |  _| | (_) | | (_) | | |_) | | (_| | | |     \n"
            + "   |_||_|    \\__| |_| |_|  \\___| |_|    \\___/   \\___/  |_.__/   \\__,_| |_|     \n"
            + "                                                                               \n";
        assertThat( asciiService.ascii( user, "#thefoobar", "" ) ).isEqualTo( expectedReply );
    }

    @Test
    public void singleLineGraffitiFontTest()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        String expectedReply = "   _  _     __   .__               _____                 ___.                    \n"
            + "__| || |___/  |_ |  |__    ____  _/ ____\\  ____    ____  \\_ |__  _____   _______ \n"
            + "\\   __   /\\   __\\|  |  \\ _/ __ \\ \\   __\\  /  _ \\  /  _ \\  | __ \\ \\__  \\  \\_  __ \\\n"
            + " |  ||  |  |  |  |   Y  \\\\  ___/  |  |   (  <_> )(  <_> ) | \\_\\ \\ / __ \\_ |  | \\/\n"
            + "/_  ~~  _\\ |__|  |___|  / \\___  > |__|    \\____/  \\____/  |___  /(____  / |__|   \n"
            + "  |_||_|              \\/      \\/                              \\/      \\/         \n";

        assertThat( asciiService.ascii( user, "graffiti", "#thefoobar" ) ).isEqualTo( expectedReply );
    }

    @Test
    public void multiLineDefaultFontTest()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        String expectedReply =
            "  _           _     _       _                                                                   _                         \n"
                + " (_)  ___    | |_  | |__   (_)  ___     _ __ ___     ___   ___   ___    __ _    __ _    ___    | |   ___    _ __     __ _ \n"
                + " | | / __|   | __| | '_ \\  | | / __|   | '_ ` _ \\   / _ \\ / __| / __|  / _` |  / _` |  / _ \\   | |  / _ \\  | '_ \\   / _` |\n"
                + " | | \\__ \\   | |_  | | | | | | \\__ \\   | | | | | | |  __/ \\__ \\ \\__ \\ | (_| | | (_| | |  __/   | | | (_) | | | | | | (_| |\n"
                + " |_| |___/    \\__| |_| |_| |_| |___/   |_| |_| |_|  \\___| |___/ |___/  \\__,_|  \\__, |  \\___|   |_|  \\___/  |_| |_|  \\__, |\n"
                + "                                                                               |___/                                |___/ \n"
                + "                                           _       ___   ___ \n"
                + "     ___   _ __     ___    _   _    __ _  | |__   |__ \\ |__ \\\n"
                + "    / _ \\ | '_ \\   / _ \\  | | | |  / _` | | '_ \\    / /   / /\n"
                + "   |  __/ | | | | | (_) | | |_| | | (_| | | | | |  |_|   |_| \n"
                + "    \\___| |_| |_|  \\___/   \\__,_|  \\__, | |_| |_|  (_)   (_) \n"
                + "                                   |___/                     \n";

        assertThat( asciiService.ascii( user, "is", "this message long enough??" ) ).isEqualTo( expectedReply );
    }

    @Test
    public void multiLineGraffitiFontTest()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        String expectedReply =
            ".__            __   .__     .__                                                                     .__                            \n"
                + "|__|  ______ _/  |_ |  |__  |__|  ______   _____    ____    ______  ___________      ____    ____   |  |    ____    ____     ____  \n"
                + "|  | /  ___/ \\   __\\|  |  \\ |  | /  ___/  /     \\ _/ __ \\  /  ___/ /  ___/\\__  \\    / ___\\ _/ __ \\  |  |   /  _ \\  /    \\   / ___\\ \n"
                + "|  | \\___ \\   |  |  |   Y  \\|  | \\___ \\  |  Y Y  \\\\  ___/  \\___ \\  \\___ \\  / __ \\_ / /_/  >\\  ___/  |  |__(  <_> )|   |  \\ / /_/  >\n"
                + "|__|/____  >  |__|  |___|  /|__|/____  > |__|_|  / \\___  >/____  >/____  >(____  / \\___  /  \\___  > |____/ \\____/ |___|  / \\___  / \n"
                + "         \\/              \\/          \\/        \\/      \\/      \\/      \\/      \\/ /_____/       \\/                     \\/ /_____/  \n"
                + "                                         .__     _________ _________ \n"
                + "   ____    ____    ____   __ __    ____  |  |__  \\_____   \\\\_____   \\\n"
                + " _/ __ \\  /    \\  /  _ \\ |  |  \\  / ___\\ |  |  \\    /   __/   /   __/\n"
                + " \\  ___/ |   |  \\(  <_> )|  |  / / /_/  >|   Y  \\  |   |     |   |   \n"
                + "  \\___  >|___|  / \\____/ |____/  \\___  / |___|  /  |___|     |___|   \n"
                + "      \\/      \\/                /_____/       \\/   <___>     <___>   \n";

        assertThat( asciiService.ascii( user, "graffiti",
                                        "is this message long enough??" ) ).isEqualTo( expectedReply );
    }

    @Test
    public void multiLineWordBreakDefaultFontTest()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        String expectedReply =
            "  _                            _                             _           _     _       _                                          \n"
                + " | |__     ___   __      __   | |   ___    _ __     __ _    (_)  ___    | |_  | |__   (_)  ___     _ __ ___     ___   ___         \n"
                + " | '_ \\   / _ \\  \\ \\ /\\ / /   | |  / _ \\  | '_ \\   / _` |   | | / __|   | __| | '_ \\  | | / __|   | '_ ` _ \\   / _ \\ / __|  _____ \n"
                + " | | | | | (_) |  \\ V  V /    | | | (_) | | | | | | (_| |   | | \\__ \\   | |_  | | | | | | \\__ \\   | | | | | | |  __/ \\__ \\ |_____|\n"
                + " |_| |_|  \\___/    \\_/\\_/     |_|  \\___/  |_| |_|  \\__, |   |_| |___/    \\__| |_| |_| |_| |___/   |_| |_| |_|  \\___| |___/        \n"
                + "                                                   |___/                                                                          \n"
                + "                                                            ___   ___ \n"
                + "  ___    __ _    __ _    ___     _ __     ___   __      __ |__ \\ |__ \\\n"
                + " / __|  / _` |  / _` |  / _ \\   | '_ \\   / _ \\  \\ \\ /\\ / /   / /   / /\n"
                + " \\__ \\ | (_| | | (_| | |  __/   | | | | | (_) |  \\ V  V /   |_|   |_| \n"
                + " |___/  \\__,_|  \\__, |  \\___|   |_| |_|  \\___/    \\_/\\_/    (_)   (_) \n"
                + "                |___/                                                 \n";

        assertThat( asciiService.ascii( user, "how", "long is this message now??" ) ).isEqualTo( expectedReply );
    }

    @Test
    public void multiLineWordBreakGraffitiFontTest()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        String expectedReply =
            ".__                       .__                             .__            __   .__     .__                                            \n"
                + "|  |__    ____  __  _  __ |  |    ____    ____     ____   |__|  ______ _/  |_ |  |__  |__|  ______   _____    ____    ______         \n"
                + "|  |  \\  /  _ \\ \\ \\/ \\/ / |  |   /  _ \\  /    \\   / ___\\  |  | /  ___/ \\   __\\|  |  \\ |  | /  ___/  /     \\ _/ __ \\  /  ___/  ______ \n"
                + "|   Y  \\(  <_> ) \\     /  |  |__(  <_> )|   |  \\ / /_/  > |  | \\___ \\   |  |  |   Y  \\|  | \\___ \\  |  Y Y  \\\\  ___/  \\___ \\  /_____/ \n"
                + "|___|  / \\____/   \\/\\_/   |____/ \\____/ |___|  / \\___  /  |__|/____  >  |__|  |___|  /|__|/____  > |__|_|  / \\___  >/____  >         \n"
                + "     \\/                                      \\/ /_____/            \\/              \\/          \\/        \\/      \\/      \\/          \n"
                + "                                                           _________ _________ \n"
                + "  ___________      ____    ____     ____    ____  __  _  __\\_____   \\\\_____   \\\n"
                + " /  ___/\\__  \\    / ___\\ _/ __ \\   /    \\  /  _ \\ \\ \\/ \\/ /   /   __/   /   __/\n"
                + " \\___ \\  / __ \\_ / /_/  >\\  ___/  |   |  \\(  <_> ) \\     /   |   |     |   |   \n"
                + "/____  >(____  / \\___  /  \\___  > |___|  / \\____/   \\/\\_/    |___|     |___|   \n"
                + "     \\/      \\/ /_____/       \\/       \\/                    <___>     <___>   \n";

        assertThat( asciiService.ascii( user, "graffiti",
                                        "how long is this message now??" ) ).isEqualTo( expectedReply );
    }

    @Test
    public void superLongMessageTest()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        String expectedReply =
            "  _                                           _                                           _           _                          _         \n"
                + " | |       ___    _ __    ___   _ __ ___     (_)  _ __    ___   _   _   _ __ ___       __| |   ___   | |   ___    _ __     ___  (_)        \n"
                + " | |      / _ \\  | '__|  / _ \\ | '_ ` _ \\    | | | '_ \\  / __| | | | | | '_ ` _ \\     / _` |  / _ \\  | |  / _ \\  | '__|   / __| | |  _____ \n"
                + " | |___  | (_) | | |    |  __/ | | | | | |   | | | |_) | \\__ \\ | |_| | | | | | | |   | (_| | | (_) | | | | (_) | | |      \\__ \\ | | |_____|\n"
                + " |_____|  \\___/  |_|     \\___| |_| |_| |_|   |_| | .__/  |___/  \\__,_| |_| |_| |_|    \\__,_|  \\___/  |_|  \\___/  |_|      |___/ |_|        \n"
                + "                                                 |_|                                                                                       \n"
                + "  _                                  _                                                      _            _                    \n"
                + " | |_      __ _   _ __ ___     ___  | |_          ___    ___    _ __    ___    ___    ___  | |_    ___  | |_   _   _   _ __   \n"
                + " | __|    / _` | | '_ ` _ \\   / _ \\ | __|        / __|  / _ \\  | '_ \\  / __|  / _ \\  / __| | __|  / _ \\ | __| | | | | | '__|  \n"
                + " | |_    | (_| | | | | | | | |  __/ | |_   _    | (__  | (_) | | | | | \\__ \\ |  __/ | (__  | |_  |  __/ | |_  | |_| | | |     \n"
                + "  \\__|    \\__,_| |_| |_| |_|  \\___|  \\__| ( )    \\___|  \\___/  |_| |_| |___/  \\___|  \\___|  \\__|  \\___|  \\__|  \\__,_| |_|     \n"
                + "                                          |/                                                                                  \n"
                + "              _   _           _                _                            _   _   _           _____   _     _ \n"
                + "   __ _    __| | (_)  _ __   (_)  ___    ___  (_)  _ __     __ _      ___  | | (_) | |_        | ____| | |_  (_)\n"
                + "  / _` |  / _` | | | | '_ \\  | | / __|  / __| | | | '_ \\   / _` |    / _ \\ | | | | | __|       |  _|   | __| | |\n"
                + " | (_| | | (_| | | | | |_) | | | \\__ \\ | (__  | | | | | | | (_| |   |  __/ | | | | | |_   _    | |___  | |_  | |\n"
                + "  \\__,_|  \\__,_| |_| | .__/  |_| |___/  \\___| |_| |_| |_|  \\__, |    \\___| |_| |_|  \\__| (_)   |_____|  \\__| |_|\n"
                + "                     |_|                                   |___/                                                \n";

        assertThat( asciiService.ascii( user, "Lorem",
                                        "ipsum dolor sit amet, consectetur adipiscing elit. Etiam vitae erat ut dui varius sodales sed." ) ).isEqualTo( expectedReply );
    }

    @Test
    public void asciifontsTest()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        String expectedReply = "Available fonts for !ascii:\n" + "            graffiti ";

        asciiService.asciifonts( user );
        for ( String line : expectedReply.split( "\n" ) )
        {
            verify( user, times( 1 ) ).sendMessage( line );
        }
    }

    @Test
    public void testChannelUseLimit()
        throws LimitExceededException
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        doThrow( new LimitExceededException( "bob at limit" ) ).when( limitService ).addUse( user );
        assertThat( asciiService.ascii( user, "#thefoobar", "" ) ).isEqualTo( "bob at limit" );
    }

    @Test
    public void testPrivateMessageDoNotUseLimit()
        throws LimitExceededException
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        String expectedReply = "    _  _     _     _               __                   _                      \n"
            + "  _| || |_  | |_  | |__     ___   / _|   ___     ___   | |__     __ _   _ __   \n"
            + " |_  ..  _| | __| | '_ \\   / _ \\ | |_   / _ \\   / _ \\  | '_ \\   / _` | | '__|  \n"
            + " |_      _| | |_  | | | | |  __/ |  _| | (_) | | (_) | | |_) | | (_| | | |     \n"
            + "   |_||_|    \\__| |_| |_|  \\___| |_|    \\___/   \\___/  |_.__/   \\__,_| |_|     \n"
            + "                                                                               \n";

        doThrow( new LimitExceededException( "bob at limit" ) ).when( limitService ).addUse( user );
        assertThat( asciiService.asciiPrivateMessage( "#thefoobar", "" ) ).isEqualTo( expectedReply );
    }

}

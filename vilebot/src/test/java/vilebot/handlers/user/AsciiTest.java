package vilebot.handlers.user;

import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.handlers.user.Ascii;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class AsciiTest
{

    Ascii asciiClass = new Ascii();

    ReceivePrivmsg event;

    @Before
    public void setSenderAndChannel()
    {
        event = mock( ReceivePrivmsg.class );
        when( event.getSender() ).thenReturn( "salman" );
        when( event.getChannel() ).thenReturn( "#thefoobar" );
    }

    @Test
    public void singleLineDefaultFontTest()
    {
        String ircmsg = "!ascii #thefoobar";
        String expectedReply = "    _  _     _     _               __                   _                    \n"
            + "  _| || |_  | |_  | |__     ___   / _|   ___     ___   | |__     __ _   _ __ \n"
            + " |_  ..  _| | __| | '_ \\   / _ \\ | |_   / _ \\   / _ \\  | '_ \\   / _` | | '__|\n"
            + " |_      _| | |_  | | | | |  __/ |  _| | (_) | | (_) | | |_) | | (_| | | |   \n"
            + "   |_||_|    \\__| |_| |_|  \\___| |_|    \\___/   \\___/  |_.__/   \\__,_| |_|   \n"
            + "                                                                             \n";
        when( event.getText() ).thenReturn( ircmsg );
        asciiClass.ascii( event );
        verify( event, times( 1 ) ).reply( expectedReply );
    }

    @Test
    public void singleLineGraffitiFontTest()
    {
        String ircmsg = "!ascii graffiti #thefoobar";
        String expectedReply = "   _  _     __   .__               _____                 ___.                    \n"
            + "__| || |___/  |_ |  |__    ____  _/ ____\\  ____    ____  \\_ |__  _____   _______ \n"
            + "\\   __   /\\   __\\|  |  \\ _/ __ \\ \\   __\\  /  _ \\  /  _ \\  | __ \\ \\__  \\  \\_  __ \\\n"
            + " |  ||  |  |  |  |   Y  \\\\  ___/  |  |   (  <_> )(  <_> ) | \\_\\ \\ / __ \\_ |  | \\/\n"
            + "/_  ~~  _\\ |__|  |___|  / \\___  > |__|    \\____/  \\____/  |___  /(____  / |__|   \n"
            + "  |_||_|              \\/      \\/                              \\/      \\/         \n";

        when( event.getText() ).thenReturn( ircmsg );
        asciiClass.ascii( event );
        verify( event, times( 1 ) ).reply( expectedReply );
    }

    @Test
    public void multiLineDefaultFontTest()
    {
        String ircmsg = "!ascii is this message long enough??";
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

        when( event.getText() ).thenReturn( ircmsg );
        asciiClass.ascii( event );
        verify( event, times( 1 ) ).reply( expectedReply );
    }

    @Test
    public void multiLineGraffitiFontTest()
    {
        String ircmsg = "!ascii graffiti is this message long enough??";
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

        when( event.getText() ).thenReturn( ircmsg );
        asciiClass.ascii( event );
        verify( event, times( 1 ) ).reply( expectedReply );
    }

    @Test
    public void multiLineWordBreakDefaultFontTest()
    {
        String ircmsg = "!ascii how long is this message now??";
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

        when( event.getText() ).thenReturn( ircmsg );
        asciiClass.ascii( event );
        verify( event, times( 1 ) ).reply( expectedReply );
    }

    @Test
    public void multiLineWordBreakGraffitiFontTest()
    {
        String ircmsg = "!ascii graffiti how long is this message now??";
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

        when( event.getText() ).thenReturn( ircmsg );
        asciiClass.ascii( event );
        verify( event, times( 1 ) ).reply( expectedReply );
    }

    @Test
    public void superLongMessageTest()
    {
        String ircmsg =
            "!ascii Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam vitae erat ut dui varius sodales sed.";
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

        when( event.getText() ).thenReturn( ircmsg );
        asciiClass.ascii( event );
        verify( event, times( 1 ) ).reply( expectedReply );
    }

    @Test
    public void asciifontsTest()
    {
        String ircmsg = "!asciifonts";
        String expectedReply = "Available fonts for !ascii:\n"
            + "         stampatello             fourtops                 mike             bulbhead            alligator \n"
            + "            drpepper           isometric3              cricket             starwars                  big \n"
            + "             rounded                 kban              letters             barbwire               fender \n"
            + "               slide         nancyj-fancy              maxfour               bubble             bigchief \n"
            + "              chunky                rozzo                short           cybersmall           smkeyboard \n"
            + "           dotmatrix               binary              diamond              stellar               nancyj \n"
            + "           hollywood             katakana                roman           caligraphy               madrid \n"
            + "          cyberlarge                tanja               sblood           isometric4               mirror \n"
            + "            mnemonic             computer              pyramid                ticks             eftipiti \n"
            + "          tinker-toy                fuzzy          cybermedium               relief               italic \n"
            + "        5lineoblique             eftiwall             graffiti                weird            tombstone \n"
            + "                 doh             coinstak              pebbles           rectangles             calgphy2 \n"
            + "           eftiwater               poison                   o8              larry3d                 trek \n"
            + "          alligator2              tengwar             alphabet           isometric1             smisome1 \n"
            + "              script             contrast               moscow                speed            eftirobot \n"
            + "             banner4             rowancap              smslant                rot13             colossal \n"
            + "             marquee                 stop           ticksslant              usaflag              tsalagi \n"
            + "                lean                 mini              relief2            smtengwar             twopoint \n"
            + "           banner3-D                 bell            acrobatic                small               invita \n"
            + "            smshadow             straight               pepper             standard             contessa \n"
            + "             cosmike           isometric2                morse              catwalk                basic \n"
            + "             cursive            eftichess             smscript                  3-d           threepoint \n"
            + "         lockergnome                runyc                  lcd                peaks                puffy \n"
            + "   nancyj-underlined                slant                 ogre                linux               avatar \n"
            + "              gothic              banner3            jerusalem               cosmic                 pawp \n"
            + "               goofy                  3x5                ivrit              jazmine                 epic \n"
            + "               runic             slscript              digital             serifcap                thick \n"
            + "             ntgreek            eftitalic                block                 term                  rev \n"
            + "             nipples                 doom             eftifont               shadow                 thin \n";
        when( event.getText() ).thenReturn( ircmsg );
        asciiClass.asciifonts( event );
        verify( event, times( 1 ) ).replyPrivately( expectedReply );
    }

}

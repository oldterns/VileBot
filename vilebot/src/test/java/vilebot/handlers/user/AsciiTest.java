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
            + "                 3-d                  3x5         5lineoblique            acrobatic            alligator \n"
            + "          alligator2             alphabet               avatar              banner3            banner3-D \n"
            + "             banner4             barbwire                basic                 bell                  big \n"
            + "            bigchief               binary                block               bubble             bulbhead \n"
            + "            calgphy2           caligraphy              catwalk               chunky             coinstak \n"
            + "            colossal             computer             contessa             contrast               cosmic \n"
            + "             cosmike              cricket              cursive           cyberlarge          cybermedium \n"
            + "          cybersmall              diamond              digital                  doh                 doom \n"
            + "           dotmatrix             drpepper            eftichess             eftifont             eftipiti \n"
            + "           eftirobot            eftitalic             eftiwall            eftiwater                 epic \n"
            + "              fender             fourtops                fuzzy                goofy               gothic \n"
            + "            graffiti            hollywood               invita           isometric1           isometric2 \n"
            + "          isometric3           isometric4               italic                ivrit              jazmine \n"
            + "           jerusalem             katakana                 kban              larry3d                  lcd \n"
            + "                lean              letters                linux          lockergnome               madrid \n"
            + "             marquee              maxfour                 mike                 mini               mirror \n"
            + "            mnemonic                morse               moscow               nancyj         nancyj-fancy \n"
            + "   nancyj-underlined              nipples              ntgreek                   o8                 ogre \n"
            + "                pawp                peaks              pebbles               pepper               poison \n"
            + "               puffy              pyramid           rectangles               relief              relief2 \n"
            + "                 rev                roman                rot13              rounded             rowancap \n"
            + "               rozzo                runic                runyc               sblood               script \n"
            + "            serifcap               shadow                short                slant                slide \n"
            + "            slscript                small             smisome1           smkeyboard             smscript \n"
            + "            smshadow              smslant            smtengwar                speed          stampatello \n"
            + "            standard             starwars              stellar                 stop             straight \n"
            + "               tanja              tengwar                 term                thick                 thin \n"
            + "          threepoint                ticks           ticksslant           tinker-toy            tombstone \n"
            + "                trek              tsalagi             twopoint              usaflag                weird \n";
        when( event.getText() ).thenReturn( ircmsg );
        asciiClass.asciifonts( event );
        verify( event, times( 1 ) ).replyPrivately( expectedReply );
    }

}

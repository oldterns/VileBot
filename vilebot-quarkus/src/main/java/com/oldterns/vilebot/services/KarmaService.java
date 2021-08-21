package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.NoHelp;
import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.vilebot.database.ChurchDB;
import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.IgnoredUsers;
import com.oldterns.vilebot.util.RandomProvider;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@Named( "karma" )
public class KarmaService
{
    @Inject
    KarmaDB karmaDB;

    @Inject
    ChurchDB churchDB;

    @Inject
    RandomProvider randomProvider;

    @Inject
    IgnoredUsers ignoredUsers;

    @Handler
    public String onUserJoin( ChannelJoinEvent channelJoinEvent )
    {
        Nick noun = Nick.getNick( channelJoinEvent.getUser() );
        if ( !ignoredUsers.getOnJoin().contains( noun ) )
            return getReplyWithRankAndKarma( noun.getBaseNick(), false, false, true );
        return null;
    }

    @OnMessage( "!total" )
    public String total()
    {
        return "" + karmaDB.getTotalKarma();
    }

    @OnMessage( "!topthree" )
    public String getTopThree()
    {
        Set<String> nouns = karmaDB.getRankNouns( 0L, 2L );
        if ( nouns != null && nouns.size() > 0 )
        {
            return nouns.stream().map( noun -> getReplyWithRankAndKarma( noun, false,
                                                                         true ) ).collect( Collectors.joining( "\n" ) );
        }
        else
        {
            return "No nouns at ranks 1 to 3.";
        }
    }

    @OnMessage( "!bottomthree" )
    public String getBottomThree()
    {
        Set<String> nouns = karmaDB.getRevRankNouns( 0L, 2L );
        if ( nouns != null && nouns.size() > 0 )
        {
            return nouns.stream().map( noun -> getReplyWithRankAndKarma( noun, true,
                                                                         true ) ).collect( Collectors.joining( "\n" ) );
        }
        else
        {
            return "No nouns at ranks 1 to 3.";
        }
    }

    @OnChannelMessage( "@channelMessage" )
    @NoHelp
    public String karmaIncOrDec( User user, String channelMessage )
    {
        Nick userNick = Nick.getNick( user );
        StringBuilder out = new StringBuilder();
        boolean insult = false;
        for ( String noun : channelMessage.split( "\\s+" ) )
        {
            Nick nick = Nick.valueOf( noun );

            if ( noun.endsWith( "++" ) )
            {
                if ( userNick.getBaseNick().equals( nick.getBaseNick() ) )
                {
                    insult = true;
                    continue;
                }
                karmaDB.modNounKarma( nick.getBaseNick(), 1 );
            }
            else if ( noun.endsWith( "--" ) )
            {
                if ( userNick.getBaseNick().equals( nick.getBaseNick() ) )
                {
                    insult = true;
                    continue;
                }
                karmaDB.modNounKarma( nick.getBaseNick(), -1 );
            }
            else if ( noun.endsWith( "+-" ) )
            {
                if ( userNick.getBaseNick().equals( nick.getBaseNick() ) )
                {
                    insult = true;
                    continue;
                }
                int karma = randomProvider.getRandomBoolean() ? 1 : -1;
                String reply = nick + " had their karma ";
                reply += karma == 1 ? "increased" : "decreased";
                reply += " by 1\n";
                out.append( reply );
                karmaDB.modNounKarma( nick.getBaseNick(), karma );
            }
        }

        if ( insult )
        {
            out.append( "I think I'm supposed to insult you now.\n" );
        }

        if ( out.length() == 0 )
        {
            return null;
        }
        // Remove last newline
        out.deleteCharAt( out.length() - 1 );
        return out.toString();
    }

    @OnMessage( "!rankn @rank" )
    public String rankn( Integer rank )
    {
        String noun = karmaDB.getRankNoun( rank );
        if ( noun != null )
        {
            return getReplyWithRankAndKarma( noun );
        }
        else
        {
            return "No noun at that rank.";
        }
    }

    @OnMessage( "!revrankn @rank" )
    public String revrankn( Integer rank )
    {
        String noun = karmaDB.getRevRankNoun( rank );
        if ( noun != null )
        {
            return getReplyWithRankAndKarma( noun, true );
        }
        else
        {
            return "No noun at that rank.";
        }
    }

    @OnMessage( "!rank @nick" )
    public String rank( Nick nick )
    {
        return getReplyWithRankAndKarma( nick.getBaseNick() );
    }

    @OnMessage( "!revrank @nick" )
    public String revrank( Nick nick )
    {
        return getReplyWithRankAndKarma( nick.getBaseNick(), true );
    }

    @OnMessage( "!rank" )
    public String selfRank( User user )
    {
        return rank( Nick.getNick( user ) );
    }

    @OnMessage( "!revrank" )
    public String selfRevrank( User user )
    {
        return revrank( Nick.getNick( user ) );
    }

    private String getReplyWithRankAndKarma( String noun )
    {
        return getReplyWithRankAndKarma( noun, false );
    }

    private String getReplyWithRankAndKarma( String noun, boolean reverseOrder )
    {
        return getReplyWithRankAndKarma( noun, reverseOrder, false );
    }

    private String getReplyWithRankAndKarma( String noun, boolean reverseOrder, boolean obfuscateNick )
    {
        return getReplyWithRankAndKarma( noun, reverseOrder, obfuscateNick, false );
    }

    private String getReplyWithRankAndKarma( String noun, boolean reverseOrder, boolean obfuscateNick,
                                             boolean useTitle )
    {
        Optional<Long> maybeNounRank;
        if ( reverseOrder )
            maybeNounRank = karmaDB.getNounRevRank( noun );
        else
            maybeNounRank = karmaDB.getNounRank( noun );

        Optional<Long> maybeNounKarma = karmaDB.getNounKarma( noun );

        if ( useTitle && churchDB.isTopDonor( noun ) )
        {
            String title = churchDB.getDonorTitle( noun );
            if ( title.trim().length() > 0 )
            {
                noun = title;
            }
        }

        if ( maybeNounRank.isPresent() && maybeNounKarma.isPresent() )
        {
            long nounRank = maybeNounRank.get();
            long nounKarma = maybeNounKarma.get();
            StringBuilder sb = new StringBuilder();

            sb.append( noun );
            if ( obfuscateNick )
                sb.reverse();

            sb.append( " is " );
            sb.append( "ranked at " );

            if ( reverseOrder )
                sb.append( "(reverse) " );

            sb.append( "#" );
            sb.append( nounRank );
            sb.append( " with " );
            sb.append( nounKarma );
            sb.append( " points of karma." );

            return sb.toString();
        }
        return noun + " has no karma.";
    }
}

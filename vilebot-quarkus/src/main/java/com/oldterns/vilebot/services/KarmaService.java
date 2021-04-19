package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.Delimiter;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.annotations.Regex;
import com.oldterns.vilebot.database.ChurchDB;
import com.oldterns.vilebot.database.KarmaDB;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
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

    @OnChannelMessage( "!total" )
    public String total()
    {
        return "" + karmaDB.getTotalKarma();
    }

    @OnChannelMessage( "!topthree" )
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

    @OnChannelMessage( "!bottomthree" )
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

    @OnChannelMessage( ".*?@nounList.*?" )
    public void karmaInc( @Regex(Nick.regex + "\\+\\+" ) @Delimiter( "\\w.+" ) List<String> nounList )
    {
        for ( String noun : nounList )
        {
            Nick nick = Nick.valueOf( noun );
            System.out.println(nick.getFullNick());
            System.out.println(nick.getBaseNick());
            karmaDB.modNounKarma( nick.getBaseNick(), 1 );
        }
    }

    @OnChannelMessage( ".*@nounList.*?" )
    public void karmaDec( @Regex(Nick.regex + "--" ) @Delimiter( ".*?" ) List<String> nounList )
    {
        for ( String noun : nounList )
        {
            Nick nick = Nick.valueOf( noun );
            karmaDB.modNounKarma( nick.getBaseNick(), -1 );
        }
    }

    @OnChannelMessage( ".*@nounList.*?" )
    public void karmaIncOrDec( @Regex(Nick.regex + "\\+-" ) @Delimiter( ".*?" ) List<String> nounList )
    {
        for ( String noun : nounList )
        {
            Nick nick = Nick.valueOf( noun );
            karmaDB.modNounKarma( nick.getBaseNick(), -1 );
        }
    }

    @OnChannelMessage( "!rank @nick" )
    public String rank( Nick nick )
    {
        return getReplyWithRankAndKarma( nick.getBaseNick() );
    }

    @OnChannelMessage( "!revrank @nick" )
    public String revrank( Nick nick )
    {
        return getReplyWithRankAndKarma( nick.getBaseNick(), true );
    }

    @OnChannelMessage( "!rank" )
    public String selfRank( ChannelMessage message )
    {
        return rank(message.getNick());
    }

    @OnChannelMessage( "!revrank" )
    public String selfRevrank( ChannelMessage message )
    {
        return revrank(message.getNick());
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

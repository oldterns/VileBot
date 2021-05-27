package com.oldterns.vilebot.services;

import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.annotations.Regex;
import com.oldterns.vilebot.database.UserlistDB;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class UserListService
{
    final static String NOUN_REGEX = "\\S+?";

    @Inject
    UserlistDB userlistDB;

    @OnChannelMessage( "!lists" )
    public String listsEnumerate( ChannelMessageEvent event )
    {
        Set<String> lists = userlistDB.getLists();

        if ( lists != null && lists.size() > 0 )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "Available lists: " );
            for ( String list : lists )
            {
                sb.append( list );
                sb.append( ", " );
            }
            sb.delete( sb.length() - 2, sb.length() );
            return sb.toString();
        }
        else
        {
            return "There are no lists.";
        }
    }

    @OnChannelMessage( "!list @listName" )
    public void listQuery( ChannelMessageEvent event, @Regex( NOUN_REGEX ) String listName )
    {

        Set<String> users = userlistDB.getUsersIn( listName );
        if ( users != null && users.size() > 0 )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "The list " );
            sb.append( listName );
            sb.append( " contains: " );

            for ( String user : users )
            {
                sb.append( user );
                sb.append( ", " );
            }
            sb.delete( sb.length() - 2, sb.length() );

            event.getActor().sendMessage( sb.toString() );
        }
        else
        {
            event.getActor().sendMessage( "The list " + listName + " does not exist or is empty." );
        }
    }

    @OnChannelMessage( "!listadd @listName @nicks" )
    public String listAdd( @Regex( NOUN_REGEX ) String listName, @Regex( NOUN_REGEX ) List<String> nicks )
    {
        StringBuilder sb = new StringBuilder();
        userlistDB.addUsersTo( listName, nicks );
        sb.append( "Added the following names to list " );
        sb.append( listName );
        sb.append( ": " );

        for ( String nick : nicks )
        {
            sb.append( nick );
            sb.append( ", " );
        }
        sb.delete( sb.length() - 2, sb.length() );

        return sb.toString();
    }

    @OnChannelMessage( "!listrem @listName @nicks" )
    public String listRemove( @Regex( NOUN_REGEX ) String listName, @Regex( NOUN_REGEX ) List<String> nicks )
    {
        StringBuilder sb = new StringBuilder();
        userlistDB.removeUsersFrom( listName, nicks );
        sb.append( "Removed the following names to list " );
        sb.append( listName );
        sb.append( ": " );

        for ( String nick : nicks )
        {
            sb.append( nick );
            sb.append( ", " );
        }
        sb.delete( sb.length() - 2, sb.length() );

        return sb.toString();
    }

    @OnChannelMessage( "@listName: @msg" )
    public String listExpansion( ChannelMessageEvent event, @Regex( NOUN_REGEX ) String listName, String msg )
    {
        String sender = event.getActor().getNick();
        Set<String> users = userlistDB.getUsersIn( listName );

        if ( users != null && users.size() > 0 )
        {
            StringBuilder sb = new StringBuilder();
            for ( String user : users )
            {
                if ( !user.equals( sender ) )
                {
                    sb.append( user );
                    sb.append( ", " );
                }
            }
            sb.delete( sb.length() - 2, sb.length() );
            sb.append( ": " );
            sb.append( msg );

            return sb.toString();
        }
        else
        {
            return null;
        }
    }
}

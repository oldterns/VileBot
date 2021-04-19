/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.database;

import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserlistDB
{
    private static final String keyOfUserlistSetsPrefix = "userlist-";

    @Inject
    RedisClient redisClient;

    /**
     * Get the nicks in a userlist.
     * 
     * @param listName The name of the list
     * @return The set of nicks for the list
     */
    public Set<String> getUsersIn( String listName )
    {
        return redisClient.smembers( keyOfUserlistSetsPrefix
            + listName ).stream().map( Response::toString ).collect( Collectors.toSet() );
    }

    /**
     * Add the given nicks to a userlist.
     * 
     * @param listName The name of the list param The set of nicks for the list
     * @param nicks The nicks to add
     */
    public void addUsersTo( String listName, Collection<String> nicks )
    {
        List<String> args = new ArrayList<>( nicks.size() + 1 );
        args.add( keyOfUserlistSetsPrefix + listName );
        args.addAll( nicks );
        redisClient.sadd( args );
    }

    /**
     * Remove the given nicks from a userlist.
     * 
     * @param listName The name of the list param The set of nicks for the list
     * @param nicks The nicks to remove
     */
    public void removeUsersFrom( String listName, Collection<String> nicks )
    {
        List<String> args = new ArrayList<>( nicks.size() + 1 );
        args.add( keyOfUserlistSetsPrefix + listName );
        args.addAll( nicks );
        redisClient.srem( args );
    }

    /**
     * Get the set of the available userlists.
     * 
     * @return The set of userlists
     */
    public Set<String> getLists()
    {
        // KEYS is O(n) operation, so it won't scale propery for large numbers of database keys.
        // We're going to use it here however, as the number of keys in our database should never get large enough
        // for this to matter, and the alternative of maintaining a set of userlists has the potential to be buggy.
        Set<String> rawlists = redisClient.keys( keyOfUserlistSetsPrefix
            + "*" ).stream().map( Response::toString ).collect( Collectors.toSet() );
        Set<String> lists = new HashSet<String>();

        // Strip the prefix from each of the keys to isolate the list name
        int cut = keyOfUserlistSetsPrefix.length();
        for ( String rawlist : rawlists )
        {
            String list = rawlist.substring( cut );
            lists.add( list );
        }

        return lists;
    }
}

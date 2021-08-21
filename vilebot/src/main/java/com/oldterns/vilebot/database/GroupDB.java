/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.database;

import io.quarkus.redis.client.RedisClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class GroupDB
{
    private static final String keyOfGroupSetsPrefix = "groups-";

    private static final String opGroupName = "ops";

    private static final String adminGroupName = "admins";

    @Inject
    RedisClient redisClient;

    /**
     * Checks if the the Admin group is empty.
     * 
     * @return true iff the Admin group has no members.
     */
    public boolean noAdmins()
    {
        return groupSize( adminGroupName ) == 0;
    }

    /**
     * Checks if the given nick is in the Admins group.
     * 
     * @param nick The nick to check
     * @return true iff the nick is in the Admin group
     */
    public boolean isAdmin( String nick )
    {
        return isInGroup( adminGroupName, nick );
    }

    /**
     * Checks if the given nick is in the Operators group.
     * 
     * @param nick The nick to check
     * @return true iff the nick is in the Op group
     */
    public boolean isOp( String nick )
    {
        return isInGroup( opGroupName, nick );
    }

    /**
     * Checks if the given nick is in the given group.
     * 
     * @param group The group name to check
     * @param nick The nick to check
     * @return true iff the nick is in the Op group
     */
    private boolean isInGroup( String group, String nick )
    {
        return nick != null && redisClient.sismember( keyOfGroupSetsPrefix + group, nick ).toBoolean();
    }

    private long groupSize( String group )
    {
        return redisClient.scard( keyOfGroupSetsPrefix + group ).toLong();
    }

    /**
     * Adds the given nick to the Admins group.
     * 
     * @param nick The nick to add
     * @return true iff the nick was not already there
     */
    public boolean addAdmin( String nick )
    {
        return addToGroup( adminGroupName, nick );
    }

    /**
     * Adds the given nick to the Operators group.
     * 
     * @param nick The nick to add
     * @return true iff the nick was not already there
     */
    public boolean addOp( String nick )
    {
        return addToGroup( opGroupName, nick );
    }

    /**
     * Adds the given nick to the given group. Makes a new group if it doesn't exist.
     * 
     * @param group The group name to use
     * @param nick The nick to add
     * @return true iff a new element was inserted
     */
    private boolean addToGroup( String group, String nick )
    {
        long reply = redisClient.sadd( List.of( keyOfGroupSetsPrefix + group, nick ) ).toLong();
        return reply == 1;
    }

    /**
     * Removes the given nick from the Admins group.
     * 
     * @param nick The nick to remove
     * @return true iff the nick existed
     */
    public boolean remAdmin( String nick )
    {
        return remFromGroup( adminGroupName, nick );
    }

    /**
     * Removes the given nick from the Operators group.
     * 
     * @param nick The nick to remove
     * @return true iff the nick existed
     */
    public boolean remOp( String nick )
    {
        return remFromGroup( opGroupName, nick );
    }

    /**
     * Removes the given nick to the given group.
     * 
     * @param group The group name to use
     * @param nick The nick to remove
     * @return true iff an element was removed
     */
    private boolean remFromGroup( String group, String nick )
    {
        long reply = redisClient.srem( List.of( keyOfGroupSetsPrefix + group, nick ) ).toLong();
        return reply == 1;
    }
}

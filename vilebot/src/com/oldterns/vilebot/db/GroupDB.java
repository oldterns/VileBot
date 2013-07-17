/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.db;

import redis.clients.jedis.Jedis;

public class GroupDB
    extends RedisDB
{
    private static final String keyOfGroupSetsPrefix = "groups-";

    private static final String opGroupName = "ops";

    private static final String adminGroupName = "admins";

    /**
     * Checks if the the Admin group is empty.
     * 
     * @return true iff the Admin group has no members.
     */
    public static boolean noAdmins()
    {
        return groupSize( adminGroupName ) == 0;
    }

    /**
     * Checks if the given nick is in the Admins group.
     * 
     * @param nick The nick to check
     * @return true iff the nick is in the Admin group
     */
    public static boolean isAdmin( String nick )
    {
        return isInGroup( adminGroupName, nick );
    }

    /**
     * Checks if the given nick is in the Operators group.
     * 
     * @param nick The nick to check
     * @return true iff the nick is in the Op group
     */
    public static boolean isOp( String nick )
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
    private static boolean isInGroup( String group, String nick )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return nick != null && jedis.sismember( keyOfGroupSetsPrefix + group, nick );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    private static long groupSize( String group )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return jedis.scard( keyOfGroupSetsPrefix + group );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Adds the given nick to the Admins group.
     * 
     * @param nick The nick to add
     * @return true iff the nick was not already there
     */
    public static boolean addAdmin( String nick )
    {
        return addToGroup( adminGroupName, nick );
    }

    /**
     * Adds the given nick to the Operators group.
     * 
     * @param nick The nick to add
     * @return true iff the nick was not already there
     */
    public static boolean addOp( String nick )
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
    private static boolean addToGroup( String group, String nick )
    {
        Jedis jedis = pool.getResource();
        try
        {
            long reply = jedis.sadd( keyOfGroupSetsPrefix + group, nick );
            return reply == 1;
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Removes the given nick from the Admins group.
     * 
     * @param nick The nick to remove
     * @return true iff the nick existed
     */
    public static boolean remAdmin( String nick )
    {
        return remFromGroup( adminGroupName, nick );
    }

    /**
     * Removes the given nick from the Operators group.
     * 
     * @param nick The nick to remove
     * @return true iff the nick existed
     */
    public static boolean remOp( String nick )
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
    private static boolean remFromGroup( String group, String nick )
    {
        Jedis jedis = pool.getResource();
        try
        {
            long reply = jedis.srem( keyOfGroupSetsPrefix + group, nick );
            return reply == 1;
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }
}

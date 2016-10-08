/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.db;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.Jedis;

public class UserlistDB extends RedisDB {
    private static final String keyOfUserlistSetsPrefix = "userlist-";

    /**
     * Get the nicks in a userlist.
     * 
     * @param noun The name of the list
     * @return The set of nicks for the list
     */
    public static Set<String> getUsersIn(String listName) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.smembers(keyOfUserlistSetsPrefix + listName);
        } finally {
            pool.returnResource(jedis);
        }
    }

    /**
     * Add the given nicks to a userlist.
     * 
     * @param noun The name of the list param The set of nicks for the list
     * @param nicks The nicks to add
     */
    public static void addUsersTo(String listName, Collection<String> nicks) {
        Jedis jedis = pool.getResource();
        try {
            String[] nicksArray = new String[nicks.size()];
            nicks.toArray(nicksArray);
            jedis.sadd(keyOfUserlistSetsPrefix + listName, nicksArray);
        } finally {
            pool.returnResource(jedis);
        }
    }

    /**
     * Remove the given nicks from a userlist.
     * 
     * @param noun The name of the list param The set of nicks for the list
     * @param nicks The nicks to remove
     */
    public static void removeUsersFrom(String listName, Collection<String> nicks) {
        Jedis jedis = pool.getResource();
        try {
            String[] nicksArray = new String[nicks.size()];
            nicks.toArray(nicksArray);
            jedis.srem(keyOfUserlistSetsPrefix + listName, nicksArray);
        } finally {
            pool.returnResource(jedis);
        }
    }

    /**
     * Get the set of the available userlists.
     * 
     * @return The set of userlists
     */
    public static Set<String> getLists() {
        Jedis jedis = pool.getResource();
        try {
            // KEYS is O(n) operation, so it won't scale propery for large numbers of database keys.
            // We're going to use it here however, as the number of keys in our database should never get large enough
            // for this to matter, and the alternative of maintaining a set of userlists has the potential to be buggy.
            Set<String> rawlists = jedis.keys(keyOfUserlistSetsPrefix + "*");

            Set<String> lists = new HashSet<String>();

            // Strip the prefix from each of the keys to isolate the list name
            int cut = keyOfUserlistSetsPrefix.length();
            for (String rawlist : rawlists) {
                String list = rawlist.substring(cut);
                lists.add(list);
            }

            return lists;
        } finally {
            pool.returnResource(jedis);
        }
    }
}

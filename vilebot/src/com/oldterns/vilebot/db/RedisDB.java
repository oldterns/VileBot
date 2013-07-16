/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.db;

import redis.clients.jedis.JedisPool;

import com.oldterns.vilebot.Vilebot;

/**
 * Interact with Redis database via Jedis. See http://redis.io/commands for info about the command methods.
 */
public abstract class RedisDB
{
    protected static final JedisPool pool = Vilebot.getPool();
}

/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.db.UserlistDB;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class Userlists {
    private static final Pattern enumeratePattern = Pattern.compile("!lists");

    private static final Pattern queryPattern = Pattern.compile("!list (\\S+)");

    private static final Pattern nickBlobPattern = Pattern.compile("(?:(\\S+?)(?:, +| +|$))");

    private static final Pattern addRemovePattern =
            Pattern.compile("!list(add|rem) (\\S+) (" + nickBlobPattern + "+)");

    private Pattern expandPattern = Pattern.compile("(\\S+): (.*)");

    @Handler
    private void listsEnumerate(ReceivePrivmsg event) {
        String text = event.getText();
        Matcher matcher = enumeratePattern.matcher(text);

        if (matcher.matches()) {
            Set<String> lists = UserlistDB.getLists();

            if (lists != null && lists.size() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Available lists: ");
                for (String list : lists) {
                    sb.append(list);
                    sb.append(", ");
                }
                sb.delete(sb.length() - 2, sb.length());
                event.reply(sb.toString());
            } else {
                event.reply("There are no lists.");
            }
        }
    }

    @Handler
    private void listQuery(ReceivePrivmsg event) {
        String text = event.getText();
        Matcher matcher = queryPattern.matcher(text);

        if (matcher.matches()) {
            String listName = matcher.group(1);

            Set<String> users = UserlistDB.getUsersIn(listName);
            if (users != null && users.size() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("The list ");
                sb.append(listName);
                sb.append(" contains: ");

                for (String user : users) {
                    sb.append(user);
                    sb.append(", ");
                }
                sb.delete(sb.length() - 2, sb.length());

                event.replyPrivately(sb.toString());
            } else {
                event.replyPrivately("The list " + listName + " does not exist or is empty.");
            }
        }
    }

    @Handler
    private void listAddRemove(ReceivePrivmsg event) {
        String text = event.getText();
        Matcher matcher = addRemovePattern.matcher(text);

        if (matcher.matches()) {
            String mode = matcher.group(1);
            String listName = matcher.group(2);
            String nickBlob = matcher.group(3);
            if (nickBlob == null) {
                nickBlob = matcher.group(4);
            }

            List<String> nicks = new LinkedList<String>();
            Matcher nickMatcher = nickBlobPattern.matcher(nickBlob);
            while (nickMatcher.find()) {
                nicks.add(nickMatcher.group(1));
            }

            StringBuilder sb = new StringBuilder();

            if ("add".equals(mode)) {
                UserlistDB.addUsersTo(listName, nicks);
                sb.append("Added the following names to list ");
            } else if ("rem".equals(mode)) {
                UserlistDB.removeUsersFrom(listName, nicks);
                sb.append("Removed the following names from list ");
            }

            sb.append(listName);
            sb.append(": ");

            for (String nick : nicks) {
                sb.append(nick);
                sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());

            event.reply(sb.toString());
        }
    }

    @Handler
    private void listExpansion(ReceivePrivmsg event) {
        String sender = event.getSender();
        String text = event.getText();
        Matcher matcher = expandPattern.matcher(text);

        if (matcher.matches()) {
            String listName = matcher.group(1);
            String msg = matcher.group(2);

            Set<String> users = UserlistDB.getUsersIn(listName);
            if (users != null && users.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (String user : users) {
                    if (!user.equals(sender)) {
                        sb.append(user);
                        sb.append(", ");
                    }
                }
                sb.delete(sb.length() - 2, sb.length());
                sb.append(": ");
                sb.append(msg);

                event.reply(sb.toString());
            }
        }
    }
}

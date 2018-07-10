package com.oldterns.vilebot.util;

public class StringUtil {
    
    private StringUtil() {
    }
    
    /**
     * Capitalizes the first letter of the message and returns the result. If
     * the argument is null, then null is returned. If the first letter is
     * capitalized this will return an identical string. If the string is empty
     * then it will return the same empty string back.
     * 
     * @param message The message to convert.
     * 
     * @return The converted message, or null if the argument is null.
     */
    public static final String capitalizeFirstLetter(String message) {
        if (message == null || message.length() == 0) {
            return message;
        }
        
        char firstUpperCaseLetter = Character.toUpperCase(message.charAt(0));
        String remainderOfMessage = message.substring(1);
        return firstUpperCaseLetter + remainderOfMessage;
    }
}

package vilebot.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.oldterns.vilebot.util.StringUtil;

public class StringUtilTest {

    @Test
    public void nullArgReturnsNull() {
        assertEquals(null, StringUtil.capitalizeFirstLetter(null));
    }
    
    @Test
    public void emptyStringReturnsEtmpy() {
        assertEquals("", StringUtil.capitalizeFirstLetter(""));
    }
    
    @Test
    public void convertsToCapitalizationProperly() {
        assertEquals("\0", StringUtil.capitalizeFirstLetter("\0"));
        assertEquals("Heh", StringUtil.capitalizeFirstLetter("heh"));
        assertEquals("123 test", StringUtil.capitalizeFirstLetter("123 test"));
        assertEquals("Some Fancy sentence. HERE", StringUtil.capitalizeFirstLetter("some Fancy sentence. HERE"));
        assertEquals("_", StringUtil.capitalizeFirstLetter("_"));
        assertEquals("YES NO MAYBE", StringUtil.capitalizeFirstLetter("YES NO MAYBE"));
    }
}

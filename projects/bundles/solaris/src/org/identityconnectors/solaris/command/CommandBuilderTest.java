package org.identityconnectors.solaris.command;

import junit.framework.Assert;

import org.junit.Test;

public class CommandBuilderTest {
    @Test
    public void test() {
        String actual = CommandBuilder.build("command", "arg1", "arg2", "arg3");
        String expected = "command arg1 arg2 arg3";
        Assert.assertEquals(expected, actual);
    }
}

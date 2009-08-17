package org.identityconnectors.solaris.constants;

import junit.framework.Assert;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.Test;

public class AttributeHelperTest {
    @Test
    public void test() {
        Attribute argument = AttributeBuilder.build("attrName", "attrValue");
        String cmdSwitch = "-b";
        String result = AttributeHelper.formatCommandSwitch(argument, cmdSwitch);
        Assert.assertEquals("-b \"attrValue\"", result);
        
        Assert.assertEquals("cmd -foo -bar -baz", AttributeHelper.fillInCommand("cmd -foo -bar -baz"));
        Assert.assertEquals("cmd -foo -bar -baz", AttributeHelper.fillInCommand("cmd -foo -bar -baz", "fooToFillIn"));
        Assert.assertEquals("cmd -foo -bar -baz fooToFillIn", AttributeHelper.fillInCommand("cmd -foo -bar -baz __fillHere__", "fooToFillIn"));
        Assert.assertEquals("cmd -foo -bar fooToFillIn -baz secondFillIn", AttributeHelper.fillInCommand("cmd -foo -bar __fillHere1__ -baz __fillHere__", "fooToFillIn", "secondFillIn"));
    }
}

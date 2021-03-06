package kr.entree.spicord;

import kr.entree.spicord.util.Parameter;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by JunHyung Lim on 2019-11-27
 */
public class AhcorasickTest {
    @Test
    public void replace() {
        String contents = "&7(%Test%){%tEst%} %test%%teSt%";
        Parameter parameter = new Parameter().put("%test%", "replaced");
        Assert.assertEquals(
                "&7(replaced){replaced} replacedreplaced",
                parameter.format(contents)
        );
    }
}

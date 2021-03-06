package org.sterl.java._15;

import org.junit.jupiter.api.Test;

/**
 * https://openjdk.java.net/jeps/378
 */
class TextBlocksTest {

    
    @Test
    void test() {
        final String textBlockString = 
            """
            Hello cool
                text block <b>including</b> also HTML "tags".
            Json: {"name":"F1","hp":100
            """;
        
        System.out.println(textBlockString);
    }

}

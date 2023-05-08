package org.stormpx.parser;

public sealed interface Token {

    String getContent();

    record Bracket(String content) implements Token{
        @Override
        public String getContent() {
            return content;
        }
    }

    record Str(String content) implements Token{
        @Override
        public String getContent() {
            return content;
        }
    }

    record Delimiter(int codepoint) implements Token{

        public boolean is(int codepoint){
            return codepoint()==codepoint;
        }
        @Override
        public String getContent() {
            return Character.toString(codepoint);
        }

    }

}

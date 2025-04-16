package org.stormpx.parser;

public sealed interface Token {

    String getContent();

    sealed interface Bracket extends Token{

        String getContentWithBracket();

    }

    record Square(String content) implements Bracket{
        @Override
        public String getContent() {
            return content;
        }

        @Override
        public String getContentWithBracket() {
            return "["+getContent()+"]";
        }
    }

    record Garden(String content) implements Bracket{
        @Override
        public String getContent() {
            return content;
        }
        @Override
        public String getContentWithBracket() {
            return "("+getContent()+")";
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

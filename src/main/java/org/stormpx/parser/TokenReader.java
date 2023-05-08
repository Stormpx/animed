package org.stormpx.parser;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class TokenReader {

    private List<Token> tokens;

    private Stack<TokenReader> readerStack;

    private Iterator<Token> iter;

    private Token current;

    private boolean hold;

    public TokenReader(List<Token> tokens) {
        this.tokens = tokens;
        this.iter = tokens.iterator();
    }

    public List<Token> tokens() {
        return tokens;
    }

    public void pushReader(TokenReader reader){
        if (this.readerStack==null){
            this.readerStack=new Stack<>();
        }
        if (reader.hasNext()){
            this.readerStack.push(reader);
        }
    }

    public Token current(){
        return current;
    }

    public void hold(){
        this.hold =true;
    }

    public boolean hasNext(){
        if (hold &&current!=null){
            return true;
        }
        if (this.readerStack!=null){
            while (!this.readerStack.isEmpty()&&!this.readerStack.peek().hasNext()){
                this.readerStack.pop();
            }
            return !this.readerStack.isEmpty();
        }
        return iter.hasNext();
    }

    public Token nextToken(){
        if (hold &&current!=null){
            hold =false;
            return current;
        }
        if (this.readerStack!=null){
            if (!this.readerStack.isEmpty()){
                TokenReader reader = this.readerStack.peek();
                if (reader.hasNext()){
                    Token nextToken = reader.nextToken();
                    this.current=nextToken;
                    return nextToken;
                }else{
                    this.readerStack.pop();
                    return nextToken();
                }
            }
        }
        if (!iter.hasNext()){
            return null;
        }
        this.current= iter.next();
        return current;
    }



}

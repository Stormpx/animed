package org.stormpx.animed

interface Matcher {

    fun match(title:String): MatchResult;

}
package org.stormpx.arimedown

interface Matcher {

    fun match(title:String): MatchResult;

}
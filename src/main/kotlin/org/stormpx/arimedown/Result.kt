package org.stormpx.arimedown


data class MatchResult(val match:Boolean,val chapter:Double?){

    fun chapter():Double{
        return chapter?: -1.0
    }

}


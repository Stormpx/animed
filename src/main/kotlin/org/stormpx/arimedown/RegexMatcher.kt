package org.stormpx.arimedown



class RegexMatcher(pattern:String) {
    companion object {
        val preProcessRegex: Regex= Regex.escapeReplacement("#chapter#").toRegex()
        val chapterRegex: Regex= "\\d+(\\.\\d*)?".toRegex()
    }

    private var index: Int=-1;
    private var regex: Regex;

    init{
        val regex= preProcessRegex.replace(pattern) {
            if (this.index == -1) {
                this.index = it.range.start
                "\\E\\d+(.\\d*)?\\Q"
            } else {
                ""
            }
        }

        this.regex = ("\\Q$regex\\E").toRegex()

        if (index==-1){
            throw RuntimeException("#chapter# is required.");
        }
    }

    fun match(title: String): MatchResult {
        if (!regex.matches(title)){
            return MatchResult(false,-1.0)
        }
        val chapter = chapterRegex.find(title,this.index)?.value?.trim()?.toDouble()

        return MatchResult(chapter!=null,chapter)
    }

}
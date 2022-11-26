package org.stormpx.animed



class ChapterMatcher(pattern:String): Matcher {
    companion object {
        private val monoRegex: Regex= Regex.escapeReplacement("#mono#").toRegex()
        private val preProcessRegex: Regex= Regex.escapeReplacement("#chapter#").toRegex()
        private val chapterRegex: Regex= "\\d+(\\.\\d*)?".toRegex()
    }

    private var chapterIndex: Int=-1;
    private var regex: Regex;

    init{
        var p=pattern
        p= monoRegex.replace(p){
            "\\E.*\\Q"
        }

        p= preProcessRegex.replace(p) {
            if (this.chapterIndex == -1) {
                this.chapterIndex = it.range.first
//                "\\E\\d+(.\\d*)?\\Q"
                "\\E(?<ep>\\d+(.\\d*)?)\\Q"
            } else {
                ""
            }
        }

        this.regex = ("\\Q$p\\E").toRegex()

        if (chapterIndex==-1){
            throw RuntimeException("#chapter# is required.");
        }
    }

    override fun match(title: String): MatchResult {
        if (!regex.matches(title)){
            return MatchResult(false,-1.0)
        }
//        val chapter = chapterRegex.find(title,this.chapterIndex)?.value?.trim()?.toDouble()
        val chapter = regex.find(title)?.groups?.get("ep")?.value?.trim()?.toDouble()

        return MatchResult(chapter!=null,chapter)
    }

}
package org.stormpx.animed

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stormpx.parser.AnimeTitle
import org.stormpx.parser.TitleParser
import java.util.Objects

class TitleMatcher(baseTitle:String) : Matcher {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(TitleMatcher.javaClass)
    }
    private val parser:TitleParser = TitleParser()
    private val expectedTitle:AnimeTitle= parser.parse(baseTitle)

    init {
        logger.info("parse base: '{}'. result: {}",baseTitle,expectedTitle);
    }

    private fun isSimilarExpect(title:AnimeTitle): Boolean{

        return Objects.equals(expectedTitle.subGroup,title.subGroup)&&Objects.equals(expectedTitle.animeTitle,title.animeTitle)
                &&Objects.equals(expectedTitle.videoResolution,title.videoResolution)&&Objects.equals(expectedTitle.language,title.language)
    }

    override fun match(title: String): MatchResult {
        val current = parser.parse(title)
        if (current.episode==null){
            return MatchResult(false,-1.0);
        }

        if (!isSimilarExpect(current)){
            return MatchResult(false,-1.0);
        }

        return MatchResult(true,current.episode)
    }
}
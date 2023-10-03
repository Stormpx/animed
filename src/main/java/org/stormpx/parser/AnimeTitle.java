package org.stormpx.parser;

import java.util.List;

public record AnimeTitle(String subGroup,
                         List<String> animeTitle,
                         String videoResolution,
                         String videoSourceName,
                         String videoSourceType,
                         Double episode,
                         Integer subVersion,
                         String language,
                         List<String> junk
) {


}

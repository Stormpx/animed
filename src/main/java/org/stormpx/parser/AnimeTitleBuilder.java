package org.stormpx.parser;

import java.util.ArrayList;
import java.util.List;

public class AnimeTitleBuilder {

    private String subGroup;
    private List<String> animeTitles=new ArrayList<>();
    private String videoResolution;
    private String videoSourceName;
    private String videoSourceType;
    private Double episode;
    //subgroup version
    private Integer subVersion;
    private String language;
    private List<String> junk=new ArrayList<>();

    public static AnimeTitleBuilder builder(){
        return new AnimeTitleBuilder();
    }

    public AnimeTitle build(){
        return new AnimeTitle(subGroup, animeTitles,videoResolution,videoSourceName,videoSourceType,episode,subVersion,language,junk);
    }

    public int animeTitleSize(){
        return this.animeTitles==null?0:this.animeTitles.size();
    }

    public AnimeTitleBuilder subGroup(String subGroup) {
        this.subGroup = subGroup;
        return this;
    }

    public AnimeTitleBuilder animeTitle(List<String> animeTitles) {
        this.animeTitles = animeTitles;
        return this;
    }

    public AnimeTitleBuilder addAnimeTitle(String animeTitle) {
        this.animeTitles.add(animeTitle);
        return this;
    }

    public AnimeTitleBuilder videoResolution(String videoResolution) {
        this.videoResolution = videoResolution;
        return this;
    }

    public AnimeTitleBuilder videoSourceName(String videoSourceName) {
        this.videoSourceName = videoSourceName;
        return this;
    }

    public AnimeTitleBuilder videoSourceType(String videoSourceType) {
        this.videoSourceType = videoSourceType;
        return this;
    }

    public Double getEpisode(){
        return episode;
    }
    public boolean isEpisodeExists(){
        return episode!=null;
    }

    public AnimeTitleBuilder episode(Double episode) {
        this.episode = episode;
        return this;
    }

    public Integer subVersion() {
        return subVersion;
    }

    public AnimeTitleBuilder subVersion(Integer subVersion) {
        this.subVersion = subVersion;
        return this;
    }

    public AnimeTitleBuilder language(String language) {
        this.language = language;
        return this;
    }

    public AnimeTitleBuilder junk(List<String> junk) {
        this.junk = junk;
        return this;
    }

    public AnimeTitleBuilder addJunk(String junk) {
        this.junk.add(junk);
        return this;
    }

}

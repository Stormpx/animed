package org.stormpx.parser;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字幕组 动画标题 集数 视频源 视频源类型 清晰度 编码方式 语言
 */
public class TitleParser {
    private final static Set<String> SUB_GROUPS= Set.of(
            "NC-Raws", "Lilith-Raws", "LoliHouse", "豌豆字幕组", "风之圣殿字幕组", "幻樱字幕组", "SweetSub", "喵萌奶茶屋", "ANi", "漫猫字幕社",
            "猎户手抄部", "织梦字幕组", "漫游字幕组", "成子坂地下室", "豌豆字幕組", "風之聖殿字幕組", "Skymoon-Raws", "动漫国字幕组", "天月搬運組",
            "云光字幕组", "jibaketa合成", "音頻壓制", "愛戀字幕社", "爱恋字幕社", "哆啦字幕组", "桜都字幕组", "桜都字幕組", "c.c動漫",
            "喵萌Production", "動漫國字幕組", "Billion Meta Lab", "云歌字幕组", "悠哈璃羽字幕社", "猎户不鸽压制", "冷番补完字幕组", "北宇治字幕组", "7³ACG",
            "北宇治Anarchism字幕组", "夜莺家族", "霜庭云花Sub", "氢气烤肉架", "GM-Team", "丸子家族", "轻之国度字幕组", "極影字幕社", "梦蓝字幕组",
            "漫貓字幕社", "五一快乐", "星空字幕組", "星空字幕组", "离谱Sub", "GalaxyRailroad-888", "MingY", "极影字幕社", "NGA",
            "今晚月色真美", "MCE汉化组", "Amor字幕组", "白戀動漫蘿蔔部", "白恋动漫萝卜部", "幻櫻字幕組", "千夏字幕组", "千夏字幕組", "DAY字幕组",
            "枫叶字幕组", "楓葉字幕組", "鈴風字幕組", "官方油管搬运", "貓戀漢化組", "猫恋汉化组", "雪飘工作室", "雪飄工作室", "XK SPIRITS",
            "MingYSub", "YYQ字幕组", "六道我大鴿漢化組","DBD-Raws"
    );

    private final static List<Pattern> SUB_GROUPS_KW=List.of(
            Pattern.compile("汉化[组組]"),
            Pattern.compile("字幕[组組]"),
            Pattern.compile("字幕社"),
            Pattern.compile("搬运[组組]"),
            Pattern.compile("搬运"),
            Pattern.compile("-[rR]aws")
    );

    private final static List<String> VIDEO_SOURCE_NAME=List.of(
            "BiliBili","BAHA","CR","CRUNCHYROLL","SENTAI","netfilx","hidive",
            "B-Global","ViuTV","AMZN"
    );

    private final static List<String> VIDEO_SOURCE_TYPE =List.of(
                    "BD", "BDRIP", "BURAY", "BU-RAY",
                    "DVD", "DVD5", "DVD9", "DVD-R2J", "DVDRIP", "DVD-RIP",
                    "R2DVD", "R2J", "R2JDVD", "R2JDVDRIP",
                    "HDTV", "HDTVRIP", "TVRIP", "TV-RIP",
                    "WEBCAST","WEB-RIP", "WEBRIP","WEB-DL","WEBDL","HD","WEB"
    );

    private final static List<String> VIDEO_TERM=List.of(
                  // Frame rate
                  "23.976FPS", "24FPS", "29.97FPS", "30FPS", "60FPS", "120FPS",
                  // Video codec
                  "8BIT", "8-BIT", "10BIT", "10BITS", "10-BIT", "10-BITS",
                  "HI10", "HI10P", "HI444", "HI444P", "HI444PP",
                  "H264", "H265", "H.264", "H.265", "X264", "X265", "X.264",
                  "AVC", "HEVC", "HEVC2", "DIVX", "DIVX5", "DIVX6", "XVID",
                  "AV1",
                  // Video format
                  "AVI", "RMVB", "WMV", "WMV3", "WMV9",
                  // Video quaity
                  "HQ", "LQ",
                  // Video resolution
                  "HD", "SD"
    );

    private final static List<String> AUDIO_TERM=List.of(
                  // Audio channels
                  "2.0CH", "2CH", "5.1", "5.1CH", "DTS", "DTS-ES", "DTS5.1",
                  "TRUEHD5.1",
                  // Audio codec
                  "AAC", "AACX2", "AACX3", "AACX4", "AC3", "EAC3", "E-AC-3",
                  "FLAC", "FLACX2", "FLACX3", "FLACX4", "LOSSLESS", "MP3", "OGG",
                  "VORBIS",
                  // Audio anguage
                  "DUALAUDIO", "DUAL AUDIO"
    );

    private final static List<String> FILE_EXTENSION=List.of(
                  "3GP", "AVI", "DIVX", "FV", "M2TS", "MKV", "MOV", "MP4", "MPG",
                  "OGM", "RM", "RMVB", "TS", "WEBM", "WMV"
    );

    private final static List<String> LANG_PART=List.of(
            "ENG", "ENGLISH", "ESPANOL", "JAP", "PT-BR", "SPANISH", "VOSTFR"
    );

    private final static List<String> LANG_KW=List.of(
            "简","繁","CHT","CHS","GB","BIG5","JP","CAN"
    );


    private final static List<String> END_KW=List.of(
            "END","FINAL","完","FIN"
    );

    //-----------------------------------------------------------episode-----------------------------------------------------------------------
    //'ep','ep.5'
    private final static Pattern NORMAL_EPISODE = Pattern.compile("\\d+(\\.\\d)?");
    //'01v2'
    private final static Pattern EPISODE_SUBVERSION = Pattern.compile("(?<ep>\\d+(\\.\\d)?)v(?<sver>\\d+)");
    //'s01e2'
    private final static Pattern SEASON_EPISODE = Pattern.compile("s(?<ss>\\d+)e(?<ep>\\d+(\\.\\d)?)");
    //ep-ep
    private final static Pattern RANGE_EPISODE = Pattern.compile("\\d+-\\d+");
    //'第ep话','第ep集'
    private final static Pattern DESC_EPISODE = Pattern.compile("第\\d+(\\.\\d+)?[话話集]");

    //----------------------------------------------------------------------------------------------------------------------------------

    //-----------------------------------------------------------resolution-----------------------------------------------------------------------
    //1080p
    private final static Pattern RESOLUTION_PATTERN =Pattern.compile("(\\d+)[pP]");
    //1920x1080
    private final static Pattern FULLSIZE_PATTERN = Pattern.compile("(\\d+)[xX](\\d+)");
    //----------------------------------------------------------------------------------------------------------------------------------

    //0:字幕组 1:动画标题 2:集数 3:other
    private int state;

    private int cautiousState;

    private AnimeTitleBuilder builder;

    private String preProcess(String title){

        return title.replaceAll("【","[")
                .replaceAll("】","]")
                .replaceAll("（","[")
                .replaceAll("）","]")
                .replaceAll("\\(","[")
                .replaceAll("\\)","]")
                .replaceAll("★","")
                .replaceAll("@","")
                .replaceAll("_"," ")
                .replaceAll("\\+"," ")
                .trim()
                ;

    }

    private final static Pattern NUMERIC_PATTERN = Pattern.compile("\\d+\\.\\d+");
    private String preProcessMetaInfo(String metaContent){
        var r = preProcess(metaContent);

        if (NUMERIC_PATTERN.matcher(r).find()){
            return r;
        }

        return r.replaceAll("\\."," ");
    }

    private boolean isJoiner(int codepoint){
        return codepoint=='/'||codepoint=='-'||codepoint=='&';
    }

    private void discardBuffer(List<Token> tokens, StringBuilder sb, Function<String,Token> tokenFunction){
        if (!sb.isEmpty()){
            if (sb.length()==1){
                if (isJoiner(sb.codePointAt(0))){
                    tokens.add(new Token.Delimiter(sb.codePointAt(0)));
                    sb.setLength(0);
                    return;
                }
            }
            tokens.add(tokenFunction.apply(sb.toString()));
            sb.setLength(0);
        }
    }

    private List<Token> splitToken(String title){
        //0: waiting_token 1:waiting_] 2:waiting_blank
        StringBuilder sb=new StringBuilder();
        List<Token> tokens =new ArrayList<>();
        int length = (int) title.codePoints().count();
        for (int idx = 0; idx < length; idx++) {
            int codepoint = title.codePointAt(idx);
            if (codepoint=='['){
                discardBuffer(tokens,sb,Token.Str::new);
                int lv = 0;
                //read until ']'
                for(idx=idx+1;idx<length;idx++){
                    int nextCodepoint =title.codePointAt(idx);
                    if (nextCodepoint=='['){
                        lv++;
                    }
                    if (nextCodepoint==']'){
                        if (lv==0)
                            break;
                        lv-=1;
                    }
                    sb.appendCodePoint(nextCodepoint);
                }
                discardBuffer(tokens,sb,Token.Bracket::new);

                continue;
            }
            if (codepoint==' '){
                discardBuffer(tokens,sb,Token.Str::new);
//                if (isJoiner(codepoint)){
//                    tokens.add(new Token.Joiner(codepoint));
//                }
                continue;
            }
            sb.appendCodePoint(codepoint);

        }
        discardBuffer(tokens,sb,Token.Str::new);
        return tokens;
    }
    private final static Pattern SESSION_PATTERN = Pattern.compile("(\\d+|[一四七十])月新番");
    private boolean isReleaseSeason(String content){
        return SESSION_PATTERN.matcher(content).matches();
    }


    private String concatToken(TokenReader tokens,boolean includeCurrent){
        StringBuilder sb=new StringBuilder();
        if (includeCurrent&&tokens.current()!=null){
            sb.append(tokens.current().getContent());
        }
        while (tokens.hasNext()){
            var next = tokens.nextToken();
            if (next instanceof Token.Bracket){
                break;
            }
            if (next instanceof Token.Delimiter){
                break;
            }
            String content = next.getContent();
            sb.append(content);
        }

        return sb.toString();
    }


    private boolean isResolution(String content){
        Matcher xxpMatcher = RESOLUTION_PATTERN.matcher(content);
        if(xxpMatcher.matches()){
            return true;
        }
        Matcher hwMatcher = FULLSIZE_PATTERN.matcher(content);
        if (hwMatcher.matches()){
            return true;
        }
        if ("4k".equalsIgnoreCase(content)|| "2k".equalsIgnoreCase(content)){
            return true;
        }

        return false;
    }

    private boolean isSubGroup(String content){
        return SUB_GROUPS.stream().anyMatch(it->it.equalsIgnoreCase(content))
                ||SUB_GROUPS_KW.stream().map(pattern -> pattern.matcher(content)).anyMatch(Matcher::find);
    }

    private boolean isVideoSourceName(String content){
        return VIDEO_SOURCE_NAME.stream().anyMatch(it->it.equalsIgnoreCase(content));
    }

    private boolean isVideoSourceType(String content){
        return VIDEO_SOURCE_TYPE.stream().anyMatch(it->it.equalsIgnoreCase(content));
    }

    private boolean isJunk(String content){
        return isReleaseSeason(content)
                ||END_KW.stream().anyMatch(it->it.equalsIgnoreCase(content))
                ||VIDEO_TERM.stream().anyMatch(it->it.equalsIgnoreCase(content))
                ||AUDIO_TERM.stream().anyMatch(it->it.equalsIgnoreCase(content))
                ||FILE_EXTENSION.stream().anyMatch(it->it.equalsIgnoreCase(content))
                ;
    }

    private boolean isLangPart(String content){
        return LANG_PART.stream().anyMatch(it-> it.equalsIgnoreCase(content))
                ||LANG_KW.stream().anyMatch(it-> content.toLowerCase().contains(it.toLowerCase()));
    }



    private Double tryParseEpisode(String content){
        content = content.toLowerCase(Locale.ROOT);
        for (String kw : END_KW) {
            content=content.replaceAll(kw.toLowerCase(Locale.ROOT),"");
        }
        content=content.replaceAll("ova","");

        if (NORMAL_EPISODE.matcher(content).matches()){
            try {
                return Double.valueOf(content);
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        if (RANGE_EPISODE.matcher(content).matches()){
            String[] split = content.split("-");
            try {
                return Double.valueOf(split[0]);
            } catch (NumberFormatException e) {
                //ignore
            }
        }

        if (DESC_EPISODE.matcher(content).matches()){
            return Double.valueOf(content.substring(1,content.length()-1));
        }

        Matcher matcher = EPISODE_SUBVERSION.matcher(content.replaceAll(" ",""));
        if (matcher.matches()){
            String ep = matcher.group("ep");
            String sver = matcher.group("sver");
            try {
                var episode = Double.valueOf(ep);
                var subVersion =Integer.valueOf(sver);
                builder.subVersion(subVersion);
                return episode;
            }catch (NumberFormatException e){

            }
        }
        var season_episode = SEASON_EPISODE.matcher(content.replaceAll(" ",""));
        if (season_episode.matches()){
            String ep = matcher.group("ep");
            try {
                return Double.valueOf(ep);
            }catch (NumberFormatException e){
            }
        }



        if (content.contains("ep"))
            return tryParseEpisode(content.replaceAll("ep",""));

        return null;
    }

    private List<Token> collectMetaInfo(List<Token> tokens,boolean parseEpisode){
        ListIterator<Token> iterator = tokens.listIterator();
        while (iterator.hasNext()){
            Token token = iterator.next();
            String content = token.getContent();
            if (isResolution(content)){
                builder.videoResolution(content);
                iterator.remove();
            }else if (isVideoSourceName(content)){
                builder.videoSourceName(content);
                iterator.remove();
            }else if (isVideoSourceType(content)){
                builder.videoSourceType(content);
                iterator.remove();
            }else if (isJunk(content)){
                builder.addJunk(content);
                iterator.remove();
            }else if (isLangPart(content)){
                iterator.remove();
                List<Token> subToken = splitToken(preProcess(content));
                if (subToken.size()==1){
                    builder.language(content);
                }else{
                    List<Token> remainTokens = collectMetaInfo(subToken, false);
                    if (token instanceof Token.Bracket){
                        remainTokens.forEach(iterator::add);
                    }
                }

            }
        }
        if (parseEpisode){
            boolean findEpisode=false;
            //从后到前找到第一个集数
            while (iterator.hasPrevious()){
                Token previousToken = iterator.previous();
                Double episode = tryParseEpisode(previousToken.getContent());
                if (episode!=null){
                    iterator.remove();
                    builder.episode(episode);
                    findEpisode=true;
                    break;
                }
            }
            //如果找到集数，则将集数后面的内容视为元信息
            while (findEpisode&& iterator.hasNext()){
                Token token = iterator.next();
                if (token instanceof Token.Bracket ){
                    for (Token subToken : collectMetaInfo(splitToken(preProcessMetaInfo(token.getContent())), false)) {
                        builder.addJunk(subToken.getContent());
                    }
                }else{
                    builder.addJunk(token.getContent());
                }
                iterator.remove();
            }
        }

        return tokens;
    }

    private void parse(TokenReader tokens){
        Token token;
        while ((token=tokens.nextToken())!=null) {
            switch (state){
                //字幕组
                case 0->{
                    var content = token.getContent();
                    builder.subGroup(content);
                    state=1;
                    //如果匹配字幕组的特征
                    if (isSubGroup(content)){
                        cautiousState=1;
                    }
                }
                //动画标题
                case 1->{
                    //如果是中括号包裹的token
                    if (token instanceof Token.Bracket bracket){
                        //怀疑前一个token不是字幕组,尝试检查当前token
                        if (cautiousState==0){
                            cautiousState=1;
                            if (isSubGroup(bracket.content())){
                                builder.subGroup(bracket.content());
                                continue;
                            }
                        }
                        //将中括号拆成一个个token
                        List<Token> subTokens = splitToken(bracket.content());
                        TokenReader subReader = new TokenReader(subTokens);

                        while (subReader.hasNext()){
                            //将空格分隔的内容拼接在一起，直到遇到分隔符
                            String title = concatToken(subReader, false);
                            if (!title.isBlank()){
                                builder.addAnimeTitle(title);
                            }
                            //将嵌套的中括号视为动画名称
                            if (subReader.current() instanceof Token.Bracket nestedBracket){
                                List<Token> tokenList = splitToken(nestedBracket.getContent());
                                subReader.pushReader(new TokenReader(tokenList));
                            }else if (subReader.current() instanceof Token.Delimiter delimiter){
                                if (delimiter.is('-')) {
                                    tokens.pushReader(subReader);
                                    state=2;
                                    break;
                                }else if (delimiter.is('/')&&subReader.hasNext()){
                                    cautiousState=2;
                                }
                            }
                        }

                    }else{
                        if (token instanceof Token.Delimiter){
                            continue;
                        }
                        //将空格分隔的内容拼接在一起，直到遇到分隔符或下一个中括号
                        var result = concatToken(tokens,true);
                        if (!result.isBlank()){
                            builder.addAnimeTitle(result);
                        }
                        var leaseToken = tokens.current();
                        if (leaseToken instanceof Token.Bracket){
                            tokens.hold();
                            state=2;
                        }else if (leaseToken instanceof Token.Delimiter delimiter ){
                            if(delimiter.is('-')){
                                state=2;
                            }else if (delimiter.is('/')){
                                continue;
                            }
                        }
                    }
                    if (cautiousState==2){
                        state=2;
                    }
                    //已经收集到至少一个动画名称
                    //最多再收集一次(因为很多标题都是中文名+罗马音)就尝试收集集数
                    cautiousState=2;
                }
                //集数
                case 2->{
                    if (token instanceof Token.Delimiter){
                        continue;
                    }
                    Double target = tryParseEpisode(token.getContent());
                    if (target!=null){
                        if (!builder.isEpisodeExists()||builder.getEpisode().compareTo(target)>=0){
                            builder.episode(target);
                        }
                        state=3;
                        cautiousState=3;
                        continue;
                    }
                    if (token instanceof Token.Bracket bracket){
                        List<Token> subTokens = splitToken(preProcess(bracket.content()));
                        List<Token> filteredTokens = collectMetaInfo(new ArrayList<>(subTokens), false);
                        boolean includeMetaInfo=subTokens.size() != filteredTokens.size();
                        //token太多并且过滤前后的数量一样，则认为是动画名称
                        if (subTokens.size()>2&&!includeMetaInfo){
                            tokens.hold();
                            state=1;
                            continue;
                        }
                        //<=2 || len1!=len2
                        if (subTokens.size()==1){
                            Token tmpToken = subTokens.get(0);
                            if (tmpToken.getContent().length()>5){
                                tokens.hold();
                                state=1;
                                continue;
                            }
                        }
                        //如果长度与过滤后的不一致，则不太可能是动画名称
                        Double episode = null;
                        for (Iterator<Token> iterator = filteredTokens.iterator(); iterator.hasNext(); ) {
                            Token subToken = iterator.next();
                            var temp = tryParseEpisode(subToken.getContent());
                            if (temp != null) {
                                episode = temp;
                                iterator.remove();
                            }
                        }
                        if (episode==null){
                            //如果找不到集数并且不包含元信息
                            if (!includeMetaInfo){
                                cautiousState=2;
                                tokens.hold();
                                state=1;
                            }
                            continue;
                        }

                        for (Token junk : filteredTokens) {
                            builder.addJunk(junk.getContent());
                        }

                        builder.episode(episode);
                        state=3;

                    }else{
                        if (cautiousState==3&&builder.isEpisodeExists()){
                            state=3;
                        }else{
                            tokens.hold();
                            state=1;
                        }
                    }
                }
                default -> {
                    if (token instanceof Token.Bracket bracket){
                        var subTokens = new TokenReader(collectMetaInfo(splitToken(preProcessMetaInfo(bracket.content())),false));
                        tokens.pushReader(subTokens);
                    }else{
                        //workaround
                        if (token instanceof Token.Delimiter delimiter){
                            if (delimiter.is('/')){
                                state=1;
                                continue;
                            }
                        }
                        builder.addJunk(token.getContent());

                    }

                }
            }
        }
    }

    public AnimeTitle parse(String title){
        this.state=0;
        this.cautiousState=0;
        this.builder=new AnimeTitleBuilder();
        title=preProcess(title);
        List<Token> list = collectMetaInfo(splitToken(title),true);
//        System.out.println(list);
        var tokens = new TokenReader(list);
        parse(tokens);
        return builder.build();
    }


    public static void main(String[] args) throws IOException, XMLStreamException {
//        new RSSReader(URI.create("https://dmhy.org/topics/rss/rss.xml").toURL().openStream())
//                .read()
//                .getItems()
//                .stream().filter(it-> "動畫".equalsIgnoreCase(it.getCategory()))
//                .forEach(item->{
//                    System.out.println("\""+item.getTitle()+"\",");
//                });
//        for (String title : titles) {
//            new TitleParser().parse(title);
//        }

//        System.out.println("(".replaceAll("\\(",""));

//        System.out.println("10月新番".matches("(\\d+|[一四七十])月新番"));

//        titles.stream().skip(80+50+50+50+50).limit(50).forEach(it->{
//            System.out.println(it);
//            System.out.println(new TitleParser().parse(it));
//            System.out.println();
//        });
        System.out.println(new TitleParser().parse("[北宇治字幕组] 葬送的芙莉莲 / Sousou no Frieren [01v2][WebRip][1080p][HEVC_AAC][简繁日内封]"));
//        new TitleParser().views();

    }

    private final static List<String> titles = List.of(
            "[NC-Raws] 秋叶原女仆战争 / Akiba Maid Sensou - 12 (B-Global 1920x1080 HEVC AAC MKV)",
            "[Lilith-Raws] 殺手奶爸 / Buddy Daddies - 12 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[LoliHouse] 杀手奶爸 / Buddy Daddies - 09 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "[LoliHouse] 文豪野犬 第四季 / Bungou Stray Dogs Season 4 - 13(50) [WebRip 1080p HEVC-10bit AAC][简繁内封字幕][END]",
            "【豌豆字幕组&风之圣殿字幕组】★10月新番[电锯人 / 链锯人 / Chainsaw_Man][12][完][简体][1080P][MP4]",
            "【幻樱字幕组】【10月新番】【电锯人 Chainsaw Man】【11】【BIG5_MP4】【1920X1080】",
            "【豌豆字幕组&风之圣殿字幕组】★10月新番[电锯人 / 链锯人 / Chainsaw_Man][10][简体][1080P][MP4]",
            "【豌豆字幕组&风之圣殿字幕组】★10月新番[电锯人 / 链锯人 / Chainsaw_Man][09][简体][1080P][MP4]",
            "【幻樱字幕组】【10月新番】【电锯人 Chainsaw Man】【08】【BIG5_MP4】【1920X1080】",
            "【幻樱字幕组】【10月新番】【电锯人 Chainsaw Man】【02】【GB_MP4】【1920X1080】",
            "[Lilith-Raws] 契約之吻 / Engage Kiss - 13 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[SweetSub&LoliHouse] 天国大魔境 / Tengoku Daimakyou - 05 [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]",
            "[Lilith-Raws] 我家的英雄 / My Home Hero - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] 我家的英雄 / My Home Hero - 01 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] 惑星公主蜥蜴騎士 / Hoshi no Samidare - 24 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "【喵萌奶茶屋】★10月新番★[异世界舅舅/Isekai Ojisan][13][1080p][简日双语][招募翻译]",
            "【喵萌奶茶屋】★10月新番★[异世界舅舅/Isekai Ojisan][10][1080p][简日双语][招募翻译]",
            "【恭喜复播第二集】【喵萌奶茶屋】★10月新番★[异世界舅舅/Isekai Ojisan][09][1080p][简日双语][招募翻译]",
            "【恭喜复播】【喵萌奶茶屋】★10月新番★[异世界舅舅/Isekai Ojisan][08][1080p][简日双语][招募翻译] [576.3MB]",
            "【喵萌奶茶屋】★07月新番★[异世界舅舅/Isekai Ojisan][07][1080p][简日双语][招募翻译]",
            "[Lilith-Raws] 我想成為影之強者！ / Kage no Jitsuryokusha ni Naritakute! - 20 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[ANi] InSpectre S2 - 虚构推理 第二季 - 12 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[Lilith-Raws] 虚构推理 / Kyokou Suiri S02 - 01 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] Lycoris Recoil - 06 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[NC-Raws] Lycoris Recoil - 05 (B-Global 3840x2160 HEVC AAC MKV)",
            "[漫猫字幕社][10月新番][灵能百分百][Mob Psycho 100 III][12Fin][1080p][MP4][GB][简中]",
            "[漫猫字幕社][10月新番][灵能百分百][Mob Psycho 100 III][07][1080p][MP4][GB][简中]",
            "[LoliHouse] 异世界悠闲农家 / Isekai Nonbiri Nouka - 12 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕][END]",
            "[LoliHouse] 异世界悠闲农家 / Isekai Nonbiri Nouka - 11 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "[猎户手抄部] 异世界悠闲农家 Isekai Nonbiri Nouka [05] [1080p] [简中内嵌] [2023年1月番]",
            "[Lilith-Raws] 【我推的孩子】 / Oshi no Ko - 04 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[LoliHouse] 大雪海的卡纳 / Ooyukiumi no Kaina - 11 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕][END]",
            "[LoliHouse] 大雪海的卡纳 / Ooyukiumi no Kaina - 02 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "【喵萌奶茶屋】★01月新番★[万事屋斋藤，到异世界 / Benriya Saitou-san, Isekai ni Iku][02][1080p][简日双语][招募翻译]",
            "【喵萌奶茶屋】★01月新番★[万事屋斋藤，到异世界 / Benriya Saitou-san, Isekai ni Iku][01][1080p][简日双语][招募翻译]",
            "[NC-Raws] 忍者一时 / Shinobi no Ittoki - 12 (CR 1920x1080 AVC AAC MKV)",
            "[LoliHouse] 忍之一时 / 忍之一刻 / Shinobi no Ittoki - 03 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "[Lilith-Raws] 間諜教室 / Spy Kyoushitsu - 12 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[织梦字幕组]Summer Time Rendering 夏日重现[25][完结][2022.09.30][1080P][简日双语][AVC]",
            "【喵萌奶茶屋】★04月新番★[夏日重现/Summer Time Rendering][18][1080p][简日双语][招募翻译片源]",
            "[Lilith-Raws] Trigun Stampede - 12 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[漫游字幕组] Mobile Suit Gundam The Witch from Mercury 机动战士高达 水星的魔女 第16话 Webrip 1080p MKV 简繁外挂",
            "[喵萌奶茶屋&LoliHouse] 机动战士高达 水星的魔女 / Mobile Suit Gundam THE WITCH FROM MERCURY - 15 [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]",
            "【喵萌奶茶屋】★10月新番★[机动战士高达 水星的魔女/Mobile Suit Gundam THE WITCH FROM MERCURY][12][1080p][简日双语][招募翻译]",
            "[Lilith-Raws] 機動戰士鋼彈 水星的魔女 / Mobile Suit Gundam：The Witch from Mercury - 09 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "【喵萌奶茶屋】★10月新番★[机动战士高达 水星的魔女/Mobile Suit Gundam THE WITCH FROM MERCURY][08][1080p][简日双语][招募翻译]",
            "[成子坂地下室]爱丽丝机甲 Expansion(机战少女Alice) /Alice Gear Aegis Expansion - 05 [简中内嵌][1080p][AVC AAC][MP4][附ASS]",
            "[豌豆字幕组&风之圣殿字幕组&LoliHouse] 新石纪 第三季 / 石纪元 第三季 / Dr.STONE S3 - 05 [WebRip 1080p HEVC-10bit AAC][简繁外挂字幕]",
            "[Lilith-Raws] 國王排名 勇氣的寶箱 / Ousama Ranking - Yuuki no Takarabako - 04 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] 勇者死了！ / Yuusha ga Shinda! - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] Opus.COLORs 色彩高校星 / Opus.COLORs - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "【喵萌奶茶屋】★04月新番★[百合是我的工作！/我的百合乃工作是也！/私の百合はお仕事です！/Watashi no Yuri wa Oshigoto desu!][05][1080p][简日双语][招募翻译校对]",
            "[ANi] Ōsama Ranking Yūki no Takarabako -  國王排名 勇氣的寶箱 - 04 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[ANi] Yūsha ga Shinda -  勇者死了！ - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "【豌豆字幕组&风之圣殿字幕组】★04月新番[Dr.STONE 新石纪 / 石纪元 第三季][05][简体][1080P][MP4]",
            "[Skymoon-Raws] 魔法使的新娘 第二季 / Mahoutsukai no Yome S02 - 05 [ViuTV][WEB-DL][1080p][AVC AAC][繁體外掛][MP4+ASS](正式版本)",
            "[Skymoon-Raws] 魔法使的新娘 第二季 / Mahoutsukai no Yome S02 - 05 [ViuTV][WEB-RIP][1080p][AVC AAC][CHT][SRT][MKV](先行版本)",
            "[动漫国字幕组&LoliHouse] 蓝色管弦乐 / Blue Orchestra - 04 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "[天月搬運組] Dr.STONE 新石紀 第三季 / Dr. Stone S03 - 05 [1080P][簡繁日外掛]",
            "[云光字幕组] 总之就是非常可爱 S2 Tonikaku Kawaii S2 [04][简体双语][1080p]招募翻译.mp4",
            "[ANi] OpusCOLORs -  Opus.COLORs 色彩高校星 - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[jibaketa合成&音頻壓制][TVB粵語]閃躍吧！星夢☆頻道 / 美妙☆频道 / Kiratto Pri-chan - 03 [粵日雙語+內封繁體中文字幕][WEB 1920x1080 x264 AACx2 SRT TVB CHT]",
            "[愛戀字幕社][4月新番][放學後失眠的你][Kimi wa Houkago Insomnia][04][1080P][MP4][繁日雙語]",
            "[爱恋字幕社][4月新番][放学后失眠的你][Kimi wa Houkago Insomnia][04][1080p][MP4][简日双语]",
            "[ANi] Dr STONE S3 -  Dr. STONE 新石紀 第三季 - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[ANi] Watashi no Yuri wa Oshigoto desu -  百合是我的工作！ - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[Lilith-Raws] 百合是我的工作！ / Watashi no Yuri wa Oshigoto desu! - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[ANi] The Ancient Magus Bride S2 -  魔法使的新娘 第二季 - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            " [哆啦字幕组][哆啦A梦新番 New Doraemon][222][2010.08.20][HDTV][1080P][简繁日][[骷髅岛的神秘宝藏][MP4+MKV][修复收藏版]",
            "[桜都字幕组] 我家的英雄 / My Home Hero [05][1080p][简繁内封]",
            "[c.c動漫][4月新番][無神世界的神明活動][05][BIG5][1080P][MP4][網盤下載]",
            "[动漫国字幕组&LoliHouse] 魔法使的新娘 第二季 / Mahou Tsukai no Yome S2 - 04 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "【喵萌Production】★04月新番★[偶像大师 灰姑娘女孩 U149 / THE IDOLM@STER CINDERELLA GIRLS U149][05][1080p][简日双语][招募翻译]",
            "[Skymoon-Raws] 【我推的孩子】 / Oshi no Ko - 04 [ViuTV][WEB-DL][1080p][AVC AAC][繁體外掛][MP4+ASS](正式版本)",
            "[Skymoon-Raws] 【我推的孩子】 / Oshi no Ko - 04 [ViuTV][WEB-RIP][1080p][AVC AAC][CHT][SRT][MKV](先行版本)",
            "[天月搬運組] 我的青春戀愛物語果然有問題。完 OVA [1080P][GB] V2",

            "[天月搬運組] 無神世界的神明活動 / Kaminaki Sekai no Kamisama Katsudou - 05 [1080P][繁體]",
            "[天月搬運組] 為美好的世界獻上爆焰！ / Kono Subarashii Sekai ni Bakuen wo! - 05 [1080P][簡繁日外掛]",
            "[动漫国字幕组&LoliHouse] 【我推的孩子】 / Oshi no Ko - 03 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "[天月搬運組] 魔術士歐菲 流浪之旅 聖域篇 / Majutsushi Orphen Hagure Tabi S04 Seiiki Hen - 04 [1080P][簡繁日外掛]",
            "[ANi] Kaminaki Sekai no Kamisama Katsudou -  無神世界的神明活動 - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[ANi]  無神世界的神明活動（僅限港澳台地區） - 05 [1080P][Bilibili][WEB-DL][AAC AVC][CHT CHS][MP4]",
            "[Lilith-Raws] 無神世界的神明活動 / Kaminaki Sekai no Kamisama Katsudou - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "【动漫国字幕组】★04月新番[魔法使的新娘 第二季][04][1080P][简体][MP4]",
            "【動漫國字幕組】★04月新番[魔法使的新娘 第二季][04][1080P][繁體][MP4]",
            "【动漫国字幕组】★04月新番[魔法使的新娘 第二季][04][720P][简体][MP4]",
            "【動漫國字幕組】★04月新番[魔法使的新娘 第二季][04][720P][繁體][MP4]",
            "[Billion Meta Lab][江户前精灵][04][1080p][HEVC 10bit][CHS&CHT]",
            "[Billion Meta Lab][江户前精灵][04][1080p][CHT]",
            "[Billion Meta Lab][江户前精灵][04][1080p][CHS]",
            "[Lilith-Raws] 為美好的世界獻上爆焰！ / Kono Subarashii Sekai ni Bakuen wo! - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[ANi]  為美好的世界獻上爆焰！ - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[Lilith-Raws] 【我推的孩子】 / Oshi no Ko - 04 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] 魔術士歐菲 流浪之旅 聖域篇 / Majutsushi Orphen Hagure Tabi S04 Seiiki Hen - 04 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[ANi] 【OSHI NO KO】 -  【我推的孩子】 - 04 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "【动漫国字幕组】★04月新番[【我推的孩子】][03][1080P][简体][MP4]",
            "【動漫國字幕組】★04月新番[【我推的孩子】][03][1080P][繁體][MP4]",
            "【动漫国字幕组】★04月新番[【我推的孩子】][03][720P][简体][MP4]",
            "【動漫國字幕組】★04月新番[【我推的孩子】][03][720P][繁體][MP4]",
            "[云歌字幕组][4月新番][熊熊勇闯异世界 Punch! Kuma Kuma Kuma Bear Punch!][05][AVC][x264 10bit][1080p][简体中文][招募翻译]",
            "【悠哈璃羽字幕社】[海盜戰記/冰海戰記_Vinland Saga S02][14][x264 1080p][CHT]",
            "【悠哈璃羽字幕社】[海盗战记/冰海战记_Vinland Saga S02][14][x264 1080p][CHS]",
            "[猎户手抄部] 转生贵族的异世界冒险录～不知自重的诸神的使徒～ Tensei Kizoku no Isekai Boukenroku [05] [1080p] [简繁内封] [2023年4月番]",
            "[猎户不鸽压制] 肌肉魔法使-MASHLE / 物理魔法使马修 Mashle [04] [1080p] [简中内嵌] [2023年4月番]",
            "[冷番补完字幕组][炫酷世界][Cool World][1992][1080p][内封中英双语字幕]",
            "[ANi] Sorcerous Stabber Orphen -  魔術士歐菲 流浪之旅 聖域篇 - 04 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "【喵萌奶茶屋】★04月新番★[放學後失眠的你 / Kimi wa Houkago Insomnia][03][1080p][繁日雙語][招募翻譯]",
            "【喵萌奶茶屋】★04月新番★[放学后失眠的你 / Kimi wa Houkago Insomnia][03][1080p][简日双语][招募翻译]",
            "【喵萌奶茶屋】★04月新番★[放學後失眠的你 / Kimi wa Houkago Insomnia][03][720p][繁日雙語][招募翻譯]",
            "【喵萌奶茶屋】★04月新番★[放学后失眠的你 / Kimi wa Houkago Insomnia][03][720p][简日双语][招募翻译]",
            "[北宇治字幕组&7³ACG] 镜之孤城 / Kagami no Kojou [Webrip][1080p][HEVC_AAC][简繁内封]",
            "[猎户手抄部] MIX：明青故事 第二季 Mix：Meisei Story S2 [05] [1080p] [简中内嵌] [2023年4月番]",
            "[猎户不鸽压制] 第二次被异世界召唤 Isekai Shoukan wa Nidome desu [04] [1080p] [简中内嵌] [2023年4月番]",
            "[猎户不鸽压制] 无神世界中的神明活动 Kaminaki Sekai no Kamisama Katsudou [04] [1080p] [简中内嵌] [2023年4月番]",
            "[猎户手抄部] 勇者死了！Yuusha ga Shinda! [04] [1080p] [简繁内封] [2023年4月番]",
            "[猎户手抄部] 勇者死了！Yuusha ga Shinda! [04] [1080p] [简中内嵌] [2023年4月番] ",
            "[北宇治字幕组] 赛马娘 Pretty Derby Road to the Top / Uma Musume - Pretty Derby - Road to the Top [03 v2][Webrip][1080p][HEVC_AAC][简体内封]",
            "[北宇治Anarchism字幕组] 魔法少女毀滅者/Mahou Shoujo Magical Destroyers [04][Webrip][1080p][HEVC_AAC][CHT][MP4]",
            "[夜莺家族][樱桃小丸子第二期(Chibi Maruko-chan II)][1341]小丸子与山田享受面具&小丸子想要种蜜瓜[2022.06.19][GB_JP][1080P][MP4]",
            "[夜莺家族][樱桃小丸子第二期(Chibi Maruko-chan II)][1340]马与胡萝卜、宏志与美酒&小丸子过度求神保佑[2022.06.12][GB_JP][1080P][MP4]",
            "[北宇治字幕组&霜庭云花Sub&氢气烤肉架]【我推的孩子】/【Oshi no ko】[03 V2][Webrip][1080p][HEVC_AAC][简繁日内封]",
            "[GM-Team][国漫][遮天][Shrouding the Heavens][2023][01-03][AVC][GB][1080P]",
            "[GM-Team][国漫][一念永恒 第2季][Yi Nian Yong Heng 2nd Season][2022][44][AVC][GB][1080P]",
            "[丸子家族][樱桃小丸子第二期(Chibi Maruko-chan II)][1384]姐姐交到秀友&小丸子享受香味[2023.04.30][简日_繁日内封][1080P][hevc-10bit_aac][MKV]",
            "[动漫国字幕组&LoliHouse] 为美好的世界献上爆焰! / Kono Subarashii Sekai ni Bakuen wo! - 04 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "[桜都字幕组] 总之就是非常可爱 S2 / Tonikaku Kawaii S2 [04][1080P][简繁内封]",
            "[桜都字幕组] 总之就是非常可爱 S2 / Tonikaku Kawaii S2 [04][1080P][繁体内嵌]",
            "[桜都字幕组] 总之就是非常可爱 S2 / Tonikaku Kawaii S2 [04][1080P][简体内嵌]",
            "[轻之国度字幕组][肌肉魔法使-MASHLE-/物理魔法使马修][04][720P][MP4]",

            "[北宇治字幕组&霜庭云花Sub&氢气烤肉架]【我推的孩子】/【Oshi no ko】[03][Webrip][1080p][HEVC_AAC][简繁日内封]",
            "【極影字幕社】小智是女孩子! 第13集 BIG5 AVC 1080p",
            "【动漫国字幕组】★04月新番[为美好的世界献上爆焰!][04][1080P][简体][MP4]",
            "[ANi]  東京喵喵 NEW ～♡（僅限港澳台地區） - 17 [1080P][Bilibili][WEB-DL][AAC AVC][CHT CHS][MP4]",
            "【动漫国字幕组】★04月新番[蓝色管弦乐][04][1080P][简体][MP4]",
            "[北宇治字幕组&霜庭云花Sub&氢气烤肉架]【我推的孩子】/【Oshi no ko】[03][Webrip][1080p][HEVC_AAC][繁日内嵌]",
            "[北宇治字幕组] 赛马娘 Pretty Derby Road to the Top / Uma Musume - Pretty Derby - Road to the Top [03][Webrip][1080p][HEVC_AAC][简体内封]",
            "[梦蓝字幕组]New Doraemon 哆啦A梦新番[756][2023.04.29][AVC][1080P][GB_JP][MP4]",
            "[梦蓝字幕组]Crayonshinchan 蜡笔小新[1179][2023.04.29][AVC][1080P][GB_JP][MP4]",
            "[漫貓字幕社][4月新番][地獄樂][Jigokuraku][05][1080P][MP4][繁日雙語]",
            "【悠哈璃羽字幕社】[放學後失眠的你_Kimi wa Houkago Insomnia][03][x264 1080p][CHT]",
            "【幻樱字幕组】【10月新番】【又酷又有点冒失的男孩子们 Cool Doji Danshi】【20-24】【GB_MP4】【1280X720】",
            "[北宇治字幕组&霜庭云花Sub&氢气烤肉架]【我推的孩子】/【Oshi no ko】[03][Webrip][1080p][HEVC_AAC][简日内嵌]",
            "[愛戀字幕社][4月新番][魔法使的新娘2][Mahoutsukai no Yome S2][04][1080P][MP4][BIG5][繁中]",
            "[豌豆字幕组&LoliHouse] 海盗战记 第二季 / 冰海战记 第二季 / Vinland Saga S2 - 17 [WebRip 1080p HEVC-10bit AAC][简繁外挂字幕]",
            "【豌豆字幕组】[海盗战记 / 冰海战记 第二季 / Vinland_Saga_S2][17][简体][1080P][MP4]",
            "[喵萌奶茶屋&LoliHouse] 在无神世界进行信仰传播活动 / Kaminaki Sekai no Kamisama Katsudou - 04 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "[喵萌奶茶屋&LoliHouse] 转生贵族的异世界冒险录 ~不知克制的众神的使徒~ / Tensei Kizoku no Isekai Boukenroku - 05 [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]",
            "【喵萌奶茶屋】★04月新番★[轉生貴族的異世界冒險錄 ~不知克制的眾神的使徒~ / Tensei Kizoku no Isekai Boukenroku][05][1080p][繁日雙語][招募翻譯]",
            "【喵萌Production】★04月新番★[世界大明星 / World Dai Star][04][1080p][繁日雙語][招募翻譯]",
            "[五一快乐][DBD-Raws][数码宝贝3：驯兽师之王/Digimon Tamers/デジモンテイマーズ][01-51TV全集][1080P][BDRip][HEVC-10bit][FLAC][MKV]",
            "[喵萌Production&LoliHouse] 世界大明星 / World Dai Star - 04 [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]",
            "丸子家族][海螺小姐(Sazae-san)][2694-2698][2023.04][简日_繁日内封][1080P][hevc-10bit_aac][MKV]",
            "[丸子家族][樱桃小丸子第二期(Chibi Maruko-chan II)][1380-1384][2023.04][简日_繁日内封][1080P][hevc-10bit_aac][MKV]",
            "[星空字幕組][我推的孩子 / Oshi no Ko][03][繁日雙語][1080P][WEBrip][MP4]（急招校對、後期）",
            "[天月搬運組] 我的青春戀愛物語果然有問題。完 OVA [1080P][GB] V1",
            "[Lilith-Raws] 絆之 Allele / Kizuna no Allele - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[丸子家族][樱桃小丸子OP][桃色幸运草Z - 大家一起来跳舞 舞蹈编导影像(Momoiro Clover Z - Odoru Popokorin Choreographer Video)]][WebRip][1080P][GB][MP4]",
            "[丸子家族][海螺小姐(Sazae-san)][2698]令人向往的大度量&註意力不集中的男人們&來吧!黃金週[2023.04.30][BIG5][1080P][MP4]",
            "[星空字幕組][魔法使的新娘2 / Mahoutsukai no Yome S2][04][繁日雙語][1080P][WEBrip][MP4]",
            "[星空字幕组][魔法使的新娘2 / Mahoutsukai no Yome S2][04][简日双语][1080P][WEBrip][MP4]",
            "[离谱Sub] 魔法少女魔法毀滅者 / 魔法少女毀滅者 / 魔法少女マジカルデストロイヤーズ / Mahou Shoujo Magical Destroyers [04][AVC AAC][1080p][繁體內嵌][招募翻校]",
            "【喵萌奶茶屋】★04月新番★[在無神世界進行信仰傳播活動 / Kaminaki Sekai no Kamisama Katsudou][04][1080p][繁體][招募翻譯]",
            "[GalaxyRailroad-888] 游戏王GO RUSH!! Yu-Gi-Oh! GO RUSH !! 056 720P [GB_简中]",
            "[MingY] 变成狗后被喜欢的人捡了。 / Inu ni Nattara Suki na Hito ni Hirowareta. [OVA02][完全wonderful版][[1080p]简日内嵌]（招募）",
            "【喵萌Production】★04月新番★[世界大明星 / World Dai Star][04][1080p][简日双语][招募翻译]",
            "【喵萌Production】★04月新番★[世界大明星 / World Dai Star][04][720p][简日双语][招募翻译]",
            "【极影字幕社】★4月新番 天国大魔境 Tengoku Daimakyou 第05话 GB 1080P MP4（字幕社招人内详）",
            "【极影字幕社】★4月新番 天国大魔境 Tengoku Daimakyou 第05话 GB 720P MP4（字幕社招人内详）",
            "[桜都字幕组] 我的百合乃工作是也！ / Watashi no Yuri wa Oshigoto desu! [04][1080P][简繁内封]",
            "[桜都字幕组] 我的百合乃工作是也！ / Watashi no Yuri wa Oshigoto desu! [04][1080P][繁体内嵌]",
            "[桜都字幕组] 我的百合乃工作是也！ / Watashi no Yuri wa Oshigoto desu! [04][1080P][简体内嵌]",
            "[LoliHouse] 亡骸游戏 / 尸体如山的死亡游戏 / Dead Mount Death Play - 04 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "[ANi] Kizuna no Allele -  絆之 Allele - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[离谱Sub] 魔法少女魔法毁灭者 / 魔法少女毁灭者 / 魔法少女マジカルデストロイヤーズ / Mahou Shoujo Magical Destroyers [04][HEVC AAC][1080p][简繁内封][招募翻校]",
            "[桜都字幕组] 为美好的世界献上爆焰！ / Kono Subarashii Sekai ni Bakuen wo!  [04][1080P@60FPS][简繁内封]",
            "[桜都字幕组] 为美好的世界献上爆焰！ / Kono Subarashii Sekai ni Bakuen wo!  [04][1080p][简繁内封]",
            "[离谱Sub] 魔法少女魔法毁灭者 / 魔法少女毁灭者 / 魔法少女マジカルデストロイヤーズ / Mahou Shoujo Magical Destroyers [04][AVC AAC][1080p][简体内嵌][招募翻校]",
            "[桜都字幕组] 为美好的世界献上爆焰！ / Kono Subarashii Sekai ni Bakuen wo!  [04][1080p][繁体内嵌]",
            "[天月搬運組] 屍體如山的死亡遊戲 / Dead Mount Death Play - 04 [1080P][簡繁日外掛]",

            "[Lilith-Raws] 在異世界獲得超強能力的我，在現實世界照樣無敵～等級提升改變人生命運～ / Iseleve - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] 屍體如山的死亡遊戲 / Dead Mount Death Play - 04 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] 帶著智慧型手機闖蕩異世界 / Isekai wa Smartphone to Tomo ni S02 - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] 熊熊勇闖異世界 PUNCH！ / Kuma Kuma Kuma Bear S02 - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[jibaketa合成&音頻壓制][代理商再版粵語]東京復仇者 / Tokyo Revengers - 05 [粵日雙語+內封繁體中文字幕] (WEB 1920x1080 AVC AACx2 PGS+SRT MUSE V2 CHT)",
            "[天月搬運組] 帶著智慧型手機闖蕩異世界II / Isekai wa Smartphone to Tomo ni S02 - 05 [1080P][簡繁日外掛]",
            "[ANi] Deddomaunto Desupurei -  屍體如山的死亡遊戲 - 04 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[ANi]  在異世界獲得超強能力的我，在現實世界照樣無敵～等級提升改變人生命運～ - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[NGA&桜都字幕组] 舰队Collection：终将在那片海 / KanColle：Itsuka Ano Umi de [01-08END][1080p][简繁内封]",
            "[NGA&桜都字幕組] 艦隊Collection 總有一天，在那片海 / KanColle：Itsuka Ano Umi de [01-08END][1080p][繁體內嵌]",
            "[NGA&桜都字幕组] 舰队Collection：终将在那片海 / KanColle：Itsuka Ano Umi de [01-08END][1080p][简体内嵌]",
            "[ANi]  帶著智慧型手機闖蕩異世界。2 - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "【今晚月色真美】[刺客信条：余烬][01][DVDRIP][720P][CHS&ENG][MP4]",
            "[天月搬運組] 機戰少女 Alice Expansion / Alice Gear Aegis Expansion - 05 [1080P][簡繁日外掛]",
            "【今晚月色真美】[Front Innocent][OVA][DVDRIP][960P/无修正][CHS&JPN][MP4]",
            "【MCE汉化组】[冬天的饋贈 / 冬のおくりもの]Fuyu no Okurimono][WEB動畫][繁體][1080P][x264 AAC]",
            "[AKito] 異世界一擊殺姊姊 My One-Hit Kill Sister - 04 [1080p][AVC AAC][CHT][MP4]",
            "[ANi] Kuma desu ga Nani ka -  熊熊勇闖異世界 PUNCH！ - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[Kaminaki Sekai no Kamisama Katsudou][01-04][HEVC][MKV][英语字幕]",
            "[Kono Subarashii Sekai ni Bakuen wo!][01-04][HEVC][MKV][英语字幕]",
            "[Oshi no Ko][01-03][HEVC][MKV][英语字幕]",
            "[Amor字幕组&云歌字幕组][4月新番][总之就是非常可爱 S2 Tonikaku Kawaii S2][01-03][AVC][x264 10bit][1080p][繁日双语][招募翻译]",
            "[Amor字幕组&云歌字幕组][4月新番][总之就是非常可爱 S2 Tonikaku Kawaii S2][01-03][AVC][x264 10bit][1080p][简日双语][招募翻译]",
            "[Mobile Suit Gundam - The Witch from Mercury][13-16][HEVC][MKV][英语字幕]",
            "[Dr. Stone S3][01-04][HEVC][MKV][英语字幕]",
            "[Iseleve][01-04][HEVC][MKV][英语字幕]",
            "[Lilith-Raws] 她去公爵家的理由 / Kanojo ga Koushaku-tei ni Itta Riyuu - 04 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[ANi] Kanojo ga Kōshakutei ni Itta Riyū -  她去公爵家的理由 - 04 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "【动漫国字幕组】★04月新番[跃动青春][04][1080P][简体][MP4]",
            "[Lilith-Raws] 機戰少女 Alice Expansion / Alice Gear Aegis Expansion - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[ANi]  機戰少女 Alice Expansion - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[豌豆字幕组&风之圣殿字幕组&LoliHouse] 鬼灭之刃 刀匠村篇 04(48) / Kimetsu no Yaiba - 48 [WebRip 1080p HEVC-10bit AAC][简繁外挂字幕]",
            "[夜莺家族][樱桃小丸子第二期(Chibi Maruko-chan II)][1384]姐姐结交了秀友&小丸子享受香味[2023.04.30][GB_JP][1080P][MP4]",
            "[Lilith-Raws] 我與機器子 / Boku to Roboko - 21 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[喵萌奶茶屋&LoliHouse] 勇者死了！/ Yuusha ga Shinda! - 02 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "【白戀動漫蘿蔔部】[機動戰士高達 水星的魔女][Mobile Suit Gundam THE WITCH FROM MERCURY][16][BIG5][1080P][MP4]",
            "【幻櫻字幕組】【4月新番】【躍動青春 Skip to Loafer】【04】【BIG5_MP4】【1920X1080】",
            "【幻樱字幕组】【4月新番】【跃动青春 Skip to Loafer】【04】【GB_MP4】【1920X1080】",
            "[千夏字幕组&喵萌奶茶屋&LoliHouse] 电影 轻旅轻营 (摇曳露营) / Yuru Camp Movie [BDRip 1080p HEVC-10bit FLAC][简繁内封字幕]",
            "[千夏字幕组&喵萌奶茶屋][电影 轻旅轻营 (摇曳露营) _Yuru Camp Movie][剧场版][BDRip_1080p_AVC][简体]",
            "[Mashle][01-04][HEVC][MKV][英语字幕]",
            "[Jigokuraku][01-05][HEVC][MKV][英语字幕]",
            "[Tensei Kizoku no Isekai Boukenroku][01-05][HEVC][MKV][英语字幕]",
            "[Gotoubun no Hanayome Movie][HEVC][MKV][英语字幕]",
            "[GM-Team][国漫][火凤燎原][The Ravages of Time][2023][02][AVC][GB][1080P]",
            "[GM-Team][国漫][火凤燎原][The Ravages of Time][2023][01][AVC][GB][1080P]",
            "[Kimetsu no Yaiba - Katanakaji no Sato-hen -][04][HEVC][MKV][英语字幕]",
            "[Kimetsu no Yaiba - Katanakaji no Sato-hen -][03][HEVC][MKV][英语字幕]",
            "[世界名作剧场 大草原的小天使灌木婴猴][Daisougen no Chiisana Tenshi Bush Baby][大草原の小さな天使 ブッシュベイビー][09][简体内嵌]",
            "[DAY字幕组][赛马娘 Pretty Derby 直道登顶 / Uma Musume: Pretty Derby Road to the Top][第3话 奔跑的理由][简体中文][AMZN.WEBrip][1080P][MP4]（含赛事注释）",

            "【豌豆字幕组&风之圣殿字幕组】★04月新番[鬼灭之刃 刀匠村篇 / Kimetsu_no_Yaiba-Katanakaji_no_Sato_Hen][04(48)][简体][1080P][MP4]",
            "[Skymoon-Raws] 鬼滅之刃 刀匠村篇 / Kimetsu no Yaiba - Katanakaji no Sato-Hen - 04 [ViuTV][WEB-DL][1080p][AVC AAC][繁體外掛][MP4+ASS](正式版本)",
            "【喵萌奶茶屋】★04月新番★[勇者死了！/勇者が死んだ！/Yuusha ga Shinda!][02][1080p][简体][招募翻译校对]",
            "【喵萌奶茶屋】★04月新番★[女神咖啡廳/女神的露天咖啡廳/女神のカフェテラス/Megami no Cafe Terrace][03][1080p][繁日雙語][招募翻譯校對]",
            "【喵萌奶茶屋】★04月新番★[百合是我的工作！/我的百合乃工作是也！/私の百合はお仕事です！/Watashi no Yuri wa Oshigoto desu!][04][1080p][繁日雙語][招募翻譯校對]",
            "[天月搬運組] 我家的英雄 / My Home Hero - 05 [1080P][簡繁日外掛]",
            "[爱恋字幕社&漫猫字幕社] 水星领航员/ARIA The ANIMATION (01-13Fin BDRIP 1080p AVC AAC MP4 2005年10月 简中)",
            "[丸子家族][櫻桃小丸子第二期(Chibi Maruko-chan II)][1384]姐姐交到秀友&小丸子享受香味[2023.04.30][BIG5][1080P][MP4]",
            "【枫叶字幕组】[宠物小精灵 / 宝可梦 地平线 莉可与罗伊的启程][004][简体][1080P][MP4]",
            "[ANi] Boku to Roboko -  我與機器子 - 21 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[漫游字幕组] Mobile Suit Gundam The Witch from Mercury 机动战士高达 水星的魔女 第16话 1080p MP4 简繁内嵌",
            "[ANi]  鬼滅之刃 刀匠村篇 - 04 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[Lilith-Raws] 鬼滅之刃 刀匠村篇 / Kimetsu no Yaiba - Katanakaji no Sato-Hen - 04 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[jibaketa合成&音頻壓制][ViuTV粵語]開闊天空！光之美少女 / Hirogaru Sky! Precure - 05 [粵語+無字幕] (WEB 1920x1080 AVC AAC CAN)",
            "[漫游字幕组] Mobile Suit Gundam The Witch from Mercury 机动战士高达 水星的魔女 第16话 Webrip 1080p MKV 简繁外挂",
            "[jibaketa合成&音頻壓制][TVB粵語]Love All Play 比賽開始 / 热血羽毛球 - 04 [粵語+內封繁體中文字幕][WEB 1920x1080 x264 AAC SRT TVB CAN CHT]",
            "【喵萌奶茶屋】★04月新番★[我內心的糟糕念頭/Boku no Kokoro no Yabai Yatsu][05][1080p][繁日雙語][招募翻譯]",
            "[Lilith-Raws] 我家的英雄 / My Home Hero - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[喵萌奶茶屋&LoliHouse] 我内心的糟糕念头 / Boku no Kokoro no Yabai Yatsu - 05 [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]",
            "[jibaketa合成&二次壓制][TVB粵語]勇者鬥惡龍 達伊的大冒險 / Dragon Quest - Dai no Daibouken (2020) - 38 [粵語+內封繁體中文字幕][BD 1920x1080 x264 AAC SRT TVB CAN CHT]",
            "[ANi] My Home Hero -  我家的英雄 - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[LoliHouse] 邻人似银河 / Otonari ni Ginga - 04 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "[喵萌奶茶屋&LoliHouse] 机动战士高达 水星的魔女 / Mobile Suit Gundam THE WITCH FROM MERCURY - 16 [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]",
            "【喵萌奶茶屋】★04月新番★[機動戰士鋼彈 水星的魔女/Mobile Suit Gundam THE WITCH FROM MERCURY][16][1080p][繁日雙語][招募翻譯]",
            "[星空字幕組][世界巨星 / 世界大明星 / World Dai Star][03][繁日雙語][1080P][WEBrip][MP4]（急招校對、後期）",
            "[LoliHouse] 赛马娘 Pretty Derby：Road to the Top/Uma Musume Pretty Derby: Road to the Top - 03 [WebRip 1080p HEVC-10bit AAC][简繁内封]",
            "[喵萌奶茶屋&LoliHouse] 女神咖啡厅 / 女神的露天咖啡厅 / Megami no Cafe Terrace - 03 [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]",
            "[Lilith-Raws] 轉生貴族的異世界冒險錄～不知自重的眾神使徒～ / Tensei Kizoku no Isekai Boukenroku - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "【鈴風字幕組】【屍體如山的死亡遊戲 / Dead Mount Death Play】[03][1080P][MKV][繁體外掛]",
            "[Isekai Quartet Movie - Another World][HEVC][MKV][英语字幕]",
            "[Kimi wo Aishita Hitori no Boku e][HEVC][MKV][英语字幕]",
            "[Tensei shitara Slime Datta Ken Movie - Guren no Kizuna-hen][HEVC][MKV][英语字幕]",
            "[Mobile Suit Gundam - Cucuruz Doan's Island][HEVC][MKV][英语字幕]",
            "[jibaketa合成&二次壓制][TVB粵語]勇者鬥惡龍 達伊的大冒險 / Dragon Quest - Dai no Daibouken (2020) - 37 [粵語+內封繁體中文字幕][BD 1920x1080 x264 AAC SRT TVB CAN CHT]",
            "【官方油管搬运】【赛马娘 Pretty Derby ROAD TO THE TOP】【第3話】【走る理由】",
            "[ANi]  轉生貴族的異世界冒險錄～不知自重的眾神使徒～ - 05 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[jibaketa合成&音頻壓制][TVB粵語]閃躍吧！星夢☆頻道 / 美妙☆频道 / Kiratto Pri-chan - 02 [粵日雙語+內封繁體中文字幕][WEB 1920x1080 x264 AACx2 SRT TVB CHT]",
            "[天月搬運組] 機動戰士高達 水星的魔女 - 第16集 『罪過的輪迴』(CN,HK,TW,JP sub)[1080P]",
            "[天月搬運組] 機動戰士高達 水星的魔女 第二季 / Mobile Suit Gundam：The Witch from Mercury S02 - 16 [1080P][簡繁日外掛]",
            "[喵萌奶茶屋&LoliHouse] 可爱过头大危机 / Kawaisugi Crisis - 04 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
            "【喵萌奶茶屋】★04月新番★[地獄樂 / Jigokuraku / Hell’s Paradise][05][1080p][繁日雙語][招募翻譯]",
            "【喵萌奶茶屋】★04月新番★[祭品公主与兽之王 / Niehime to Kemono no Ou][02][1080p][简体][招募翻译]",
            "【喵萌奶茶屋】★04月新番★[可愛過頭大危機 / Kawaisugi Crisis][04][1080p][繁體][招募翻譯]",
            "【极影字幕社】可爱过头大危机 第04集 GB_CN HEVC_opus 1080p",
            "[喵萌奶茶屋&LoliHouse] 祭品公主与兽之王 / Niehime to Kemono no Ou - 02 [WebRip 1080p HEVC-10bit AAC][简体内封字幕]",
            "[Lilith-Raws] 機動戰士鋼彈 水星的魔女 / Mobile Suit Gundam：The Witch from Mercury S02 - 04 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[c.c動漫][4月新番][第二次被異世界召喚][04][BIG5][1080P][MP4][網盤下載]",
            "[c.c動漫][4月新番][總之就是非常可愛 第二季][04][BIG5][1080P][MP4][網盤下載]",
            "[ANi]  機動戰士鋼彈 水星的魔女 Season 2 - 16 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "[漫貓字幕社&貓戀漢化組][4月新番][為美好的世界獻上爆焰！][Kono Subarashii Sekai ni Bakuen wo!][04][1080P][MP4][繁日雙語]",
            "[雪飘工作室][海阔天空！光之美少女/Hirogaru_Sky！Precure/ひろがるスカイ！プリキュア][WEBDL][1080p][13][简繁外挂](检索:Q娃)",

            "[Skymoon-Raws][One Piece 海賊王][1060][ViuTV][WEB-DL][1080p][AVC AAC][CHT][MP4+ASS](正式版本)",
            "[桜都字幕组] 熊熊勇闯异世界 Punch! / Kuma Kuma Kuma Bear Punch! [04][1080p][简繁内封]",
            "[北宇治Anarchism字幕组] 魔法少女毁灭者/Mahou Shoujo Magical Destroyers [04][Webrip][1080p][HEVC_AAC][CHS][MP4]",
            "[星空字幕组][国王战队君王者 / Ohsama Sentai King-Ohger][09][简日双语][1080P][WEBrip][MP4]（急招校对、后期）",
            "[XK SPIRITS][Kamen Rider Geats][33][简日双语][1080P][WEBrip][MP4]（急招校对、时轴）",
            "[雪飘工作室][海阔天空！光之美少女/Hirogaru_Sky！Precure/ひろがるスカイ！プリキュア][13][720p][简体内嵌](检索:Q娃)",
            "【幻櫻字幕組】【4月新番】【不知內情的轉學生不管三七二十一纏了上來 Jijou wo Shiranai Tenkousei ga Guigui Kuru】【04】【BIG5_MP4】【1920X1080】",
            "[北宇治字幕组&MingYSub] 為美好的世界獻上爆焰！/Kono Subarashii Sekai ni Bakuen wo! [04][Webrip][1080p][HEVC_AAC][繁日內嵌]",
            "【喵萌奶茶屋】★04月新番★[我内心的糟糕念头/Boku no Kokoro no Yabai Yatsu][05][1080p][简日双语][招募翻译]",
            "[MingY] 跃动青春 / Skip to Loafer [03][1080p][简日内嵌]（招募）",
            "[夜莺家族&YYQ字幕组]New Doraemon 哆啦A梦新番[756][2023.04.29][AVC][1080P][GB_JP]",
            "[动漫国字幕组&LoliHouse] THE MARGINAL SERVICE - 03 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]（检索用：边缘服务）",
            "[MingY] 躍動青春 / Skip to Loafer [03][1080p][繁日內嵌]（招募）",
            "[MingY] 跃动青春 / Skip to Loafer [03][1080p][简繁日内封]（招募）",
            "[六道我大鴿漢化組][六道的惡女們][Rokudou no Onna-tachi][04][1080p][AVC AAC][繁中]",
            "[SweetSub&LoliHouse] 天国大魔境 / Tengoku Daimakyou - 05 [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]",
            "[SweetSub][天國大魔境][Heavenly Delusion][05][WebRip][1080P][AVC 8bit][繁日雙語]",
            "[SweetSub][天国大魔境][Heavenly Delusion][05][WebRip][1080P][AVC 8bit][简日双语]",
            "[豌豆字幕组&风之圣殿字幕组&LoliHouse] 地狱乐 / Jigokuraku - 05 [WebRip 1080p HEVC-10bit AAC][简繁外挂字幕]",
            "[Lilith-Raws] 我內心的糟糕念頭 / Boku no Kokoro no Yabai Yatsu - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] 第二次被異世界召喚 / Isekai Shoukan wa Nidome desu - 04 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] 鄰人似銀河 / Otonari ni Ginga - 04 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[Lilith-Raws] 和山田談場 Lv999 的戀愛 / Yamada-kun to Lv999 no Koi wo Suru - 05 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
            "[千夏字幕组][偶像大师 灰姑娘女孩 U149_THE IDOLM@STER CINDERELLA GIRLS U149][第04话][1080p_HEVC][简繁内封][招募新人]"
    );
}

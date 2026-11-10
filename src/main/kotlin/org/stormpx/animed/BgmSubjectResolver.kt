package org.stormpx.animed

import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 把 bgm 搜索返回的一堆候选（同一关键词经常同时命中续作、同名的剧场版、名字相近的其它作品）
 * 收敛成一个结论：要么唯一命中，要么明确告诉调用方"我分不出来"。
 *
 * 判据可靠性从高到低：
 * 1. 名字（归一化后，含别名）—— 硬门槛，低于 [minNameScore] 直接出局
 * 2. 季/部分标记是否一致 —— 区分"罗梅莉亚战记"和"罗梅莉亚战记 第2部分"
 * 3. 开播日期与 RSS 条目发布时间的相容性 —— 2011 年开播的作品不可能有 2026 年的连载资源
 * 4. 集号可行性 —— 条目集号不该超过该条目的集数
 *
 * 刻意不引入热度（bgm 的 collection.doing）：新番冷门时它会把正确答案压下去。
 */
class BgmSubjectResolver(
    private val minNameScore: Double = 0.60,
    private val acceptNameScore: Double = 0.85,
    private val acceptMargin: Double = 0.60,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(BgmSubjectResolver::class.java)

        /** 有集号连载资源不会来自这些介质 */
        private val REJECTED_PLATFORMS = setOf("剧场版", "音乐")

        private const val NAME_WEIGHT = 3.0
        private const val SEASON_SAME = 0.50
        private const val SEASON_ITEM_MISSING = -1.00
        private const val SEASON_CANDIDATE_MISSING = -0.50
        private const val SEASON_CONFLICT = -1.50
        private const val DATE_IN_WINDOW = 0.40
        private const val DATE_UNKNOWN = -0.30
        private const val DATE_NOT_AIRED = -1.00
        private const val DATE_TOO_OLD = -0.20
        private const val EPS_FEASIBLE = 0.20
        private const val EPS_EXCEEDED = -0.40
        private const val EPS_IMPOSSIBLE = -1.00
    }

    data class Candidate(
        val id: Int,
        val names: List<String>,
        val platform: String? = null,
        val date: LocalDate? = null,
        val eps: Int? = null,
    )

    data class Scored(
        val candidate: Candidate,
        val nameScore: Double,
        val matchedName: String,
        val seasonAdjust: Double,
        val dateAdjust: Double,
        val epsAdjust: Double,
    ) {
        val total: Double
            get() = nameScore * NAME_WEIGHT + seasonAdjust + dateAdjust + epsAdjust

        fun report(): String {
            return "id=${candidate.id} '${matchedName}' total=%.2f (name=%.2f season=%+.2f date=%+.2f eps=%+.2f)".format(
                total, nameScore, seasonAdjust, dateAdjust, epsAdjust
            )
        }
    }

    sealed interface Decision {
        data class Resolved(val best: Scored, val ranked: List<Scored>) : Decision
        data class Ambiguous(val ranked: List<Scored>) : Decision
        object NotFound : Decision
    }

    private data class TitleForm(val base: String, val season: Int?)

    fun resolve(
        titles: List<String>,
        episode: Double?,
        pubDate: LocalDateTime?,
        candidates: Collection<Candidate>,
    ): Decision {
        val forms = titles.asSequence()
            .map { TitleNormalizer.normalize(it) }
            .filter { it.isNotEmpty() }
            .map { SeasonMarker.parse(it) }
            .map { TitleForm(it.base, it.season) }
            .distinct()
            .toList()

        if (forms.isEmpty() || candidates.isEmpty()) {
            return Decision.NotFound
        }

        val ranked = candidates.mapNotNull { score(it, forms, episode, pubDate) }
            .sortedByDescending { it.total }

        val best = ranked.firstOrNull() ?: return Decision.NotFound
        val runnerUp = ranked.getOrNull(1)
        val margin = if (runnerUp == null) Double.MAX_VALUE else best.total - runnerUp.total

        if (best.nameScore >= acceptNameScore && best.total > 0 && margin >= acceptMargin) {
            logger.debug(
                "bgm resolve {} -> {} | runner up: {}",
                forms.first().base,
                best.report(),
                runnerUp?.report() ?: "-"
            )
            return Decision.Resolved(best, ranked)
        }

        logger.debug(
            "bgm resolve {} -> ambiguous (margin=%.2f): {}",
            forms.first().base,
            margin,
            ranked.take(3).joinToString(" | ") { it.report() }
        )
        return Decision.Ambiguous(ranked)
    }

    private fun score(
        candidate: Candidate,
        forms: List<TitleForm>,
        episode: Double?,
        pubDate: LocalDateTime?,
    ): Scored? {
        if (episode != null && candidate.platform in REJECTED_PLATFORMS) {
            return null
        }

        var best: Scored? = null
        for (form in forms) {
            for (name in candidate.names) {
                val normalizedName = TitleNormalizer.normalize(name)
                if (normalizedName.isEmpty()) {
                    continue
                }
                val nameForm = SeasonMarker.parse(normalizedName)
                val score = nameScore(form.base, nameForm.base)
                if (score < minNameScore) {
                    continue
                }
                val scored = Scored(
                    candidate = candidate,
                    nameScore = score,
                    matchedName = name,
                    seasonAdjust = seasonAdjust(form.season, nameForm.season),
                    dateAdjust = dateAdjust(candidate.date, pubDate, candidate.eps),
                    epsAdjust = epsAdjust(candidate.eps, episode),
                )
                if (best == null || scored.total > best.total) {
                    best = scored
                }
            }
        }
        return best
    }

    /**
     * 基名相似度。一方是另一方的严格前缀时（"罗小黑战记" vs "罗小黑战记 众生之门篇"）视为
     * 篇章/子系列，不再按"多几个字"线性给分，避免派生作压过本体。
     */
    private fun nameScore(itemBase: String, candidateBase: String): Double {
        if (itemBase == candidateBase) {
            return 1.0
        }
        val similarity = TitleNormalizer.similarity(itemBase, candidateBase)
        val prefix = candidateBase.startsWith(itemBase) || itemBase.startsWith(candidateBase)
        return if (prefix) minOf(similarity, 0.80) else similarity
    }

    private fun seasonAdjust(item: Int?, candidate: Int?): Double = when {
        item == null && candidate == null -> SEASON_SAME
        item != null && candidate != null -> if (item == candidate) SEASON_SAME else SEASON_CONFLICT
        item == null -> SEASON_ITEM_MISSING
        else -> SEASON_CANDIDATE_MISSING
    }

    private fun dateAdjust(date: LocalDate?, pubDate: LocalDateTime?, eps: Int?): Double {
        if (date == null) {
            return DATE_UNKNOWN
        }
        if (pubDate == null) {
            return 0.0
        }
        val aired = pubDate.toLocalDate()
        if (aired.isBefore(date.minusDays(3))) {
            return DATE_NOT_AIRED
        }
        val span = maxOf(eps ?: 1, 1).toLong() * 10
        return if (aired.isAfter(date.plusDays(span))) DATE_TOO_OLD else DATE_IN_WINDOW
    }

    private fun epsAdjust(eps: Int?, episode: Double?): Double = when {
        eps == null || eps <= 0 || episode == null -> 0.0
        episode <= eps -> EPS_FEASIBLE
        // 分割放送/连续编号会让集号超过本季集数，只做轻微惩罚，避免把正确条目压死
        episode > eps * 2 -> EPS_IMPOSSIBLE
        else -> EPS_EXCEEDED
    }

}

fun BgmApi.BgmInfo.toCandidate(): BgmSubjectResolver.Candidate {
    return BgmSubjectResolver.Candidate(
        id = id,
        names = name,
        platform = platform,
        date = date,
        eps = eps,
    )
}

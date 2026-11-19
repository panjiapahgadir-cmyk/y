package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.max

enum class GhostId(
  val key: String,
  val displayName: String,
  val role: String,
  val cost: Int,
  val cooldown: Float,
  val baseHp: Float,
  val baseDmg: Float,
  val speed: Float,
  val range: Float,
  val atkInterval: Float,
  val animDuration: Float,
  val impactTime: Float,
  val skillSummary: String,
  val lore: String,
  val spriteHeight: Float,
  val isFlying: Boolean,
  val isStarter: Boolean,
  val unlockPrice: Int,
  val accentColor: Color,
) {
  TUYUL(
    key = "tuyul",
    displayName = "Tuyul",
    role = "Pencuri Arwah",
    cost = 30,
    cooldown = 1.4f,
    baseHp = 45f,
    baseDmg = 7f,
    speed = 62f,
    range = 26f,
    atkInterval = 0.6f,
    animDuration = 0.4f,
    impactTime = 0.2f,
    skillSummary = "Mencuri +3 arwah musuh tiap pukulan",
    lore = "Roh kecil gesit pemburu pesugihan. Meski tubuhnya mungil, serangannya cepat dan menyedot arwah lawan menjadi energi pasukanmu.",
    spriteHeight = 62f,
    isFlying = false,
    isStarter = true,
    unlockPrice = 0,
    accentColor = Color(0xFF4ADE80),
  ),
  POCONG(
    key = "pocong",
    displayName = "Pocong",
    role = "Pendobrak Pingsan",
    cost = 50,
    cooldown = 2.5f,
    baseHp = 120f,
    baseDmg = 15f,
    speed = 38f,
    range = 34f,
    atkInterval = 1.2f,
    animDuration = 0.7f,
    impactTime = 0.62f,
    skillSummary = "Melompat menimpa area, musuh terpental & pingsan",
    lore = "Arwah terikat kain kafan yang melompat tinggi lalu menghantam tanah dengan daya kejut hebat untuk melumpuhkan barisan depan.",
    spriteHeight = 88f,
    isFlying = false,
    isStarter = true,
    unlockPrice = 0,
    accentColor = Color(0xFFE2E8F0),
  ),
  API(
    key = "api",
    displayName = "Banaspati",
    role = "Penyerang Api",
    cost = 80,
    cooldown = 4.0f,
    baseHp = 80f,
    baseDmg = 12f,
    speed = 48f,
    range = 30f,
    atkInterval = 1.1f,
    animDuration = 0.45f,
    impactTime = 0.25f,
    skillSummary = "Membakar musuh selama 3 detik (damage lanjutan)",
    lore = "Bola api berwajah seram yang melayang di kegelapan malam. Setiap sentuhannya meninggalkan bara api abadi di tubuh lawan.",
    spriteHeight = 84f,
    isFlying = true,
    isStarter = true,
    unlockPrice = 0,
    accentColor = Color(0xFFFB923C),
  ),
  KUYANG(
    key = "kuyang",
    displayName = "Kuyang",
    role = "Penghisap Darah",
    cost = 90,
    cooldown = 4.5f,
    baseHp = 60f,
    baseDmg = 14f,
    speed = 44f,
    range = 28f,
    atkInterval = 1.0f,
    animDuration = 0.5f,
    impactTime = 0.25f,
    skillSummary = "Menyedot darah 80% damage untuk memulihkan diri",
    lore = "Siluman kepala terbang dari pedalaman Kalimantan yang menyerap sari kehidupan korban demi bertahan lebih lama di medan tempur.",
    spriteHeight = 76f,
    isFlying = true,
    isStarter = true,
    unlockPrice = 0,
    accentColor = Color(0xFFFB7185),
  ),
  WEWE(
    key = "wewe",
    displayName = "Wewe Gombel",
    role = "Penyembuh Area",
    cost = 100,
    cooldown = 6.0f,
    baseHp = 110f,
    baseDmg = 8f,
    speed = 30f,
    range = 34f,
    atkInterval = 1.4f,
    animDuration = 0.5f,
    impactTime = 0.25f,
    skillSummary = "Memancarkan aura penyembuh +24 HP untuk sekutu sekitar",
    lore = "Sosok penjaga anak-anak di rimba raya. Setiap ayunan tangannya memancarkan cahaya hijau gaib yang merawat pasukan sekutu.",
    spriteHeight = 90f,
    isFlying = false,
    isStarter = false,
    unlockPrice = 120,
    accentColor = Color(0xFF34D399),
  ),
  KUNTI(
    key = "kunti",
    displayName = "Kuntilanak",
    role = "Penyerang Jauh",
    cost = 110,
    cooldown = 5.0f,
    baseHp = 70f,
    baseDmg = 24f,
    speed = 30f,
    range = 130f,
    atkInterval = 1.6f,
    animDuration = 0.6f,
    impactTime = 0.3f,
    skillSummary = "Tawa mencekam jarak jauh: musuh area ketakutan & melambat",
    lore = "Tawa nyaringnya menembus batas jarak, membuat nyali para hantu lawan ciut dan gerakan mereka melambat drastis.",
    spriteHeight = 94f,
    isFlying = true,
    isStarter = true,
    unlockPrice = 0,
    accentColor = Color(0xFFE879F9),
  ),
  LEAK(
    key = "leak",
    displayName = "Leak",
    role = "Penarik Barisan",
    cost = 120,
    cooldown = 6.0f,
    baseHp = 150f,
    baseDmg = 18f,
    speed = 32f,
    range = 95f,
    atkInterval = 1.5f,
    animDuration = 0.55f,
    impactTime = 0.3f,
    skillSummary = "Lidah panjang menarik musuh jauh ke depan & pingsan",
    lore = "Penyihir ilmu hitam Bali bertaring tajam dengan lidah gaib yang menyeret target jarak jauh langsung ke barisan tempur.",
    spriteHeight = 84f,
    isFlying = false,
    isStarter = false,
    unlockPrice = 280,
    accentColor = Color(0xFFF43F5E),
  ),
  GENDRUWO(
    key = "gendruwo",
    displayName = "Genderuwo",
    role = "Raksasa Tank",
    cost = 140,
    cooldown = 7.0f,
    baseHp = 260f,
    baseDmg = 30f,
    speed = 26f,
    range = 38f,
    atkInterval = 1.5f,
    animDuration = 0.6f,
    impactTime = 0.36f,
    skillSummary = "Menghantam bumi: ledakan area besar, dorong & pingsan 0.5s",
    lore = "Raksasa penguasa pohon beringin tua. Satu gebukan tangannya menggetarkan tanah, menghempaskan kerumunan musuh sekaligus.",
    spriteHeight = 96f,
    isFlying = false,
    isStarter = false,
    unlockPrice = 220,
    accentColor = Color(0xFFA78BFA),
  ),
  SUNDEL(
    key = "sundel",
    displayName = "Sundel Bolong",
    role = "Pengendali Pikiran",
    cost = 150,
    cooldown = 9.0f,
    baseHp = 90f,
    baseDmg = 10f,
    speed = 34f,
    range = 36f,
    atkInterval = 2.2f,
    animDuration = 0.6f,
    impactTime = 0.3f,
    skillSummary = "Memikat musuh menjadi sekutu selama 3.5 detik",
    lore = "Pesona kelamnya mampu membalikkan kesetiaan lawan hingga berbalik menyerang kawanannya sendiri.",
    spriteHeight = 94f,
    isFlying = false,
    isStarter = false,
    unlockPrice = 360,
    accentColor = Color(0xFFF9A8D4),
  ),
  PALASIK(
    key = "palasik",
    displayName = "Palasik",
    role = "Pemburu Bayangan",
    cost = 135,
    cooldown = 5.5f,
    baseHp = 135f,
    baseDmg = 28f,
    speed = 68f,
    range = 42f,
    atkInterval = 0.95f,
    animDuration = 0.45f,
    impactTime = 0.2f,
    skillSummary = "Serbuan bayangan cepat: curi +8 arwah & lambatkan musuh 50%",
    lore = "Pemburu malam berjubah kabut dari ranah Minang. Meluncur secepat bayangan dengan cakar obsidian untuk memutus aliran arwah lawan.",
    spriteHeight = 90f,
    isFlying = false,
    isStarter = false,
    unlockPrice = 340,
    accentColor = Color(0xFF22D3EE),
  ),
  BARONG(
    key = "barong",
    displayName = "Barong Ket",
    role = "Pelindung Perisai",
    cost = 170,
    cooldown = 8.0f,
    baseHp = 320f,
    baseDmg = 24f,
    speed = 32f,
    range = 40f,
    atkInterval = 1.4f,
    animDuration = 0.65f,
    impactTime = 0.34f,
    skillSummary = "Perisai Sakti: lindungi diri & sekutu (-45% damage) + pulihkan HP",
    lore = "Raja roh pelindung kebajikan dari Bali. Setiap aumannya memancarkan kubah cahaya emas yang menahan gempuran pasukan kegelapan.",
    spriteHeight = 100f,
    isFlying = false,
    isStarter = false,
    unlockPrice = 420,
    accentColor = Color(0xFFFBBF24),
  ),
  BUTO_IJO(
    key = "buto_ijo",
    displayName = "Buto Ijo",
    role = "Titan Pemecah Bumi",
    cost = 180,
    cooldown = 8.5f,
    baseHp = 380f,
    baseDmg = 42f,
    speed = 25f,
    range = 46f,
    atkInterval = 1.75f,
    animDuration = 0.72f,
    impactTime = 0.44f,
    skillSummary = "Gada Gempa Bumi: ledakan area raksasa, lempar mundur & pingsan 0.8s",
    lore = "Raksasa bertaring emas bersenjata gada batu retak energi giok. Satu hantamannya menggetarkan medan perang dan menghempaskan barisan musuh.",
    spriteHeight = 108f,
    isFlying = false,
    isStarter = false,
    unlockPrice = 450,
    accentColor = Color(0xFF10B981),
  ),
  RANGDA(
    key = "rangda",
    displayName = "Rangda",
    role = "Ratu Petir Hitam",
    cost = 185,
    cooldown = 9.0f,
    baseHp = 165f,
    baseDmg = 34f,
    speed = 31f,
    range = 145f,
    atkInterval = 1.65f,
    animDuration = 0.62f,
    impactTime = 0.32f,
    skillSummary = "Petir Hitam Berantai menyambar 4 musuh, membakar & memperlambat",
    lore = "Ratu sihir agung bermahkota api. Sambaran petir ungu dari cakarnya melompat antar musuh sambil meninggalkan kutukan bara membara.",
    spriteHeight = 98f,
    isFlying = true,
    isStarter = false,
    unlockPrice = 480,
    accentColor = Color(0xFFC084FC),
  ),
  NYI_BLORONG(
    key = "nyi_blorong",
    displayName = "Nyi Blorong",
    role = "Ratu Ombak Emas",
    cost = 195,
    cooldown = 9.5f,
    baseHp = 210f,
    baseDmg = 30f,
    speed = 35f,
    range = 105f,
    atkInterval = 1.55f,
    animDuration = 0.62f,
    impactTime = 0.32f,
    skillSummary = "Gelombang Pusaka: sapu area, dorong musuh & hasilkan +2 koin tiap serang",
    lore = "Penguasa pesisir gaib bertubuh ular zamrud berkilau emas. Hempasan ombak mistisnya memukul mundur lawan sekaligus menjatuhkan koin pusaka.",
    spriteHeight = 100f,
    isFlying = false,
    isStarter = false,
    unlockPrice = 520,
    accentColor = Color(0xFF2DD4BF),
  );

  val isBossThreat: Boolean
    get() =
      this == GENDRUWO ||
        this == LEAK ||
        this == SUNDEL ||
        this == BARONG ||
        this == BUTO_IJO ||
        this == RANGDA ||
        this == NYI_BLORONG

  val isMythicNew: Boolean
    get() =
      this == PALASIK ||
        this == BARONG ||
        this == BUTO_IJO ||
        this == RANGDA ||
        this == NYI_BLORONG

  companion object {
    val ordered: List<GhostId> = entries.toList()
    val unlockables: List<GhostId> = entries.filter { !it.isStarter }
    fun fromKey(key: String): GhostId? = entries.firstOrNull { it.key == key }
  }
}

enum class JimatId(
  val key: String,
  val title: String,
  val desc: String,
  val price: Int,
  val color: Color,
) {
  KERIS("keris", "Keris Kramat", "Damage semua hantumu +50% selama 15 detik", 60, Color(0xFFFBBF24)),
  KEMENYAN("kemenyan", "Dupa Kemenyan", "Arwah mengalir 2x lebih cepat selama 12 detik", 50, Color(0xFF38BDF8)),
  KEMBANG("kembang", "Air Kembang 7 Rupa", "Pulihkan penuh semua hantu & markas +150 HP", 70, Color(0xFFF472B6)),
  GARAM("garam", "Garam Kasar", "Semua musuh melambat 50% selama 8 detik", 45, Color(0xFFE2E8F0)),
  PETIR("petir", "Petir Kyai", "Menyambar semua musuh dengan 120 damage", 85, Color(0xFFFDE047)),
  AYAM("ayam", "Ayam Cemani", "Semua musuh pingsan membeku selama 3 detik", 90, Color(0xFFC084FC)),
}

enum class UpgradeId(
  val key: String,
  val title: String,
  val desc: String,
  val maxLevel: Int,
  val basePrice: Int,
) {
  GUDANG("gudang", "Gudang Arwah", "Kapasitas arwah maksimal +100 per level", 3, 100),
  SUMUR("sumur", "Sumur Arwah", "Kecepatan aliran arwah +4/detik per level", 4, 90),
  BENTENG("benteng", "Benteng Kuat", "HP markasmu meningkat +15% per level", 4, 110),
  DUKUN("dukun", "Dukun Sakti", "Cooldown semua kartu hantu -8% per level", 4, 120);

  fun priceForLevel(level: Int): Int = basePrice * (level + 1)
}

data class StageDef(
  val index: Int,
  val name: String,
  val subtitle: String,
  val tint: Color,
  val baseHp: Float,
  val gapMin: Float,
  val gapMax: Float,
  val multiplier: Float,
  val reward: Int,
  val weights: Map<GhostId, Float>,
) {
  val hasBakedBg: Boolean get() = true
}

data class TournamentDef(
  val index: Int,
  val name: String,
  val badge: String,
  val entryFee: Int,
  val rounds: Int,
  val startMul: Float,
  val prize: Int,
  val color: Color,
)

enum class FreeDifficulty(
  val label: String,
  val mul: Float,
  val baseHp: Float,
  val gapMul: Float,
  val reward: Int,
  val weightStageIdx: Int,
  val accent: Color,
) {
  SANTAI("Santai", 0.8f, 450f, 1.3f, 15, 2, Color(0xFF34D399)),
  NORMAL("Normal", 1.2f, 700f, 1.0f, 30, 4, Color(0xFF38BDF8)),
  SULIT("Sulit", 1.8f, 1000f, 0.75f, 60, 6, Color(0xFFF59E0B)),
  GILA("Gila", 2.6f, 1400f, 0.55f, 100, 9, Color(0xFFFB7185)),
}

enum class BattleModeType { STAGE, TOURNAMENT, INFINITY, FREE }

data class BattleConfig(
  val mode: BattleModeType,
  val stageIndex: Int = 0,
  val tournamentIndex: Int = 0,
  val tournamentRound: Int = 0,
  val infinityLevel: Int = 1,
  val freeDifficulty: FreeDifficulty = FreeDifficulty.NORMAL,
  val title: String,
  val label: String,
  val tint: Color,
  val bgIndex: Int,
  val enemyBaseHp: Float,
  val gapMin: Float,
  val gapMax: Float,
  val enemyMul: Float,
  val rewardCoins: Int,
  val coinFactor: Float = 1f,
  val weights: Map<GhostId, Float>,
)

object GameCatalog {
  private val tints =
    listOf(
      Color(0xFF8B5CF6),
      Color(0xFF22C55E),
      Color(0xFFEF4444),
      Color(0xFF3B82F6),
      Color(0xFFF59E0B),
      Color(0xFF06B6D4),
      Color(0xFFEC4899),
      Color(0xFF10B981),
      Color(0xFFF97316),
      Color(0xFFA855F7),
    )

  private val stageNames =
    listOf(
      "Kuburan Tua" to "Nisan retak dan kabut makam membangkitkan arwah penasaran.",
      "Hutan Angker" to "Pohon beringin raksasa tempat Kuntilanak mengintai dari dahan.",
      "Rumah Kosong" to "Gedung kolonial tua dengan lorong gelap penuh bisikan Leak.",
      "Sawah Tengah Malam" to "Pematang sunyi diterangi obor tipis dan Kuyang pemburu.",
      "Jembatan Tua" to "Jembatan kayu lapuk di atas sungai berarus tempat Genderuwo berjaga.",
      "Pabrik Terbengkalai" to "Mesin berkarat berdengung bersama rombongan Wewe Gombel.",
      "Pura Gelap" to "Gerbang candi kuno Bali di bawah bulan merah penuh pesona Sundel Bolong.",
      "Alas Roban" to "Jalur rimba legendaris tempat para lelembut terkuat berkumpul.",
      "Gunung Keramat" to "Kawah berkabut panas tempat Banaspati dan raksasa berkuasa.",
      "Istana Hantu" to "Tahta kerajaan gaib Nusantara — pertarungan puncak penakluk arwah.",
    )

  val stages: List<StageDef> =
    stageNames.mapIndexed { i, (name, sub) ->
      val w = linkedMapOf(
        GhostId.TUYUL to 5f,
        GhostId.POCONG to 3f,
        GhostId.API to (1f + i / 2f),
      )
      if (i >= 1) w[GhostId.KUNTI] = 1f + i / 3f
      if (i >= 2) w[GhostId.LEAK] = i / 2f
      if (i >= 3) w[GhostId.KUYANG] = i / 2f
      if (i >= 4) w[GhostId.GENDRUWO] = i / 2f
      if (i >= 5) {
        w[GhostId.WEWE] = 1f
        w[GhostId.PALASIK] = 1.2f
      }
      if (i >= 6) w[GhostId.SUNDEL] = i / 3f
      if (i >= 7) {
        w[GhostId.BUTO_IJO] = 1.1f
        w[GhostId.BARONG] = 0.9f
      }
      if (i >= 8) w[GhostId.RANGDA] = 1.1f
      if (i >= 9) w[GhostId.NYI_BLORONG] = 1.3f
      StageDef(
        index = i,
        name = name,
        subtitle = sub,
        tint = tints[i],
        baseHp = 450f + i * 80f,
        gapMin = max(1.8f, 6.5f - 0.5f * i),
        gapMax = max(3.0f, 9.0f - 0.6f * i),
        multiplier = 1f + 0.13f * i,
        reward = 40 + i * 25,
        weights = w,
      )
    }

  val tournaments: List<TournamentDef> =
    listOf(
      TournamentDef(0, "Piala Perunggu", "🥉", 0, 3, 1.0f, 150, Color(0xFFCD7F32)),
      TournamentDef(1, "Piala Perak", "🥈", 50, 4, 1.5f, 400, Color(0xFFCBD5E1)),
      TournamentDef(2, "Piala Emas", "🥇", 120, 5, 2.0f, 1000, Color(0xFFFBBF24)),
    )

  fun stageConfig(stageIndex: Int): BattleConfig {
    val st = stages[stageIndex.coerceIn(stages.indices)]
    return BattleConfig(
      mode = BattleModeType.STAGE,
      stageIndex = st.index,
      title = st.name,
      label = "Stage ${st.index + 1} · ${st.name}",
      tint = st.tint,
      bgIndex = st.index,
      enemyBaseHp = st.baseHp,
      gapMin = st.gapMin,
      gapMax = st.gapMax,
      enemyMul = st.multiplier,
      rewardCoins = st.reward,
      weights = st.weights,
    )
  }

  fun tournamentConfig(tIdx: Int, round: Int): BattleConfig {
    val t = tournaments[tIdx.coerceIn(tournaments.indices)]
    val isFinal = round == t.rounds - 1
    val weightIdx = (t.index * 2 + round + 1).coerceAtMost(9)
    return BattleConfig(
      mode = BattleModeType.TOURNAMENT,
      tournamentIndex = t.index,
      tournamentRound = round,
      title = t.name,
      label = "${t.name} · ${if (isFinal) "Final" else "Babak ${round + 1}/${t.rounds}"}",
      tint = t.color,
      bgIndex = if (isFinal) 9 else 5,
      enemyBaseHp = 500f + t.index * 250f + round * 120f,
      gapMin = max(1.8f, 5.5f - 0.4f * round - 0.6f * t.index),
      gapMax = max(3.0f, 7.5f - 0.5f * round - 0.6f * t.index),
      enemyMul = t.startMul + 0.2f * round,
      rewardCoins = 30 + 20 * round,
      weights = stages[weightIdx].weights,
    )
  }

  fun infinityConfig(level: Int): BattleConfig {
    val safeLv = max(1, level)
    val weightIdx = ((safeLv - 1) / 3).coerceAtMost(9)
    return BattleConfig(
      mode = BattleModeType.INFINITY,
      infinityLevel = safeLv,
      title = "Infinity Level $safeLv",
      label = "Infinity · Level $safeLv",
      tint = tints[(safeLv - 1) % 10],
      bgIndex = (safeLv - 1) % 10,
      enemyBaseHp = minOf(2500f, 420f + safeLv * 45f),
      gapMin = max(1.3f, 6.0f - 0.18f * safeLv),
      gapMax = max(2.2f, 8.5f - 0.22f * safeLv),
      enemyMul = 1f + 0.09f * safeLv,
      rewardCoins = 25 + 7 * safeLv,
      weights = stages[weightIdx].weights,
    )
  }

  fun freeConfig(stageIndex: Int, diff: FreeDifficulty): BattleConfig {
    val st = stages[stageIndex.coerceIn(stages.indices)]
    return BattleConfig(
      mode = BattleModeType.FREE,
      stageIndex = st.index,
      freeDifficulty = diff,
      title = st.name,
      label = "Bebas · ${st.name} · ${diff.label}",
      tint = st.tint,
      bgIndex = st.index,
      enemyBaseHp = diff.baseHp,
      gapMin = max(1.5f, 5.0f * diff.gapMul),
      gapMax = max(2.5f, 7.5f * diff.gapMul),
      enemyMul = diff.mul,
      rewardCoins = diff.reward,
      coinFactor = 0.5f,
      weights = stages[diff.weightStageIdx].weights,
    )
  }
}

data class AchievementDef(
  val id: String,
  val title: String,
  val description: String,
  val rewardCoins: Int,
  val progressProvider: (PlayerProgress) -> Pair<Int, Int>,
)

object AchievementCatalog {
  val all: List<AchievementDef> =
    listOf(
      AchievementDef("first_win", "Penjaga Kuburan", "Selesaikan Stage 1: Kuburan Tua", 60) {
        it.clearStage.coerceAtMost(1) to 1
      },
      AchievementDef("stage_5", "Penguasa Rimba", "Taklukkan hingga Stage 5: Jembatan Tua", 150) {
        it.clearStage.coerceAtMost(5) to 5
      },
      AchievementDef("stage_10", "Raja Alam Gaib", "Tamatkan semua 10 Stage cerita", 400) {
        it.clearStage.coerceAtMost(10) to 10
      },
      AchievementDef("collector", "Kolektor Pasukan", "Miliki seluruh 14 jenis hantu Nusantara", 450) {
        it.ownedGhosts.size.coerceAtMost(14) to 14
      },
      AchievementDef("mythic_duo", "Pemanggil Legenda", "Rekrut minimal 2 hantu Legendaris baru", 220) {
        it.ownedGhosts.count { g -> g.isMythicNew }.coerceAtMost(2) to 2
      },
      AchievementDef("tourn_champ", "Juara Turnamen", "Menangi minimal 1 piala di Mode Turnamen", 180) {
        it.totalTrophies.coerceAtMost(1) to 1
      },
      AchievementDef("inf_5", "Bertahan di Kegelapan", "Capai rekor Level 5 di Mode Infinity", 140) {
        it.infinityRecord.coerceAtMost(5) to 5
      },
      AchievementDef("inf_10", "Legenda Abadi", "Capai rekor Level 10 di Mode Infinity", 320) {
        it.infinityRecord.coerceAtMost(10) to 10
      },
      AchievementDef("slayer_100", "Pembasmi Lelembut", "Kalahkan total 100 hantu musuh", 160) {
        it.totalEnemiesDefeated.coerceAtMost(100) to 100
      },
    )
}

data class PlayerProgress(
  val coins: Int = 0,
  val clearStage: Int = 0,
  val infinityRecord: Int = 0,
  val trophyBronze: Int = 0,
  val trophySilver: Int = 0,
  val trophyGold: Int = 0,
  val ownedGhosts: Set<GhostId> = GhostId.entries.filter { it.isStarter }.toSet(),
  val ghostLevels: Map<GhostId, Int> = emptyMap(),
  val upgrades: Map<UpgradeId, Int> = UpgradeId.entries.associateWith { 0 },
  val inventory: Map<JimatId, Int> = emptyMap(),
  val claimedAchievements: Set<String> = emptySet(),
  val totalVictories: Int = 0,
  val totalEnemiesDefeated: Int = 0,
  val totalGhostsSummoned: Int = 0,
) {
  val totalTrophies: Int get() = trophyBronze + trophySilver + trophyGold

  val playerRank: String
    get() =
      when {
        clearStage >= 10 && trophyGold > 0 -> "Penguasa Alam Gaib"
        clearStage >= 7 || infinityRecord >= 10 -> "Pendekar Keramat"
        clearStage >= 4 || totalTrophies >= 1 -> "Dukun Muda"
        clearStage >= 1 -> "Pemburu Lelembut"
        else -> "Pendekar Pemula"
      }

  fun ghostLevel(id: GhostId): Int = (ghostLevels[id] ?: 0).coerceIn(0, 5)
  fun upgradeLevel(id: UpgradeId): Int = (upgrades[id] ?: 0).coerceIn(0, id.maxLevel)
  fun jimatCount(id: JimatId): Int = (inventory[id] ?: 0).coerceAtLeast(0)
  fun trophyCount(index: Int): Int =
    when (index) {
      0 -> trophyBronze
      1 -> trophySilver
      else -> trophyGold
    }
}

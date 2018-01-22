package karballo.bitboard

import karballo.log.Logger
import karballo.util.Utils

class BitboardAttacksMagic : BitboardAttacks() {

    // Mask = Attacks without border for magic bitboards
    var rookMask: LongArray
    var rookMagic: Array<LongArray?>
    var bishopMask: LongArray
    var bishopMagic: Array<LongArray?>

    init {
        logger.debug("Generating magic tables...")
        val time1 = Utils.instance.currentTimeMillis()
        rookMask = LongArray(64)
        rookMagic = arrayOfNulls<LongArray>(64)
        bishopMask = LongArray(64)
        bishopMagic = arrayOfNulls<LongArray>(64)

        var square: Long = 1
        var i = 0
        while (square != 0L) {
            rookMask[i] = squareAttackedAuxSliderMask(square, +8, BitboardUtils.b_u) or
                    squareAttackedAuxSliderMask(square, -8, BitboardUtils.b_d) or
                    squareAttackedAuxSliderMask(square, -1, BitboardUtils.b_r) or
                    squareAttackedAuxSliderMask(square, +1, BitboardUtils.b_l)

            bishopMask[i] = squareAttackedAuxSliderMask(square, +9, BitboardUtils.b_u or BitboardUtils.b_l) or
                    squareAttackedAuxSliderMask(square, +7, BitboardUtils.b_u or BitboardUtils.b_r) or
                    squareAttackedAuxSliderMask(square, -7, BitboardUtils.b_d or BitboardUtils.b_l) or
                    squareAttackedAuxSliderMask(square, -9, BitboardUtils.b_d or BitboardUtils.b_r)

            // And now generate magics
            val rookPositions = 1 shl rookShiftBits[i]
            rookMagic[i] = LongArray(rookPositions)
            for (j in 0..rookPositions - 1) {
                val pieces = generatePieces(j, rookShiftBits[i], rookMask[i])
                val magicIndex = magicTransform(pieces, rookMagicNumber[i], rookShiftBits[i])
                rookMagic[i]!![magicIndex] = getRookShiftAttacks(square, pieces)
            }

            val bishopPositions = 1 shl bishopShiftBits[i]
            bishopMagic[i] = LongArray(bishopPositions)
            for (j in 0..bishopPositions - 1) {
                val pieces = generatePieces(j, bishopShiftBits[i], bishopMask[i])
                val magicIndex = magicTransform(pieces, bishopMagicNumber[i], bishopShiftBits[i])
                bishopMagic[i]!![magicIndex] = getBishopShiftAttacks(square, pieces)
            }
            square = square shl 1
            i++
        }
        val time2 = Utils.instance.currentTimeMillis()
        logger.debug("Generated magic tables in " + (time2 - time1) + "ms")
    }

    /**
     * Fills pieces from a mask. Necessary for magic generation variable bits is
     * the mask bytes number index goes from 0 to 2^bits
     */
    private fun generatePieces(index: Int, bits: Int, mask: Long): Long {
        var m = mask
        var i = 0
        var lsb: Long
        var result = 0L
        while (i < bits) {
            lsb = m and -m
            m = m xor lsb // Deactivates lsb bit of the mask to get next bit next time
            if (index and (1 shl i) != 0)
                result = result or lsb // if bit is set to 1
            i++
        }
        return result
    }

    private fun squareAttackedAuxSliderMask(square: Long, shift: Int, border: Long): Long {
        var s = square
        var ret: Long = 0
        while (s and border == 0L) {
            if (shift > 0) {
                s = s shl shift
            } else {
                s = s ushr (-shift)
            }
            if (s and border == 0L) {
                ret = ret or s
            }
        }
        return ret
    }

    /**
     * Magic! attacks, very fast method
     */
    override fun getRookAttacks(index: Int, all: Long): Long {
        val i = magicTransform(all and rookMask[index], rookMagicNumber[index], rookShiftBits[index])
        return rookMagic[index]!![i]
    }

    override fun getBishopAttacks(index: Int, all: Long): Long {
        val i = magicTransform(all and bishopMask[index], bishopMagicNumber[index], bishopShiftBits[index])
        return bishopMagic[index]!![i]
    }

    companion object {
        private val logger = Logger.getLogger("BitboardAttacksMagic")

        val rookShiftBits = intArrayOf(
                12, 11, 11, 11, 11, 11, 11, 12,
                11, 10, 10, 10, 10, 10, 10, 11,
                11, 10, 10, 10, 10, 10, 10, 11,
                11, 10, 10, 10, 10, 10, 10, 11,
                11, 10, 10, 10, 10, 10, 10, 11,
                11, 10, 10, 10, 10, 10, 10, 11,
                11, 10, 10, 10, 10, 10, 10, 11,
                12, 11, 11, 11, 11, 11, 11, 12
        )

        val bishopShiftBits = intArrayOf(
                6, 5, 5, 5, 5, 5, 5, 6,
                5, 5, 5, 5, 5, 5, 5, 5,
                5, 5, 7, 7, 7, 7, 5, 5,
                5, 5, 7, 9, 9, 7, 5, 5,
                5, 5, 7, 9, 9, 7, 5, 5,
                5, 5, 7, 7, 7, 7, 5, 5,
                5, 5, 5, 5, 5, 5, 5, 5,
                6, 5, 5, 5, 5, 5, 5, 6
        )

        // Magic numbers generated with MagicNumbersGen
        val rookMagicNumber = longArrayOf(36028936609595408L, 18014467231055936L, 72075323679705152L, 36037595260518404L, 36033195199725570L, 72058693683905536L, 36030996050608384L, 2449958336881361156L, 140739637952512L, 70506451582976L, 4620834505194741760L, 141287512606720L, 4644371483853824L, 140746078552192L, 141287277723904L, 4644338206244992L, 35734132097160L, 148619062583230528L, 4503737200803872L, 282574623604770L, 1267187218122752L, 141287311278592L, 1103806857728L, 2199027483649L, 18067726970929152L, 4503875579027457L, 17628695363712L, 1155190898754584704L, 288239174400607232L, 2201179128832L, 1157427305413345536L, 576882966915989760L, 4647715090328461312L, 141012643094528L, 35185462612224L, 8798248898560L, 140771856483330L, 2201179128832L, -9223369837814677500L, 140738570486016L, 141012368392194L, 1152956690321129474L, 18049583150039168L, 17592320295040L, 281492290863120L, 2199090397312L, 4406636511488L, 554055106564L, 140738562105472L, 70437465751616L, 9015996422553664L, 144132784691118720L, 4400194519168L, 140754668355712L, 281492190134528L, 35751324566016L, 35463546044417L, 36064532220612865L, 1152939371807113473L, 17592320532737L, 281509336581137L, -9223090553153650687L, 2267759544324L, 1100115640386L)
        val bishopMagicNumber = longArrayOf(9016003945300000L, 73044964136779784L, 4575076475240464L, 1130368820445184L, 299342040662016L, 571836307869696L, 1126466911733824L, 563508308608004L, 158398670766144L, 4466775032960L, 4432842473504L, 4415503204352L, 72062009801179392L, 618744250400L, 2207648842752L, 4611687119147238400L, 2306973376155093024L, 4503634255577248L, 2251816995913744L, 1125902324989952L, -9223090526980923359L, 562954259005696L, 1127003780678160L, 2322172870131968L, 2322443570120704L, 285873560650241L, 70643689259520L, 563087526625312L, 4611968592916791296L, 2251937286226944L, 2306125583706492992L, 72199431583193088L, 290408777670656L, 1154065031059800352L, 36600560312516641L, 2201171263616L, 1161092868874368L, -9218797785015369472L, 10137498281871360L, 637717817917504L, 142971945623552L, 564067198566912L, -9223350045951130624L, 283543341056L, 8800455098432L, 563568437100800L, 19184287806914624L, 2287538240782848L, 598752935542785L, 45114371080192L, 288230927116009472L, -8070450531702144512L, 576460786933956608L, 17602118221824L, 289444795737243680L, 1127583550865984L, 18150819110912L, 2815866475643392L, 8606746690L, 2305844125909516800L, 72568842232960L, 146375943165837888L, 35193029664896L, 4613973006941692160L)

        fun magicTransform(b: Long, magic: Long, bits: Int): Int {
            return (b * magic).ushr(64 - bits).toInt()
        }
    }
}

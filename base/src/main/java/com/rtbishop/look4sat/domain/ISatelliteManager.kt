0x0000000000000000000000000000000000000000


interface ISatelliteManager {

    val calculatedPasses: SharedFlow<List<SatPass>>

    fun getPasses(): List<SatPass>

    suspend fun getPosition(sat: Satellite, pos: GeoPos, time: Long): SatPos

    suspend fun getTrack(sat: Satellite, pos: GeoPos, start: Long, end: Long): List<SatPos>

    suspend fun processRadios(
        sat: Satellite,
        pos: GeoPos,
        radios: List<SatRadio>,
        time: Long
    ): List<SatRadio>

    suspend fun processPasses(passList: List<SatPass>, time: Long): List<SatPass>

    suspend fun calculatePasses(
        satList: List<Satellite>,
        pos: GeoPos,
        time: Long,
        hoursAhead: Int = 1,
        minElevation: Double = 0
    )
}

package au.barney.tripkit.data.model

data class WeightDetails(
    val totalGearWeightGrams: Int,
    val extraPayloadWeightGrams: Int,
    val payloadAnalysis: List<PayloadAnalysis>
)

data class PayloadAnalysis(
    val location: PayloadLocation,
    val currentWeightGrams: Int
)

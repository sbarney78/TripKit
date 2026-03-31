package au.barney.tripkit.data.model

data class FullTripData(
    val list: ListItem,
    val itinerary: List<ItineraryItem>,
    val entries: List<Entry>,
    val allItems: List<Item>,
    val menu: List<MenuItem>,
    val ingredientGroups: List<IngredientGroup>,
    val allIngredients: List<Ingredient>
)

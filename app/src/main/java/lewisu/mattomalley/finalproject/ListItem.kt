package lewisu.mattomalley.finalproject

// simple data class for a note
data class ListItem(
    var uid: String = "",
    var title: String = "",
    var description: String = "",
    var category: String = "", // make List<String> Later
    var priority: Int = 0,
    var complete: Boolean = false
)
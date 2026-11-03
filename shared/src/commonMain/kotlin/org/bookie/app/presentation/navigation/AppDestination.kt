package org.bookie.app.presentation.navigation

enum class AppDestination(
    val route: String,
    val label: String,
) {
    HOME("home", "Inicio"),
    SEARCH("search", "Búsqueda"),
    LIBRARY("library", "Biblioteca"),
    COMMUNITY("community", "Comunidad"),
    PROFILE("profile", "Perfil");

    companion object {
        fun fromRoute(route: String?): AppDestination? =
            entries.firstOrNull { it.route == route }
    }
}

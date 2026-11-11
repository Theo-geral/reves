package com.theo.reves

import java.text.Normalizer
import java.util.Locale

/**
 * Tags automatiques par mots-clés (français), calculés sur le téléphone.
 * Un mot finissant par « * » est un préfixe (« tomb* » = tomber, tombais…),
 * sinon le mot doit correspondre exactement. Les expressions sont cherchées telles quelles.
 * Les accents sont ignorés. Volontairement simple : l'utilisateur corrige à la main.
 */
object AutoTags {

    private class Rule(val tag: String, val words: List<String>, val phrases: List<String> = emptyList())

    private val rules = listOf(
        Rule("Famille", listOf("maman", "papa", "mere", "pere", "frere*", "soeur*", "parent", "parents",
            "mamie", "papi", "papy", "grandmere", "grandpere", "cousin*", "oncle*", "tante*", "famille*"),
            listOf("grand mere", "grand pere")),
        Rule("Amis", listOf("ami", "amie", "amis", "amies", "pote", "potes", "copain*", "copine*")),
        Rule("Amour", listOf("amoureu*", "embrass*", "bisou*", "couple*", "mariage*", "epoux", "epouse")),
        Rule("Travail", listOf("travail*", "boulot", "bureau*", "collegue*", "reunion*", "patron*", "chef",
            "these", "labo*", "laser*", "manip", "experience*")),
        Rule("École / examen", listOf("ecole*", "examen*", "exam", "partiel*", "prof", "profs", "professeur*",
            "classe*", "lycee*", "college", "cours", "interro*", "diplome*")),
        Rule("Chute", listOf("tomb*", "chute*", "chuter", "falaise*"), listOf("dans le vide")),
        Rule("Voler / flotter", listOf("voler", "volais", "volait", "volions", "envol*", "flott*", "survol*",
            "planer", "planais", "planait", "levit*")),
        Rule("Poursuite", listOf("poursui*", "pourchass*", "fuir", "fuis", "fuyais", "fuyait", "fuite",
            "traque*", "echapp*", "courir", "courais", "courait", "courions")),
        Rule("Peur", listOf("peur*", "cauchemar*", "angoiss*", "terrifi*", "effray*", "panique*", "horreur*",
            "monstre*", "hurl*", "stress*")),
        Rule("Mort", listOf("mort", "morte", "morts", "mourir", "mourais", "mourait", "meurt", "decede*",
            "deces", "enterrement*", "cadavre*", "tuer", "tuait", "tue")),
        Rule("Eau / mer", listOf("mer", "ocean*", "plage*", "eau", "eaux", "nager", "nageais", "nageait",
            "piscine*", "vague*", "riviere*", "lac", "lacs", "noy*", "bateau*")),
        Rule("Animaux", listOf("chien*", "chat", "chats", "chaton*", "cheval", "chevaux", "oiseau*", "serpent*",
            "loup*", "araignee*", "poisson*", "animal", "animaux", "lion*", "ours", "souris", "rat", "rats",
            "insecte*", "requin*")),
        Rule("Voyage", listOf("voyage*", "avion*", "aeroport*", "gare", "gares", "valise*", "vacances",
            "etranger*", "hotel*", "pays")),
        Rule("Transports", listOf("voiture*", "conduire", "conduisais", "conduisait", "velo", "velos", "bus",
            "metro*", "moto", "motos", "route*", "train", "trains")),
        Rule("Maison", listOf("maison*", "appart*", "chambre*", "cuisine*", "salon*")),
        Rule("Sport", listOf("sport*", "triathlon*", "match*", "football", "rugby", "entrainement*",
            "marathon*", "competition*")),
        Rule("Nourriture", listOf("manger", "mangeais", "mangeait", "repas", "restau*", "gateau*",
            "dejeuner", "diner")),
        Rule("Fête", listOf("fete*", "soiree*", "anniversaire*", "concert*", "danse*", "danser")),
        Rule("Enfance", listOf("enfance", "enfant*", "gamin*")),
        Rule("Fantastique", listOf("magie*", "magique*", "sorci*", "dragon*", "extraterrestre*", "alien*",
            "fantome*", "zombie*", "teleport*", "superpouvoir*"), listOf("super pouvoir")),
        Rule("Perdu / en retard", listOf("retard*", "perdu", "perdue", "perdus", "egare*")),
        Rule("Dents", listOf("dent", "dents")),
        Rule("Joie", listOf("heureu*", "joie", "rire", "rigol*", "content", "contente")),
        Rule("Jeux vidéo", listOf("console*", "manette*"), listOf("jeu video", "jeux video")),
        Rule("Rêve lucide", listOf("lucide*"), listOf("savais que je revais", "compris que je revais",
            "conscience de rever", "conscience que je revais")),
    )

    fun normalize(s: String): String {
        val lower = s.lowercase(Locale.FRENCH).replace("œ", "oe").replace("æ", "ae")
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    }

    fun detect(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val norm = normalize(text)
        val tokens = Regex("[a-z]+").findAll(norm).map { it.value }.toSet()
        val spaced = " " + norm.replace(Regex("[^a-z]+"), " ") + " "
        return rules.filter { rule ->
            rule.words.any { w ->
                if (w.endsWith("*")) {
                    val prefix = w.dropLast(1)
                    tokens.any { it.startsWith(prefix) }
                } else {
                    w in tokens
                }
            } || rule.phrases.any { spaced.contains(" $it ") }
        }.map { it.tag }
    }
}

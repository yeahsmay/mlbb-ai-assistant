// language: Kotlin, file: HeroCounterEngine.kt
package com.example.mlbbassistant

object HeroCounterEngine {
    
    private val counterDB = mapOf(
        // ASSASSIN
        "fanny" to "Khufra, Chou, Saber, Phoveus, Kaja",
        "ling" to "Khufra, Saber, Franco, Kaja",
        "lancelot" to "Khufra, Chou, Ruby, Franco",
        "gusion" to "Khufra, Franco, Chou, Saber",
        "hayabusa" to "Khufra, Saber, Chou, Franco",
        "helcurt" to "Khufra, Chou, Lancelot, Wanwan",
        "karina" to "Chou, Franco, Kaja, Khufra",
        "selena" to "Lancelot, Chou, Khufra, Wanwan",
        "hanzo" to "Khufra, Chou, Saber, Kaja",
        "benedetta" to "Khufra, Phoveus, Chou, Ruby",
        "julian" to "Khufra, Chou, Phoveus, Franco",
        "joy" to "Khufra, Phoveus, Chou, Saber",
        "yin" to "Khufra, Chou, Akai, Franco",
        "aamon" to "Lancelot, Chou, Khufra, Saber",
        "harley" to "Chou, Khufra, Franco",
        "natalia" to "Chou, Khufra, Saber, Kaja",
        "lesley" to "Ling, Gusion, Fanny, Chou",
        "kadita" to "Khufra, Chou, Lancelot, Wanwan",
        
        // MARKSMAN
        "wanwan" to "Phoveus, Franco, Kaja, Chou",
        "beatrix" to "Ling, Fanny, Gusion, Chou",
        "claude" to "Chou, Franco, Kaja, Khufra",
        "brody" to "Ling, Fanny, Gusion, Khufra",
        "melissa" to "Ling, Gusion, Khufra, Chou",
        "irithel" to "Ling, Fanny, Gusion, Chou",
        "hanabi" to "Ling, Fanny, Gusion, Chou",
        "clint" to "Ling, Fanny, Gusion, Khufra",
        "granger" to "Chou, Khufra, Ling, Fanny",
        "moskov" to "Ling, Fanny, Gusion, Khufra",
        "karrie" to "Ling, Fanny, Gusion, Franco",
        "miya" to "Ling, Fanny, Gusion, Chou",
        "layla" to "Ling, Fanny, Gusion, Franco",
        "bruno" to "Ling, Fanny, Gusion, Khufra",
        "natan" to "Ling, Fanny, Gusion, Khufra",
        "popol" to "Ling, Fanny, Gusion, Chou",
        
        // MAGE
        "lunox" to "Saber, Hanzo, Ling, Franco",
        "harith" to "Khufra, Chou, Kaja, Franco",
        "lylia" to "Lancelot, Chou, Khufra, Saber",
        "pharsa" to "Ling, Fanny, Gusion, Chou",
        "valentina" to "Saber, Franco, Kaja, Chou",
        "cecilion" to "Ling, Fanny, Gusion, Chou",
        "yve" to "Ling, Fanny, Gusion, Lancelot",
        "kagura" to "Khufra, Chou, Franco, Kaja",
        "gord" to "Ling, Fanny, Gusion, Chou",
        "eudora" to "Lancelot, Chou, Khufra",
        "aurora" to "Lancelot, Chou, Khufra",
        "vexana" to "Lancelot, Chou, Khufra, Saber",
        "zhask" to "Ling, Chou, Khufra, Lancelot",
        "chang'e" to "Ling, Fanny, Gusion, Chou",
        "cici" to "Ling, Fanny, Gusion, Khufra",
        "xavier" to "Ling, Fanny, Gusion, Chou",
        "novaria" to "Ling, Fanny, Gusion, Chou",
        "zhuxin" to "Ling, Fanny, Gusion, Chou",
        "lukas" to "Khufra, Chou, Saber, Phoveus",

        // FIGHTER
        "chou" to "Phoveus, Ruby, Khufra, Kaja",
        "aldous" to "Khufra, Chou, Kaja, Ruby",
        "yu zhong" to "Khufra, Chou, Baxia, Kaja",
        "paquito" to "Khufra, Phoveus, Chou, Ruby",
        "phoveus" to "Chou, Khufra, Wanwan, Esmeralda",
        "terizla" to "Wanwan, Phoveus, Khufra, Chou",
        "dyrroth" to "Wanwan, Phoveus, Khufra, Chou",
        "argus" to "Wanwan, Khufra, Chou, Kaja",
        "leomord" to "Wanwan, Khufra, Chou, Phoveus",
        "badang" to "Wanwan, Phoveus, Chou, Khufra",
        "freya" to "Wanwan, Phoveus, Khufra, Chou",
        "jawhead" to "Chou, Khufra, Franco, Kaja",
        "ruby" to "Wanwan, Phoveus, Khufra, Chou",
        "silvanna" to "Wanwan, Phoveus, Khufra, Chou",
        "masha" to "Wanwan, Phoveus, Khufra, Chou",
        "khaleed" to "Wanwan, Phoveus, Khufra, Chou",
        "lapu" to "Wanwan, Phoveus, Khufra, Chou",
        "balmond" to "Wanwan, Phoveus, Khufra, Chou",
        "alucard" to "Wanwan, Khufra, Chou, Kaja",
        "aulus" to "Wanwan, Phoveus, Khufra, Chou",
        "thamus" to "Wanwan, Phoveus, Khufra, Chou",
        "yuzhong" to "Khufra, Chou, Baxia, Kaja", 
        "arlott" to "Wanwan, Phoveus, Khufra, Chou",
        "fredrinn" to "Wanwan, Phoveus, Khufra, Chou",

        // TANK / SUPPORT
        "khufra" to "Chou, Phoveus, Wanwan, Kaja",
        "franco" to "Chou, Wanwan, Lancelot, Khufra",
        "kaja" to "Chou, Wanwan, Lancelot, Khufra",
        "atlas" to "Chou, Wanwan, Khufra, Franco",
        "tigreal" to "Chou, Wanwan, Khufra, Franco",
        "grock" to "Chou, Wanwan, Khufra, Phoveus",
        "akai" to "Chou, Wanwan, Khufra, Phoveus",
        "baxia" to "Chou, Wanwan, Khufra, Phoveus",
        "hylos" to "Chou, Wanwan, Khufra, Baxia",
        "belerick" to "Chou, Wanwan, Khufra, Baxia",
        "gatotkaca" to "Chou, Wanwan, Khufra, Baxia",
        "minotaur" to "Chou, Wanwan, Khufra, Franco",
        "lolita" to "Chou, Wanwan, Khufra, Franco",
        "estes" to "Baxia, Lylia, Kaja (Beli Necklace)",
        "angela" to "Kaja, Franco, Baxia, Chou",
        "rafaela" to "Kaja, Franco, Baxia, Chou",
        "diggie" to "Kaja, Franco, Chou, Khufra",
        "mathilda" to "Chou, Khufra, Franco, Kaja",
        "floryn" to "Kaja, Franco, Baxia, Chou",
        "carmilla" to "Chou, Wanwan, Khufra, Baxia",
        "chop" to "Chou, Wanwan, Khufra, Baxia" // placeholder
    )

    fun getCounters(draftText: String): String {
        val lowerText = draftText.lowercase()
        val foundCounters = mutableListOf<String>()

        for ((hero, counterList) in counterDB) {
            if (lowerText.contains(hero)) {
                // Biar rapi, huruf depannya dibikin kapital
                val heroName = hero.split(" ").joinToString(" ") { 
                    it.replaceFirstChar { char -> char.uppercase() } 
                }
                foundCounters.add("$heroName ➔ $counterList")
            }
        }

        return if (foundCounters.isNotEmpty()) {
            "COUNTER PICK:\n" + foundCounters.joinToString("\n")
        } else {
            "Membaca draft musuh..."
        }
    }
}

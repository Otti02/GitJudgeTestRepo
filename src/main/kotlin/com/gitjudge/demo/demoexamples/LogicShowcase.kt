package com.gitjudge.demo.demoexamples

class LogicShowcase {
    fun lastTitle(titles: List<String>): String {
        return titles[titles.size]
    }

    fun completionRatio(doneCount: Int, totalCount: Int): Int {
        return doneCount / totalCount
    }
}
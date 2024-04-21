package com.planeat.planeat.ai.client


/** An immutable result returned by a TextClassifier describing what was classified.  */
class Result(
    /**
     * A unique identifier for what has been classified. Specific to the class, not the instance of
     * the object.
     */
    val id: String?,
    /** Display name for the result.  */
    var title: String?, confidence: Float?
) :
    Comparable<Result?> {

    /** A sortable score for how good the result is relative to others. Higher should be better.  */
    val confidence: Float?

    init {
        title = title
        this.confidence = confidence
    }

    override fun toString(): String {
        var resultString = ""
        if (id != null) {
            resultString += "[$id] "
        }
        if (title != null) {
            resultString += "$title "
        }
        if (confidence != null) {
            resultString += String.format("(%.1f%%) ", confidence * 100.0f)
        }
        return resultString.trim { it <= ' ' }
    }

    override fun compareTo(o: Result?): Int {
        if (o != null) {
            return o?.confidence!!.compareTo(confidence!!)
        } else {
            return 0
        }
    }
}
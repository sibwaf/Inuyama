package ru.dyatel.inuyama.model

import com.google.gson.annotations.SerializedName
import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany

data class RuranobeProject(
        @Id(assignable = true) @SerializedName("projectId") var id: Long = 0,

        var title: String = "",
        var author: String = "",

        var works: Boolean = false,

        var status: String = "",
        var issueStatus: String? = "",
        var translationStatus: String? = ""
) {
    @Backlink lateinit var volumes: ToMany<RuranobeVolume>
}
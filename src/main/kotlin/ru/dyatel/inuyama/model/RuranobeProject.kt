package ru.dyatel.inuyama.model

import com.google.gson.annotations.SerializedName
import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import ru.dyatel.inuyama.utilities.NoJson

@Entity
data class RuranobeProject(
        @Id(assignable = true) @SerializedName("projectId") var id: Long = 0,
        var url: String = "",

        var title: String = "",
        @SerializedName("nameRomaji") var titleRomaji: String? = null,
        var author: String = "",

        var works: Boolean = false,

        var status: String = "",
        var issueStatus: String? = "",
        var translationStatus: String? = "",

        @NoJson var watching: Boolean = false
) {
    @NoJson lateinit var directory: ToOne<Directory>
    @NoJson @Backlink lateinit var volumes: ToMany<RuranobeVolume>
}
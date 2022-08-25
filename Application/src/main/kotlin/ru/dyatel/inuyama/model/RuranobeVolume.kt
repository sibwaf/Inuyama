package ru.dyatel.inuyama.model

import com.google.gson.annotations.SerializedName
import hirondelle.date4j.DateTime
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne
import ru.dyatel.inuyama.utilities.DateTimeConverter
import ru.sibwaf.inuyama.common.utilities.gson.NoJson

@Entity
data class RuranobeVolume(
    @Id(assignable = true) @SerializedName("volumeId") var id: Long = 0,
    @NoJson var order: Int = 0,

    var url: String = "",
    @NoJson var coverUrl: String? = null,
    @SerializedName("nameTitle") var title: String = "",
    @SerializedName("volumeStatus") var status: String = "",

    @NoJson @Convert(converter = DateTimeConverter::class, dbType = String::class)
    var updateDatetime: DateTime? = null,

    @NoJson var dispatched: Boolean = false
) {
    @NoJson
    lateinit var project: ToOne<RuranobeProject>
}

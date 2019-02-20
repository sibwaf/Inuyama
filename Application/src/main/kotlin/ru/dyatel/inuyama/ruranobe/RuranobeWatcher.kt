package ru.dyatel.inuyama.ruranobe

import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.UpdateDispatcher
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeProject_
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.model.RuranobeVolume_
import ru.dyatel.inuyama.model.Update
import java.util.TimeZone

class RuranobeWatcher(override val kodein: Kodein) : Watcher(), KodeinAware {

    private val api by instance<RuranobeApi>()

    private val boxStore by instance<BoxStore>()
    private val projectBox by instance<Box<RuranobeProject>>()
    private val volumeBox by instance<Box<RuranobeVolume>>()

    private val watchingQuery by lazy {
        projectBox.query()
                .equal(RuranobeProject_.watching, true)
                .build()
    }

    private val undispatchedQuery by lazy {
        volumeBox.query()
                .equal(RuranobeVolume_.dispatched, false)
                .build()
    }

    fun syncProjects() {
        val projects = try {
            api.fetchProjects()
        } catch (e: Exception) {
            return
        }

        boxStore.runInTx {
            for (project in projects) {
                val persisted = projectBox[project.id]
                if (persisted != null) {
                    project.watching = persisted.watching
                }
            }

            projectBox.put(projects)
        }
    }

    fun syncVolumes(project: RuranobeProject): Boolean {
        val volumes = try {
            api.fetchVolumes(project)
        } catch (e: Exception) {
            return false
        }

        var updated = false

        boxStore.runInTx {
            for (volume in volumes) {
                val persisted = volumeBox[volume.id]
                if (persisted == null) {
                    updated = true
                    continue
                }

                val persistedUpdate = persisted.updateDatetime
                val refreshedUpdate = volume.updateDatetime

                if (refreshedUpdate != null && (persistedUpdate == null || refreshedUpdate.gt(persistedUpdate))) {
                    updated = true
                } else {
                    volume.updateDatetime = persistedUpdate
                    volume.dispatched = persisted.dispatched
                }
            }

            volumeBox.put(volumes)
        }

        return updated
    }

    override fun checkUpdates(): List<String> {
        val updates = mutableListOf<String>()

        boxStore.runInTx {
            for (project in watchingQuery.find()) {
                if (syncVolumes(project)) {
                    updates.add(project.title)
                }
            }
        }

        return updates
    }

    override fun dispatchUpdates(dispatcher: UpdateDispatcher) {
        // TODO
    }

    override fun listUpdates(): List<Update> {
        return volumeBox.all
                .filter { it.project.target.watching }
                .mapNotNull { update ->
                    update.updateDatetime?.let {
                        Update(update.title, it.getMilliseconds(TimeZone.getDefault()))
                    }
                }
                .distinctBy { it.description }
    }

}

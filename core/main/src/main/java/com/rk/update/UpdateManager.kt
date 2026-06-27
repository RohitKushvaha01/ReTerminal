package com.rk.update

import android.content.Context
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.localBinDir
import java.io.File

class UpdateManager(private val context: Context) {
    fun onUpdate() {
        with(context) {
            val initFile: File = localBinDir().child("init-host")
            if (initFile.exists()) {
                initFile.delete()
            }

            if (initFile.exists().not()) {
                initFile.createFileIfNot()
                assets.open("init-host.sh").bufferedReader().use { it.readText() }.let {
                    initFile.writeText(it)
                }
            }

            val initFilex: File = localBinDir().child("init")
            if (initFilex.exists()) {
                initFilex.delete()
            }

            if (initFilex.exists().not()) {
                initFilex.createFileIfNot()
                assets.open("init.sh").bufferedReader().use { it.readText() }.let {
                    initFilex.writeText(it)
                }
            }
        }
    }
}

package com.appunite.firebasetestlabplugin.cloud

import com.appunite.firebasetestlabplugin.FirebaseTestLabPlugin
import com.appunite.firebasetestlabplugin.model.ResultTypes
import com.appunite.firebasetestlabplugin.utils.asCommand
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File


internal class CloudTestResultDownloader(
        private val sdk: FirebaseTestLabPlugin.Sdk,
        private val resultsTypes: ResultTypes,
        private val gCloudDirectory: File,
        private val resultsPath: File,
        private val gCloudBucketName: String,
        private val logger: Logger) {

    /**
     * Get all test results of types resultsTypes
     */
    fun getResults() {
        if (!resultsTypes.junit && !resultsTypes.logcat && !resultsTypes.video && !resultsTypes.xml) {
            return
        }
        val gCloudFullPath = "$gCloudBucketName/$gCloudDirectory"
        logger.lifecycle("DOWNLOAD: Downloading results from $gCloudFullPath")

        prepareDownloadDirectory()
        downloadTestResults()
    }

    private fun prepareDownloadDirectory() {
        resultsPath.mkdirs()
        if (!resultsPath.exists()) {
            throw GradleException("Issue when creating destination dir $gCloudDirectory")
        }
    }

    private fun downloadTestResults() {
        val excludeQuery = StringBuilder().append(".*\\.txt$|.*\\.apk$")
        if (!resultsTypes.xml) {
            excludeQuery.append("|.*\\.xml$")
        }
        if (!resultsTypes.xml) {
            excludeQuery.append("|.*\\.results$")
        }
        if (!resultsTypes.logcat) {
            excludeQuery.append("|.*\\logcat$")
        }
        if (!resultsTypes.video) {
            excludeQuery.append("|.*\\.mp4$")
        }
        excludeQuery.append("|.*\\.txt$").toString()
        // Download requested files under the results directory using multiple threads (-m)
        val processCreator = ProcessBuilder(sdk.gsutil.absolutePath,
                "-m", "rsync",
                "-x", excludeQuery.toString(),
                "-r", "gs://$gCloudBucketName/$gCloudDirectory", resultsPath.absolutePath)
        val process = processCreator.start()

        process.errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        process.waitFor()
    }

    fun clearResultsDir() {
        // Delete all files under the results directory using multiple threads (-m)
        val processCreator = ProcessBuilder(sdk.gsutil.absolutePath, "-m",
                "rm", "gs://$gCloudBucketName/$gCloudDirectory/**")
        val process = processCreator.start()

        process.errorStream.bufferedReader().forEachLine { logger.lifecycle(it) }
        process.inputStream.bufferedReader().forEachLine { logger.lifecycle(it) }

        process.waitFor()
    }
}
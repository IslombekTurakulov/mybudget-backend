package ru.iuturakulov.mybudgetbackend.utils

import java.io.InputStreamReader
import java.util.Locale
import java.util.MissingResourceException
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

class UTF8Control : ResourceBundle.Control() {
    override fun newBundle(
        baseName: String,
        locale: Locale,
        format: String,
        loader: ClassLoader,
        reload: Boolean
    ): ResourceBundle {
        val bundleName = toBundleName(baseName, locale)
        val resourceName = toResourceName(bundleName, "properties")
        val stream = if (reload) {
            loader.getResource(resourceName)?.openConnection()?.apply { useCaches = false }?.getInputStream()
        } else {
            loader.getResourceAsStream(resourceName)
        } ?: throw MissingResourceException("Missing $resourceName", baseName, "")

        return PropertyResourceBundle(InputStreamReader(stream, Charsets.UTF_8))
    }
}

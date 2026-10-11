package com.wstxda.lineageicons

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Xml
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.xmlpull.v1.XmlPullParser

/**
 * Browses every icon in the pack and hands the chosen one back to a launcher.
 *
 * Launchers with their own picker (Lawnchair, Kvaesitso, ...) never start this
 * activity - they read `res/xml/drawable.xml` straight out of the APK. The
 * rest start us with a pick action and expect the icon back as a result.
 */
class MainActivity : Activity() {

    private val density get() = resources.displayMetrics.density

    private val isPickMode: Boolean
        get() {
            val startedWith = intent ?: return false
            val action = startedWith.action.orEmpty()
            val categories = startedWith.categories.orEmpty()

            return when {
                action.endsWith("ACTION_PICK_ICON") -> true
                action.endsWith("PICK_ICON") -> true
                action == LAWNCHAIR_LEGACY -> true
                action == NOVA_THEME -> NOVA_CUSTOM_ICON_PICKER in categories
                else -> false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    // region ui

    private fun buildContent(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad(), pad(), pad(), pad())
        }

        if (!isPickMode) {
            content.addView(
                TextView(this).apply {
                    setText(R.string.picker_hint)
                    setTextSize(TEXT_SIZE_SMALL)
                    setPadding(0, 0, 0, pad(12))
                }
            )
        }

        categories().forEach { category ->
            content.addView(
                TextView(this).apply {
                    text = category.title
                    setTextSize(TEXT_SIZE_LARGE)
                    setPadding(0, pad(16), 0, pad(8))
                }
            )

            val rows = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            category.icons.chunked(perRow()).forEach { row ->
                rows.addView(
                    LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        row.forEach { addIcon(this, it) }
                        // Keep a short last row left-aligned.
                        repeat(perRow() - row.size) { addView(View(this@MainActivity)) }
                    }
                )
            }
            content.addView(rows)
        }

        return ScrollView(this).apply { addView(content) }
    }

    private fun addIcon(parent: LinearLayout, icon: Icon) {
        parent.addView(
            ImageView(this).apply {
                setImageDrawable(icon.drawable())
                scaleType = ImageView.ScaleType.FIT_CENTER
                isClickable = true
                contentDescription = icon.name
                setOnClickListener { onIconSelected(icon) }
                layoutParams = LinearLayout.LayoutParams(cell(), cell()).apply {
                    rightMargin = pad(8)
                }
            }
        )
    }

    private fun cell() = (ICON_CELL_DP * density).toInt()

    private fun perRow() =
        (resources.displayMetrics.widthPixels / cell()).coerceAtLeast(MIN_COLUMNS)

    private fun pad(dp: Int = BASE_PADDING_DP) = (dp * density).toInt()

    private fun onIconSelected(icon: Icon) {
        if (isPickMode) {
            setResult(RESULT_OK, icon.asResult())
            finish()
        } else {
            AlertDialog.Builder(this)
                .setTitle(icon.name)
                .setView(
                    ImageView(this).apply {
                        setImageDrawable(icon.drawable())
                        setPadding(pad(24), pad(24), pad(24), pad(24))
                    }
                )
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    // endregion

    // region pick-icon result protocol

    // EXTRA_SHORTCUT_ICON* are deprecated in the modern shortcut APIs, but they
    // remain the established contract between launchers and icon packs.
    @Suppress("DEPRECATION")
    private fun Icon.asResult(): Intent {
        val bitmap = asBitmap()

        // The same icon under every extra key the known launchers read, so the
        // pack works with whichever of them started us.
        return Intent()
            .putExtra(EXTRA_ICON_ADW, bitmap)
            .putExtra(EXTRA_ICON_NOVA, bitmap)
            .putExtra(EXTRA_ICON_LAWNCHAIR, bitmap)
            .putExtra(EXTRA_ICON_ONEPLUS, bitmap)
            .putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap)
            // Preferable form: lets the launcher re-render the adaptive icon at
            // its native density instead of rescaling our bitmap.
            .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, name)
            .putExtra(EXTRA_PACKAGE_NAME, packageName)
    }

    private fun Icon.asBitmap(): Bitmap {
        val size = (192 * density / 2f).toInt().coerceIn(MIN_BITMAP, MAX_BITMAP)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            drawable()?.apply {
                setBounds(0, 0, size, size)
                draw(Canvas(bitmap))
            }
        }
    }

    // endregion

    // region drawable.xml

    private class Icon(val name: String, val resId: Int)

    private class Category(val title: String, val icons: MutableList<Icon> = mutableListOf())

    private fun Icon.drawable(): Drawable? =
        runCatching { resources.getDrawable(resId, theme) }.getOrNull()

    private fun categories(): List<Category> {
        val found = mutableListOf<Category>()
        var current: Category? = null

        val parser = resources.getXml(R.xml.drawable)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "category" -> current =
                        Category(parser.getAttributeValue(null, "title")).also(found::add)
                    "item" -> {
                        val name = parser.getAttributeValue(null, "drawable")
                        val resId = name?.let {
                            resources.getIdentifier(it, "drawable", packageName)
                        } ?: 0
                        if (resId != 0) current?.icons?.add(Icon(name!!, resId))
                    }
                }
            }
            event = parser.next()
        }
        return found.filter { it.icons.isNotEmpty() }
    }

    // endregion

    private companion object {
        const val TEXT_SIZE_SMALL = 14f
        const val TEXT_SIZE_LARGE = 16f
        const val BASE_PADDING_DP = 16
        const val ICON_CELL_DP = 64
        const val MIN_COLUMNS = 3
        const val MIN_BITMAP = 144
        const val MAX_BITMAP = 512

        const val LAWNCHAIR_LEGACY = "ch.deletescape.lawnchair.ICONPACK"
        const val NOVA_THEME = "com.novalauncher.THEME"
        const val NOVA_CUSTOM_ICON_PICKER = "com.novalauncher.category.CUSTOM_ICON_PICKER"

        const val EXTRA_ICON_ADW = "org.adw.launcher.icons.EXTRA_ICON"
        const val EXTRA_ICON_NOVA = "com.novalauncher.EXTRA_ICON"
        const val EXTRA_ICON_LAWNCHAIR = "ch.deletescape.lawnchair.EXTRA_ICON"
        const val EXTRA_ICON_ONEPLUS = "net.oneplus.launcher.icons.EXTRA_ICON"
        const val EXTRA_PACKAGE_NAME = "com.novalauncher.EXTRA_PACKAGE_NAME"
    }
}
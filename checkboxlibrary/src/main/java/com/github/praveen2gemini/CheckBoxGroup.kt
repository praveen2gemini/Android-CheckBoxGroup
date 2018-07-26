package com.github.praveen2gemini

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Build
import android.support.annotation.ColorInt
import android.support.annotation.IdRes
import android.support.annotation.StyleRes
import android.support.v4.content.ContextCompat
import android.support.v4.widget.CompoundButtonCompat
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.autofill.AutofillManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import com.praveen2gemini.design.R
import java.util.*


/**
 * @author Praveen on 26/07/18.
 */
class CheckBoxGroup : LinearLayout {


    private val mCheckedIds = ArrayList<Int>()
    // tracks children checkbox buttons checked state
    private lateinit var mChildOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener
    private lateinit var mPassThroughListener: PassThroughHierarchyChangeListener
    private var mOnCheckedChangeListener: CheckBoxGroup.OnCheckedChangeListener? = null

    private var isCheckGroupHeaderEnabled: Boolean = false

    private var checkGroupHeaderTitle: String? = null

    @ColorInt
    private var checkGroupHeaderTitleColor: Int? = null

    @ColorInt
    private var checkGroupHeaderBackgroundColor: Int? = null

    private var innerMarginValue: Int? = null

    private var checkGroupHeaderMaxLines: Int = Int.MAX_VALUE

    private var checkGroupBoxTextSize: Float? = null

    @StyleRes
    private var checkGroupBoxTextAppearance: Int? = null

    /**
     *
     * Returns the identifier of the selected checkbox button in this group.
     * Upon empty selection, the returned value is -1.
     *
     * @return the unique id of the selected checkbox button in this group
     * @attr ref android.R.styleable#checkboxGroup_checkedButton
     * @see .check
     * @see .clearCheck
     */
    val checkedCheckboxButtonIds: ArrayList<Int>
        @IdRes
        get() = ArrayList(HashSet(mCheckedIds))

    constructor(context: Context) : super(context) {
        if (!isInEditMode) {
            init(null)
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        if (!isInEditMode) {
            init(attrs)
        }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        if (!isInEditMode) {
            // checkboxGroup is important by default, unless app developer overrode attribute.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (importantForAutofill == View.IMPORTANT_FOR_AUTOFILL_AUTO) {
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                }
            }
            init(attrs)
        }
    }


    private fun init(attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.CheckBoxGroup,
                0, 0)
        try {
            isCheckGroupHeaderEnabled = a.getBoolean(R.styleable.CheckBoxGroup_isCheckGroupHeaderEnabled, false)
            checkGroupHeaderTitle = a.getString(R.styleable.CheckBoxGroup_checkGroupHeaderTitle)
            checkGroupHeaderTitleColor = a.getColor(R.styleable.CheckBoxGroup_checkGroupHeaderTextColor, Color.BLACK)
            checkGroupHeaderBackgroundColor = a.getColor(R.styleable.CheckBoxGroup_checkGroupHeaderBackgroundColor, Color.TRANSPARENT)
            innerMarginValue = a.getDimensionPixelOffset(R.styleable.CheckBoxGroup_checkGroupChildInnerMargin, 10)
            checkGroupBoxTextSize = a.getDimension(R.styleable.CheckBoxGroup_checkGroupHeaderTextSize, -1f)
            checkGroupHeaderMaxLines = a.getInteger(R.styleable.CheckBoxGroup_checkGroupHeaderMaxLines, Int.MAX_VALUE)
            checkGroupBoxTextAppearance = a.getResourceId(
                    R.styleable.CheckBoxGroup_checkGroupHeaderTextAppearance, -1)
        } finally {
            a.recycle();
        }
        mChildOnCheckedChangeListener = CheckedStateTracker()
        mPassThroughListener = PassThroughHierarchyChangeListener()
        super.setOnHierarchyChangeListener(mPassThroughListener)
    }

    @ColorInt
    private fun findAccentColor(): Int {
        var a: TypedArray? = null
        try {
            val typedValue = TypedValue()
            a = context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
            return a.getColor(0, Color.BLACK) // Default BLACK
        } finally {
            a?.recycle()
        }
    }

    /**
     *
     * Sets the selection to the checkbox button whose identifier is passed in
     * parameter. Using -1 as the selection identifier clears the selection;
     * such an operation is equivalent to invoking
     *
     * @param id the unique id of the checkbox button to select in this group
     */
    fun check(@IdRes id: Int) {
        val isChecked = mCheckedIds.contains(id)
        if (id != -1) {
            setCheckedStateForView(id, isChecked)
        }
        setCheckedId(id, isChecked)
    }


    private fun setCheckedId(@IdRes id: Int, isChecked: Boolean) {
        if (id == DEFAULT_HEADER_ID) {
            for (i in 1..childCount) {
                this@CheckBoxGroup.getChildAt(i)?.let {
                    val headerCheckBox = it as CheckBox
                    headerCheckBox.isChecked = isChecked
                }
            }
        } else {
            if (mCheckedIds.contains(id) && !isChecked) {
                mCheckedIds.remove(id)
            } else {
                mCheckedIds.add(id)
            }
        }
        val headerCheckBox = this@CheckBoxGroup.getChildAt(0) as CheckBox

        when {
            checkedCheckboxButtonIds.size == 0 -> {
                headerCheckBox.isActivated = false
                headerCheckBox.isChecked = false
            }
            checkedCheckboxButtonIds.size == childCount - 1 -> {
                headerCheckBox.isActivated = false
                headerCheckBox.isChecked = true
            }
            checkedCheckboxButtonIds.size in 1..(childCount - 2) -> {
                headerCheckBox.isActivated = true
                CompoundButtonCompat.setButtonTintList(headerCheckBox, ColorStateList.valueOf(findAccentColor()))
            }
        }

        mOnCheckedChangeListener?.onCheckedChanged(this, id, isChecked)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val afm = context.getSystemService(AutofillManager::class.java)
            afm?.notifyValueChanged(this)
        }
    }

    private fun setCheckedStateForView(@IdRes viewId: Int, checked: Boolean) {
        val checkedView = findViewById<View>(viewId)
        if (checkedView != null && checkedView is CheckBox) {
            checkedView.isChecked = checked
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return CheckBoxGroup.LayoutParams(context, attrs)
    }

    override fun setOnHierarchyChangeListener(listener: ViewGroup.OnHierarchyChangeListener) {
        mPassThroughListener.mOnHierarchyChangeListener = listener
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        for (mCheckedId in mCheckedIds) {
            // checks the appropriate checkbox button as requested in the XML file
            if (mCheckedId <= 0) continue
            setCheckedStateForView(mCheckedId, true)
            setCheckedId(mCheckedId, true)
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        addCheckBoxHeader(0, params)
        val marginAddedParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        if (child is CheckBox) {
            Log.e("***********", "checkboz added " + child.text)
            Log.e("***********", "checkboz this.childCount " + this.childCount)
            innerMarginValue?.let {
                marginAddedParams.leftMargin = getInnerChildMargin(it)
                if (params is LinearLayout.LayoutParams) {
                    marginAddedParams.rightMargin = params.rightMargin
                    marginAddedParams.topMargin = params.topMargin
                    marginAddedParams.bottomMargin = params.bottomMargin
                }
            }
            CompoundButtonCompat.setButtonTintList(child, ColorStateList.valueOf(findAccentColor()))
            if (child.isChecked) {
                setCheckedId(child.id, true)
            }
        }
        super.addView(child, index, marginAddedParams)
    }


    private fun getInnerChildMargin(dpValue: Int): Int {
        val d = context.resources.displayMetrics.density
        return (dpValue * d).toInt() // margin in pixels
    }

    private fun addCheckBoxHeader(index: Int, params: ViewGroup.LayoutParams?) {
        params?.let {
            if (childCount == 0 && isCheckGroupHeaderEnabled) {
                val headerBox = CheckBox(context)
                headerBox.id = DEFAULT_HEADER_ID
                headerBox.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                checkGroupHeaderTitleColor?.let { headerBox.setTextColor(it) }
                checkGroupHeaderBackgroundColor?.let { headerBox.setBackgroundColor(it) }
                headerBox.text = checkGroupHeaderTitle
                headerBox.maxLines = checkGroupHeaderMaxLines
                checkGroupBoxTextAppearance?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        headerBox.setTextAppearance(it)
                    } else {
                        headerBox.setTextAppearance(context, it)
                    }
                }
                checkGroupBoxTextSize?.let {
                    if (it != -1f) {
                        headerBox.textSize = it
                    }
                }
                headerBox.buttonDrawable = ContextCompat.getDrawable(context, R.drawable.checkbox_selector)
                CompoundButtonCompat.setButtonTintList(headerBox, ColorStateList.valueOf(findAccentColor()))
                super.addView(headerBox, index, it)
            }
        }
    }

    /**
     *
     * Clears the selection. When the selection is cleared, no checkbox button
     * in this group is selected
     * null.
     */
    fun clearCheck() {
        check(-1)
    }

    /**
     *
     * Register a callback to be invoked when the checked checkbox button
     * changes in this group.
     *
     * @param listener the callback to call on checked state change
     */
    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener) {
        mOnCheckedChangeListener = listener
    }

    /**
     *
     * Interface definition for a callback to be invoked when the checked
     * checkbox button changed in this group.
     */
    interface OnCheckedChangeListener {
        /**
         *
         * Called when the checked checkbox button has changed. When the
         * selection is cleared, checkedId is -1.
         *
         * @param group     the group in which the checked checkbox button has changed
         * @param checkedId the unique identifier of the newly checked checkbox button
         */
        fun onCheckedChanged(group: CheckBoxGroup, @IdRes checkedId: Int, isChecked: Boolean)
    }

    /**
     *
     * This set of layout parameters defaults the width and the height of
     * the children to [.WRAP_CONTENT] when they are not specified in the
     * XML file. Otherwise, this class ussed the value read from the XML file.
     *
     *
     * for a list of all child view attributes that this class supports.
     */
    class LayoutParams : LinearLayout.LayoutParams {

        constructor(c: Context, attrs: AttributeSet) : super(c, attrs)

        constructor(w: Int, h: Int) : super(w, h)


        constructor(w: Int, h: Int, initWeight: Float) : super(w, h, initWeight)

        constructor(p: ViewGroup.LayoutParams) : super(p)

        constructor(source: ViewGroup.MarginLayoutParams) : super(source)

        /**
         *
         * Fixes the child's width to
         * [android.view.ViewGroup.LayoutParams.WRAP_CONTENT] and the child's
         * height to  [android.view.ViewGroup.LayoutParams.WRAP_CONTENT]
         * when not specified in the XML file.
         *
         * @param a          the styled attributes set
         * @param widthAttr  the width attribute to fetch
         * @param heightAttr the height attribute to fetch
         */
        override fun setBaseAttributes(a: TypedArray,
                                       widthAttr: Int, heightAttr: Int) {

            width = if (a.hasValue(widthAttr)) {
                a.getLayoutDimension(widthAttr, "layout_width")
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }

            height = if (a.hasValue(heightAttr)) {
                a.getLayoutDimension(heightAttr, "layout_height")
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private inner class CheckedStateTracker : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            val id = buttonView.id
            setCheckedId(id, isChecked)
        }
    }

    /**
     *
     * A pass-through listener acts upon the events and dispatches them
     * to another listener. This allows the table layout to set its own internal
     * hierarchy change listener without preventing the user to setup his.
     */
    private inner class PassThroughHierarchyChangeListener : ViewGroup.OnHierarchyChangeListener {
        internal var mOnHierarchyChangeListener: ViewGroup.OnHierarchyChangeListener? = null

        override fun onChildViewAdded(parent: View, child: View) {
            if (parent === this@CheckBoxGroup && child is CheckBox) {
                var id = child.getId()
                // generates an id if it's missing
                if (id == View.NO_ID) {
                    id = View.generateViewId()
                    child.setId(id)
                }
                child.setOnCheckedChangeListener(
                        mChildOnCheckedChangeListener)
            }

            mOnHierarchyChangeListener?.onChildViewAdded(parent, child)
        }

        override fun onChildViewRemoved(parent: View, child: View) {
            if (parent === this@CheckBoxGroup && child is CheckBox) {
                child.setOnCheckedChangeListener(null)
            }

            mOnHierarchyChangeListener?.onChildViewRemoved(parent, child)
        }
    }

    companion object {
        val DEFAULT_HEADER_ID: Int = 1234
    }
}
